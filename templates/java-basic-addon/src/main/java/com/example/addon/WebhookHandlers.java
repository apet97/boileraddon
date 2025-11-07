package com.example.addon;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;

/**
 * Handles webhook events from Clockify.
 *
 * Configure which events to receive in manifest.json under webhooks.events.
 */
public class WebhookHandlers {
    public static void register(ClockifyAddon addon) {
        addon.onWebhook(request -> {
            String eventType = request.getWebhookEvent();
            String workspaceId = request.getResourceId();

            System.out.println("Received webhook: " + eventType + " for workspace: " + workspaceId);

            // TODO: Process the webhook event based on eventType
            // Payload available via: request.getPayload()

            return addonsdk.shared.response.HttpResponse.ok("Webhook processed");
        });
    }
}
