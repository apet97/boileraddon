package com.example.autotagassistant.sdk;

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
        lifecycleHandlers.put(lifecycleType, handler);

        // Auto-register in manifest if not already present
        if (manifest.getLifecycle().stream().noneMatch(l -> l.getType().equals(lifecycleType))) {
            manifest.getLifecycle().add(new ClockifyManifest.LifecycleEndpoint(lifecycleType, "/lifecycle"));
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

    /**
     * Get webhook handlers
     */
    public Map<String, RequestHandler> getWebhookHandlers() {
        return webhookHandlers;
    }
}
