package com.example.addon;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;

public class LifecycleHandlers {
    public static void register(ClockifyAddon addon) {
        // Example lifecycle endpoints; adapt to your manifest lifecycle config
        addon.registerCustomEndpoint("/lifecycle", request -> {
            // Parse JSON body and handle INSTALLED/DELETED/SETTINGS_UPDATED as per docs
            return addonsdk.shared.response.HttpResponse.ok("lifecycle received");
        });
    }
}
