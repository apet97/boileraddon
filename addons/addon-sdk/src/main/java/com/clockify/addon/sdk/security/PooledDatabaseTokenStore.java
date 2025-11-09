package com.clockify.addon.sdk.security;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * PRODUCTION: Database token store with HikariCP connection pooling.
 *
 * Features:
 * - High-performance connection pooling (HikariCP)
 * - Configurable pool size and timeouts
 * - Thread-safe and production-ready
 * - Metrics and monitoring integration
 * - Automatic connection validation
 *
 * HikariCP is the fastest and most reliable JDBC connection pool for Java.
 * Default pool: 10 connections, 30-second idle timeout.
 *
 * Usage:
 * <pre>{@code
 * PooledDatabaseTokenStore tokenStore = new PooledDatabaseTokenStore(
 *     "jdbc:postgresql://localhost/clockify_addon",
 *     "clockify_user",
 *     "secure_password"
 * );
 * // Uses connection pooling transparently
 * tokenStore.save("workspace-id", "token");
 * tokenStore.shutdown();  // Close pool on application shutdown
 * }</pre>
 */
public class PooledDatabaseTokenStore implements TokenStoreSPI, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PooledDatabaseTokenStore.class);

    private final HikariDataSource dataSource;

    // Default HikariCP configuration
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 30000;  // 30 seconds
    private static final long DEFAULT_MAX_LIFETIME_MS = 1800000; // 30 minutes

    /**
     * Creates a pooled token store with default HikariCP configuration.
     *
     * @param jdbcUrl JDBC connection URL
     * @param username database username
     * @param password database password
     */
    public PooledDatabaseTokenStore(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, DEFAULT_POOL_SIZE);
    }

    /**
     * Creates a pooled token store with custom pool size.
     *
     * @param jdbcUrl JDBC connection URL
     * @param username database username
     * @param password database password
     * @param poolSize number of connections to maintain
     */
    public PooledDatabaseTokenStore(String jdbcUrl, String username, String password, int poolSize) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl is required");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Pool configuration
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.max(2, poolSize / 3));  // At least 2 idle connections
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);
        config.setConnectionTimeout(30000);  // 30 seconds to acquire connection

        // Validation
        config.setLeakDetectionThreshold(60000);  // Log if connection held > 60 seconds
        config.setAutoCommit(true);

        // Pool name for metrics and monitoring
        config.setPoolName("ClockifyAddonPool");

        // Register shutdown hook for graceful shutdown
        config.setRegisterMbeans(true);

        this.dataSource = new HikariDataSource(config);

        logger.info("PooledDatabaseTokenStore initialized: jdbcUrl={}, poolSize={}, idleTimeout={}ms",
                jdbcUrl, poolSize, DEFAULT_IDLE_TIMEOUT_MS);

        // Ensure table exists
        ensureTable();
    }

    /**
     * Creates a pooled token store from environment variables.
     *
     * @return configured PooledDatabaseTokenStore
     * @throws IllegalStateException if required variables are missing
     */
    public static PooledDatabaseTokenStore fromEnvironment() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String pass = System.getenv("DB_PASSWORD");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL is required to use PooledDatabaseTokenStore");
        }

        String poolSizeEnv = System.getenv("DB_POOL_SIZE");
        int poolSize = DEFAULT_POOL_SIZE;
        if (poolSizeEnv != null && !poolSizeEnv.isBlank()) {
            try {
                poolSize = Integer.parseInt(poolSizeEnv);
                if (poolSize < 1) poolSize = DEFAULT_POOL_SIZE;
            } catch (NumberFormatException e) {
                logger.warn("Invalid DB_POOL_SIZE: {} (using default: {})", poolSizeEnv, DEFAULT_POOL_SIZE);
            }
        }

        return new PooledDatabaseTokenStore(url, user, pass, poolSize);
    }

    @Override
    public void save(String workspaceId, String token) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId required");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token required");
        }

        long now = System.currentTimeMillis();
        String upsert = "INSERT INTO addon_tokens (workspace_id, auth_token, created_at, last_accessed_at) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (workspace_id) DO UPDATE SET auth_token = EXCLUDED.auth_token, last_accessed_at = EXCLUDED.last_accessed_at";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, workspaceId);
            ps.setString(2, token);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to save token for workspace %s: %s",
                    workspaceId, e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public Optional<String> get(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT auth_token FROM addon_tokens WHERE workspace_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, workspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to fetch token for workspace %s: %s",
                    workspaceId, e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        return Optional.empty();
    }

    @Override
    public void remove(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return;
        }

        String sql = "DELETE FROM addon_tokens WHERE workspace_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, workspaceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to delete token for workspace %s: %s",
                    workspaceId, e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Health check: returns number of active tokens.
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM addon_tokens";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to count tokens: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Gets HikariCP pool statistics for monitoring.
     */
    public HikariPoolStats getPoolStats() {
        return new HikariPoolStats(
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Pool statistics for monitoring and debugging.
     */
    public static class HikariPoolStats {
        public final int activeConnections;
        public final int idleConnections;
        public final int totalConnections;
        public final int threadsWaiting;

        public HikariPoolStats(int active, int idle, int total, int waiting) {
            this.activeConnections = active;
            this.idleConnections = idle;
            this.totalConnections = total;
            this.threadsWaiting = waiting;
        }

        @Override
        public String toString() {
            return String.format("HikariPool[active=%d, idle=%d, total=%d, waiting=%d]",
                    activeConnections, idleConnections, totalConnections, threadsWaiting);
        }
    }

    /**
     * Ensures token table exists in database.
     */
    private void ensureTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS addon_tokens (" +
                "workspace_id VARCHAR(255) PRIMARY KEY, " +
                "auth_token TEXT NOT NULL, " +
                "api_base_url VARCHAR(512), " +
                "created_at BIGINT NOT NULL, " +
                "last_accessed_at BIGINT NOT NULL)";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(ddl)) {
            ps.execute();
            logger.debug("Ensured addon_tokens table exists");
        } catch (SQLException e) {
            logger.warn("Could not auto-create addon_tokens table (may already exist or require manual setup): {}",
                    e.getMessage());
        }
    }

    /**
     * Closes the connection pool.
     * IMPORTANT: Call this on application shutdown to prevent resource leaks.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing PooledDatabaseTokenStore connection pool");
            dataSource.close();
        }
    }

    /**
     * Checks if the pool is closed.
     */
    public boolean isClosed() {
        return dataSource.isClosed();
    }

    /**
     * Validates database connection.
     * Useful for health checks.
     */
    public void validateConnection() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            if (c.isClosed()) {
                throw new SQLException("Connection is closed");
            }
            logger.debug("Database connection validated successfully");
        }
    }
}
