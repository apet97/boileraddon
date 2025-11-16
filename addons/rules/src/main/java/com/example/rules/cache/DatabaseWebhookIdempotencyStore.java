package com.example.rules.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Database-backed dedup store suitable for multi-node deployments.
 */
public final class DatabaseWebhookIdempotencyStore implements WebhookIdempotencyStore {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseWebhookIdempotencyStore.class);
    private static final long DEFAULT_CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    private final String url;
    private final String username;
    private final String password;
    private final long cleanupIntervalMillis;
    private final AtomicLong nextCleanup = new AtomicLong(0);

    public DatabaseWebhookIdempotencyStore(String url, String username, String password) {
        this(url, username, password, DEFAULT_CLEANUP_INTERVAL_MS);
    }

    DatabaseWebhookIdempotencyStore(String url, String username, String password, long cleanupIntervalMillis) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL is required for webhook dedup store");
        }
        this.url = url;
        this.username = username;
        this.password = password;
        this.cleanupIntervalMillis = cleanupIntervalMillis;
        ensureSchema();
    }

    @Override
    public boolean isDuplicate(String workspaceId, String eventType, String dedupKey, long ttlMillis) {
        maybeCleanup();
        long now = System.currentTimeMillis();
        long expiresAt = now + ttlMillis;
        try (Connection connection = conn()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO webhook_dedup (workspace_id, event_type, dedup_key, expires_at) VALUES (?,?,?,?)")) {
                insert.setString(1, workspaceId);
                insert.setString(2, eventType);
                insert.setString(3, dedupKey);
                insert.setLong(4, expiresAt);
                insert.executeUpdate();
                return false;
            } catch (SQLException insertError) {
                if (!isConstraintViolation(insertError)) {
                    throw insertError;
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE webhook_dedup SET expires_at=? WHERE workspace_id=? AND event_type=? AND dedup_key=? AND expires_at < ?")) {
                    update.setLong(1, expiresAt);
                    update.setString(2, workspaceId);
                    update.setString(3, eventType);
                    update.setString(4, dedupKey);
                    update.setLong(5, now);
                    int rows = update.executeUpdate();
                    if (rows > 0) {
                        return false;
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to evaluate webhook idempotency", e);
        }
    }

    @Override
    public void clear() {
        try (Connection connection = conn(); Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM webhook_dedup");
        } catch (SQLException e) {
            logger.warn("Failed to clear webhook_dedup entries: {}", e.getMessage());
        }
    }

    private Connection conn() throws SQLException {
        if (username == null || username.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    private void ensureSchema() {
        try (Connection connection = conn(); Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS webhook_dedup (
                        workspace_id VARCHAR(128) NOT NULL,
                        event_type   VARCHAR(128) NOT NULL,
                        dedup_key    VARCHAR(256) NOT NULL,
                        expires_at   BIGINT NOT NULL,
                        PRIMARY KEY (workspace_id, event_type, dedup_key)
                    )
                    """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS webhook_dedup_expires_idx ON webhook_dedup (expires_at)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to ensure webhook_dedup schema", e);
        }
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        long target = nextCleanup.get();
        if (now < target && target != 0) {
            return;
        }
        if (!nextCleanup.compareAndSet(target, now + cleanupIntervalMillis)) {
            return;
        }
        try (Connection connection = conn();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM webhook_dedup WHERE expires_at < ?")) {
            ps.setLong(1, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to purge expired webhook dedup entries: {}", e.getMessage());
        }
    }

    private static boolean isConstraintViolation(SQLException e) {
        String sqlState = e.getSQLState();
        if (sqlState == null) {
            return false;
        }
        String normalized = sqlState.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("23");
    }
}
