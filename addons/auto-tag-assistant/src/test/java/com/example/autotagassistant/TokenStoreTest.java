package com.example.autotagassistant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenStoreTest {

    @BeforeEach
    void setUp() {
        com.clockify.addon.sdk.security.TokenStore.clear();
    }

    @AfterEach
    void tearDown() {
        com.clockify.addon.sdk.security.TokenStore.clear();
    }

    @Test
    void saveUsesDefaultBaseUrlWhenMissing() {
        com.clockify.addon.sdk.security.TokenStore.save("workspace-default", "token", null);

        com.clockify.addon.sdk.security.TokenStore.WorkspaceToken token = com.clockify.addon.sdk.security.TokenStore.get("workspace-default").orElseThrow();
        assertEquals("https://api.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveAppendsVersionWhenBaseUrlLacksApiVersion() {
        com.clockify.addon.sdk.security.TokenStore.save("workspace-missing-version", "token", "https://developer.clockify.me/api");

        com.clockify.addon.sdk.security.TokenStore.WorkspaceToken token = com.clockify.addon.sdk.security.TokenStore.get("workspace-missing-version").orElseThrow();
        assertEquals("https://developer.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveTrimsTrailingSlashBeforeAppendingVersion() {
        com.clockify.addon.sdk.security.TokenStore.save("workspace-trailing-slash", "token", "https://developer.clockify.me/api/");

        com.clockify.addon.sdk.security.TokenStore.WorkspaceToken token = com.clockify.addon.sdk.security.TokenStore.get("workspace-trailing-slash").orElseThrow();
        assertEquals("https://developer.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveAppendsApiPathWhenMissingCompletely() {
        com.clockify.addon.sdk.security.TokenStore.save("workspace-no-api", "token", "https://custom.clockify.test");

        com.clockify.addon.sdk.security.TokenStore.WorkspaceToken token = com.clockify.addon.sdk.security.TokenStore.get("workspace-no-api").orElseThrow();
        assertEquals("https://custom.clockify.test/api/v1", token.apiBaseUrl());
    }

    @Test
    void savePreservesProvidedApiVersion() {
        com.clockify.addon.sdk.security.TokenStore.save("workspace-existing-version", "token", "https://staging.clockify.me/api/v2");

        com.clockify.addon.sdk.security.TokenStore.WorkspaceToken token = com.clockify.addon.sdk.security.TokenStore.get("workspace-existing-version").orElseThrow();
        assertEquals("https://staging.clockify.me/api/v2", token.apiBaseUrl());
    }
}
