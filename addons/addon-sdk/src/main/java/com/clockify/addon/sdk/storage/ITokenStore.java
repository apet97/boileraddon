package com.clockify.addon.sdk.storage;

import java.util.Optional;

/**
 * Interface for storing and retrieving addon tokens.
 * Implementations can use in-memory storage, databases, or secret managers.
 */
public interface ITokenStore {

    /**
     * Saves authentication token for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @param authToken The authentication token
     * @param apiBaseUrl The API base URL for this workspace
     * @throws StorageException if save operation fails
     */
    void save(String workspaceId, String authToken, String apiBaseUrl) throws StorageException;

    /**
     * Retrieves authentication token for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @return Optional containing TokenData if found
     */
    Optional<TokenData> get(String workspaceId);

    /**
     * Deletes authentication token for a workspace (on uninstall).
     *
     * @param workspaceId The workspace identifier
     * @return true if token was deleted, false if not found
     */
    boolean delete(String workspaceId);

    /**
     * Checks if a token exists for a workspace.
     *
     * @param workspaceId The workspace identifier
     * @return true if token exists
     */
    boolean exists(String workspaceId);

    /**
     * Gets the number of stored tokens.
     *
     * @return Count of stored tokens
     */
    int count();

    /**
     * Clears all tokens (use with caution!).
     */
    void clear();

    /**
     * Data class holding token information.
     */
    class TokenData {
        private final String workspaceId;
        private final String authToken;
        private final String apiBaseUrl;
        private final long createdAt;
        private final long lastAccessedAt;

        public TokenData(String workspaceId, String authToken, String apiBaseUrl) {
            this(workspaceId, authToken, apiBaseUrl, System.currentTimeMillis(), System.currentTimeMillis());
        }

        public TokenData(String workspaceId, String authToken, String apiBaseUrl, long createdAt, long lastAccessedAt) {
            this.workspaceId = workspaceId;
            this.authToken = authToken;
            this.apiBaseUrl = apiBaseUrl;
            this.createdAt = createdAt;
            this.lastAccessedAt = lastAccessedAt;
        }

        public String getWorkspaceId() { return workspaceId; }
        public String getAuthToken() { return authToken; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public long getCreatedAt() { return createdAt; }
        public long getLastAccessedAt() { return lastAccessedAt; }

        public TokenData withUpdatedAccessTime() {
            return new TokenData(workspaceId, authToken, apiBaseUrl, createdAt, System.currentTimeMillis());
        }

        @Override
        public String toString() {
            return String.format("TokenData{workspace=%s, apiBaseUrl=%s, created=%d}",
                    workspaceId, apiBaseUrl, createdAt);
        }
    }

    /**
     * Exception thrown when storage operations fail.
     */
    class StorageException extends Exception {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
