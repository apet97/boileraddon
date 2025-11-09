package com.example.templateaddon;

import com.clockify.addon.sdk.*;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.config.SecretsPolicy;

public class TemplateAddonApp {
    public static void main(String[] args) throws Exception {
        SecretsPolicy.enforce();
        String baseUrl = ConfigValidator.validateUrl(System.getenv("ADDON_BASE_URL"),
                "http://localhost:8080/_template-addon", "ADDON_BASE_URL");
        int port = ConfigValidator.validatePort(System.getenv("ADDON_PORT"), 8080, "ADDON_PORT");

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("_template-addon")
                .name("Template Add-on")
                .description("Starter skeleton for a new add-on")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Serve runtime manifest and a simple health check
        addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));
        addon.registerCustomEndpoint("/health", r -> HttpResponse.ok("OK"));

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
        server.addFilter(new SecurityHeadersFilter());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.stop(); } catch (Exception ignored) {} }));
        server.start(port);
    }

    private static String sanitize(String base) {
        try { var u = new java.net.URI(base); var p = u.getPath(); if (p==null||p.isBlank()) return "/"; p=p.replaceAll("/+$","" ); return p.isEmpty()?"/":p; } catch (Exception e){ return "/"; }
    }
}
