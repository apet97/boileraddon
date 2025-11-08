package com.clockify.addon.sdk.security;

import java.util.Optional;

/**
 * TokenStoreSPI defines the minimal contract for storing per-workspace installation tokens.
 * Implementations must be thread-safe.
 */
public interface TokenStoreSPI {
    /** Save or update the installation token for a workspace. */
    void save(String workspaceId, String token);

    /** Retrieve the installation token for a workspace. */
    Optional<String> get(String workspaceId);

    /** Remove the installation token for a workspace. */
    void remove(String workspaceId);
}
