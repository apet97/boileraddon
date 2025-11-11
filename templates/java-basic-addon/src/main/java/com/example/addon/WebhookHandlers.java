package com.example.addon;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

/**
 * Handles webhook events from Clockify.
 *
 * Configure which events to receive in manifest.json under webhooks.events.
 */
public class WebhookHandlers {
    private static final Logger logger = LoggerFactory.getLogger(WebhookHandlers.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String eventType = payload != null && payload.has("event")
                        ? payload.get("event").asText()
                        : "TIME_ENTRY_CREATED";
                String workspaceId = payload != null && payload.has("workspaceId")
                        ? payload.get("workspaceId").asText()
                        : "unknown";

                logger.info("Received webhook: {} for workspace: {}", eventType, workspaceId);

                // Process the webhook event based on eventType
                // Example: Extract time entry data and implement your business logic
                if (payload != null && payload.has("data")) {
                    JsonNode data = payload.get("data");
                    // Add your custom logic here
                }

                return HttpResponse.ok("Webhook processed");
            } catch (Exception e) {
                logger.error("Failed to process webhook payload", e);
                return HttpResponse.error(500, "Failed to process webhook event");
            }
        });
    }

    private static JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        Object cachedBody = request.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String bodyString) {
            if (bodyString.isBlank()) {
                return null;
            }
            JsonNode json = objectMapper.readTree(bodyString);
            request.setAttribute("clockify.jsonBody", json);
            return json;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        if (body.length() == 0) {
            return null;
        }

        JsonNode json = objectMapper.readTree(body.toString());
        request.setAttribute("clockify.jsonBody", json);
        request.setAttribute("clockify.rawBody", body.toString());
        return json;
    }
}
