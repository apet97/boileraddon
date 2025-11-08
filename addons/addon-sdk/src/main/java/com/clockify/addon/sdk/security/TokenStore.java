package com.clockify.addon.sdk.security;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory demo token store. For production, replace with a persistent store.
 */
public final class TokenStore {
    private static final String DEFAULT_API_BASE_URL = "https://api.clockify.me/api/v1";
    private static final ConcurrentMap<String, WorkspaceToken> TOKENS = new ConcurrentHashMap<>();

    private TokenStore() {}

    public static void save(String workspaceId, String authToken, String apiBaseUrl) {
        String ws = normalize(workspaceId);
        String token = normalize(authToken);
        if (ws == null || token == null) throw new IllegalArgumentException("workspaceId and authToken are required");
        String base = normalizeApiBaseUrl(apiBaseUrl);
        TOKENS.put(ws, new WorkspaceToken(token, base));
    }

    public static Optional<WorkspaceToken> get(String workspaceId) {
        String ws = normalize(workspaceId);
        return ws == null ? Optional.empty() : Optional.ofNullable(TOKENS.get(ws));
    }

    public static boolean delete(String workspaceId) {
        String ws = normalize(workspaceId);
        return ws != null && TOKENS.remove(ws) != null;
    }

    public static void clear() {
        TOKENS.clear();
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeApiBaseUrl(String apiBaseUrl) {
        String trimmed = apiBaseUrl == null ? "" : apiBaseUrl.trim();
        String base = trimmed.isEmpty() ? DEFAULT_API_BASE_URL : trimmed;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/api")) return base + "/v1";
        if (base.matches(".*/api/v\\d+$")) return base;
        return base + "/api/v1";
    }

    public static final class WorkspaceToken {
        private final String authToken;
        private final String apiBaseUrl;

        public WorkspaceToken(String authToken, String apiBaseUrl) {
            this.authToken = Objects.requireNonNull(authToken, "authToken");
            this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl, "apiBaseUrl");
        }

        public String authToken() { return authToken; }
        public String apiBaseUrl() { return apiBaseUrl; }
    }
}

