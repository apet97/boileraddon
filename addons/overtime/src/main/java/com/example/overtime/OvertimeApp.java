package com.example.overtime;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.DefaultManifestController;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.middleware.CorsFilter;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.middleware.RateLimiter;
import com.clockify.addon.sdk.middleware.RequestLoggingFilter;
import com.clockify.addon.sdk.middleware.ScopedPlatformAuthFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.middleware.SensitiveHeaderFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.clockify.addon.sdk.config.SecretsPolicy;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifierFactory;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvertimeApp {
    private static final Logger logger = LoggerFactory.getLogger(OvertimeApp.class);
    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
        OvertimeConfiguration config = OvertimeConfiguration.fromEnvironment();
        String baseUrl = config.baseUrl();
        int port = config.port();

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
        JwtVerifier jwtVerifier = initializeJwtVerifier(config);

        // Controllers and stores
        SettingsStore settings = new SettingsStore();
        SettingsController settingsController = new SettingsController(settings, jwtVerifier, config.isDev());

        // Endpoints
        addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));
        // Health with optional DB probe (if DB env is set in a future persistent store variant)
        HealthCheck health = new HealthCheck("overtime", "0.1.0");
        addon.registerCustomEndpoint("/health", health);
        addon.registerCustomEndpoint("/status", request -> {
            String workspaceId = (String) request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID);
            if (workspaceId == null || workspaceId.isBlank()) {
                Object attr = request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
                if (attr instanceof String attrValue && !attrValue.isBlank()) {
                    workspaceId = attrValue;
                }
            }
            if (workspaceId == null || workspaceId.isBlank()) {
                return HttpResponse.error(403, "{\"error\":\"workspace context required\"}", "application/json");
            }
            boolean tokenPresent = workspaceId != null && !workspaceId.isBlank()
                    && com.clockify.addon.sdk.security.TokenStore.get(workspaceId).isPresent();
            String json = String.format(
                    "{\"addonKey\":\"%s\",\"workspaceId\":\"%s\",\"tokenPresent\":%s,\"environment\":\"%s\",\"baseUrl\":\"%s\"}",
                    config.addonKey(),
                    workspaceId,
                    Boolean.toString(tokenPresent),
                    config.environment(),
                    baseUrl
            );
            return HttpResponse.ok(json, "application/json");
        });
        addon.registerCustomEndpoint("/settings", settingsController::handleHtml);
        addon.registerCustomEndpoint("/api/settings", settingsController::handleApi);
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());
        registerDevConfigEndpoint(addon, config);

        // Lifecycle and webhook handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon, settings);

        // Start server
        AddonServlet servlet = new AddonServlet(addon);
        String contextPath = sanitizeContextPath(baseUrl);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);
        if (jwtVerifier != null) {
            server.addFilter(new WorkspaceContextFilter(jwt -> {
                try {
                    JwtVerifier.DecodedJwt decoded = jwtVerifier.verify(jwt);
                    return decoded.payload();
                } catch (Exception e) {
                    return null;
                }
            }));
            server.addFilter(new ScopedPlatformAuthFilter(
                    new PlatformAuthFilter(jwtVerifier),
                    Set.of("/status", "/metrics"),
                    List.of("/api")
            ));
        } else if (!config.isDev()) {
            throw new IllegalStateException("JWT verifier must be configured when ENV=" + config.environment());
        } else {
            System.getLogger(OvertimeApp.class.getName()).log(System.Logger.Level.WARNING,
                    "PlatformAuthFilter disabled (ENV=dev and no CLOCKIFY_JWT_* configuration).");
        }
        server.addFilter(new SecurityHeadersFilter());
        server.addFilter(new SensitiveHeaderFilter());

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

    static void registerDevConfigEndpoint(ClockifyAddon addon, OvertimeConfiguration config) {
        if (!config.isDev()) {
            logger.info("Dev config endpoint disabled (ENV={})", config.environment());
            return;
        }
        addon.registerCustomEndpoint("/debug/config", new DevConfigController(config));
        logger.info("Dev config endpoint registered at /debug/config");
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

    private static JwtVerifier initializeJwtVerifier(OvertimeConfiguration config) throws Exception {
        var jwtConfigOpt = config.jwtBootstrap();
        if (jwtConfigOpt.isEmpty()) {
            if (config.isDev()) {
                return null;
            }
            throw new IllegalStateException("CLOCKIFY_JWT_* env vars are required when ENV=" + config.environment());
        }
        JwtBootstrapConfig jwtConfig = jwtConfigOpt.get();
        String expectedIssuer = jwtConfig.expectedIssuer()
                .filter(value -> !value.isBlank())
                .orElse("clockify");
        String expectedAudience = jwtConfig.expectedAudience()
                .filter(value -> !value.isBlank())
                .orElse(config.addonKey());
        JwtVerifier.Constraints constraints = new JwtVerifier.Constraints(
                expectedIssuer,
                expectedAudience,
                jwtConfig.leewaySeconds(),
                Set.of("RS256")
        );
        return JwtVerifierFactory.create(jwtConfig, constraints, config.addonKey());
    }
}
