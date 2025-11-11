package com.clockify.addon.sdk.benchmarks;

import com.clockify.addon.sdk.metrics.DatabaseMetrics;
import com.clockify.addon.sdk.security.DatabaseTokenStore;
import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for database operations.
 *
 * Critical paths:
 * - Connection pool performance
 * - Token store operations
 * - Query execution timing
 * - Database metrics collection
 *
 * These operations are critical for data persistence and performance.
 *
 * Run with: mvn test -Dtest=DatabaseOperationsBenchmark -pl addons/addon-sdk
 * Or: java -jar target/benchmarks.jar DatabaseOperationsBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = "-Xmx2g")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class DatabaseOperationsBenchmark {

    private DataSource dataSource;
    private DatabaseTokenStore tokenStore;
    private PooledDatabaseTokenStore pooledTokenStore;
    private DatabaseMetrics databaseMetrics;

    private String workspaceId;
    private String token;

    @Setup
    public void setup() throws Exception {
        // Setup in-memory H2 database for benchmarks
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);

        // Initialize database schema
        initializeDatabase();

        // Initialize components
        tokenStore = new DatabaseTokenStore();
        pooledTokenStore = new PooledDatabaseTokenStore();
        databaseMetrics = new DatabaseMetrics();

        // Setup test data
        workspaceId = "ws-db-bench-001";
        token = "db-bench-token-1234567890abcdef";

        // Pre-populate data
        tokenStore.saveToken(workspaceId, token);
        pooledTokenStore.saveToken(workspaceId, token);

        // Register metrics
        databaseMetrics.registerDataSourceMetrics(dataSource, "benchmark-pool");
    }

    @TearDown
    public void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    /**
     * Benchmark: Database connection acquisition
     * Measures performance of getting connections from pool.
     */
    @Benchmark
    public void databaseConnectionAcquisition(Blackhole bh) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            bh.consume(connection);
        }
    }

    /**
     * Benchmark: Simple SELECT query execution
     * Measures performance of basic query execution.
     */
    @Benchmark
    public void simpleSelectQuery(Blackhole bh) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int result = rs.getInt(1);
                bh.consume(result);
            }
        }
    }

    /**
     * Benchmark: Token store save operation
     * Measures performance of saving tokens to database.
     */
    @Benchmark
    public void tokenStoreSaveOperation(Blackhole bh) {
        String newWorkspaceId = "ws-save-" + System.currentTimeMillis();
        String newToken = "token-save-" + System.currentTimeMillis();
        tokenStore.saveToken(newWorkspaceId, newToken);
        bh.consume(newWorkspaceId);
    }

    /**
     * Benchmark: Token store validation (valid token)
     * Measures performance of successful token validation.
     */
    @Benchmark
    public void tokenStoreValidationValid(Blackhole bh) {
        boolean result = tokenStore.validateToken(workspaceId, token);
        bh.consume(result);
    }

    /**
     * Benchmark: Token store validation (invalid token)
     * Measures performance of failed token validation.
     */
    @Benchmark
    public void tokenStoreValidationInvalid(Blackhole bh) {
        boolean result = tokenStore.validateToken(workspaceId, "invalid-token");
        bh.consume(result);
    }

    /**
     * Benchmark: Pooled token store validation
     * Measures performance of pooled token validation.
     */
    @Benchmark
    public void pooledTokenStoreValidation(Blackhole bh) {
        boolean result = pooledTokenStore.validateToken(workspaceId, token);
        bh.consume(result);
    }

    /**
     * Benchmark: Token store delete operation
     * Measures performance of token removal.
     */
    @Benchmark
    public void tokenStoreDeleteOperation(Blackhole bh) {
        String tempWorkspaceId = "ws-delete-" + System.currentTimeMillis();
        tokenStore.saveToken(tempWorkspaceId, "temp-token");
        tokenStore.deleteToken(tempWorkspaceId);
        bh.consume(tempWorkspaceId);
    }

    /**
     * Benchmark: Multiple token operations in sequence
     * Measures combined performance of common token operations.
     */
    @Benchmark
    public void multipleTokenOperations(Blackhole bh) {
        String tempWorkspaceId = "ws-multi-" + System.currentTimeMillis();
        String tempToken = "token-multi-" + System.currentTimeMillis();

        // Save token
        tokenStore.saveToken(tempWorkspaceId, tempToken);

        // Validate token
        boolean valid = tokenStore.validateToken(tempWorkspaceId, tempToken);
        bh.consume(valid);

        // Delete token
        tokenStore.deleteToken(tempWorkspaceId);

        bh.consume(tempWorkspaceId);
    }

    /**
     * Benchmark: Database metrics collection
     * Measures performance of collecting database metrics.
     */
    @Benchmark
    public void databaseMetricsCollection(Blackhole bh) {
        // This would typically involve querying database metrics
        // For now, we simulate the operation
        try {
            Thread.sleep(1); // Simulate metrics collection time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bh.consume(dataSource);
    }

    /**
     * Benchmark: Connection pool stress test
     * Measures performance under concurrent connection requests.
     */
    @Benchmark
    @Threads(4)
    public void connectionPoolStressTest(Blackhole bh) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM tokens");
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int count = rs.getInt(1);
                bh.consume(count);
            }
        }
    }

    /**
     * Benchmark: Batch token operations
     * Measures performance of batch token operations.
     */
    @Benchmark
    public void batchTokenOperations(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            String batchWorkspaceId = "ws-batch-" + i + "-" + System.currentTimeMillis();
            String batchToken = "token-batch-" + i + "-" + System.currentTimeMillis();

            tokenStore.saveToken(batchWorkspaceId, batchToken);
            boolean valid = tokenStore.validateToken(batchWorkspaceId, batchToken);
            tokenStore.deleteToken(batchWorkspaceId);

            bh.consume(valid);
        }
    }

    /**
     * Benchmark: Complex query with joins
     * Measures performance of more complex database queries.
     */
    @Benchmark
    public void complexQueryWithJoins(Blackhole bh) throws SQLException {
        // Create a more complex query scenario
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                 "SELECT t.workspace_id, t.token, COUNT(*) as usage_count " +
                 "FROM tokens t " +
                 "LEFT JOIN token_usage u ON t.workspace_id = u.workspace_id " +
                 "WHERE t.workspace_id = ? " +
                 "GROUP BY t.workspace_id, t.token"
             )) {

            stmt.setString(1, workspaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String wsId = rs.getString("workspace_id");
                    String tokenValue = rs.getString("token");
                    int usageCount = rs.getInt("usage_count");

                    bh.consume(wsId);
                    bh.consume(tokenValue);
                    bh.consume(usageCount);
                }
            }
        }
    }

    /**
     * Benchmark: Transaction performance
     * Measures performance of database transactions.
     */
    @Benchmark
    public void transactionPerformance(Blackhole bh) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Perform multiple operations in transaction
                String tempWorkspaceId = "ws-tx-" + System.currentTimeMillis();
                String tempToken = "token-tx-" + System.currentTimeMillis();

                // Save token
                tokenStore.saveToken(tempWorkspaceId, tempToken);

                // Validate token
                boolean valid = tokenStore.validateToken(tempWorkspaceId, tempToken);
                bh.consume(valid);

                // Delete token
                tokenStore.deleteToken(tempWorkspaceId);

                connection.commit();
                bh.consume(tempWorkspaceId);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Benchmark: Database connection pool metrics
     * Measures performance of HikariCP metrics collection.
     */
    @Benchmark
    public void connectionPoolMetrics(Blackhole bh) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
            int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
            int totalConnections = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
            int threadsAwaitingConnection = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

            bh.consume(activeConnections);
            bh.consume(idleConnections);
            bh.consume(totalConnections);
            bh.consume(threadsAwaitingConnection);
        }
    }

    /**
     * Benchmark: High-frequency database operations
     * Measures performance under high request frequency.
     */
    @Benchmark
    @Threads(8)
    public void highFrequencyDatabaseOperations(Blackhole bh) {
        // Simulate high-frequency database operations
        for (int i = 0; i < 3; i++) {
            String tempWorkspaceId = "ws-hf-" + i + "-" + System.currentTimeMillis();
            String tempToken = "token-hf-" + i + "-" + System.currentTimeMillis();

            tokenStore.saveToken(tempWorkspaceId, tempToken);
            boolean valid = tokenStore.validateToken(tempWorkspaceId, tempToken);
            tokenStore.deleteToken(tempWorkspaceId);

            bh.consume(valid);
        }
    }

    /**
     * Benchmark: Database error handling performance
     * Measures performance when handling database errors.
     */
    @Benchmark
    public void databaseErrorHandling(Blackhole bh) {
        try {
            // Attempt to validate token for non-existent workspace
            boolean result = tokenStore.validateToken("non-existent-workspace", "any-token");
            bh.consume(result);
        } catch (Exception e) {
            // Expected to handle database errors gracefully
            bh.consume(e);
        }
    }

    // ============ Helper Methods ============

    private void initializeDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Create tokens table
            try (PreparedStatement stmt = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS tokens (" +
                "  workspace_id VARCHAR(255) PRIMARY KEY," +
                "  token VARCHAR(512) NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")")) {
                stmt.executeUpdate();
            }

            // Create token_usage table for complex queries
            try (PreparedStatement stmt = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS token_usage (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  workspace_id VARCHAR(255) NOT NULL," +
                "  usage_type VARCHAR(50) NOT NULL," +
                "  usage_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (workspace_id) REFERENCES tokens(workspace_id)" +
                ")")) {
                stmt.executeUpdate();
            }
        }
    }
}