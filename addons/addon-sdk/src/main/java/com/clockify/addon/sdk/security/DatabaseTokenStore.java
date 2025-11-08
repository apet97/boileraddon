package com.clockify.addon.sdk.security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Minimal JDBC-based TokenStore implementation suitable for production use.
 *
 * Notes:
 * - Uses a simple table named {@code addon_tokens} with columns
 *   (workspace_id VARCHAR PRIMARY KEY, auth_token TEXT, created_at BIGINT, last_accessed_at BIGINT, api_base_url VARCHAR).
 * - Opens a connection per operation. For production, prefer a connection pool (e.g., HikariCP).
 * - Does not manage schema migrations; will attempt to create the table if it does not exist.
 */
public class DatabaseTokenStore implements TokenStoreSPI {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseTokenStore(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.username = username;
        this.password = password;
        ensureTable();
    }

    /** Construct from environment variables: DB_URL, DB_USER, DB_PASSWORD. */
    public static DatabaseTokenStore fromEnvironment() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASSWORD");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL is required to use DatabaseTokenStore");
        }
        return new DatabaseTokenStore(url, user, pass);
    }

    @Override
    public void save(String workspaceId, String token) {
        if (workspaceId == null || workspaceId.isBlank()) throw new IllegalArgumentException("workspaceId required");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token required");
        long now = System.currentTimeMillis();
        String upsert = "INSERT INTO addon_tokens (workspace_id, auth_token, created_at, last_accessed_at) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (workspace_id) DO UPDATE SET auth_token = EXCLUDED.auth_token, last_accessed_at = EXCLUDED.last_accessed_at";
        // MySQL fallback will ignore ON CONFLICT; users can adapt schema/SQL as needed.
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                ps.setString(1, workspaceId);
                ps.setString(2, token);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                // Fallback to separate insert/update for drivers without ON CONFLICT support
                if (!tryUpdate(c, workspaceId, token, now)) {
                    tryInsert(c, workspaceId, token, now);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }

    @Override
    public Optional<String> get(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return Optional.empty();
        String sql = "SELECT auth_token FROM addon_tokens WHERE workspace_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, workspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch token", e);
        }
        return Optional.empty();
    }

    @Override
    public void remove(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return;
        String sql = "DELETE FROM addon_tokens WHERE workspace_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, workspaceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete token", e);
        }
    }

    /** Simple utility for health checks: returns number of rows. */
    public long count() {
        String sql = "SELECT COUNT(*) FROM addon_tokens";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count tokens", e);
        }
    }

    private boolean tryUpdate(Connection c, String ws, String token, long now) {
        String upd = "UPDATE addon_tokens SET auth_token = ?, last_accessed_at = ? WHERE workspace_id = ?";
        try (PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, token);
            ps.setLong(2, now);
            ps.setString(3, ws);
            int n = ps.executeUpdate();
            return n > 0;
        } catch (SQLException ex) {
            return false;
        }
    }

    private void tryInsert(Connection c, String ws, String token, long now) throws SQLException {
        String ins = "INSERT INTO addon_tokens (workspace_id, auth_token, created_at, last_accessed_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setString(1, ws);
            ps.setString(2, token);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        if (username != null) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private void ensureTable() {
        String ddlPg = "CREATE TABLE IF NOT EXISTS addon_tokens (" +
                "workspace_id VARCHAR(255) PRIMARY KEY, " +
                "auth_token TEXT NOT NULL, " +
                "api_base_url VARCHAR(512), " +
                "created_at BIGINT NOT NULL, " +
                "last_accessed_at BIGINT NOT NULL)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(ddlPg)) {
            ps.execute();
        } catch (SQLException ignored) {
            // Best effort; allow external schema management
        }
    }
}

