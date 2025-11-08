package com.clockify.addon.sdk.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * Database-backed token storage implementation.
 *
 * This is a production-ready implementation that persists tokens to a database.
 * Requires a JDBC-compatible database (PostgreSQL, MySQL, etc.).
 *
 * Setup SQL:
 * <pre>
 * CREATE TABLE addon_tokens (
 *     workspace_id VARCHAR(255) PRIMARY KEY,
 *     auth_token TEXT NOT NULL,
 *     api_base_url VARCHAR(512) NOT NULL,
 *     created_at BIGINT NOT NULL,
 *     last_accessed_at BIGINT NOT NULL,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 * );
 *
 * CREATE INDEX idx_tokens_created ON addon_tokens(created_at);
 * CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);
 * </pre>
 *
 * Environment variables:
 * - DB_URL: JDBC connection URL (e.g., jdbc:postgresql://localhost:5432/clockify_addons)
 * - DB_USER: Database username
 * - DB_PASSWORD: Database password
 */
public class DatabaseTokenStore implements ITokenStore {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTokenStore.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;

    /**
     * Creates a database token store.
     *
     * @param jdbcUrl JDBC connection URL
     * @param username Database username
     * @param password Database password
     */
    public DatabaseTokenStore(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;

        logger.info("DatabaseTokenStore initialized with URL: {}", jdbcUrl);
        initializeSchema();
    }

    /**
     * Creates a database token store from environment variables.
     */
    public static DatabaseTokenStore fromEnvironment() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("DB_URL environment variable is required");
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("DB_USER environment variable is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("DB_PASSWORD environment variable is required");
        }

        return new DatabaseTokenStore(url, user, password);
    }

    /**
     * Initializes database schema if it doesn't exist.
     */
    private void initializeSchema() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS addon_tokens (
                workspace_id VARCHAR(255) PRIMARY KEY,
                auth_token TEXT NOT NULL,
                api_base_url VARCHAR(512) NOT NULL,
                created_at BIGINT NOT NULL,
                last_accessed_at BIGINT NOT NULL
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTable);
            logger.info("Database schema initialized successfully");

        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    @Override
    public void save(String workspaceId, String authToken, String apiBaseUrl) throws StorageException {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            throw new StorageException("workspaceId is required");
        }
        if (authToken == null || authToken.trim().isEmpty()) {
            throw new StorageException("authToken is required");
        }
        if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
            throw new StorageException("apiBaseUrl is required");
        }

        String sql = """
            INSERT INTO addon_tokens (workspace_id, auth_token, api_base_url, created_at, last_accessed_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (workspace_id)
            DO UPDATE SET
                auth_token = EXCLUDED.auth_token,
                api_base_url = EXCLUDED.api_base_url,
                last_accessed_at = EXCLUDED.last_accessed_at
            """;

        long now = System.currentTimeMillis();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, workspaceId.trim());
            stmt.setString(2, authToken.trim());
            stmt.setString(3, apiBaseUrl.trim());
            stmt.setLong(4, now);
            stmt.setLong(5, now);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Saved token for workspace: {} (rows affected: {})", workspaceId, rowsAffected);

        } catch (SQLException e) {
            logger.error("Failed to save token for workspace: " + workspaceId, e);
            throw new StorageException("Failed to save token", e);
        }
    }

    @Override
    public Optional<TokenData> get(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return Optional.empty();
        }

        String selectSql = "SELECT auth_token, api_base_url, created_at, last_accessed_at FROM addon_tokens WHERE workspace_id = ?";
        String updateSql = "UPDATE addon_tokens SET last_accessed_at = ? WHERE workspace_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setString(1, workspaceId.trim());
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String authToken = rs.getString("auth_token");
                String apiBaseUrl = rs.getString("api_base_url");
                long createdAt = rs.getLong("created_at");
                long lastAccessedAt = rs.getLong("last_accessed_at");

                // Update last accessed time
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    long now = System.currentTimeMillis();
                    updateStmt.setLong(1, now);
                    updateStmt.setString(2, workspaceId.trim());
                    updateStmt.executeUpdate();

                    logger.debug("Retrieved token for workspace: {}", workspaceId);
                    return Optional.of(new TokenData(workspaceId.trim(), authToken, apiBaseUrl, createdAt, now));
                }
            }

            logger.debug("No token found for workspace: {}", workspaceId);
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Failed to get token for workspace: " + workspaceId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return false;
        }

        String sql = "DELETE FROM addon_tokens WHERE workspace_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, workspaceId.trim());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Deleted token for workspace: {}", workspaceId);
                return true;
            } else {
                logger.debug("No token to delete for workspace: {}", workspaceId);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Failed to delete token for workspace: " + workspaceId, e);
            return false;
        }
    }

    @Override
    public boolean exists(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM addon_tokens WHERE workspace_id = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, workspaceId.trim());
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.error("Failed to check existence for workspace: " + workspaceId, e);
            return false;
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM addon_tokens";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Failed to count tokens", e);
            return 0;
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM addon_tokens";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(sql);
            logger.warn("Cleared all tokens ({} tokens removed)", rowsAffected);

        } catch (SQLException e) {
            logger.error("Failed to clear tokens", e);
        }
    }

    /**
     * Gets a database connection.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Cleanup method to close any resources (optional).
     */
    public void close() {
        logger.info("DatabaseTokenStore closed");
    }
}
