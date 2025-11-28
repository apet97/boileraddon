package com.clockify.addon.rules;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.DefaultManifestController;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.config.SecretsPolicy;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.middleware.ScopedPlatformAuthFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.middleware.SensitiveHeaderFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.clockify.addon.sdk.security.TokenStore;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifierFactory;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RulesApp {
    private static final Logger logger = LoggerFactory.getLogger(RulesApp.class);

    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
        RulesConfiguration config = RulesConfiguration.fromEnvironment();
        String baseUrl = config.baseUrl();
        int port = config.port();

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key(config.addonKey())
                .name("Rules")
                .description("Automate Clockify time entries with if-this-then-that actions.")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{
                        "TIME_ENTRY_READ",
                        "TIME_ENTRY_WRITE",
                        "TAG_READ",
                        "TAG_WRITE"
                })
                .build();
        manifest.getComponents()
                .add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Rules", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        JwtVerifier jwtVerifier = initializeJwtVerifier(config);

        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));
        addon.registerCustomEndpoint("/settings", new SettingsController(config.environment()));
        addon.registerCustomEndpoint("/api/rules", new RulesController(config.environment()));
        addon.registerCustomEndpoint("/api/test", new TestController()::handle);
        HealthCheck health = new HealthCheck("rules", "0.1.0");
        addon.registerCustomEndpoint("/health", health);
        addon.registerCustomEndpoint("/ready", health);
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());
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
            boolean tokenPresent = TokenStore.get(workspaceId).isPresent();
            String json = String.format(
                    "{\"addonKey\":\"%s\",\"workspaceId\":\"%s\",\"tokenPresent\":%s,\"environment\":\"%s\",\"baseUrl\":\"%s\",\"applyChanges\":%s}",
                    manifest.getKey(),
                    workspaceId,
                    Boolean.toString(tokenPresent),
                    config.environment(),
                    baseUrl,
                    Boolean.toString(config.applyChanges())
            );
            return HttpResponse.ok(json, "application/json");
        });
        registerDevConfigEndpoint(addon, config);

        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon, config.applyChanges());

        AddonServlet servlet = new AddonServlet(addon);
        String contextPath = sanitize(baseUrl);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);
        if (jwtVerifier != null) {
            JwtVerifier finalJwtVerifier = jwtVerifier;
            server.addFilter(new WorkspaceContextFilter(jwt -> {
                try {
                    JwtVerifier.DecodedJwt decoded = finalJwtVerifier.verify(jwt);
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
            throw new IllegalStateException("CLOCKIFY_JWT_* env vars are required when ENV=" + config.environment());
        }
        server.addFilter(new SecurityHeadersFilter());
        server.addFilter(new SensitiveHeaderFilter());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.stop(); } catch (Exception ignored) {} }));
        server.start(port);
        logger.info("Rules add-on started on {} (port {})", baseUrl, port);
    }

    static void registerDevConfigEndpoint(ClockifyAddon addon, RulesConfiguration config) {
        if (!config.isDev()) {
            return;
        }
        addon.registerCustomEndpoint("/debug/config", new DevConfigController(config));
    }

    private static JwtVerifier initializeJwtVerifier(RulesConfiguration config) throws Exception {
        Optional<JwtBootstrapConfig> jwtConfigOpt = config.jwtBootstrap();
        if (jwtConfigOpt.isEmpty()) {
            if (config.isDev()) {
                return null;
            }
            throw new IllegalStateException("CLOCKIFY_JWT_* env vars are required when ENV=" + config.environment());
        }
        JwtBootstrapConfig jwtConfig = jwtConfigOpt.get();
        String expectedIssuer = jwtConfig.expectedIssuer().filter(value -> !value.isBlank()).orElse("clockify");
        String expectedAudience = jwtConfig.expectedAudience().filter(value -> !value.isBlank()).orElse(config.addonKey());
        JwtVerifier.Constraints constraints = new JwtVerifier.Constraints(
                expectedIssuer,
                expectedAudience,
                jwtConfig.leewaySeconds(),
                Set.of("RS256")
        );
        return JwtVerifierFactory.create(jwtConfig, constraints, config.addonKey());
    }

    private static String sanitize(String base) {
        try { var u = new java.net.URI(base); var p = u.getPath(); if (p==null||p.isBlank()) return "/"; p=p.replaceAll("/+$","" ); return p.isEmpty()?"/":p; } catch (Exception e){ return "/"; }
    }
}
