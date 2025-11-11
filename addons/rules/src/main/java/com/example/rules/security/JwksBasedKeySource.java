package com.example.rules.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Map;

/**
 * JWKS-based key source that dynamically fetches keys from a JWKS endpoint.
 */
public class JwksBasedKeySource implements JwksKeySource {

    private final JwksClient jwksClient;

    public JwksBasedKeySource(URI jwksUri) {
        this(jwksUri, HttpClient.newHttpClient(), Duration.ofMinutes(5), Duration.ofSeconds(10));
    }

    public JwksBasedKeySource(URI jwksUri, HttpClient httpClient, Duration cacheTtl, Duration timeout) {
        this.jwksClient = new JwksClient(jwksUri, httpClient, cacheTtl, timeout);
    }

    @Override
    public PublicKey getKey(String kid) throws KeySourceException {
        try {
            return jwksClient.getKey(kid);
        } catch (JwksClient.JwksException e) {
            throw new KeySourceException("Failed to get key from JWKS: " + kid, e);
        }
    }

    @Override
    public Map<String, PublicKey> getAllKeys() throws KeySourceException {
        try {
            return jwksClient.getAllKeys();
        } catch (Exception e) {
            throw new KeySourceException("Failed to get all keys from JWKS", e);
        }
    }

    @Override
    public void refresh() throws KeySourceException {
        try {
            jwksClient.refreshKeys();
        } catch (JwksClient.JwksException e) {
            throw new KeySourceException("Failed to refresh JWKS keys", e);
        }
    }

    @Override
    public CacheStats getCacheStats() {
        JwksClient.CacheStats stats = jwksClient.getCacheStats();
        return new CacheStats(
                stats.keyCount(),
                stats.lastFetchTime(),
                stats.rotationAlarmTriggered()
        );
    }
}