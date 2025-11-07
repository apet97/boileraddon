package com.example.autotagassistant;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store for workspace auth context.
 *
 * Stores the Clockify auth token and API base URL obtained during the
 * INSTALLED lifecycle event so webhook handlers can create API clients.
 */
public class WorkspaceTokenStore {
    private static final WorkspaceTokenStore INSTANCE = new WorkspaceTokenStore();

    private final Map<String, WorkspaceToken> tokens = new ConcurrentHashMap<>();

    public static WorkspaceTokenStore getInstance() {
        return INSTANCE;
    }

    private WorkspaceTokenStore() {
    }

    public void save(String workspaceId, String authToken, String apiBaseUrl) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId must not be blank");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("authToken must not be blank");
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw new IllegalArgumentException("apiBaseUrl must not be blank");
        }
        tokens.put(workspaceId, new WorkspaceToken(workspaceId, authToken, apiBaseUrl));
    }

    public Optional<WorkspaceToken> find(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokens.get(workspaceId));
    }

    public void delete(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return;
        }
        tokens.remove(workspaceId);
    }

    /**
     * Workspace auth context.
     */
    public static final class WorkspaceToken {
        private final String workspaceId;
        private final String authToken;
        private final String apiBaseUrl;

        private WorkspaceToken(String workspaceId, String authToken, String apiBaseUrl) {
            this.workspaceId = workspaceId;
            this.authToken = authToken;
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }

        public String getAuthToken() {
            return authToken;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }
    }
}
