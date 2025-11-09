package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreRotationTest {
    private static final String WORKSPACE = "workspace-rot";
    private static final String TOKEN_A = "token-old";
    private static final String TOKEN_B = "token-new";

    @BeforeEach
    void setUp() {
        TokenStore.clear();
        System.setProperty("clockify.token.ttl.ms", "50");
        System.setProperty("clockify.token.rotation.grace.ms", "30");
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
        System.clearProperty("clockify.token.ttl.ms");
        System.clearProperty("clockify.token.rotation.grace.ms");
    }

    @Test
    void rotationKeepsPreviousTokenDuringGracePeriod() {
        TokenStore.save(WORKSPACE, TOKEN_A, "https://api.clockify.me/api/v1");
        TokenStore.rotate(WORKSPACE, TOKEN_B);

        assertTrue(TokenStore.isValidToken(WORKSPACE, TOKEN_B));
        assertTrue(TokenStore.isValidToken(WORKSPACE, TOKEN_A));
    }

    @Test
    void rotationRejectsPreviousTokenAfterGrace() throws InterruptedException {
        TokenStore.save(WORKSPACE, TOKEN_A, "https://api.clockify.me/api/v1");
        TokenStore.rotate(WORKSPACE, TOKEN_B);
        Thread.sleep(50);
        assertFalse(TokenStore.isValidToken(WORKSPACE, TOKEN_A));
        assertTrue(TokenStore.isValidToken(WORKSPACE, TOKEN_B));
    }

    @Test
    void tokensExpireAfterTtl() throws InterruptedException {
        TokenStore.save(WORKSPACE, TOKEN_A, "https://api.clockify.me/api/v1");
        Thread.sleep(60);
        assertFalse(TokenStore.isValidToken(WORKSPACE, TOKEN_A));
    }
}
