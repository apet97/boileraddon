package com.example.addon;

import com.example.addon.sdk.AddonServlet;
import com.example.addon.sdk.ClockifyAddon;
import com.example.addon.sdk.ClockifyManifest;
import com.example.addon.sdk.EmbeddedServer;
import com.example.addon.sdk.HttpResponse;

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
                HttpResponse.ok("OK"));

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
