package com.clockify.addon.sdk;

import com.clockify.addon.sdk.util.PathSanitizer;

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
    public static final String DEFAULT_WEBHOOK_PATH = "/webhook";

    private final ClockifyManifest manifest;
    private final Map<String, RequestHandler> endpoints = new HashMap<>();
    private final Map<String, RequestHandler> lifecycleHandlers = new HashMap<>();
    private final Map<String, RequestHandler> lifecycleHandlersByPath = new HashMap<>();
    private final Map<String, String> lifecyclePathsByType = new HashMap<>();
    private final Map<String, Map<String, RequestHandler>> webhookHandlersByPath = new HashMap<>();
    private final Map<String, String> webhookPathsByEvent = new HashMap<>();

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
        String normalizedPath = PathSanitizer.sanitize(path);
        endpoints.put(normalizedPath, handler);
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
        registerWebhookHandler(event, DEFAULT_WEBHOOK_PATH, handler);
    }

    public void registerWebhookHandler(String event, String path, RequestHandler handler) {
        String normalizedPath = normalizeWebhookPath(path);

        Map<String, RequestHandler> handlersForPath = webhookHandlersByPath
                .computeIfAbsent(normalizedPath, key -> new HashMap<>());
        handlersForPath.put(event, handler);

        String previousPath = webhookPathsByEvent.put(event, normalizedPath);
        if (previousPath != null && !previousPath.equals(normalizedPath)) {
            Map<String, RequestHandler> previousHandlers = webhookHandlersByPath.get(previousPath);
            if (previousHandlers != null) {
                previousHandlers.remove(event);
                if (previousHandlers.isEmpty()) {
                    webhookHandlersByPath.remove(previousPath);
                }
            }
        }

        ClockifyManifest.WebhookEndpoint endpoint = manifest.getWebhooks().stream()
                .filter(w -> w.getEvent().equals(event))
                .findFirst()
                .orElse(null);

        if (endpoint == null) {
            manifest.getWebhooks().add(new ClockifyManifest.WebhookEndpoint(event, normalizedPath));
        } else {
            endpoint.setPath(normalizedPath);
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
        return webhookHandlersByPath.computeIfAbsent(DEFAULT_WEBHOOK_PATH, key -> new HashMap<>());
    }

    public Map<String, Map<String, RequestHandler>> getWebhookHandlersByPath() {
        return webhookHandlersByPath;
    }

    public Map<String, String> getWebhookPathsByEvent() {
        return webhookPathsByEvent;
    }

    private String normalizeLifecyclePath(String lifecycleType, String path) {
        return PathSanitizer.sanitizeLifecyclePath(lifecycleType, path);
    }

    private String normalizeWebhookPath(String path) {
        return PathSanitizer.sanitizeWebhookPath(path);
    }
}
