package com.clockify.addon.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Central coordinator that keeps track of all handlers and manifest metadata
 * for a Clockify add-on.
 * <p>
 * Example lifecycle configuration:
 * </p>
 * <pre>{@code
 * ClockifyAddon addon = new ClockifyAddon(manifest);
 * addon.registerLifecycleHandler("INSTALLED", request -> HttpResponse.ok("installed"));
 * addon.registerLifecycleHandler("DELETED", request -> HttpResponse.ok("deleted"));
 * addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> HttpResponse.ok("ok"));
 * addon.registerCustomEndpoint("/settings", new SettingsController());
 * }</pre>
 */
public class ClockifyAddon {
    private final ClockifyManifest manifest;
    private final Map<String, RequestHandler> endpoints = new HashMap<>();
    private final Map<String, RequestHandler> lifecycleHandlers = new HashMap<>();
    private final Map<String, RequestHandler> lifecycleHandlersByPath = new HashMap<>();
    private final Map<String, String> lifecyclePathsByType = new HashMap<>();
    private final Map<String, RequestHandler> webhookHandlers = new HashMap<>();

    public ClockifyAddon(ClockifyManifest manifest) {
        this.manifest = manifest;
    }

    public ClockifyManifest getManifest() {
        return manifest;
    }

    /**
     * Register a custom endpoint (e.g., {@code /manifest.json}, {@code /settings}, {@code /health}).
     *
     * @param path     path relative to the add-on base URL
     * @param handler  handler that will process requests to {@code path}
     */
    public void registerCustomEndpoint(String path, RequestHandler handler) {
        endpoints.put(path, handler);
    }

    /**
     * Register a lifecycle handler (e.g., {@code INSTALLED}, {@code DELETED}).
     *
     * @param lifecycleType lifecycle event type reported by Clockify
     * @param handler       handler that should process the request
     */
    public void registerLifecycleHandler(String lifecycleType, RequestHandler handler) {
        registerLifecycleHandler(lifecycleType, null, handler);
    }

    public void registerLifecycleHandler(String lifecycleType, String path, RequestHandler handler) {
        String normalizedPath = normalizeLifecyclePath(lifecycleType, path);

        lifecycleHandlers.put(lifecycleType, handler);

        String previousPath = lifecyclePathsByType.put(lifecycleType, normalizedPath);
        if (previousPath != null && !previousPath.equals(normalizedPath)) {
            lifecycleHandlersByPath.remove(previousPath);
        }
        lifecycleHandlersByPath.put(normalizedPath, handler);

        ClockifyManifest.LifecycleEndpoint endpoint = manifest.getLifecycle().stream()
                .filter(l -> l.getType().equals(lifecycleType))
                .findFirst()
                .orElse(null);

        if (endpoint == null) {
            manifest.getLifecycle().add(new ClockifyManifest.LifecycleEndpoint(lifecycleType, normalizedPath));
        } else {
            endpoint.setPath(normalizedPath);
        }
    }

    /**
     * Register a webhook handler (e.g., {@code TIME_ENTRY_CREATED}).
     * The handler is auto-registered in the manifest if missing.
     *
     * @param event   webhook event identifier
     * @param handler handler that processes the event
     */
    public void registerWebhookHandler(String event, RequestHandler handler) {
        webhookHandlers.put(event, handler);

        // Auto-register in manifest if not already present
        if (manifest.getWebhooks().stream().noneMatch(w -> w.getEvent().equals(event))) {
            manifest.getWebhooks().add(new ClockifyManifest.WebhookEndpoint(event, "/webhook"));
        }
    }

    /**
     * Get all registered endpoints.
     *
     * @return map of endpoint paths to handler implementations
     */
    public Map<String, RequestHandler> getEndpoints() {
        return endpoints;
    }

    /**
     * Get lifecycle handlers keyed by lifecycle type.
     *
     * @return map of lifecycle event type to handler implementation
     */
    public Map<String, RequestHandler> getLifecycleHandlers() {
        return lifecycleHandlers;
    }

    public Map<String, RequestHandler> getLifecycleHandlersByPath() {
        return lifecycleHandlersByPath;
    }

    /**
     * Get webhook handlers keyed by webhook event.
     *
     * @return map of webhook event to handler implementation
     */
    public Map<String, RequestHandler> getWebhookHandlers() {
        return webhookHandlers;
    }

    private String normalizeLifecyclePath(String lifecycleType, String path) {
        String normalizedPath = path != null ? path.trim() : "";
        if (normalizedPath.isEmpty()) {
            normalizedPath = "/lifecycle/" + lifecycleType.toLowerCase();
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedPath;
    }
}
