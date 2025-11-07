package com.example.auto_tag_assistant;

import addonsdk.shared.AddonServlet;
import addonsdk.shared.EmbeddedServer;
import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;

public class AddonApplication {
    private static String loadResourceFile(String path) {
        try {
            return new String(AddonApplication.class.getClassLoader()
                    .getResourceAsStream(path).readAllBytes());
        } catch (Exception e) {
            return "<!DOCTYPE html><html><body><h1>Settings Page Not Found</h1></body></html>";
        }
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault("ADDON_BASE_URL", "http://localhost:8080/auto-tag-assistant");
        String addonKey = System.getenv().getOrDefault("ADDON_KEY", "auto-tag-assistant");

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Auto-Tag Assistant")
                .baseUrl(baseUrl)
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Health endpoint
        addon.registerCustomEndpoint("/health", request ->
                addonsdk.shared.response.HttpResponse.ok("OK"));

        // Manifest endpoint
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // Settings UI endpoint
        addon.registerCustomEndpoint("/settings", request -> {
            String html = loadResourceFile("public/settings.html");
            return addonsdk.shared.response.HttpResponse.ok(html, "text/html");
        });

        // Lifecycle and webhook handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon);

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        server.start(port);
    }
}
