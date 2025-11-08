package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registers webhook handlers.
 *
 * TODO: Subscribe to the specific events you need and implement business logic.
 */
public final class WebhookHandlers {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private WebhookHandlers() {
    }

    public static void register(ClockifyAddon addon) {
        // Example subscription (TIME_ENTRY_CREATED). Remove if not needed.
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
            JsonNode payload = (JsonNode) request.getAttribute("clockify.jsonBody");
            if (payload == null) {
                String body = objectMapper.createObjectNode()
                        .put("status", "ignored")
                        .put("message", "No payload received")
                        .toString();
                return HttpResponse.ok(body, "application/json");
            }

            System.out.println("Webhook event received: TIME_ENTRY_CREATED");
            System.out.println(payload.toPrettyString());

            // TODO: Replace with real processing, like calling the Clockify API.

            String body = objectMapper.createObjectNode()
                    .put("status", "ok")
                    .put("message", "Webhook handled (TODO)")
                    .toString();
            return HttpResponse.ok(body, "application/json");
        });
    }
}
