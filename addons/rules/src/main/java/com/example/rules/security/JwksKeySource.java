package com.example.rules.security;

import java.security.PublicKey;
import java.util.Map;

/**
 * Key source abstraction for JWT verification that can use either
 * static PEM keys or dynamic JWKS discovery.
 */
public interface JwksKeySource {

    /**
     * Get a public key by kid.
     *
     * @param kid the key identifier
     * @return the public key
     * @throws KeySourceException if the key is not found or an error occurs
     */
    PublicKey getKey(String kid) throws KeySourceException;

    /**
     * Get all currently available keys.
     *
     * @return map of kid to public key
     * @throws KeySourceException if an error occurs
     */
    Map<String, PublicKey> getAllKeys() throws KeySourceException;

    /**
     * Refresh keys if using a dynamic source like JWKS.
     *
     * @throws KeySourceException if refresh fails
     */
    default void refresh() throws KeySourceException {
        // Default implementation does nothing for static sources
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return cache statistics
     */
    default CacheStats getCacheStats() {
        return new CacheStats(0, null, false);
    }

    record CacheStats(int keyCount, java.time.Instant lastFetchTime, boolean rotationAlarmTriggered) {}

    class KeySourceException extends Exception {
        public KeySourceException(String message) {
            super(message);
        }

        public KeySourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}