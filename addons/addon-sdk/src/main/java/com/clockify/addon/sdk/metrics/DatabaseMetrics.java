package com.clockify.addon.sdk.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Database metrics for HikariCP connection pools and database operations.
 * Provides comprehensive monitoring for database performance and health.
 */
public final class DatabaseMetrics {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetrics.class);
    private static final MeterRegistry REGISTRY = MetricsHandler.registry();

    private DatabaseMetrics() {
    }

    /**
     * Registers connection pool gauges for real-time monitoring.
     */
    public static void registerConnectionPoolGauges(String poolName, com.zaxxer.hikari.HikariDataSource dataSource) {
        // Active connections gauge
        Gauge.builder("database_connection_pool_active", dataSource, ds -> ds.getHikariPoolMXBean().getActiveConnections())
                .description("Number of active database connections")
                .tag("pool_name", sanitize(poolName))
                .register(REGISTRY);

        // Idle connections gauge
        Gauge.builder("database_connection_pool_idle", dataSource, ds -> ds.getHikariPoolMXBean().getIdleConnections())
                .description("Number of idle database connections")
                .tag("pool_name", sanitize(poolName))
                .register(REGISTRY);

        // Total connections gauge
        Gauge.builder("database_connection_pool_total", dataSource, ds -> ds.getHikariPoolMXBean().getTotalConnections())
                .description("Total number of database connections")
                .tag("pool_name", sanitize(poolName))
                .register(REGISTRY);

        // Waiting threads gauge
        Gauge.builder("database_connection_pool_waiting", dataSource, ds -> ds.getHikariPoolMXBean().getThreadsAwaitingConnection())
                .description("Number of threads waiting for database connections")
                .tag("pool_name", sanitize(poolName))
                .register(REGISTRY);

        // Connection timeout counter - removed as getConnectionTimeout() doesn't exist on HikariPoolMXBean

        logger.debug("Registered Micrometer connection pool gauges for: {}", poolName);
    }

    /**
     * Records connection pool events.
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
     * Records database operation metrics.
     */
    public static void recordOperation(String operation, String entityType, boolean success, long durationMs) {
        Counter.builder("database_operations_total")
                .description("Total database operations")
                .tag("operation", sanitize(operation))
                .tag("entity_type", sanitize(entityType))
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .increment();

        Timer.builder("database_operation_duration_ms")
                .description("Duration of database operations")
                .tag("operation", sanitize(operation))
                .tag("entity_type", sanitize(entityType))
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records database error metrics.
     */
    public static void recordError(String operation, String entityType, String errorType) {
        Counter.builder("database_errors_total")
                .description("Database operation errors by type")
                .tag("operation", sanitize(operation))
                .tag("entity_type", sanitize(entityType))
                .tag("error_type", sanitize(errorType))
                .register(REGISTRY)
                .increment();
    }

    /**
     * Records query performance metrics.
     */
    public static void recordQueryMetrics(String queryType, String entityType, long rowCount, long durationMs) {
        Counter.builder("database_queries_total")
                .description("Database queries executed")
                .tag("query_type", sanitize(queryType))
                .tag("entity_type", sanitize(entityType))
                .register(REGISTRY)
                .increment();

        if (rowCount >= 0) {
            Counter.builder("database_rows_processed_total")
                    .description("Total rows processed by queries")
                    .tag("query_type", sanitize(queryType))
                    .tag("entity_type", sanitize(entityType))
                    .register(REGISTRY)
                    .increment(rowCount);
        }

        Timer.builder("database_query_duration_ms")
                .description("Database query execution duration")
                .tag("query_type", sanitize(queryType))
                .tag("entity_type", sanitize(entityType))
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
                .tag("transaction_type", sanitize(transactionType))
                .tag("entity_type", sanitize(entityType))
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .increment();

        Timer.builder("database_transaction_duration_ms")
                .description("Database transaction duration")
                .tag("transaction_type", sanitize(transactionType))
                .tag("entity_type", sanitize(entityType))
                .tag("status", success ? "success" : "failure")
                .register(REGISTRY)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records database size metrics for capacity planning.
     */
    public static void recordDatabaseSizeMetrics(String databaseType, long tableCount,
                                               long totalSizeBytes, long indexSizeBytes) {
        // Table count gauge
        Gauge.builder("database_tables_total", () -> (double) tableCount)
                .description("Total number of database tables")
                .tag("database_type", sanitize(databaseType))
                .register(REGISTRY);

        // Total database size gauge
        Gauge.builder("database_size_bytes", () -> (double) totalSizeBytes)
                .description("Total database size in bytes")
                .tag("database_type", sanitize(databaseType))
                .register(REGISTRY);

        // Index size gauge
        Gauge.builder("database_index_size_bytes", () -> (double) indexSizeBytes)
                .description("Database index size in bytes")
                .tag("database_type", sanitize(databaseType))
                .register(REGISTRY);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        // Limit length for metric tag values
        return value.length() > 64 ? value.substring(0, 64) : value;
    }
}