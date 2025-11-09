package com.example.overtime;

import com.clockify.addon.sdk.*;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.middleware.CorsFilter;
import com.clockify.addon.sdk.middleware.RateLimiter;
import com.clockify.addon.sdk.middleware.RequestLoggingFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.config.SecretsPolicy;

public class OvertimeApp {
    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
        String baseUrl = ConfigValidator.validateUrl(System.getenv("ADDON_BASE_URL"),
                "http://localhost:8080/overtime", "ADDON_BASE_URL");
        int port = ConfigValidator.validatePort(System.getenv("ADDON_PORT"), 8080, "ADDON_PORT");

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("overtime")
                .name("Overtime Policy")
                .description("Detect and tag overtime based on daily/weekly thresholds.")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Controllers and stores
        SettingsStore settings = new SettingsStore();
        SettingsController settingsController = new SettingsController(settings);

        // Endpoints
        addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));
        // Health with optional DB probe (if DB env is set in a future persistent store variant)
        HealthCheck health = new HealthCheck("overtime", "0.1.0");
        addon.registerCustomEndpoint("/health", health);
        addon.registerCustomEndpoint("/settings", settingsController::handleHtml);
        addon.registerCustomEndpoint("/api/settings", settingsController::handleApi);
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());

        // Lifecycle and webhook handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon, settings);

        // Start server
        AddonServlet servlet = new AddonServlet(addon);
        String contextPath = sanitizeContextPath(baseUrl);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);
        server.addFilter(new SecurityHeadersFilter());

        String rateLimit = System.getenv("ADDON_RATE_LIMIT");
        if (rateLimit != null && !rateLimit.isBlank()) {
            double permits = Double.parseDouble(rateLimit.trim());
            String limitBy = System.getenv().getOrDefault("ADDON_LIMIT_BY", "ip");
            server.addFilter(new RateLimiter(permits, limitBy));
        }

        String cors = System.getenv("ADDON_CORS_ORIGINS");
        if (cors != null && !cors.isBlank()) server.addFilter(new CorsFilter(cors));
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))) {
            server.addFilter(new RequestLoggingFilter());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.stop(); } catch (Exception ignored) {} }));
        server.start(port);
    }

    static String sanitizeContextPath(String baseUrl) {
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) return "/";
            String s = path.replaceAll("/+$", "");
            return s.isEmpty() ? "/" : s;
        } catch (Exception e) {
            return "/";
        }
    }
}
