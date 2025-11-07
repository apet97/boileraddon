package com.example.addon;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;

public class WebhookHandlers {
    public static void register(ClockifyAddon addon) {
        addon.registerCustomEndpoint("/webhook", request -> {
            // Verify signature if provided and process event
            return addonsdk.shared.response.HttpResponse.ok("webhook received");
        });
    }
}
