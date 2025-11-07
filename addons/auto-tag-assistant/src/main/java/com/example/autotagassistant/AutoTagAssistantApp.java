package com.example.autotagassistant;

import com.example.autotagassistant.sdk.AddonServlet;
import com.example.autotagassistant.sdk.EmbeddedServer;
import com.example.autotagassistant.sdk.ClockifyAddon;
import com.example.autotagassistant.sdk.ClockifyManifest;

/**
 * Auto-Tag Assistant Add-on
 *
 * Detects missing tags on time entries and suggests appropriate tags based on project/task context.
 *
 * How Clockify calls this addon:
 * 1. Manifest URL: Clockify fetches {baseUrl}/manifest.json to discover endpoints
 * 2. Lifecycle INSTALLED: POST to {baseUrl}/lifecycle with workspace token
 * 3. Sidebar component: GET to {baseUrl}/settings renders iframe in time entry sidebar
 * 4. Webhooks: POST to {baseUrl}/webhook when time entry events occur
 *
 * To run locally:
 * 1. Build: mvn clean package
 * 2. Run: java -jar target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
 * 3. Start ngrok: ngrok http 8080
 * 4. Update manifest.json baseUrl to: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant
 * 5. In Clockify Admin > Add-ons, install using: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant/manifest.json
 */
public class AutoTagAssistantApp {
    public static void main(String[] args) throws Exception {
        // Read configuration from environment
        String baseUrl = System.getenv().getOrDefault("ADDON_BASE_URL", "http://localhost:8080/auto-tag-assistant");
        int port = Integer.parseInt(System.getenv().getOrDefault("ADDON_PORT", "8080"));
        String addonKey = "auto-tag-assistant";

        // Build manifest programmatically (aligns with manifest.json)
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Register endpoints
        // GET /auto-tag-assistant/manifest.json - Returns runtime manifest (NO $schema field)
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // GET /auto-tag-assistant/settings - Sidebar iframe for time entry
        addon.registerCustomEndpoint("/settings", new SettingsController());

        // POST /auto-tag-assistant/lifecycle - Handle INSTALLED and DELETED events
        LifecycleHandlers.register(addon);

        // POST /auto-tag-assistant/webhook - Handle time entry events
        WebhookHandlers.register(addon);

        // Health check
        addon.registerCustomEndpoint("/health", request ->
                com.example.autotagassistant.sdk.HttpResponse.ok("Auto-Tag Assistant is running"));

        // Start embedded Jetty server
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet);

        System.out.println("=".repeat(80));
        System.out.println("Auto-Tag Assistant Add-on Starting");
        System.out.println("=".repeat(80));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Port: " + port);
        System.out.println();
        System.out.println("Endpoints:");
        System.out.println("  Manifest:  " + baseUrl + "/manifest.json");
        System.out.println("  Settings:  " + baseUrl + "/settings");
        System.out.println("  Lifecycle: " + baseUrl + "/lifecycle");
        System.out.println("  Webhook:   " + baseUrl + "/webhook");
        System.out.println("  Health:    " + baseUrl + "/health");
        System.out.println("=".repeat(80));

        server.start(port);
    }
}
