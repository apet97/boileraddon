package com.example.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicWebhookHandlersTest {

    @Test
    void computeDelayUsesRetryAfterCap() {
        long delay = DynamicWebhookHandlers.computeDelay(1, 7_000L);
        assertEquals(5_000L, delay);
    }

    @Test
    void computeDelayAppliesJitter() {
        long delay = DynamicWebhookHandlers.computeDelay(2, null);
        assertTrue(delay >= 550 && delay <= 649, "Delay within jitter window");
    }
}
