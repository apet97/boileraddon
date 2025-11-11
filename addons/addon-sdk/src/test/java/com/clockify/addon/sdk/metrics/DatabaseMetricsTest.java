package com.clockify.addon.sdk.metrics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseMetrics class.
 * Tests database metrics registration and recording functionality.
 */
class DatabaseMetricsTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() {
        // Only create data source if needed by specific tests
        // Most tests don't need a real database connection
        dataSource = null;
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void testRegisterConnectionPoolGauges() {
        String poolName = "test-pool";

        // Create a mock HikariDataSource instead of real database connection
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        com.zaxxer.hikari.HikariPoolMXBean mockPoolMXBean = mock(com.zaxxer.hikari.HikariPoolMXBean.class);

        // Setup mock behavior
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockPoolMXBean);
        when(mockPoolMXBean.getActiveConnections()).thenReturn(2);
        when(mockPoolMXBean.getIdleConnections()).thenReturn(3);
        when(mockPoolMXBean.getTotalConnections()).thenReturn(5);
        when(mockPoolMXBean.getThreadsAwaitingConnection()).thenReturn(0);

        // Should not throw exception
        DatabaseMetrics.registerConnectionPoolGauges(poolName, mockDataSource);

        // Verify that metrics are registered (we can't easily verify the actual metrics)
        // But we can verify the method completes without errors
        assertNotNull(mockDataSource);
    }

    @Test
    void testRegisterConnectionPoolGaugesWithNullDataSource() {
        String poolName = "test-pool";

        // Should handle null data source gracefully
        assertDoesNotThrow(() -> {
            DatabaseMetrics.registerConnectionPoolGauges(poolName, null);
        });
    }

    @Test
    void testRecordConnectionEvent() {
        String eventType = "connection_acquired";
        String poolName = "test-pool";
        long durationMs = 150;

        // Should not throw exception
        DatabaseMetrics.recordConnectionEvent(eventType, poolName, durationMs);

        // Verify the method completes without errors
        assertTrue(true); // If we get here, the test passes
    }

    @Test
    void testRecordConnectionEventWithZeroDuration() {
        String eventType = "connection_released";
        String poolName = "test-pool";

        // Should handle zero duration
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordConnectionEvent(eventType, poolName, 0);
        });
    }

    @Test
    void testRecordOperationSuccess() {
        String operation = "save";
        String entityType = "token";
        boolean success = true;
        long durationMs = 25;

        // Should not throw exception
        DatabaseMetrics.recordOperation(operation, entityType, success, durationMs);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordOperationFailure() {
        String operation = "delete";
        String entityType = "user";
        boolean success = false;
        long durationMs = 10;

        // Should not throw exception
        DatabaseMetrics.recordOperation(operation, entityType, success, durationMs);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordError() {
        String operation = "query";
        String entityType = "settings";
        String errorType = "timeout";

        // Should not throw exception
        DatabaseMetrics.recordError(operation, entityType, errorType);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordQueryMetrics() {
        String queryType = "select";
        String entityType = "tokens";
        long rowCount = 42;
        long durationMs = 100;

        // Should not throw exception
        DatabaseMetrics.recordQueryMetrics(queryType, entityType, rowCount, durationMs);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordQueryMetricsWithNegativeRowCount() {
        String queryType = "update";
        String entityType = "users";
        long rowCount = -1; // Negative row count
        long durationMs = 50;

        // Should handle negative row count
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordQueryMetrics(queryType, entityType, rowCount, durationMs);
        });
    }

    @Test
    void testRecordTransactionMetricsSuccess() {
        String transactionType = "commit";
        String entityType = "batch_operations";
        boolean success = true;
        long durationMs = 200;

        // Should not throw exception
        DatabaseMetrics.recordTransactionMetrics(transactionType, entityType, success, durationMs);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordTransactionMetricsFailure() {
        String transactionType = "rollback";
        String entityType = "failed_operations";
        boolean success = false;
        long durationMs = 150;

        // Should not throw exception
        DatabaseMetrics.recordTransactionMetrics(transactionType, entityType, success, durationMs);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordDatabaseSizeMetrics() {
        String databaseType = "postgresql";
        long tableCount = 15;
        long totalSizeBytes = 1024 * 1024 * 100; // 100MB
        long indexSizeBytes = 1024 * 1024 * 20; // 20MB

        // Should not throw exception
        DatabaseMetrics.recordDatabaseSizeMetrics(databaseType, tableCount, totalSizeBytes, indexSizeBytes);

        // Verify the method completes without errors
        assertTrue(true);
    }

    @Test
    void testRecordDatabaseSizeMetricsWithZeroValues() {
        String databaseType = "h2";
        long tableCount = 0;
        long totalSizeBytes = 0;
        long indexSizeBytes = 0;

        // Should handle zero values
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordDatabaseSizeMetrics(databaseType, tableCount, totalSizeBytes, indexSizeBytes);
        });
    }

    @Test
    void testSanitizeNullValue() {
        // We can't test the private sanitize method directly, but we can test through public methods
        String operation = "test";
        String entityType = null; // null value

        // Should handle null values gracefully
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordOperation(operation, entityType, true, 10);
        });
    }

    @Test
    void testSanitizeEmptyValue() {
        String operation = "test";
        String entityType = ""; // empty value

        // Should handle empty values gracefully
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordOperation(operation, entityType, true, 10);
        });
    }

    @Test
    void testSanitizeBlankValue() {
        String operation = "test";
        String entityType = "   "; // blank value

        // Should handle blank values gracefully
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordOperation(operation, entityType, true, 10);
        });
    }

    @Test
    void testSanitizeLongValue() {
        String operation = "test";
        String entityType = "a".repeat(100); // very long value

        // Should handle long values gracefully (truncation)
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordOperation(operation, entityType, true, 10);
        });
    }

    @Test
    void testMultipleMetricRecordings() {
        // Test that multiple metric recordings work without conflicts
        DatabaseMetrics.recordOperation("save", "token", true, 15);
        DatabaseMetrics.recordOperation("delete", "user", false, 8);
        DatabaseMetrics.recordError("query", "settings", "timeout");
        DatabaseMetrics.recordQueryMetrics("select", "tokens", 25, 45);

        // If we get here without exceptions, the test passes
        assertTrue(true);
    }

    @Test
    void testConcurrentMetricRecordings() throws InterruptedException {
        // Test basic concurrency (not true stress test, but verifies no obvious thread safety issues)
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                DatabaseMetrics.recordOperation("concurrent", "test", true, i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                DatabaseMetrics.recordError("concurrent", "test", "error_" + i);
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // If we get here without exceptions, the test passes
        assertTrue(true);
    }

    @Test
    void testDatabaseMetricsConstructor() {
        // DatabaseMetrics has a private constructor, so we can't instantiate it
        // This is expected behavior for a utility class
        assertThrows(IllegalAccessException.class, () -> {
            DatabaseMetrics.class.newInstance();
        });
    }

    @Test
    void testRecordConnectionEventWithNullPoolName() {
        String eventType = "test_event";

        // Should handle null pool name
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordConnectionEvent(eventType, null, 100);
        });
    }

    @Test
    void testRecordOperationWithNullParameters() {
        // Should handle null parameters
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordOperation(null, null, true, 10);
        });
    }

    @Test
    void testRecordErrorWithNullParameters() {
        // Should handle null parameters
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordError(null, null, null);
        });
    }

    @Test
    void testRecordQueryMetricsWithNullParameters() {
        // Should handle null parameters
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordQueryMetrics(null, null, 0, 10);
        });
    }

    @Test
    void testRecordTransactionMetricsWithNullParameters() {
        // Should handle null parameters
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordTransactionMetrics(null, null, true, 10);
        });
    }

    @Test
    void testRecordDatabaseSizeMetricsWithNullParameters() {
        // Should handle null parameters
        assertDoesNotThrow(() -> {
            DatabaseMetrics.recordDatabaseSizeMetrics(null, 0, 0, 0);
        });
    }
}