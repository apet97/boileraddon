package com.clockify.addon.sdk;

import com.clockify.addon.sdk.security.TokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockifyAddonSmokeTest {

    @AfterEach
    void tearDown() {
        TokenStore.clear();
    }

    @Test
    void registersLifecycleAndWebhookHandlersAndPersistsTokens() throws Exception {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("smoke-addon")
                .name("Smoke Addon")
                .baseUrl("https://example.com/smoke")
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        RequestHandler handler = request -> HttpResponse.ok("ok");

        addon.registerLifecycleHandler("INSTALLED", handler);
        addon.registerLifecycleHandler("DELETED", "/lifecycle/cleanup", handler);

        addon.registerWebhookHandler("TIME_ENTRY_CREATED", handler);
        addon.registerWebhookHandler("TIME_ENTRY_UPDATED", "/webhooks/time-entry", handler);

        assertEquals("/lifecycle/installed", manifest.getLifecycle().stream()
                .filter(lc -> lc.getType().equals("INSTALLED"))
                .findFirst()
                .orElseThrow()
                .getPath());
        assertEquals("/lifecycle/cleanup", manifest.getLifecycle().stream()
                .filter(lc -> lc.getType().equals("DELETED"))
                .findFirst()
                .orElseThrow()
                .getPath());

        assertEquals("/webhook", manifest.getWebhooks().stream()
                .filter(wh -> wh.getEvent().equals("TIME_ENTRY_CREATED"))
                .findFirst()
                .orElseThrow()
                .getPath());
        assertEquals("/webhooks/time-entry", manifest.getWebhooks().stream()
                .filter(wh -> wh.getEvent().equals("TIME_ENTRY_UPDATED"))
                .findFirst()
                .orElseThrow()
                .getPath());

        assertEquals("/webhooks/time-entry", addon.getWebhookPathsByEvent().get("TIME_ENTRY_UPDATED"));
        assertTrue(addon.getWebhookHandlersByPath()
                .get("/webhooks/time-entry")
                .containsKey("TIME_ENTRY_UPDATED"));

        TokenStore.save("workspace-smoke", "token-123", "https://developer.clockify.me/api");
        TokenStore.WorkspaceToken token = TokenStore.get("workspace-smoke").orElseThrow();
        assertEquals("https://developer.clockify.me/api/v1", token.apiBaseUrl());
        assertTrue(TokenStore.delete("workspace-smoke"));
        assertFalse(TokenStore.get("workspace-smoke").isPresent());
    }
}
