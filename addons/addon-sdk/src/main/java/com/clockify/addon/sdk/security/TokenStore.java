package com.clockify.addon.sdk.security;

import java.net.URI;
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
    private TokenStore() {}

    /** Workspace token shape used by demo modules. */
    public record WorkspaceToken(String token, String apiBaseUrl) {}

    private static final Map<String, WorkspaceToken> STORE = new ConcurrentHashMap<>();

    /** Clears all stored tokens (used by tests). */
    public static void clear() { STORE.clear(); }

    /** Save workspace token and normalize apiBaseUrl to include /api/vN if missing. */
    public static void save(String workspaceId, String token, String apiBaseUrl) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        String normalized = normalizeApiBaseUrl(apiBaseUrl);
        STORE.put(workspaceId, new WorkspaceToken(token, normalized));
    }

    /** Fetch workspace token. */
    public static Optional<WorkspaceToken> get(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return Optional.empty();
        return Optional.ofNullable(STORE.get(workspaceId));
    }

    /** Remove workspace token. Returns true if a token was removed. */
    public static boolean delete(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) return false;
        return STORE.remove(workspaceId) != null;
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
}

