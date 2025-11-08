package com.clockify.addon.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
            logger.error("Error handling request: {} {}", method, path, e);
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Internal server error")
                    .put("details", e.getMessage())
                    .toString();
            sendResponse(resp, HttpResponse.error(500, errorBody, "application/json"));
        }
    }

    private HttpResponse handleRequest(HttpServletRequest req, String path) throws Exception {
        RequestHandler customHandler = addon.getEndpoints().get(path);
        if (customHandler != null) {
            return customHandler.handle(req);
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            if ("/webhook".equals(path)) {
                return handleWebhook(req);
            }

            HttpResponse lifecycleResponse = tryHandleLifecycle(req, path);
            if (lifecycleResponse != null) {
                return lifecycleResponse;
            }
        }

        return HttpResponse.error(404, "Endpoint not found: " + path);
    }

    private HttpResponse tryHandleLifecycle(HttpServletRequest req, String path) throws Exception {
        RequestHandler handlerByPath = addon.getLifecycleHandlersByPath().get(path);
        if (handlerByPath != null) {
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
            return handlerByPath.handle(req);
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

    private HttpResponse handleWebhook(HttpServletRequest req) throws Exception {
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
            return HttpResponse.error(400, "Missing webhook event type");
        }

        RequestHandler handler = addon.getWebhookHandlers().get(event);
        if (handler != null) {
            return handler.handle(req);
        }

        logger.warn("No handler registered for webhook event: {}", event);
        return HttpResponse.ok("Webhook event received but not handled: " + event);
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
