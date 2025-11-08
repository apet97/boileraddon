package com.clockify.addon.sdk.security;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory demo implementation. Not suitable for production.
 */
public class InMemoryTokenStore implements TokenStore {
    private final Map<String, String> tokenByWorkspace = new ConcurrentHashMap<>();

    @Override
    public void save(String workspaceId, String token) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (token == null) {
            throw new IllegalArgumentException("token is required");
        }
        tokenByWorkspace.put(workspaceId, token);
    }

    @Override
    public Optional<String> get(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return Optional.empty();
        return Optional.ofNullable(tokenByWorkspace.get(workspaceId));
    }

    @Override
    public void remove(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        tokenByWorkspace.remove(workspaceId);
    }
}

