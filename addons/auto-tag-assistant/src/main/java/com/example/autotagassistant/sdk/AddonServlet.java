package com.example.autotagassistant.sdk;

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
 * Main servlet that routes requests to registered handlers.
 */
public class AddonServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AddonServlet.class);
    private final ClockifyAddon addon;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // Check custom endpoints first
        if (addon.getEndpoints().containsKey(path)) {
            return addon.getEndpoints().get(path).handle(req);
        }

        // Handle /lifecycle endpoint
        if ("/lifecycle".equals(path) && "POST".equals(req.getMethod())) {
            return handleLifecycle(req);
        }

        // Handle /webhook endpoint
        if ("/webhook".equals(path) && "POST".equals(req.getMethod())) {
            return handleWebhook(req);
        }

        // Not found
        return HttpResponse.error(404, "Endpoint not found: " + path);
    }

    private HttpResponse handleLifecycle(HttpServletRequest req) throws Exception {
        String body = req.getReader().lines().collect(Collectors.joining());
        JsonNode json = objectMapper.readTree(body);
        String lifecycleType = json.has("lifecycle") ? json.get("lifecycle").asText() : null;

        if (lifecycleType == null) {
            String errorBody = objectMapper.createObjectNode()
                    .put("message", "Missing 'lifecycle' field in request")
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
        String body = req.getReader().lines().collect(Collectors.joining());
        JsonNode json = objectMapper.readTree(body);
        String event = json.has("event") ? json.get("event").asText() : null;

        if (event == null) {
            return HttpResponse.error(400, "Missing 'event' field in request");
        }

        RequestHandler handler = addon.getWebhookHandlers().get(event);
        if (handler != null) {
            return handler.handle(req);
        }

        logger.warn("No handler registered for webhook event: {}", event);
        return HttpResponse.ok("Webhook event received but not handled: " + event);
    }

    private void sendResponse(HttpServletResponse resp, HttpResponse response) throws IOException {
        resp.setStatus(response.getStatusCode());
        resp.setContentType(response.getContentType());
        resp.getWriter().write(response.getBody());
    }
}
