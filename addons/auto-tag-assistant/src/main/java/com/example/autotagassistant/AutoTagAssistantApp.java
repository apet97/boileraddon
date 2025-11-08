package com.example.autotagassistant;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.ConfigValidator;

/**
 * Auto-Tag Assistant Add-on
 *
 * Detects missing tags on time entries and suggests appropriate tags based on project/task context.
 *
 * How Clockify calls this addon:
 * 1. Manifest URL: Clockify fetches {baseUrl}/manifest.json to discover endpoints
 * 2. Lifecycle INSTALLED: POST to {baseUrl}/lifecycle/installed with workspace token
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
        // Read and validate configuration from environment
        String baseUrl = ConfigValidator.validateUrl(
            System.getenv("ADDON_BASE_URL"),
            "http://localhost:8080/auto-tag-assistant",
            "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
            System.getenv("ADDON_PORT"),
            8080,
            "ADDON_PORT"
        );
        String addonKey = "auto-tag-assistant";

        // Build manifest programmatically (aligns with manifest.json)
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();

        // Add sidebar component to manifest
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Token store selection: the demo module uses its local TokenStore.
        // For production, replace with a persistent store as documented in docs/DATABASE_TOKEN_STORE.md.

        // Register endpoints
        // GET /auto-tag-assistant/manifest.json - Returns runtime manifest (NO $schema field)
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // GET /auto-tag-assistant/settings - Sidebar iframe for time entry
        addon.registerCustomEndpoint("/settings", new SettingsController());

        // POST /auto-tag-assistant/lifecycle/installed & /lifecycle/deleted - Lifecycle events
        LifecycleHandlers.register(addon);

        // POST /auto-tag-assistant/webhook - Handle time entry events
        WebhookHandlers.register(addon);

        preloadLocalSecrets();

        // Health check
        addon.registerCustomEndpoint("/health", request ->
                HttpResponse.ok("Auto-Tag Assistant is running"));

        // Extract context path from base URL
        // Example: http://localhost:8080/auto-tag-assistant -> /auto-tag-assistant
        String contextPath = sanitizeContextPath(baseUrl);

        // Start embedded Jetty server
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);
        // Always add basic security headers; configure frame-ancestors via ADDON_FRAME_ANCESTORS
        server.addFilter(new com.clockify.addon.sdk.middleware.SecurityHeadersFilter());

        // Optional rate limiter via env: ADDON_RATE_LIMIT (double, requests/sec), ADDON_LIMIT_BY (ip|workspace)
        String rateLimit = System.getenv("ADDON_RATE_LIMIT");
        if (rateLimit != null && !rateLimit.isBlank()) {
            try {
                double permits = Double.parseDouble(rateLimit.trim());
                String limitBy = System.getenv().getOrDefault("ADDON_LIMIT_BY", "ip");
                server.addFilter(new com.clockify.addon.sdk.middleware.RateLimiter(permits, limitBy));
                System.out.println("RateLimiter enabled: " + permits + "/sec by " + limitBy);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid ADDON_RATE_LIMIT value. Expected number, got: " + rateLimit);
            }
        }

        // Optional CORS allowlist via env: ADDON_CORS_ORIGINS (comma-separated origins)
        String cors = System.getenv("ADDON_CORS_ORIGINS");
        if (cors != null && !cors.isBlank()) {
            server.addFilter(new com.clockify.addon.sdk.middleware.CorsFilter(cors));
            System.out.println("CORS enabled for origins: " + cors);
        }

        // Optional request logging (headers scrubbed): ADDON_REQUEST_LOGGING=true
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))
                || "1".equals(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "0"))) {
            server.addFilter(new com.clockify.addon.sdk.middleware.RequestLoggingFilter());
            System.out.println("Request logging enabled (sensitive headers redacted)");
        }

        System.out.println("=".repeat(80));
        System.out.println("Auto-Tag Assistant Add-on Starting");
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

        // Add shutdown hook for graceful stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception ignored) {
            }
        }));

        server.start(port);
    }

    private static void preloadLocalSecrets() {
        String workspaceId = System.getenv("CLOCKIFY_WORKSPACE_ID");
        String installationToken = System.getenv("CLOCKIFY_INSTALLATION_TOKEN");
        if (workspaceId == null || workspaceId.isBlank() || installationToken == null || installationToken.isBlank()) {
            return;
        }

        String apiBaseUrl = System.getenv().getOrDefault("CLOCKIFY_API_BASE_URL", "https://api.clockify.me/api");
        try {
            TokenStore.save(workspaceId, installationToken, apiBaseUrl);
            System.out.println("Preloaded installation token for workspace " + workspaceId);
        } catch (Exception e) {
            System.err.println("Failed to preload local installation token: " + e.getMessage());
        }
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
