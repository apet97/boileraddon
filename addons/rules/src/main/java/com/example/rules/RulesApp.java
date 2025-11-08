package com.example.rules;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.CorsFilter;
import com.clockify.addon.sdk.middleware.RateLimiter;
import com.clockify.addon.sdk.middleware.RequestLoggingFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.example.rules.store.RulesStore;
import com.example.rules.store.RulesStoreSPI;
import com.example.rules.store.DatabaseRulesStore;

/**
 * Rules Add-on for Clockify
 *
 * Provides declarative automation rules for time entries:
 * - Define conditions (AND/OR logic)
 * - Execute actions when conditions match
 * - Automatic tag management, description updates, and more
 *
 * How Clockify calls this addon:
 * 1. Manifest URL: Clockify fetches {baseUrl}/manifest.json to discover endpoints
 * 2. Lifecycle INSTALLED: POST to {baseUrl}/lifecycle/installed with workspace token
 * 3. Sidebar component: GET to {baseUrl}/settings renders iframe
 * 4. Webhooks: POST to {baseUrl}/webhook when time entry events occur
 * 5. API: CRUD operations on rules via {baseUrl}/api/rules
 *
 * To run locally:
 * 1. Build: mvn clean package
 * 2. Run: java -jar target/rules-0.1.0-jar-with-dependencies.jar
 * 3. Start ngrok: ngrok http 8080
 * 4. Update baseUrl env to: https://YOUR-SUBDOMAIN.ngrok-free.app/rules
 * 5. In Clockify Admin > Add-ons, install using manifest URL
 */
public class RulesApp {

    public static void main(String[] args) throws Exception {
        // Read and validate configuration from environment
        String baseUrl = ConfigValidator.validateUrl(
                System.getenv("ADDON_BASE_URL"),
                "http://localhost:8080/rules",
                "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
                System.getenv("ADDON_PORT"),
                8080,
                "ADDON_PORT"
        );
        String addonKey = "rules";

        // Build manifest programmatically (v1.3, no $schema in runtime)
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Rules")
                .description("Declarative automations for Clockify: if conditions then actions")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{
                        "TIME_ENTRY_READ",
                        "TIME_ENTRY_WRITE",
                        "TAG_READ",
                        "TAG_WRITE",
                        "PROJECT_READ"
                })
                .build();

        // Add sidebar component to manifest
        manifest.getComponents().add(
                new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Rules", "ADMINS")
        );

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Initialize stores
        RulesStoreSPI rulesStore = selectRulesStore();
        RulesController rulesController = new RulesController(rulesStore);

        // Register endpoints
        // GET /rules/manifest.json - Returns runtime manifest (NO $schema field)
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // GET /rules/settings - Sidebar iframe
        addon.registerCustomEndpoint("/settings", new SettingsController());

        // Rules CRUD API
        addon.registerCustomEndpoint("/api/rules", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return rulesController.listRules().handle(request);
            } else if ("POST".equals(method)) {
                return rulesController.saveRule().handle(request);
            } else if ("DELETE".equals(method)) {
                return rulesController.deleteRule().handle(request);
            } else {
                return HttpResponse.error(405, "{\"error\":\"Method not allowed\"}", "application/json");
            }
        });

        // POST /rules/api/test — dry-run evaluation (no side effects)
        addon.registerCustomEndpoint("/api/test", rulesController.testRules());

        // POST /rules/lifecycle/installed & /lifecycle/deleted - Lifecycle events
        LifecycleHandlers.register(addon, rulesStore);

        // POST /rules/webhook - Handle time entry events
        WebhookHandlers.register(addon, rulesStore);

        // Preload local secrets for development
        preloadLocalSecrets();

        // Health check
        addon.registerCustomEndpoint("/health", request ->
                HttpResponse.ok("Rules add-on is running"));

        // Extract context path from base URL
        String contextPath = sanitizeContextPath(baseUrl);

        // Start embedded Jetty server with middleware
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        // Always add basic security headers; configure frame-ancestors via ADDON_FRAME_ANCESTORS
        server.addFilter(new SecurityHeadersFilter());

        // Optional rate limiter via env: ADDON_RATE_LIMIT (double, requests/sec), ADDON_LIMIT_BY (ip|workspace)
        String rateLimit = System.getenv("ADDON_RATE_LIMIT");
        if (rateLimit != null && !rateLimit.isBlank()) {
            try {
                double permits = Double.parseDouble(rateLimit.trim());
                String limitBy = System.getenv().getOrDefault("ADDON_LIMIT_BY", "ip");
                server.addFilter(new RateLimiter(permits, limitBy));
                System.out.println("RateLimiter enabled: " + permits + "/sec by " + limitBy);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid ADDON_RATE_LIMIT value. Expected number, got: " + rateLimit);
            }
        }

        // Optional CORS allowlist via env: ADDON_CORS_ORIGINS (comma-separated origins)
        String cors = System.getenv("ADDON_CORS_ORIGINS");
        if (cors != null && !cors.isBlank()) {
            server.addFilter(new CorsFilter(cors));
            System.out.println("CORS enabled for origins: " + cors);
        }

        // Optional request logging (headers scrubbed): ADDON_REQUEST_LOGGING=true
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))
                || "1".equals(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "0"))) {
            server.addFilter(new RequestLoggingFilter());
            System.out.println("Request logging enabled (sensitive headers redacted)");
        }

        System.out.println("=".repeat(80));
        System.out.println("Rules Add-on Starting");
        System.out.println("=".repeat(80));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Port: " + port);
        System.out.println("Context Path: " + contextPath);
        System.out.println();
        System.out.println("Endpoints:");
        System.out.println("  Manifest:  " + baseUrl + "/manifest.json");
        System.out.println("  Settings:  " + baseUrl + "/settings");
        System.out.println("  Lifecycle: " + baseUrl + "/lifecycle/installed");
        System.out.println("             " + baseUrl + "/lifecycle/deleted");
        System.out.println("  Webhook:   " + baseUrl + "/webhook");
        System.out.println("  Health:    " + baseUrl + "/health");
        System.out.println("  Rules API: " + baseUrl + "/api/rules");
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

    private static RulesStoreSPI selectRulesStore() {
        // Prefer RULES_DB_URL if present; fallback to DB_URL; else in-memory
        String rulesDbUrl = System.getenv("RULES_DB_URL");
        String dbUrl = System.getenv("DB_URL");
        if ((rulesDbUrl != null && !rulesDbUrl.isBlank()) || (dbUrl != null && !dbUrl.isBlank())) {
            try {
                return DatabaseRulesStore.fromEnvironment();
            } catch (Exception e) {
                System.err.println("Failed to init DatabaseRulesStore: " + e.getMessage() + "; falling back to in-memory");
            }
        }
        return new RulesStore();
    }

    private static void preloadLocalSecrets() {
        String workspaceId = System.getenv("CLOCKIFY_WORKSPACE_ID");
        String installationToken = System.getenv("CLOCKIFY_INSTALLATION_TOKEN");
        if (workspaceId == null || workspaceId.isBlank() || installationToken == null || installationToken.isBlank()) {
            return;
        }

        String apiBaseUrl = System.getenv().getOrDefault("CLOCKIFY_API_BASE_URL", "https://api.clockify.me/api");
        try {
            com.clockify.addon.sdk.security.TokenStore.save(workspaceId, installationToken, apiBaseUrl);
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
            System.err.println("Warning: Could not parse base URL '" + baseUrl + "', using '/' as context path: "
                    + e.getMessage());
        }
        return contextPath;
    }
}
