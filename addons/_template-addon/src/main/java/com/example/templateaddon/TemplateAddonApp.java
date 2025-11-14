package com.example.templateaddon;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.config.SecretsPolicy;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.middleware.ScopedPlatformAuthFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.middleware.SensitiveHeaderFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifierFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TemplateAddonApp {
    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
        TemplateAddonConfiguration config = TemplateAddonConfiguration.fromEnvironment();
        String baseUrl = config.baseUrl();
        int port = config.port();

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key(config.addonKey())
                .name("Template Add-on")
                .description("Starter skeleton for a new add-on")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        JwtVerifier jwtVerifier = initializeJwtVerifier(config);

        // Serve runtime manifest and a simple health check
        addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));
        addon.registerCustomEndpoint("/health", r -> HttpResponse.ok("OK"));
        addon.registerCustomEndpoint("/status", request -> {
            String workspaceId = (String) request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
            if (workspaceId == null) {
                workspaceId = "";
            }
            boolean tokenPresent = !workspaceId.isBlank() && com.clockify.addon.sdk.security.TokenStore.get(workspaceId).isPresent();
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

        // Lifecycle handlers (persist/remove token)
        LifecycleHandlers.register(addon);

        // Webhook handlers (verify signature, no-op demo)
        WebhookHandlers.register(addon);

        // Dry-run endpoint pattern for fast iteration (no side effects)
        addon.registerCustomEndpoint("/api/test", new TestController()::handle);

        // Start embedded server
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
                    Set.of("/status"),
                    List.of("/api")
            ));
        } else if (!config.isDev()) {
            throw new IllegalStateException("CLOCKIFY_JWT_* env vars are required when ENV=" + config.environment());
        }
        server.addFilter(new SecurityHeadersFilter());
        server.addFilter(new SensitiveHeaderFilter());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.stop(); } catch (Exception ignored) {} }));
        server.start(port);
    }

    private static JwtVerifier initializeJwtVerifier(TemplateAddonConfiguration config) throws Exception {
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
