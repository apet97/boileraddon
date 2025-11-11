package com.example.rules.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.TimeUnit;

/**
 * Structured logging and performance metrics for database operations.
 * Provides consistent observability across all database interactions.
 */
public final class DatabaseMetrics {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMetrics.class);
    private static final MeterRegistry REGISTRY = com.clockify.addon.sdk.metrics.MetricsHandler.registry();

    private DatabaseMetrics() {
    }

    /**
     * Records a database operation with timing and structured logging.
     */
    public static <T> T recordOperation(String operation, String workspaceId, String entityType,
                                       DatabaseOperation<T> operationFunc) {
        Timer.Sample sample = Timer.start(REGISTRY);
        String requestId = MDC.get("requestId");

        try {
            // Set structured logging context
            try (var ctx = com.clockify.addon.sdk.logging.LoggingContext.create()
                    .workspace(workspaceId)
                    .request(requestId)) {

                log.debug("Starting database operation: {} for workspace: {}, entity: {}",
                         operation, workspaceId, entityType);

                T result = operationFunc.execute();

                // Record success metrics
                recordSuccess(operation, workspaceId, entityType);

                log.debug("Completed database operation: {} for workspace: {}, entity: {}",
                         operation, workspaceId, entityType);

                return result;
            }
        } catch (Exception e) {
            // Record failure metrics
            recordFailure(operation, workspaceId, entityType, e.getClass().getSimpleName());

            log.error("Database operation failed: {} for workspace: {}, entity: {}, error: {}",
                     operation, workspaceId, entityType, e.getMessage(), e);

            throw e;
        } finally {
            // Record timing
            sample.stop(Timer.builder("database_operation_duration_ms")
                    .description("Duration of database operations")
                    .tag("operation", operation)
                    .tag("entity_type", entityType)
                    .tag("workspace_id", sanitize(workspaceId))
                    .register(REGISTRY));
        }
    }

    /**
     * Records a database operation without workspace context.
     */
    public static <T> T recordOperation(String operation, String entityType,
                                       DatabaseOperation<T> operationFunc) {
        return recordOperation(operation, null, entityType, operationFunc);
    }

    private static void recordSuccess(String operation, String workspaceId, String entityType) {
        Counter.builder("database_operations_total")
                .description("Total database operations")
                .tag("operation", operation)
                .tag("entity_type", entityType)
                .tag("workspace_id", sanitize(workspaceId))
                .tag("status", "success")
                .register(REGISTRY)
                .increment();
    }

    private static void recordFailure(String operation, String workspaceId, String entityType, String errorType) {
        Counter.builder("database_operations_total")
                .description("Total database operations")
                .tag("operation", operation)
                .tag("entity_type", entityType)
                .tag("workspace_id", sanitize(workspaceId))
                .tag("status", "failure")
                .tag("error_type", sanitize(errorType))
                .register(REGISTRY)
                .increment();

        Counter.builder("database_errors_total")
                .description("Database operation errors by type")
                .tag("operation", operation)
                .tag("entity_type", entityType)
                .tag("error_type", sanitize(errorType))
                .register(REGISTRY)
                .increment();
    }

    /**
     * Records connection pool metrics.
     */
    public static void recordConnectionEvent(String eventType, String poolName, long durationMs) {
        Counter.builder("database_connection_events_total")
                .description("Database connection pool events")
                .tag("event_type", eventType)
                .tag("pool_name", sanitize(poolName))
                .register(REGISTRY)
                .increment();

        if (durationMs > 0) {
            Timer.builder("database_connection_duration_ms")
                    .description("Database connection operation duration")
                    .tag("event_type", eventType)
                    .tag("pool_name", sanitize(poolName))
                    .register(REGISTRY)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Records query performance metrics.
     */
    public static void recordQueryMetrics(String queryType, String entityType, long rowCount, long durationMs) {
        Counter.builder("database_queries_total")
                .description("Database queries executed")
                .tag("query_type", queryType)
                .tag("entity_type", entityType)
                .register(REGISTRY)
                .increment();

        if (rowCount >= 0) {
            Counter.builder("database_rows_processed_total")
                    .description("Total rows processed by queries")
                    .tag("query_type", queryType)
                    .tag("entity_type", entityType)
                    .register(REGISTRY)
                    .increment(rowCount);
        }

        Timer.builder("database_query_duration_ms")
                .description("Database query execution duration")
                .tag("query_type", queryType)
                .tag("entity_type", entityType)
                .register(REGISTRY)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records transaction metrics.
     */
    public static void recordTransactionMetrics(String transactionType, String entityType,
                                               boolean success, long durationMs) {
        Counter.builder("database_transactions_total")
                .description("Database transactions")
                .tag("transaction_type", transactionType)
                .tag("entity_type", entityType)
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .increment();

        Timer.builder("database_transaction_duration_ms")
                .description("Database transaction duration")
                .tag("transaction_type", transactionType)
                .tag("entity_type", entityType)
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records cache metrics for database-backed caches.
     */
    public static void recordCacheMetrics(String cacheName, String operation, boolean hit, long durationMs) {
        Counter.builder("database_cache_operations_total")
                .description("Database cache operations")
                .tag("cache_name", sanitize(cacheName))
                .tag("operation", operation)
                .tag("result", hit ? "hit" : "miss")
                .register(REGISTRY)
                .increment();

        Timer.builder("database_cache_duration_ms")
                .description("Database cache operation duration")
                .tag("cache_name", sanitize(cacheName))
                .tag("operation", operation)
                .tag("result", hit ? "hit" : "miss")
                .register(REGISTRY)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        // Limit length for metric tag values
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    /**
     * Functional interface for database operations.
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute();
    }
}