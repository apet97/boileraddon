package com.clockify.addon.sdk.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory implementation of token storage.
 *
 * WARNING: This implementation is suitable for development and testing only.
 * Tokens are lost on application restart. For production use, implement a
 * persistent storage backend (database, Redis, secret manager, etc.).
 */
public class InMemoryTokenStore implements ITokenStore {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryTokenStore.class);
    private final ConcurrentMap<String, TokenData> tokens = new ConcurrentHashMap<>();

    public InMemoryTokenStore() {
        logger.warn("Using InMemoryTokenStore - NOT SUITABLE FOR PRODUCTION. Tokens will be lost on restart!");
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

        String normalizedWorkspaceId = workspaceId.trim();
        TokenData tokenData = new TokenData(normalizedWorkspaceId, authToken.trim(), apiBaseUrl.trim());

        tokens.put(normalizedWorkspaceId, tokenData);
        logger.info("Saved token for workspace: {} (total: {})", normalizedWorkspaceId, tokens.size());
    }

    @Override
    public Optional<TokenData> get(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedWorkspaceId = workspaceId.trim();
        TokenData tokenData = tokens.get(normalizedWorkspaceId);

        if (tokenData != null) {
            // Update last accessed time
            TokenData updated = tokenData.withUpdatedAccessTime();
            tokens.put(normalizedWorkspaceId, updated);
            logger.debug("Retrieved token for workspace: {}", normalizedWorkspaceId);
            return Optional.of(updated);
        }

        logger.debug("No token found for workspace: {}", normalizedWorkspaceId);
        return Optional.empty();
    }

    @Override
    public boolean delete(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return false;
        }

        String normalizedWorkspaceId = workspaceId.trim();
        boolean removed = tokens.remove(normalizedWorkspaceId) != null;

        if (removed) {
            logger.info("Deleted token for workspace: {} (remaining: {})", normalizedWorkspaceId, tokens.size());
        } else {
            logger.debug("No token to delete for workspace: {}", normalizedWorkspaceId);
        }

        return removed;
    }

    @Override
    public boolean exists(String workspaceId) {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            return false;
        }
        return tokens.containsKey(workspaceId.trim());
    }

    @Override
    public int count() {
        return tokens.size();
    }

    @Override
    public void clear() {
        int count = tokens.size();
        tokens.clear();
        logger.warn("Cleared all tokens ({} tokens removed)", count);
    }
}
