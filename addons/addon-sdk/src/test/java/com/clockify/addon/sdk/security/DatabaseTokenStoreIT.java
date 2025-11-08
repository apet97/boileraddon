package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DatabaseTokenStoreIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("addons")
            .withUsername("addons")
            .withPassword("addons");

    @AfterAll
    static void stop() {
        // Testcontainers handles lifecycle, but ensure clean shutdown for local runs
        if (POSTGRES != null) {
            POSTGRES.stop();
        }
    }

    @Test
    void saveGetAndRemoveToken() {
        DatabaseTokenStore store = new DatabaseTokenStore(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        String ws = "ws-it-1";
        String token = "tkn-1";

        store.save(ws, token);
        Optional<String> got = store.get(ws);
        assertTrue(got.isPresent());
        assertEquals(token, got.get());

        store.remove(ws);
        assertTrue(store.get(ws).isEmpty());
    }

    @Test
    void upsertUpdatesExistingToken() {
        DatabaseTokenStore store = new DatabaseTokenStore(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        String ws = "ws-it-2";
        store.save(ws, "first");
        assertEquals("first", store.get(ws).orElseThrow());

        store.save(ws, "second");
        assertEquals("second", store.get(ws).orElseThrow());
    }
}

