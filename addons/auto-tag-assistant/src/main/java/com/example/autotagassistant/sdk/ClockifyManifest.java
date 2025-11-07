package com.example.autotagassistant.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Clockify Add-on Manifest Model (v1.3)
 *
 * This is a simplified manifest builder that doesn't require annotation processing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClockifyManifest {
    @JsonProperty("schemaVersion")
    private String schemaVersion = "1.3";

    private String key;
    private String name;
    private String description;

    private String baseUrl;

    private String minimalSubscriptionPlan;

    private String[] scopes;
    private List<LifecycleEndpoint> lifecycle;
    private List<WebhookEndpoint> webhooks;
    private List<ComponentEndpoint> components;
    private Object settings;

    // Getters
    public String getSchemaVersion() { return schemaVersion; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBaseUrl() { return baseUrl; }
    public String getMinimalSubscriptionPlan() { return minimalSubscriptionPlan; }
    public String[] getScopes() { return scopes; }
    public List<LifecycleEndpoint> getLifecycle() { return lifecycle; }
    public List<WebhookEndpoint> getWebhooks() { return webhooks; }
    public List<ComponentEndpoint> getComponents() { return components; }
    public Object getSettings() { return settings; }

    // Setters (for Jackson)
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public void setKey(String key) { this.key = key; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setMinimalSubscriptionPlan(String minimalSubscriptionPlan) { this.minimalSubscriptionPlan = minimalSubscriptionPlan; }
    public void setScopes(String[] scopes) { this.scopes = scopes; }
    public void setLifecycle(List<LifecycleEndpoint> lifecycle) { this.lifecycle = lifecycle; }
    public void setWebhooks(List<WebhookEndpoint> webhooks) { this.webhooks = webhooks; }
    public void setComponents(List<ComponentEndpoint> components) { this.components = components; }
    public void setSettings(Object settings) { this.settings = settings; }

    // Builder
    public static Builder v1_3Builder() {
        return new Builder();
    }

    public static class Builder {
        private final ClockifyManifest manifest = new ClockifyManifest();

        public Builder key(String key) {
            manifest.key = key;
            return this;
        }

        public Builder name(String name) {
            manifest.name = name;
            return this;
        }

        public Builder description(String description) {
            manifest.description = description;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            manifest.baseUrl = baseUrl;
            return this;
        }

        public Builder minimalSubscriptionPlan(String plan) {
            manifest.minimalSubscriptionPlan = plan;
            return this;
        }

        public Builder scopes(String[] scopes) {
            manifest.scopes = scopes;
            return this;
        }

        public ClockifyManifest build() {
            // Initialize default lifecycle endpoints
            if (manifest.lifecycle == null) {
                manifest.lifecycle = new ArrayList<>();
            }

            // Initialize default webhooks
            if (manifest.webhooks == null) {
                manifest.webhooks = new ArrayList<>();
            }

            // Initialize default components
            if (manifest.components == null) {
                manifest.components = new ArrayList<>();
            }

            return manifest;
        }
    }

    // Nested classes for manifest structure
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LifecycleEndpoint {
        private String type;
        private String path;

        public LifecycleEndpoint() {}

        public LifecycleEndpoint(String type, String path) {
            this.type = type;
            this.path = path;
        }

        public String getType() { return type; }
        public String getPath() { return path; }
        public void setType(String type) { this.type = type; }
        public void setPath(String path) { this.path = path; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WebhookEndpoint {
        private String event;
        private String path;

        public WebhookEndpoint() {}

        public WebhookEndpoint(String event, String path) {
            this.event = event;
            this.path = path;
        }

        public String getEvent() { return event; }
        public String getPath() { return path; }
        public void setEvent(String event) { this.event = event; }
        public void setPath(String path) { this.path = path; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComponentEndpoint {
        private String type;
        private String path;
        private String label;
        private String accessLevel;

        public ComponentEndpoint() {}

        public ComponentEndpoint(String type, String path, String label) {
            this.type = type;
            this.path = path;
            this.label = label;
        }

        public ComponentEndpoint(String type, String path, String label, String accessLevel) {
            this.type = type;
            this.path = path;
            this.label = label;
            this.accessLevel = accessLevel;
        }

        public String getType() { return type; }
        public String getPath() { return path; }
        public String getLabel() { return label; }
        public String getAccessLevel() { return accessLevel; }
        public void setType(String type) { this.type = type; }
        public void setPath(String path) { this.path = path; }
        public void setLabel(String label) { this.label = label; }
        public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    }
}
