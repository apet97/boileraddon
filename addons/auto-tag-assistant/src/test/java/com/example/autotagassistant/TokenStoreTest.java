package com.example.autotagassistant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenStoreTest {

    @BeforeEach
    void setUp() {
        TokenStore.clear();
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
    }

    @Test
    void saveUsesDefaultBaseUrlWhenMissing() {
        TokenStore.save("workspace-default", "token", null);

        TokenStore.WorkspaceToken token = TokenStore.get("workspace-default").orElseThrow();
        assertEquals("https://api.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveAppendsVersionWhenBaseUrlLacksApiVersion() {
        TokenStore.save("workspace-missing-version", "token", "https://developer.clockify.me/api");

        TokenStore.WorkspaceToken token = TokenStore.get("workspace-missing-version").orElseThrow();
        assertEquals("https://developer.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveTrimsTrailingSlashBeforeAppendingVersion() {
        TokenStore.save("workspace-trailing-slash", "token", "https://developer.clockify.me/api/");

        TokenStore.WorkspaceToken token = TokenStore.get("workspace-trailing-slash").orElseThrow();
        assertEquals("https://developer.clockify.me/api/v1", token.apiBaseUrl());
    }

    @Test
    void saveAppendsApiPathWhenMissingCompletely() {
        TokenStore.save("workspace-no-api", "token", "https://custom.clockify.test");

        TokenStore.WorkspaceToken token = TokenStore.get("workspace-no-api").orElseThrow();
        assertEquals("https://custom.clockify.test/api/v1", token.apiBaseUrl());
    }

    @Test
    void savePreservesProvidedApiVersion() {
        TokenStore.save("workspace-existing-version", "token", "https://staging.clockify.me/api/v2");

        TokenStore.WorkspaceToken token = TokenStore.get("workspace-existing-version").orElseThrow();
        assertEquals("https://staging.clockify.me/api/v2", token.apiBaseUrl());
    }
}
