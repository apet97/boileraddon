package com.example.rules.metrics;

import com.clockify.addon.sdk.logging.LoggingContext;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseMetrics utility class.
 */
class DatabaseMetricsTest {

    private PrometheusMeterRegistry testRegistry;

    @BeforeEach
    void setUp() {
        // Use the actual singleton registry from MetricsHandler
        testRegistry = MetricsHandler.registry();
        // Clear any existing metrics before each test
        testRegistry.clear();
    }

    @Test
    void testRecordOperationSuccess() {
        String result = DatabaseMetrics.recordOperation("testOperation", "workspace123", "testEntity",
            () -> "successResult");

        assertEquals("successResult", result);

        // Verify metrics were recorded
        Counter successCounter = testRegistry.find("database_operations_total")
            .tag("operation", "testOperation")
            .tag("entity_type", "testEntity")
            .tag("workspace_id", "workspace123")
            .tag("status", "success")
            .counter();

        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        // Verify timing was recorded
        Timer timer = testRegistry.find("database_operation_duration_ms")
            .tag("operation", "testOperation")
            .tag("entity_type", "testEntity")
            .tag("workspace_id", "workspace123")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordOperationWithoutWorkspace() {
        String result = DatabaseMetrics.recordOperation("testOperation", "testEntity",
            () -> "successResult");

        assertEquals("successResult", result);

        // Verify metrics were recorded with "unknown" workspace
        Counter successCounter = testRegistry.find("database_operations_total")
            .tag("operation", "testOperation")
            .tag("entity_type", "testEntity")
            .tag("workspace_id", "unknown")
            .tag("status", "success")
            .counter();

        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());
    }

    @Test
    void testRecordOperationFailure() {
        RuntimeException expectedException = new RuntimeException("Test failure");

        assertThrows(RuntimeException.class, () -> {
            DatabaseMetrics.recordOperation("testOperation", "workspace123", "testEntity",
                () -> { throw expectedException; });
        });

        // Verify failure metrics were recorded
        Counter failureCounter = testRegistry.find("database_operations_total")
            .tag("operation", "testOperation")
            .tag("entity_type", "testEntity")
            .tag("workspace_id", "workspace123")
            .tag("status", "failure")
            .tag("error_type", "RuntimeException")
            .counter();

        assertNotNull(failureCounter);
        assertEquals(1.0, failureCounter.count());

        // Verify error counter was incremented
        Counter errorCounter = testRegistry.find("database_errors_total")
            .tag("operation", "testOperation")
            .tag("entity_type", "testEntity")
            .tag("error_type", "RuntimeException")
            .counter();

        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count());
    }

    @Test
    void testRecordConnectionEvent() {
        DatabaseMetrics.recordConnectionEvent("acquire", "testPool", 150L);

        Counter eventCounter = testRegistry.find("database_connection_events_total")
            .tag("event_type", "acquire")
            .tag("pool_name", "testPool")
            .counter();

        assertNotNull(eventCounter);
        assertEquals(1.0, eventCounter.count());

        Timer timer = testRegistry.find("database_connection_duration_ms")
            .tag("event_type", "acquire")
            .tag("pool_name", "testPool")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordConnectionEventNoDuration() {
        DatabaseMetrics.recordConnectionEvent("release", "testPool", 0L);

        Counter eventCounter = testRegistry.find("database_connection_events_total")
            .tag("event_type", "release")
            .tag("pool_name", "testPool")
            .counter();

        assertNotNull(eventCounter);
        assertEquals(1.0, eventCounter.count());

        // Should not record timing for zero duration
        Timer timer = testRegistry.find("database_connection_duration_ms")
            .tag("event_type", "release")
            .tag("pool_name", "testPool")
            .timer();

        assertNull(timer);
    }

    @Test
    void testRecordQueryMetrics() {
        DatabaseMetrics.recordQueryMetrics("select", "users", 100L, 50L);

        Counter queryCounter = testRegistry.find("database_queries_total")
            .tag("query_type", "select")
            .tag("entity_type", "users")
            .counter();

        assertNotNull(queryCounter);
        assertEquals(1.0, queryCounter.count());

        Counter rowsCounter = testRegistry.find("database_rows_processed_total")
            .tag("query_type", "select")
            .tag("entity_type", "users")
            .counter();

        assertNotNull(rowsCounter);
        assertEquals(100.0, rowsCounter.count());

        Timer timer = testRegistry.find("database_query_duration_ms")
            .tag("query_type", "select")
            .tag("entity_type", "users")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordQueryMetricsNoRowCount() {
        DatabaseMetrics.recordQueryMetrics("update", "projects", -1L, 25L);

        Counter queryCounter = testRegistry.find("database_queries_total")
            .tag("query_type", "update")
            .tag("entity_type", "projects")
            .counter();

        assertNotNull(queryCounter);
        assertEquals(1.0, queryCounter.count());

        // Should not record rows counter for negative row count
        Counter rowsCounter = testRegistry.find("database_rows_processed_total")
            .tag("query_type", "update")
            .tag("entity_type", "projects")
            .counter();

        assertNull(rowsCounter);
    }

    @Test
    void testRecordTransactionMetrics() {
        DatabaseMetrics.recordTransactionMetrics("commit", "rules", true, 100L);

        Counter transactionCounter = testRegistry.find("database_transactions_total")
            .tag("transaction_type", "commit")
            .tag("entity_type", "rules")
            .tag("status", "success")
            .counter();

        assertNotNull(transactionCounter);
        assertEquals(1.0, transactionCounter.count());

        Timer timer = testRegistry.find("database_transaction_duration_ms")
            .tag("transaction_type", "commit")
            .tag("entity_type", "rules")
            .tag("status", "success")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordCacheMetrics() {
        DatabaseMetrics.recordCacheMetrics("ruleCache", "get", true, 5L);

        Counter cacheCounter = testRegistry.find("database_cache_operations_total")
            .tag("cache_name", "ruleCache")
            .tag("operation", "get")
            .tag("result", "hit")
            .counter();

        assertNotNull(cacheCounter);
        assertEquals(1.0, cacheCounter.count());

        Timer timer = testRegistry.find("database_cache_duration_ms")
            .tag("cache_name", "ruleCache")
            .tag("operation", "get")
            .tag("result", "hit")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordDatabaseSpecificMetricsCounter() {
        DatabaseMetrics.recordDatabaseSpecificMetrics("postgresql", "connections", "counter", 10.0);

        Counter counter = testRegistry.find("database_specific_connections")
            .tag("database_type", "postgresql")
            .counter();

        assertNotNull(counter);
        assertEquals(10.0, counter.count());
    }

    @Test
    void testRecordDatabaseSpecificMetricsGauge() {
        DatabaseMetrics.recordDatabaseSpecificMetrics("mysql", "buffer_pool_size", "gauge", 1024.0);

        // Gauges are registered but we can't easily verify their values in tests
        // Just verify the metric exists
        var gauge = testRegistry.find("database_specific_buffer_pool_size")
            .tag("database_type", "mysql")
            .gauge();

        assertNotNull(gauge);
    }

    @Test
    void testRecordDatabaseSpecificMetricsTimer() {
        DatabaseMetrics.recordDatabaseSpecificMetrics("postgresql", "vacuum", "timer", 500.0);

        Timer timer = testRegistry.find("database_specific_vacuum_duration_ms")
            .tag("database_type", "postgresql")
            .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordDatabaseSpecificMetricsUnknownType() {
        DatabaseMetrics.recordDatabaseSpecificMetrics("postgresql", "unknown_metric", "unknown_type", 1.0);

        // Should log warning but not create any metrics
        var counter = testRegistry.find("database_specific_unknown_metric")
            .tag("database_type", "postgresql")
            .counter();

        assertNull(counter);
    }

    @Test
    void testRecordDatabaseSizeMetrics() {
        DatabaseMetrics.recordDatabaseSizeMetrics("postgresql", 25L, 1024L * 1024L, 256L * 1024L);

        // Gauges are registered but we can't easily verify their values in tests
        // Just verify the metrics exist
        var tableGauge = testRegistry.find("database_tables_total")
            .tag("database_type", "postgresql")
            .gauge();

        var sizeGauge = testRegistry.find("database_size_bytes")
            .tag("database_type", "postgresql")
            .gauge();

        var indexGauge = testRegistry.find("database_index_size_bytes")
            .tag("database_type", "postgresql")
            .gauge();

        assertNotNull(tableGauge);
        assertNotNull(sizeGauge);
        assertNotNull(indexGauge);
    }

    @Test
    void testSanitizeHandlesNull() {
        // Test private sanitize method through public methods
        DatabaseMetrics.recordConnectionEvent("test", null, 0L);

        Counter counter = testRegistry.find("database_connection_events_total")
            .tag("event_type", "test")
            .tag("pool_name", "unknown")
            .counter();

        assertNotNull(counter);
    }

    @Test
    void testSanitizeHandlesBlank() {
        // Test private sanitize method through public methods
        DatabaseMetrics.recordConnectionEvent("test", "   ", 0L);

        Counter counter = testRegistry.find("database_connection_events_total")
            .tag("event_type", "test")
            .tag("pool_name", "unknown")
            .counter();

        assertNotNull(counter);
    }

    @Test
    void testSanitizeTruncatesLongValues() {
        String longValue = "a".repeat(100);
        DatabaseMetrics.recordConnectionEvent("test", longValue, 0L);

        Counter counter = testRegistry.find("database_connection_events_total")
            .tag("event_type", "test")
            .tag("pool_name", "a".repeat(64))
            .counter();

        assertNotNull(counter);
    }

    @Test
    void testDatabaseOperationInterface() {
        DatabaseMetrics.DatabaseOperation<String> operation = () -> "testResult";
        assertEquals("testResult", operation.execute());
    }
}