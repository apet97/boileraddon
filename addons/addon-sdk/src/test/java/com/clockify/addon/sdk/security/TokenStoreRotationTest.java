package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreRotationTest {
    private static final String WORKSPACE = "workspace-rot";
    private static final String TOKEN_A = "token-old";
    private static final String TOKEN_B = "token-new";

    private Clock baseClock;

    @BeforeEach
    void setUp() {
        TokenStore.clear();
        baseClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        TokenStore.setClock(baseClock);
        System.setProperty("clockify.token.ttl.ms", "200");
        System.setProperty("clockify.token.rotation.grace.ms", "30");
    }

    @AfterEach
    void tearDown() {
        TokenStore.resetClock();
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
    void rotationRejectsPreviousTokenAfterGrace() {
        TokenStore.save(WORKSPACE, TOKEN_A, "https://api.clockify.me/api/v1");
        TokenStore.rotate(WORKSPACE, TOKEN_B);
        TokenStore.setClock(Clock.offset(baseClock, Duration.ofMillis(250)));

        assertFalse(TokenStore.isValidToken(WORKSPACE, TOKEN_A));
        assertTrue(TokenStore.isValidToken(WORKSPACE, TOKEN_B));
    }

    @Test
    void tokensExpireAfterTtl() {
        TokenStore.save(WORKSPACE, TOKEN_A, "https://api.clockify.me/api/v1");
        TokenStore.setClock(Clock.offset(baseClock, Duration.ofMillis(500)));

        assertFalse(TokenStore.isValidToken(WORKSPACE, TOKEN_A));
    }
}
