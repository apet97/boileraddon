package com.example.rules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookIdempotencyCacheTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        WebhookIdempotencyCache.clear();
        WebhookIdempotencyCache.configureTtl(60_000);
    }

    @AfterEach
    void tearDown() {
        WebhookIdempotencyCache.clear();
    }

    @Test
    void duplicatePayloadIsDetected() throws Exception {
        ObjectNode payload = MAPPER.createObjectNode()
                .put("workspaceId", "ws")
                .put("event", "NEW_TIME_ENTRY")
                .put("id", "entry-123");

        assertFalse(WebhookIdempotencyCache.isDuplicate("ws", "NEW_TIME_ENTRY", payload));
        assertTrue(WebhookIdempotencyCache.isDuplicate("ws", "NEW_TIME_ENTRY", payload));
    }

    @Test
    void fallbackHashUsedWhenNoIdentifiersPresent() throws Exception {
        ObjectNode payload = MAPPER.createObjectNode()
                .put("timestamp", "12345")
                .put("random", "value");

        String dedupKey = WebhookIdempotencyCache.deriveDedupKey(payload);
        assertNotNull(dedupKey);
        assertFalse(dedupKey.isBlank());

        String dedupKeyAgain = WebhookIdempotencyCache.deriveDedupKey(payload);
        assertEquals(dedupKey, dedupKeyAgain);
    }
}
