package com.example.autotagassistant;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.security.DatabaseTokenStore;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.config.SecretsPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 4. Restart with: ADDON_BASE_URL=https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant
 * 5. In Clockify Admin > Add-ons, install using: https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant/manifest.json
 */
public class AutoTagAssistantApp {
    private static final Logger logger = LoggerFactory.getLogger(AutoTagAssistantApp.class);

    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
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

        // Configure persistent token storage if database credentials are provided
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv().getOrDefault("DB_USER", System.getenv("DB_USERNAME"));
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl != null && !dbUrl.isBlank() && dbUser != null && !dbUser.isBlank()) {
            try {
                com.clockify.addon.sdk.security.DatabaseTokenStore dbStore =
                        new com.clockify.addon.sdk.security.DatabaseTokenStore(dbUrl, dbUser, dbPassword);
                com.clockify.addon.sdk.security.TokenStore.configurePersistence(dbStore);
                System.out.println("✓ TokenStore configured with database persistence (PostgreSQL)");
            } catch (Exception e) {
                System.err.println("⚠ Failed to initialize database token store: " + e.getMessage());
                System.err.println("  Falling back to in-memory token storage (tokens will be lost on restart)");
            }
        } else {
            System.out.println("ℹ TokenStore using in-memory storage (set DB_URL, DB_USER, DB_PASSWORD for persistence)");
        }

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

        // Health check with optional DB connectivity probe
        HealthCheck health = new HealthCheck("auto-tag-assistant", "0.1.0");
        // Reuse DB credentials from token store configuration above
        if (dbUrl != null && !dbUrl.isBlank() && dbUser != null && !dbUser.isBlank()) {
            health.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
                @Override public String getName() { return "database"; }
                @Override public HealthCheck.HealthCheckResult check() {
                    try {
                        DatabaseTokenStore store = new DatabaseTokenStore(dbUrl, dbUser, dbPassword);
                        long n = store.count();
                        return new HealthCheck.HealthCheckResult("database", true, "Connected", n);
                    } catch (Exception e) {
                        return new HealthCheck.HealthCheckResult("database", false, e.getMessage());
                    }
                }
            });
        }
        addon.registerCustomEndpoint("/health", health);
        // Prometheus metrics (optional; text/plain scrape)
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());

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
                logger.info("RateLimiter enabled: {}/sec by {}", permits, limitBy);
            } catch (NumberFormatException nfe) {
                logger.error("Invalid ADDON_RATE_LIMIT value. Expected number, got: {}", rateLimit);
            }
        }

        // Optional CORS allowlist via env: ADDON_CORS_ORIGINS (comma-separated origins)
        String cors = System.getenv("ADDON_CORS_ORIGINS");
        if (cors != null && !cors.isBlank()) {
            server.addFilter(new com.clockify.addon.sdk.middleware.CorsFilter(cors));
            logger.info("CORS enabled for origins: {}", cors);
        }

        // Optional request logging (headers scrubbed): ADDON_REQUEST_LOGGING=true
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))
                || "1".equals(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "0"))) {
            server.addFilter(new com.clockify.addon.sdk.middleware.RequestLoggingFilter());
            logger.info("Request logging enabled (sensitive headers redacted)");
        }

        logger.info("Auto-Tag Assistant Add-on Starting");
        logger.info("Base URL: {}", baseUrl);
        logger.info("Port: {}", port);
        logger.info("Context Path: {}", contextPath);
        logger.info("Endpoints:");
        logger.info("  Manifest:  {}/manifest.json", baseUrl);
        logger.info("  Settings:  {}/settings", baseUrl);
        logger.info("  Lifecycle: {}/lifecycle/installed", baseUrl);
        logger.info("              {}/lifecycle/deleted", baseUrl);
        logger.info("  Webhook:   {}/webhook", baseUrl);
        logger.info("  Health:    {}/health", baseUrl);

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
            com.clockify.addon.sdk.security.TokenStore.save(workspaceId, installationToken, apiBaseUrl);
            logger.info("Preloaded installation token for workspace {}", workspaceId);
        } catch (Exception e) {
            logger.error("Failed to preload local installation token", e);
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
            logger.warn("Could not parse base URL '{}', using '/' as context path", baseUrl, e);
        }
        return contextPath;
    }
}
