package com.clockify.addon.sdk.security;

import java.util.Optional;

/**
 * TokenStoreSPI defines the contract for storing per-workspace installation tokens.
 * Implementations must be thread-safe.
 *
 * SECURITY: Token rotation support for long-lived secrets.
 * Supports graceful token transitions with dual-token acceptance.
 */
public interface TokenStoreSPI {
    /** Save or update the installation token for a workspace. */
    void save(String workspaceId, String token);

    /** Retrieve the installation token for a workspace. */
    Optional<String> get(String workspaceId);

    /** Remove the installation token for a workspace. */
    void remove(String workspaceId);

    /**
     * SECURITY: Rotate token to a new value while keeping old token briefly.
     * Allows graceful transition when tokens are updated.
     *
     * @param workspaceId workspace identifier
     * @param newToken the new token to use
     * @throws UnsupportedOperationException if implementation doesn't support rotation
     */
    default void rotate(String workspaceId, String newToken) {
        throw new UnsupportedOperationException("Token rotation not supported by this TokenStore implementation");
    }

    /**
     * SECURITY: Checks if token is valid (either current or recent previous).
     * Used to validate webhook signatures during token rotation period.
     *
     * @param workspaceId workspace identifier
     * @param token token to validate
     * @return true if token is current or recently rotated (within grace period)
     */
    default boolean isValidToken(String workspaceId, String token) {
        Optional<String> current = get(workspaceId);
        return current.isPresent() && current.get().equals(token);
    }

    /**
     * SECURITY: Gets rotation metadata (last rotated time, grace period remaining).
     * Useful for monitoring token rotation status.
     *
     * @param workspaceId workspace identifier
     * @return rotation metadata or empty if no metadata available
     */
    default Optional<RotationMetadata> getRotationMetadata(String workspaceId) {
        return Optional.empty();
    }

    /**
     * Metadata about token rotation state.
     */
    class RotationMetadata {
        public final long lastRotatedAt;
        public final long gracePeriodEndAt;

        public RotationMetadata(long lastRotatedAt, long gracePeriodEndAt) {
            this.lastRotatedAt = lastRotatedAt;
            this.gracePeriodEndAt = gracePeriodEndAt;
        }

        public boolean isInGracePeriod() {
            return System.currentTimeMillis() < gracePeriodEndAt;
        }
    }
}
