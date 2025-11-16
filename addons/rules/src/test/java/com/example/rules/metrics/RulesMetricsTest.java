package com.example.rules.metrics;

import com.clockify.addon.sdk.metrics.MetricsHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RulesMetrics utility class.
 */
class RulesMetricsTest {

    private PrometheusMeterRegistry testRegistry;

    @BeforeEach
    void setUp() {
        // Use the actual singleton registry from MetricsHandler
        testRegistry = MetricsHandler.registry();
        // Clear any existing metrics before each test
        testRegistry.clear();
    }

    @Test
    void testStartWebhookTimer() {
        Timer.Sample sample = RulesMetrics.startWebhookTimer();
        assertNotNull(sample);
    }

    @Test
    void testStopWebhookTimer() {
        Timer.Sample sample = RulesMetrics.startWebhookTimer();
        RulesMetrics.stopWebhookTimer(sample, "NEW_TIME_ENTRY", "processed");

        Timer timer = testRegistry.find("rules_webhook_latency_ms")
            .tag("event", "NEW_TIME_ENTRY")
            .tag("outcome", "processed")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testStopWebhookTimerWithNullSample() {
        // Should not throw exception when sample is null
        RulesMetrics.stopWebhookTimer(null, "NEW_TIME_ENTRY", "processed");

        Timer timer = testRegistry.find("rules_webhook_latency_ms")
            .tag("event", "NEW_TIME_ENTRY")
            .tag("outcome", "processed")
            .timer();

        // Timer should not be created when sample is null
        assertNull(timer);
    }

    @Test
    void testRecordRuleEvaluation() {
        RulesMetrics.recordRuleEvaluation("NEW_TIME_ENTRY", 10, 2);

        Counter evaluatedCounter = testRegistry.find("rules_evaluated_total")
            .tag("event", "NEW_TIME_ENTRY")
            .counter();

        Counter matchedCounter = testRegistry.find("rules_matched_total")
            .tag("event", "NEW_TIME_ENTRY")
            .counter();

        assertNotNull(evaluatedCounter);
        assertEquals(10.0, evaluatedCounter.count());

        assertNotNull(matchedCounter);
        assertEquals(2.0, matchedCounter.count());
    }

    @Test
    void testRecordActionResultSuccess() {
        RulesMetrics.recordActionResult("webhook", true);

        Counter actionCounter = testRegistry.find("rules_actions_total")
            .tag("type", "webhook")
            .tag("result", "success")
            .counter();

        assertNotNull(actionCounter);
        assertEquals(1.0, actionCounter.count());
    }

    @Test
    void testRecordActionResultFailure() {
        RulesMetrics.recordActionResult("api_call", false);

        Counter actionCounter = testRegistry.find("rules_actions_total")
            .tag("type", "api_call")
            .tag("result", "failure")
            .counter();

        assertNotNull(actionCounter);
        assertEquals(1.0, actionCounter.count());
    }

    @Test
    void testRecordDeduplicatedEvent() {
        RulesMetrics.recordDeduplicatedEvent("NEW_TIME_ENTRY");

        Counter dedupCounter = testRegistry.find("rules_webhook_dedup_hits_total")
            .tag("event", "NEW_TIME_ENTRY")
            .counter();

        assertNotNull(dedupCounter);
        assertEquals(1.0, dedupCounter.count());
    }

    @Test
    void testRecordDedupMiss() {
        RulesMetrics.recordDedupMiss("NEW_TIME_ENTRY");

        Counter dedupCounter = testRegistry.find("rules_webhook_dedup_misses_total")
            .tag("event", "NEW_TIME_ENTRY")
            .counter();

        assertNotNull(dedupCounter);
        assertEquals(1.0, dedupCounter.count());
    }

    @Test
    void testRecordAsyncBacklog() {
        RulesMetrics.recordAsyncBacklog("fallback");

        Counter backlogCounter = testRegistry.find("rules_async_backlog_total")
            .tag("outcome", "fallback")
            .counter();

        assertNotNull(backlogCounter);
        assertEquals(1.0, backlogCounter.count());
    }

    @Test
    void testRecordWorkspaceCacheTruncation() {
        RulesMetrics.recordWorkspaceCacheTruncation("tasks");

        Counter truncationCounter = testRegistry.find("rules_workspace_cache_truncated_total")
            .tag("dataset", "tasks")
            .counter();

        assertNotNull(truncationCounter);
        assertEquals(1.0, truncationCounter.count());
    }

    @Test
    void testRecordIdempotencyBackend() {
        RulesMetrics.recordIdempotencyBackend("database");
        Gauge db = testRegistry.find("rules_webhook_idempotency_backend")
                .tag("backend", "database")
                .gauge();
        assertNotNull(db);
        assertEquals(1.0, db.value());

        RulesMetrics.recordIdempotencyBackend("in_memory");
        Gauge mem = testRegistry.find("rules_webhook_idempotency_backend")
                .tag("backend", "in_memory")
                .gauge();
        assertNotNull(mem);
        assertEquals(1.0, mem.value());
        assertEquals(0.0, db.value());
    }

    @Test
    void testSanitizeHandlesNull() {
        // Test private sanitize method through public methods
        RulesMetrics.recordRuleEvaluation(null, 5, 1);

        Counter evaluatedCounter = testRegistry.find("rules_evaluated_total")
            .tag("event", "unknown")
            .counter();

        assertNotNull(evaluatedCounter);
    }

    @Test
    void testSanitizeHandlesBlank() {
        // Test private sanitize method through public methods
        RulesMetrics.recordRuleEvaluation("   ", 5, 1);

        Counter evaluatedCounter = testRegistry.find("rules_evaluated_total")
            .tag("event", "unknown")
            .counter();

        assertNotNull(evaluatedCounter);
    }

    @Test
    void testSanitizeTruncatesLongValues() {
        String longValue = "a".repeat(100);
        RulesMetrics.recordRuleEvaluation(longValue, 5, 1);

        Counter evaluatedCounter = testRegistry.find("rules_evaluated_total")
            .tag("event", "a".repeat(64))
            .counter();

        assertNotNull(evaluatedCounter);
    }

    @Test
    void testConstructorIsPrivate() {
        // Verify that the constructor is private and cannot be instantiated
        assertThrows(IllegalAccessException.class, () -> {
            RulesMetrics.class.getDeclaredConstructor().newInstance();
        });
    }
}
