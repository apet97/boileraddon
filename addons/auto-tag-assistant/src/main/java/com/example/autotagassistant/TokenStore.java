package com.example.autotagassistant;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory store for workspace authentication tokens and API base URLs.
 *
 * <p>This store is intended for demo purposes only. Production implementations should
 * persist credentials in a secure datastore.</p>
 */
public final class TokenStore {
    private static final String DEFAULT_API_BASE_URL = "https://api.clockify.me/api/v1";
    private static final ConcurrentMap<String, WorkspaceToken> TOKENS = new ConcurrentHashMap<>();

    private TokenStore() {
        // Utility class
    }

    /**
     * Save or update the token information for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @param authToken   The workspace-scoped auth token
     * @param apiBaseUrl  The API base URL (optional)
     */
    public static void save(String workspaceId, String authToken, String apiBaseUrl) {
        String normalizedWorkspaceId = normalizeWorkspaceId(workspaceId);
        if (normalizedWorkspaceId == null) {
            throw new IllegalArgumentException("workspaceId is required");
        }

        String normalizedToken = normalizeToken(authToken);
        if (normalizedToken == null) {
            throw new IllegalArgumentException("authToken is required");
        }

        String normalizedBaseUrl = normalizeApiBaseUrl(apiBaseUrl);
        TOKENS.put(normalizedWorkspaceId, new WorkspaceToken(normalizedToken, normalizedBaseUrl));
    }

    /**
     * Retrieve stored token information for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @return Optional containing the token data if present
     */
    public static Optional<WorkspaceToken> get(String workspaceId) {
        String normalizedWorkspaceId = normalizeWorkspaceId(workspaceId);
        if (normalizedWorkspaceId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TOKENS.get(normalizedWorkspaceId));
    }

    /**
     * Remove stored credentials for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @return {@code true} if credentials were removed; {@code false} otherwise
     */
    public static boolean delete(String workspaceId) {
        String normalizedWorkspaceId = normalizeWorkspaceId(workspaceId);
        if (normalizedWorkspaceId == null) {
            return false;
        }
        return TOKENS.remove(normalizedWorkspaceId) != null;
    }

    /**
     * Clear all stored credentials. Primarily intended for tests.
     */
    public static void clear() {
        TOKENS.clear();
    }

    private static String normalizeWorkspaceId(String workspaceId) {
        if (workspaceId == null) {
            return null;
        }
        String trimmed = workspaceId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeToken(String authToken) {
        if (authToken == null) {
            return null;
        }
        String trimmed = authToken.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeApiBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null) {
            return DEFAULT_API_BASE_URL;
        }
        String trimmed = apiBaseUrl.trim();
        return trimmed.isEmpty() ? DEFAULT_API_BASE_URL : trimmed;
    }

    /**
     * Represents stored credentials for a workspace.
     */
    public static final class WorkspaceToken {
        private final String authToken;
        private final String apiBaseUrl;

        private WorkspaceToken(String authToken, String apiBaseUrl) {
            this.authToken = Objects.requireNonNull(authToken, "authToken");
            this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl, "apiBaseUrl");
        }

        public String authToken() {
            return authToken;
        }

        public String apiBaseUrl() {
            return apiBaseUrl;
        }
    }
}
