package com.clockify.addon.sdk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static facade used by demo modules and tests.
 * Stores a WorkspaceToken consisting of the installation token and normalized apiBaseUrl.
 *
 * Default behavior is in-memory. For production, implement a TokenStoreSPI (e.g., DatabaseTokenStore)
 * and wire it at your application entry if needed.
 */
public final class TokenStore {
    private static final Logger logger = LoggerFactory.getLogger(TokenStore.class);
    private static final ObjectMapper PERSISTENCE_MAPPER = new ObjectMapper();

    private TokenStore() {}

    /** Workspace token shape used by demo modules. */
    public record WorkspaceToken(String token, String apiBaseUrl, long createdAt, long expiresAt, long rotatedAt) {}

    private static final Map<String, WorkspaceToken> STORE = new ConcurrentHashMap<>();
    private static final Map<String, WorkspaceToken> ROTATED = new ConcurrentHashMap<>();
    private static volatile TokenStoreSPI persistentStore;

    private static Clock clock = Clock.systemUTC();

    public static void setClock(Clock custom) { clock = custom == null ? Clock.systemUTC() : custom; }
    public static void resetClock() { clock = Clock.systemUTC(); }

    private static final long DEFAULT_TOKEN_TTL_MS = Duration.ofHours(24).toMillis();
    private static final long DEFAULT_ROTATION_GRACE_MS = Duration.ofMinutes(15).toMillis();
    private static final String TTL_PROPERTY = "clockify.token.ttl.ms";
    private static final String GRACE_PROPERTY = "clockify.token.rotation.grace.ms";

    /** Clears all stored tokens (used by tests). */
    public static void clear() {
        STORE.clear();
        ROTATED.clear();
    }

    /**
     * Configure an optional persistent store. When configured, tokens are serialized to the
     * backing store (e.g., PostgreSQL) and lazily restored on first access.
     */
    public static void configurePersistence(TokenStoreSPI store) {
        persistentStore = store;
        if (store != null) {
            logger.info("TokenStore persistence enabled via {}", store.getClass().getSimpleName());
        } else {
            logger.info("TokenStore persistence disabled");
        }
    }

    /** Save workspace token and normalize apiBaseUrl to include /api/vN if missing. */
    public static void save(String workspaceId, String token, String apiBaseUrl) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        String normalized = normalizeApiBaseUrl(apiBaseUrl);
        long now = Instant.now(clock).toEpochMilli();
        ROTATED.remove(workspaceId);
        saveRecord(workspaceId, token, normalized, now, true);
    }

    /** Fetch workspace token. */
    public static Optional<WorkspaceToken> get(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return Optional.empty();
        WorkspaceToken current = STORE.get(workspaceId);
        if (current != null && !isExpired(current)) {
            return Optional.of(current);
        }

        WorkspaceToken previous = ROTATED.get(workspaceId);
        if (previous != null && !isExpired(previous)) {
            return Optional.of(previous);
        }

        return restoreFromPersistence(workspaceId);
    }

    /** Checks whether the provided token is currently valid (current or rotated). */
    public static boolean isValidToken(String workspaceId, String token) {
        if (workspaceId == null || workspaceId.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        WorkspaceToken current = STORE.get(workspaceId);
        if (current != null && !isExpired(current) && current.token().equals(token)) {
            return true;
        }
        WorkspaceToken previous = ROTATED.get(workspaceId);
        return previous != null && !isExpired(previous) && previous.token().equals(token);
    }

    /** Remove workspace token. Returns true if a token was removed. */
    public static boolean delete(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return false;
        ROTATED.remove(workspaceId);
        boolean removed = STORE.remove(workspaceId) != null;
        if (persistentStore != null) {
            try {
                persistentStore.remove(workspaceId);
            } catch (Exception e) {
                logger.warn("Failed to delete token for {} from persistent store: {}", workspaceId, e.getMessage());
            }
        }
        if (removed) {
            AuditLogger.log(AuditLogger.AuditEvent.TOKEN_REMOVED)
                    .workspace(workspaceId)
                    .info();
        }
        return removed;
    }

    /**
     * Rotates the workspace token while keeping the previous token valid for the grace period.
     */
    public static void rotate(String workspaceId, String newToken) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalArgumentException("new token is required");
        }

        WorkspaceToken current = STORE.get(workspaceId);
        long now = Instant.now(clock).toEpochMilli();

        if (current != null) {
            long grace = rotationGraceMs();
            WorkspaceToken rotated = new WorkspaceToken(
                    current.token(),
                    current.apiBaseUrl(),
                    current.createdAt(),
                    now + grace,
                    now
            );
            ROTATED.put(workspaceId, rotated);
        }

        String apiBaseUrl = current != null ? current.apiBaseUrl() : normalizeApiBaseUrl(null);
        saveRecord(workspaceId, newToken, apiBaseUrl, now, false);
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_ROTATED)
                .workspace(workspaceId)
                .detail("rotatedAt", now)
                .info();
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_SAVED)
                .workspace(workspaceId)
                .detail("apiBaseUrl", apiBaseUrl)
                .detail("expiresAt", now + tokenTtlMs())
                .info();
        persistState(workspaceId);
    }

    private static void saveRecord(String workspaceId, String token, String apiBaseUrl, long now, boolean log) {
        long ttl = tokenTtlMs();
        long expiresAt = now + ttl;
        STORE.put(workspaceId, new WorkspaceToken(token, apiBaseUrl, now, expiresAt, now));
        if (log) {
            AuditLogger.log(AuditLogger.AuditEvent.TOKEN_SAVED)
                    .workspace(workspaceId)
                    .detail("apiBaseUrl", apiBaseUrl)
                    .detail("expiresAt", expiresAt)
                    .info();
        }
        persistState(workspaceId);
    }

    private static long tokenTtlMs() {
        return parseLongProperty(TTL_PROPERTY, DEFAULT_TOKEN_TTL_MS);
    }

    private static long rotationGraceMs() {
        return parseLongProperty(GRACE_PROPERTY, DEFAULT_ROTATION_GRACE_MS);
    }

    private static long parseLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            try {
                long parsed = Long.parseLong(value.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static boolean isExpired(WorkspaceToken token) {
        return token.expiresAt() > 0 && currentTimeMs() > token.expiresAt();
    }

    private static long currentTimeMs() {
        return Instant.now(clock).toEpochMilli();
    }

    private static String normalizeApiBaseUrl(String apiBaseUrl) {
        String DEFAULT = "https://api.clockify.me/api/v1";
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return DEFAULT;
        }
        try {
            URI uri = URI.create(apiBaseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();

            // trim trailing slash
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

            // if already contains /api/v<digits>, keep as-is
            if (path.matches(".*/api/v\\d+")) {
                return rebuild(scheme, host, port, path);
            }
            // if contains /api (no version), append /v1
            if (path.endsWith("/api") || path.contains("/api")) {
                return rebuild(scheme, host, port, path + "/v1");
            }
            // otherwise append /api/v1
            String newPath = (path.isEmpty() ? "/api/v1" : path + "/api/v1");
            return rebuild(scheme, host, port, newPath);
        } catch (Exception e) {
            // Fallback to default on malformed input
            return DEFAULT;
        }
    }

    private static String rebuild(String scheme, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme != null ? scheme : "https").append("://").append(host);
        if (port > 0 && port != 80 && port != 443) sb.append(":" + port);
        sb.append(path);
        return sb.toString();
    }

    private static void persistState(String workspaceId) {
        if (persistentStore == null || workspaceId == null || workspaceId.isBlank()) {
            return;
        }
        try {
            PersistentState state = new PersistentState(STORE.get(workspaceId), ROTATED.get(workspaceId));
            String payload = PERSISTENCE_MAPPER.writeValueAsString(state);
            persistentStore.save(workspaceId, payload);
        } catch (Exception e) {
            logger.warn("Failed to persist token for {}: {}", workspaceId, e.getMessage());
        }
    }

    private static Optional<WorkspaceToken> restoreFromPersistence(String workspaceId) {
        if (persistentStore == null || workspaceId == null || workspaceId.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<String> raw = persistentStore.get(workspaceId);
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            String serialized = raw.get();
            if (looksLikeJson(serialized)) {
                PersistentState state = PERSISTENCE_MAPPER.readValue(serialized, PersistentState.class);
                if (state.current != null) {
                    STORE.put(workspaceId, state.current);
                }
                if (state.rotated != null) {
                    ROTATED.put(workspaceId, state.rotated);
                }
                WorkspaceToken current = STORE.get(workspaceId);
                if (current != null && !isExpired(current)) {
                    return Optional.of(current);
                }
                WorkspaceToken rotated = ROTATED.get(workspaceId);
                if (rotated != null && !isExpired(rotated)) {
                    return Optional.of(rotated);
                }
            } else {
                // Legacy format: plain token string without metadata
                WorkspaceToken token = new WorkspaceToken(serialized, normalizeApiBaseUrl(null),
                        currentTimeMs(), 0, 0);
                STORE.put(workspaceId, token);
                return Optional.of(token);
            }
        } catch (Exception e) {
            logger.warn("Failed to restore token for {} from persistent store: {}", workspaceId, e.getMessage());
        }
        return Optional.empty();
    }

    private static boolean looksLikeJson(String data) {
        if (data == null) return false;
        String trimmed = data.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private static class PersistentState {
        public WorkspaceToken current;
        public WorkspaceToken rotated;

        public PersistentState() {
        }

        public PersistentState(WorkspaceToken current, WorkspaceToken rotated) {
            this.current = current;
            this.rotated = rotated;
        }
    }
}
