package com.example.rules.cache;

/**
 * Backend storage interface for webhook idempotency markers.
 */
public interface WebhookIdempotencyStore extends AutoCloseable {

    /**
     * Returns {@code true} if the provided workspace/event/dedupKey tuple has already been
     * processed within the TTL window. Otherwise stores the tuple with the provided TTL and
     * returns {@code false}.
     */
    boolean isDuplicate(String workspaceId, String eventType, String dedupKey, long ttlMillis);

    /**
     * Clears any stored entries (best-effort). Used primarily by tests.
     */
    default void clear() {
        // no-op
    }

    @Override
    default void close() {
        // no-op
    }
}
