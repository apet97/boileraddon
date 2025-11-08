package com.example.templateaddon;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.ConfigValidator;

/**
 * Starter application for building a new Clockify add-on.
 */
public class TemplateAddonApp {
    public static void main(String[] args) throws Exception {
        // Read and validate configuration from environment
        String baseUrl = ConfigValidator.validateUrl(
            System.getenv("ADDON_BASE_URL"),
            "http://localhost:8080/_template-addon",
            "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
            System.getenv("ADDON_PORT"),
            8080,
            "ADDON_PORT"
        );
        String addonKey = "_template-addon";

        // TODO: Rename "Template Add-on" and customize description before publishing.
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Template Add-on")
                .description("Describe what your add-on does for Clockify users")
                .baseUrl(baseUrl)
                // TODO: Change the plan and scopes so they match your add-on needs.
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"WORKSPACE_READ"})
                .build();

        // TODO: Add or remove components as needed. For example, add a sidebar component.
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Template Add-on", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // GET /_template-addon/manifest.json - Returns runtime manifest (NO $schema field)
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // GET /_template-addon/settings - Render iframe content for Clockify UI components
        addon.registerCustomEndpoint("/settings", new SettingsController());

        // POST /_template-addon/lifecycle/* - Handle install/uninstall events
        LifecycleHandlers.register(addon);

        // POST /_template-addon/webhook - Handle subscribed webhooks
        WebhookHandlers.register(addon);

        // GET /_template-addon/health - Simple readiness probe
        addon.registerCustomEndpoint("/health", request ->
                HttpResponse.ok("Template add-on is running"));

        String contextPath = sanitizeContextPath(baseUrl);

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        System.out.println("=".repeat(80));
        System.out.println("Template Add-on starting");
        System.out.println("=".repeat(80));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Port: " + port);
        System.out.println("Context Path: " + contextPath);
        System.out.println();
        System.out.println("Endpoints:");
        System.out.println("  Manifest:  " + baseUrl + "/manifest.json");
        System.out.println("  Settings:  " + baseUrl + "/settings");
        System.out.println("  Lifecycle: " + baseUrl + "/lifecycle/installed");
        System.out.println("              " + baseUrl + "/lifecycle/deleted");
        System.out.println("  Webhook:   " + baseUrl + "/webhook");
        System.out.println("  Health:    " + baseUrl + "/health");
        System.out.println("=".repeat(80));

        server.start(port);
    }

    static String sanitizeContextPath(String baseUrl) {
        String contextPath = "/";
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String sanitized = path.replaceAll("/+$", "");
                if (!sanitized.isEmpty()) {
                    contextPath = sanitized;
                }
            }
        } catch (java.net.URISyntaxException e) {
            System.err.println("Warning: Could not parse base URL '" + baseUrl + "', using '/' as context path: " + e.getMessage());
        }
        return contextPath;
    }
}
