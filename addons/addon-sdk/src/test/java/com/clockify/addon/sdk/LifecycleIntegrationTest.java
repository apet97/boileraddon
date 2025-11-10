package com.clockify.addon.sdk;

import com.clockify.addon.sdk.security.TokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for lifecycle handlers: INSTALLED -> DELETED flow.
 * Tests the addon lifecycle registration and token management.
 */
class LifecycleIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Tests that lifecycle handlers can be registered and token storage works
     */
    @Test
    void lifecycleHandlersCanBeRegisteredAndTokenStorageWorks() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("lifecycle-test-addon")
                .name("Lifecycle Test Addon")
                .description("Tests lifecycle handler registration")
                .baseUrl("http://localhost:8080/lifecycle-test")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Register a simple INSTALLED lifecycle handler
        addon.registerLifecycleHandler("INSTALLED", request ->
                HttpResponse.ok("{\"status\":\"installed\"}", "application/json"));

        // Register a simple DELETED lifecycle handler
        addon.registerLifecycleHandler("DELETED", request ->
                HttpResponse.ok("{\"status\":\"uninstalled\"}", "application/json"));

        // Verify handlers were registered
        assertTrue(addon.getLifecycleHandlers().containsKey("INSTALLED"),
                "INSTALLED handler should be registered");
        assertTrue(addon.getLifecycleHandlers().containsKey("DELETED"),
                "DELETED handler should be registered");
    }

    /**
     * Tests that tokens can be saved and retrieved through lifecycle workflow
     */
    @Test
    void tokenCanBeSavedDuringInstalledAndRemovedDuringDeleted() {
        String workspaceId = "ws-test-lifecycle-" + System.currentTimeMillis();
        String token = "test-token-xyz-123";
        String apiUrl = "https://api.clockify.me";

        // Simulate INSTALLED lifecycle
        TokenStore.save(workspaceId, token, apiUrl);

        // Verify token was saved
        assertTrue(TokenStore.get(workspaceId).isPresent(), "Token should be present after save");
        assertEquals(token, TokenStore.get(workspaceId).get().token(),
                "Retrieved token should match saved token");

        // Simulate DELETED lifecycle
        TokenStore.delete(workspaceId);

        // Verify token was removed
        assertTrue(TokenStore.get(workspaceId).isEmpty(), "Token should be absent after delete");
    }

    /**
     * Tests that addon manifest includes webhook definitions for lifecycle integration
     */
    @Test
    void addonManifestCanIncludeWebhookDefinitions() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("webhook-test-addon")
                .name("Webhook Test Addon")
                .baseUrl("http://localhost:8080/webhook-test")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        // Add webhook definition
        manifest.getWebhooks().add(
                new ClockifyManifest.WebhookEndpoint("TIME_ENTRY_CREATED", "/webhook"));

        // Verify webhook was registered
        assertTrue(manifest.getWebhooks().size() > 0, "Webhooks should be registered");
        assertTrue(manifest.getWebhooks().stream()
                        .anyMatch(w -> w.getEvent().equals("TIME_ENTRY_CREATED")),
                "TIME_ENTRY_CREATED webhook should be present");
    }

    /**
     * Tests that lifecycle handlers can be registered at custom paths
     */
    @Test
    void lifecycleHandlersCanBeRegisteredAtCustomPaths() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("custom-path-addon")
                .name("Custom Path Addon")
                .baseUrl("http://localhost:8080/custom-path")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Register handlers at custom paths
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed",
                request -> HttpResponse.ok("{\"status\":\"installed\"}", "application/json"));

        addon.registerLifecycleHandler("DELETED", "/lifecycle/deleted",
                request -> HttpResponse.ok("{\"status\":\"uninstalled\"}", "application/json"));

        // Verify handlers were registered by path
        assertTrue(addon.getLifecycleHandlersByPath().containsKey("/lifecycle/installed"),
                "Handler should be registered at custom path /lifecycle/installed");
        assertTrue(addon.getLifecycleHandlersByPath().containsKey("/lifecycle/deleted"),
                "Handler should be registered at custom path /lifecycle/deleted");
    }

    /**
     * Tests that webhook handlers can be registered for processing events during lifecycle
     */
    @Test
    void webhookHandlersCanBeRegisteredForLifecycleIntegration() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("webhook-handler-addon")
                .name("Webhook Handler Addon")
                .baseUrl("http://localhost:8080/webhook-handler")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        manifest.getWebhooks().add(
                new ClockifyManifest.WebhookEndpoint("TIME_ENTRY_CREATED", "/webhook"));
        manifest.getWebhooks().add(
                new ClockifyManifest.WebhookEndpoint("TIME_ENTRY_UPDATED", "/webhook"));

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Register webhook handlers
        addon.registerWebhookHandler("TIME_ENTRY_CREATED",
                request -> HttpResponse.ok("{\"processed\":true}", "application/json"));
        addon.registerWebhookHandler("TIME_ENTRY_UPDATED",
                request -> HttpResponse.ok("{\"processed\":true}", "application/json"));

        // Verify handlers were registered
        assertTrue(addon.getWebhookHandlers().containsKey("TIME_ENTRY_CREATED"),
                "TIME_ENTRY_CREATED handler should be registered");
        assertTrue(addon.getWebhookHandlers().containsKey("TIME_ENTRY_UPDATED"),
                "TIME_ENTRY_UPDATED handler should be registered");
    }
}
