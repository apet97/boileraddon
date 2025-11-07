package com.example.addon;

import addonsdk.shared.AddonServlet;
import addonsdk.shared.EmbeddedServer;
import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;

public class AddonApplication {
    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("ADDON_BASE_URL", "http://localhost:8080");
        String addonKey = System.getenv().getOrDefault("ADDON_KEY", "example.addon");

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Java Basic Addon")
                .baseUrl(baseUrl)
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Health endpoint
        addon.registerCustomEndpoint("/health", request ->
                addonsdk.shared.response.HttpResponse.ok("OK"));

        // Manifest endpoint
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // Lifecycle and webhook handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon);

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        server.start(port);
    }
}
