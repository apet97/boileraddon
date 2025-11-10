package com.clockify.addon.sdk.health;

import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check provider for database connectivity and pool status.
 *
 * Verifies:
 * - Database connection successful
 * - Connection pool not exhausted
 * - Connection pool not over threshold
 * - Database operations responsive
 */
public class DatabaseHealthCheck implements HealthCheck.HealthCheckProvider {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheck.class);

    private final PooledDatabaseTokenStore tokenStore;
    private final int warningThresholdPercent;  // Warn if pool > 80% used

    public DatabaseHealthCheck(PooledDatabaseTokenStore tokenStore) {
        this(tokenStore, 80);
    }

    public DatabaseHealthCheck(PooledDatabaseTokenStore tokenStore, int warningThresholdPercent) {
        this.tokenStore = tokenStore;
        this.warningThresholdPercent = warningThresholdPercent;
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public HealthCheck.HealthCheckResult check() {
        try {
            // Test database connectivity
            long count = tokenStore.count();
            logger.debug("Database health check: {} tokens in store", count);

            // Get pool statistics
            PooledDatabaseTokenStore.HikariPoolStats stats = tokenStore.getPoolStats();

            // Build details map
            Map<String, Object> details = new HashMap<>();
            details.put("active_connections", stats.activeConnections);
            details.put("idle_connections", stats.idleConnections);
            details.put("total_connections", stats.totalConnections);
            details.put("threads_waiting", stats.threadsWaiting);
            details.put("tokens_stored", count);

            // Determine health status
            boolean healthy = true;
            String message = "Database OK";

            // Check if pool is approaching exhaustion
            if (stats.totalConnections > 0) {
                double usagePercent = (stats.activeConnections * 100.0) / stats.totalConnections;
                details.put("pool_usage_percent", (int) usagePercent);

                if (usagePercent > 90) {
                    healthy = false;
                    message = "Database pool nearly exhausted (" + (int) usagePercent + "%)";
                } else if (usagePercent > warningThresholdPercent) {
                    message = "Database pool usage at " + (int) usagePercent + "%";
                }
            }

            // Check for waiting threads (indicates pool exhaustion elsewhere)
            if (stats.threadsWaiting > 0) {
                healthy = false;
                message = "Threads waiting for pool: " + stats.threadsWaiting;
            }

            return new HealthCheck.HealthCheckResult(
                getName(),
                healthy,
                message,
                details
            );

        } catch (Exception e) {
            logger.error("Database health check failed: {}", e.getMessage(), e);
            return new HealthCheck.HealthCheckResult(
                getName(),
                false,
                "Database error: " + e.getMessage(),
                null
            );
        }
    }
}
