package com.clockify.addon.sdk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * Example JDBC-backed TokenStore. Requires a JDBC driver on the runtime classpath.
 * Provides a basic schema and naive upsert; adapt for your target database.
 */
public class DatabaseTokenStore implements TokenStoreSPI {
    private static final Logger log = LoggerFactory.getLogger(DatabaseTokenStore.class);

    private final String url;
    private final String username;
    private final String password;

    public DatabaseTokenStore(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        ensureSchema();
    }

    public static DatabaseTokenStore fromEnvironment() {
        String url = getenvRequired("DB_URL");
        String user = System.getenv().getOrDefault("DB_USERNAME", "");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "");
        return new DatabaseTokenStore(url, user, pass);
    }

    @Override
    public void save(String workspaceId, String token) {
        try (Connection c = getConnection()) {
            // Try update first
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE addon_tokens SET token = ? WHERE workspace_id = ?")) {
                ps.setString(1, token);
                ps.setString(2, workspaceId);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    // Insert
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO addon_tokens(workspace_id, token) VALUES(?, ?)")) {
                        ins.setString(1, workspaceId);
                        ins.setString(2, token);
                        ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }

    @Override
    public Optional<String> get(String workspaceId) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT token FROM addon_tokens WHERE workspace_id = ?")) {
            ps.setString(1, workspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch token", e);
        }
    }

    @Override
    public void remove(String workspaceId) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM addon_tokens WHERE workspace_id = ?")) {
            ps.setString(1, workspaceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove token", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return (username == null || username.isEmpty())
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, username, password);
    }

    private void ensureSchema() {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS addon_tokens (" +
                    "workspace_id VARCHAR(128) PRIMARY KEY, " +
                    "token TEXT NOT NULL)");
        } catch (SQLException e) {
            log.warn("Could not ensure schema for addon_tokens: {}", e.getMessage());
        }
    }

    private static String getenvRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env: " + key);
        }
        return v;
    }
}
