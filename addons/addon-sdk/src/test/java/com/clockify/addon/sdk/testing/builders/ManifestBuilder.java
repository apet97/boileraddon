package com.clockify.addon.sdk.testing.builders;

import com.clockify.addon.sdk.ClockifyManifest;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating test addon manifests.
 * Provides sensible defaults for testing addon configuration and registration.
 *
 * Usage:
 * <pre>
 * ClockifyManifest manifest = ManifestBuilder.create()
 *     .key("test-addon")
 *     .name("Test Addon")
 *     .baseUrl("http://localhost:8080/test")
 *     .withWebhook("TIME_ENTRY_CREATED", "/webhook")
 *     .build();
 * </pre>
 */
public class ManifestBuilder {
    private String key = "test-addon-" + System.currentTimeMillis();
    private String name = "Test Addon";
    private String description = "Test addon for integration testing";
    private String baseUrl = "http://localhost:8080/test-addon";
    private String minimalSubscriptionPlan = "FREE";
    private final List<String> scopes = new ArrayList<>();
    private final List<ClockifyManifest.WebhookEndpoint> webhooks = new ArrayList<>();
    private final List<String> customProperties = new ArrayList<>();

    /**
     * Create a new ManifestBuilder with defaults
     */
    public static ManifestBuilder create() {
        ManifestBuilder builder = new ManifestBuilder();
        builder.scopes.add("TIME_ENTRY_READ");
        return builder;
    }

    /**
     * Set the addon key
     */
    public ManifestBuilder key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Set the addon name
     */
    public ManifestBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the addon description
     */
    public ManifestBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the base URL
     */
    public ManifestBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Set the minimal subscription plan
     */
    public ManifestBuilder minimalSubscriptionPlan(String plan) {
        this.minimalSubscriptionPlan = plan;
        return this;
    }

    /**
     * Add a scope
     */
    public ManifestBuilder withScope(String scope) {
        this.scopes.add(scope);
        return this;
    }

    /**
     * Add multiple scopes
     */
    public ManifestBuilder withScopes(String... scopeArray) {
        for (String scope : scopeArray) {
            this.scopes.add(scope);
        }
        return this;
    }

    /**
     * Add a webhook endpoint
     */
    public ManifestBuilder withWebhook(String event, String path) {
        this.webhooks.add(new ClockifyManifest.WebhookEndpoint(event, path));
        return this;
    }

    /**
     * Build the manifest
     */
    public ClockifyManifest build() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(key)
                .name(name)
                .description(description)
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan(minimalSubscriptionPlan)
                .scopes(scopes.toArray(new String[0]))
                .build();

        // Add webhooks if configured
        for (ClockifyManifest.WebhookEndpoint webhook : webhooks) {
            manifest.getWebhooks().add(webhook);
        }

        return manifest;
    }
}
