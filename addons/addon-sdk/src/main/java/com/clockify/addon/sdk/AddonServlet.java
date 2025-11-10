package com.clockify.addon.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.error.ErrorHandler;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Primary servlet entry point that routes HTTP requests to {@link RequestHandler}
 * implementations registered on a {@link ClockifyAddon} instance.
 * <p>
 * Typical usage embeds the servlet in a Jetty server via {@link EmbeddedServer}:
 * </p>
 * <pre>{@code
 * ClockifyAddon addon = new ClockifyAddon(manifest);
 * ObjectMapper mapper = new ObjectMapper();
 * addon.registerCustomEndpoint("/manifest.json", request ->
 *     HttpResponse.ok(mapper.writeValueAsString(manifest), "application/json"));
 * addon.registerWebhookHandler("TIME_ENTRY_UPDATED", request -> HttpResponse.ok("handled"));
 *
 * AddonServlet servlet = new AddonServlet(addon);
 * new EmbeddedServer(servlet, "/my-addon").start(8080);
 * }</pre>
 * The servlet automatically wires lifecycle and webhook handlers that were
 * previously registered on the {@link ClockifyAddon}.
 */
public class AddonServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AddonServlet.class);
    private final ClockifyAddon addon;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new servlet wrapper around the provided add-on instance.
     *
     * @param addon configured add-on containing registered handlers
     */
    public AddonServlet(ClockifyAddon addon) {
        this.addon = addon;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() != null ? req.getPathInfo() : "/";
        String method = req.getMethod();

        logger.info("{} {}", method, path);

        try {
            HttpResponse response = handleRequest(req, path);
            sendResponse(resp, response);
        } catch (Exception e) {
            // Use centralized error handler for safe error responses
            ErrorHandler.ErrorResponse errorResponse = ErrorHandler.unknownError(e,
                String.format("%s %s", method, path));
            HttpResponse response = HttpResponse.error(
                errorResponse.getStatusCode(),
                errorResponse.getBody(),
                errorResponse.getContentType()
            );
            sendResponse(resp, response);
        }
    }

    private HttpResponse handleRequest(HttpServletRequest req, String path) throws Exception {
        RequestHandler customHandler = addon.getEndpoints().get(path);
        if (customHandler != null) {
            return customHandler.handle(req);
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            HttpResponse webhookResponse = tryHandleWebhook(req, path);
            if (webhookResponse != null) {
                return webhookResponse;
            }

            HttpResponse lifecycleResponse = tryHandleLifecycle(req, path);
            if (lifecycleResponse != null) {
                return lifecycleResponse;
            }
        }

        return HttpResponse.error(404, "Endpoint not found: " + path);
    }

    private HttpResponse tryHandleWebhook(HttpServletRequest req, String path) throws Exception {
        // Never treat lifecycle endpoints as webhooks
        if (path != null && path.startsWith("/lifecycle")) {
            return null;
        }
        Map<String, RequestHandler> handlersForPath = addon.getWebhookHandlersByPath().get(path);
        boolean usingDefaultPath = ClockifyAddon.DEFAULT_WEBHOOK_PATH.equals(path);

        if (handlersForPath == null && !usingDefaultPath) {
            handlersForPath = addon.getWebhookHandlersByPath().get(ClockifyAddon.DEFAULT_WEBHOOK_PATH);
            usingDefaultPath = true;
        }

        if (handlersForPath == null && usingDefaultPath) {
            handlersForPath = addon.getWebhookHandlers();
        }

        if (handlersForPath == null) {
            return null;
        }

        if (handlersForPath.isEmpty() && !usingDefaultPath) {
            return null;
        }

        return handleWebhook(req, handlersForPath);
    }

    private HttpResponse tryHandleLifecycle(HttpServletRequest req, String path) throws Exception {
        RequestHandler handlerByPath = addon.getLifecycleHandlersByPath().get(path);
        if (handlerByPath != null) {
            // Defer JSON parsing to the concrete handler for explicit lifecycle paths.
            // This avoids double-reading the request stream and allows custom payload handling.
            return handlerByPath.handle(req);
        }

        // Fallback: support direct POST to /lifecycle/{type} even if path map is not populated.
        if (path != null && path.startsWith("/lifecycle/") && path.length() > "/lifecycle/".length()) {
            String type = path.substring("/lifecycle/".length());
            if (!type.isBlank()) {
                String key = type.toUpperCase();
                RequestHandler byType = addon.getLifecycleHandlers().get(key);
                if (byType != null) {
                    return byType.handle(req);
                }
            }
        }

        if (!"/lifecycle".equals(path)) {
            return null;
        }

        JsonNode json;
        try {
            json = readAndCacheJsonBody(req);
        } catch (IOException e) {
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Invalid JSON payload")
                    .put("details", e.getMessage())
                    .toString();
            return HttpResponse.error(400, errorBody, "application/json");
        }

        if (json == null) {
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Lifecycle payload is required")
                    .toString();
            return HttpResponse.error(400, errorBody, "application/json");
        }

        String lifecycleType = extractLifecycleType(json);

        if (lifecycleType == null || lifecycleType.isBlank()) {
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Missing lifecycle identifier in request")
                    .toString();
            return HttpResponse.error(400, errorBody, "application/json");
        }

        RequestHandler handler = addon.getLifecycleHandlers().get(lifecycleType);
        if (handler != null) {
            return handler.handle(req);
        }

        logger.warn("No handler registered for lifecycle: {}", lifecycleType);
        String responseBody = objectMapper.createObjectNode()
                .put("status", "ignored")
                .put("message", "Lifecycle event received but not handled: " + lifecycleType)
                .toString();
        return HttpResponse.ok(responseBody, "application/json");
    }

    private HttpResponse handleWebhook(HttpServletRequest req, Map<String, RequestHandler> handlers) throws Exception {
        JsonNode json;
        try {
            json = readAndCacheJsonBody(req);
        } catch (IOException e) {
            // metrics: invalid payload
            Counter.builder("webhook_errors_total")
                    .tag("reason", "invalid_json")
                    .register(MetricsHandler.registry())
                    .increment();
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Invalid JSON payload")
                    .put("details", e.getMessage())
                    .toString();
            return HttpResponse.error(400, errorBody, "application/json");
        }

        String event = null;
        String headerEventType = req.getHeader("clockify-webhook-event-type");
        if (headerEventType != null) {
            headerEventType = headerEventType.trim();
            if (!headerEventType.isEmpty()) {
                event = headerEventType;
            }
        }

        if (event == null) {
            if (json == null) {
                Counter.builder("webhook_errors_total")
                        .tag("reason", "missing_body")
                        .register(MetricsHandler.registry())
                        .increment();
                return HttpResponse.error(400, "Missing request body");
            }
            if (json.has("event")) {
                event = json.get("event").asText(null);
                if (event != null) {
                    event = event.trim();
                    if (event.isEmpty()) {
                        event = null;
                    }
                }
            }
        }

        if (event == null) {
            Counter.builder("webhook_errors_total")
                    .tag("reason", "missing_event")
                    .register(MetricsHandler.registry())
                    .increment();
            return HttpResponse.error(400, "Missing webhook event type");
        }

        // SECURITY: Validate event type against manifest and sanitize for metrics
        String validationError = validateWebhookEventType(event);
        if (validationError != null) {
            String sanitizedEvent = sanitizeForLogging(event);
            logger.warn("Invalid webhook event type: {}", sanitizedEvent);
            Counter.builder("webhook_errors_total")
                    .tag("reason", "invalid_event_type")
                    .register(MetricsHandler.registry())
                    .increment();
            return HttpResponse.error(400, validationError, "application/json");
        }

        RequestHandler handler = handlers.get(event);
        if (handler != null) {
            // metrics: count + duration per event/path
            String path = req.getPathInfo() != null ? req.getPathInfo() : "/";
            String sanitizedEvent = sanitizeForLogging(event);
            Timer.Sample sample = Timer.start(MetricsHandler.registry());
            Counter.builder("webhook_requests_total")
                    .tag("event", sanitizedEvent)
                    .tag("path", path)
                    .register(MetricsHandler.registry())
                    .increment();
            HttpResponse response;
            try {
                response = handler.handle(req);
            } finally {
                Timer timer = Timer.builder("webhook_request_seconds")
                        .tag("event", sanitizedEvent)
                        .tag("path", path)
                        .register(MetricsHandler.registry());
                sample.stop(timer);
            }
            return response;
        }

        String sanitizedEvent = sanitizeForLogging(event);
        logger.warn("No handler registered for webhook event: {}", sanitizedEvent);
        Counter.builder("webhook_not_handled_total")
                .tag("event", sanitizedEvent)
                .register(MetricsHandler.registry())
                .increment();
        return HttpResponse.ok("Webhook event received but not handled: " + event);
    }

    /**
     * SECURITY: Validates webhook event type against registered handlers in manifest.
     * Prevents log injection and unexpected behavior from malicious event types.
     * Only whitelisted events from the manifest are accepted.
     *
     * @param event the event type to validate
     * @return null if valid, or error message JSON string if invalid
     */
    private String validateWebhookEventType(String event) {
        if (event == null || event.isBlank()) {
            return objectMapper.createObjectNode()
                    .put("message", "Event type cannot be null or empty")
                    .toString();
        }

        // Check length to prevent DoS/memory exhaustion
        if (event.length() > 255) {
            return objectMapper.createObjectNode()
                    .put("message", "Event type exceeds maximum length (255 characters)")
                    .toString();
        }

        // Validate event type contains only alphanumeric, underscore, and hyphen
        if (!event.matches("^[A-Za-z0-9_-]+$")) {
            return objectMapper.createObjectNode()
                    .put("message", "Event type contains invalid characters")
                    .toString();
        }

        // Whitelist: event must be registered in manifest webhooks
        boolean isValidEvent = addon.getManifest().getWebhooks().stream()
                .anyMatch(w -> w.getEvent().equals(event));

        if (!isValidEvent) {
            return objectMapper.createObjectNode()
                    .put("message", "Event type not registered in addon manifest")
                    .toString();
        }

        return null; // valid
    }

    /**
     * SECURITY: Sanitizes event type for safe logging/metrics.
     * Truncates and escapes to prevent log injection attacks.
     *
     * @param event the event type to sanitize
     * @return sanitized version safe for logging
     */
    private String sanitizeForLogging(String event) {
        if (event == null) {
            return "(null)";
        }
        // Truncate to prevent log flood
        String truncated = event.length() > 64 ? event.substring(0, 64) + "..." : event;
        // Replace newlines and control characters that could enable log injection
        return truncated.replaceAll("[\\r\\n\\t\\x00-\\x1F]", "?");
    }

    private JsonNode readAndCacheJsonBody(HttpServletRequest req) throws IOException {
        Object cachedJson = req.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode) {
            return (JsonNode) cachedJson;
        }

        Object cachedBody = req.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String) {
            String bodyString = (String) cachedBody;
            if (bodyString.isBlank()) {
                return null;
            }
            JsonNode jsonNode = objectMapper.readTree(bodyString);
            req.setAttribute("clockify.jsonBody", jsonNode);
            return jsonNode;
        }

        String body = req.getReader().lines().collect(Collectors.joining());
        req.setAttribute("clockify.rawBody", body);

        if (body.isBlank()) {
            return null;
        }

        JsonNode json = objectMapper.readTree(body);
        req.setAttribute("clockify.jsonBody", json);
        return json;
    }

    private String extractLifecycleType(JsonNode json) {
        if (json == null) {
            return null;
        }

        if (json.hasNonNull("lifecycle")) {
            return json.get("lifecycle").asText();
        }

        if (json.hasNonNull("type")) {
            return json.get("type").asText();
        }

        return null;
    }

    private void sendResponse(HttpServletResponse resp, HttpResponse response) throws IOException {
        resp.setStatus(response.getStatusCode());
        resp.setContentType(response.getContentType());
        resp.getWriter().write(response.getBody());
    }
}
