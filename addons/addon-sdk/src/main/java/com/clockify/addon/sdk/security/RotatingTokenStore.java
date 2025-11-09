package com.clockify.addon.sdk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SECURITY: Token rotation wrapper that adds graceful token transition support.
 *
 * Allows applications to rotate tokens to new values while still accepting
 * the previous token for a configurable grace period. This prevents immediate
 * breakage when tokens are updated in Clockify workspace settings.
 *
 * Usage:
 * <pre>{@code
 * TokenStoreSPI baseStore = new DatabaseTokenStore(url, user, pass);
 * TokenStoreSPI rotatingStore = new RotatingTokenStore(baseStore);
 * rotatingStore.rotate("workspace-1", "new-token");  // Supports old and new for grace period
 * }</pre>
 *
 * Thread-safe: Uses ConcurrentHashMap for rotation state tracking.
 */
public class RotatingTokenStore implements TokenStoreSPI {
    private static final Logger logger = LoggerFactory.getLogger(RotatingTokenStore.class);

    private final TokenStoreSPI delegate;
    private final long gracePeriodMs;  // How long to accept old token

    // Track rotation state: workspaceId -> {previousToken, rotatedAt}
    private final Map<String, RotationState> rotationState = new ConcurrentHashMap<>();

    /**
     * Default grace period: 1 hour (3600000 ms)
     * Enough time for all running instances to pick up the new token
     */
    private static final long DEFAULT_GRACE_PERIOD_MS = 3600000;

    /**
     * Creates a rotating token store with default grace period (1 hour).
     *
     * @param delegate the underlying token store to wrap
     */
    public RotatingTokenStore(TokenStoreSPI delegate) {
        this(delegate, DEFAULT_GRACE_PERIOD_MS);
    }

    /**
     * Creates a rotating token store with custom grace period.
     *
     * @param delegate the underlying token store to wrap
     * @param gracePeriodMs milliseconds to accept old token after rotation
     */
    public RotatingTokenStore(TokenStoreSPI delegate, long gracePeriodMs) {
        this.delegate = delegate;
        this.gracePeriodMs = gracePeriodMs;
        logger.info("RotatingTokenStore initialized with grace period: {}ms", gracePeriodMs);
    }

    @Override
    public void save(String workspaceId, String token) {
        delegate.save(workspaceId, token);
        // Clear rotation state when saving (not a rotation)
        rotationState.remove(workspaceId);
    }

    @Override
    public Optional<String> get(String workspaceId) {
        return delegate.get(workspaceId);
    }

    @Override
    public void remove(String workspaceId) {
        delegate.remove(workspaceId);
        rotationState.remove(workspaceId);
    }

    /**
     * SECURITY: Rotates token to new value while keeping old value in grace period.
     *
     * @param workspaceId workspace identifier
     * @param newToken the new token to use
     */
    @Override
    public void rotate(String workspaceId, String newToken) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId required");
        }
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalArgumentException("newToken required");
        }

        // Get current token before updating
        Optional<String> currentOpt = delegate.get(workspaceId);

        // Save new token
        delegate.save(workspaceId, newToken);

        // Track old token in rotation state
        if (currentOpt.isPresent()) {
            String previousToken = currentOpt.get();
            long now = System.currentTimeMillis();
            RotationState state = new RotationState(previousToken, now, now + gracePeriodMs);
            rotationState.put(workspaceId, state);
            logger.info("Token rotated for workspace: {} (grace period: {}ms)", workspaceId, gracePeriodMs);
        } else {
            // No previous token, just clear rotation state
            rotationState.remove(workspaceId);
        }
    }

    /**
     * SECURITY: Validates token against current and recent previous tokens.
     *
     * @param workspaceId workspace identifier
     * @param token token to validate
     * @return true if token matches current OR previous (within grace period)
     */
    @Override
    public boolean isValidToken(String workspaceId, String token) {
        if (token == null) {
            return false;
        }

        // Check current token
        Optional<String> current = delegate.get(workspaceId);
        if (current.isPresent() && current.get().equals(token)) {
            return true;
        }

        // Check previous token (if in grace period)
        RotationState state = rotationState.get(workspaceId);
        if (state != null && state.isInGracePeriod()) {
            if (state.previousToken.equals(token)) {
                logger.debug("Token validation: accepting previous token for workspace: {} (in grace period)",
                        workspaceId);
                return true;
            }
        } else if (state != null) {
            // Grace period expired, clean up rotation state
            rotationState.remove(workspaceId);
        }

        return false;
    }

    /**
     * SECURITY: Gets rotation metadata for monitoring.
     *
     * @param workspaceId workspace identifier
     * @return rotation metadata if token is currently rotating
     */
    @Override
    public Optional<RotationMetadata> getRotationMetadata(String workspaceId) {
        RotationState state = rotationState.get(workspaceId);
        if (state == null) {
            return Optional.empty();
        }

        if (!state.isInGracePeriod()) {
            // Expired, clean up
            rotationState.remove(workspaceId);
            return Optional.empty();
        }

        return Optional.of(new RotationMetadata(state.rotatedAt, state.gracePeriodEndAt));
    }

    /**
     * Internal state for token rotation.
     */
    private static class RotationState {
        final String previousToken;
        final long rotatedAt;
        final long gracePeriodEndAt;

        RotationState(String previousToken, long rotatedAt, long gracePeriodEndAt) {
            this.previousToken = previousToken;
            this.rotatedAt = rotatedAt;
            this.gracePeriodEndAt = gracePeriodEndAt;
        }

        boolean isInGracePeriod() {
            return System.currentTimeMillis() < gracePeriodEndAt;
        }
    }
}
