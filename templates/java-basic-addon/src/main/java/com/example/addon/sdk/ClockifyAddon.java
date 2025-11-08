package com.example.addon.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Main Clockify Add-on coordinator.
 * Manages endpoint routing and manifest configuration.
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
     * Register a custom endpoint (e.g., /manifest.json, /settings, /health)
     */
    public void registerCustomEndpoint(String path, RequestHandler handler) {
        endpoints.put(path, handler);
    }

    /**
     * Register a lifecycle handler (INSTALLED, DELETED)
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
     * Register a webhook handler (TIME_ENTRY_CREATED, etc.)
     */
    public void registerWebhookHandler(String event, RequestHandler handler) {
        webhookHandlers.put(event, handler);

        // Auto-register in manifest if not already present
        if (manifest.getWebhooks().stream().noneMatch(w -> w.getEvent().equals(event))) {
            manifest.getWebhooks().add(new ClockifyManifest.WebhookEndpoint(event, "/webhook"));
        }
    }

    /**
     * Get all registered endpoints
     */
    public Map<String, RequestHandler> getEndpoints() {
        return endpoints;
    }

    /**
     * Get lifecycle handlers
     */
    public Map<String, RequestHandler> getLifecycleHandlers() {
        return lifecycleHandlers;
    }

    public Map<String, RequestHandler> getLifecycleHandlersByPath() {
        return lifecycleHandlersByPath;
    }

    /**
     * Get webhook handlers
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
