package com.example.rules.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseWebhookIdempotencyStoreTest {

    private DatabaseWebhookIdempotencyStore store;
    private static final String URL = "jdbc:h2:mem:dedup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @BeforeEach
    void setUp() {
        store = new DatabaseWebhookIdempotencyStore(URL, "sa", "", 1000);
    }

    @AfterEach
    void tearDown() {
        store.clear();
    }

    @Test
    void firstInsertIsNotDuplicate() {
        assertFalse(store.isDuplicate("ws", "EVENT", "key", 5_000));
        assertTrue(store.isDuplicate("ws", "EVENT", "key", 5_000));
    }

    @Test
    void expiredEntryAllowsReprocessing() throws InterruptedException {
        assertFalse(store.isDuplicate("ws2", "EVENT", "id", 5));
        Thread.sleep(10);
        assertFalse(store.isDuplicate("ws2", "EVENT", "id", 5));
    }
}
