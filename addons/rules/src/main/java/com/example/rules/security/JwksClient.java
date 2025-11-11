package com.example.rules.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWKS (JSON Web Key Set) client for dynamic key discovery and rotation.
 * Supports automatic key refresh with caching and rotation alarms.
 */
public final class JwksClient {
    private static final Logger logger = LoggerFactory.getLogger(JwksClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final URI jwksUri;
    private final HttpClient httpClient;
    private final Duration cacheTtl;
    private final Duration timeout;

    private volatile Map<String, PublicKey> cachedKeys = new ConcurrentHashMap<>();
    private volatile Instant lastFetchTime = Instant.MIN;
    private volatile boolean rotationAlarmTriggered = false;

    public JwksClient(URI jwksUri) {
        this(jwksUri, HttpClient.newHttpClient(), Duration.ofMinutes(5), Duration.ofSeconds(10));
    }

    public JwksClient(URI jwksUri, HttpClient httpClient, Duration cacheTtl, Duration timeout) {
        this.jwksUri = jwksUri;
        this.httpClient = httpClient;
        this.cacheTtl = cacheTtl;
        this.timeout = timeout;
    }

    /**
     * Get a public key by kid from JWKS endpoint.
     * Automatically refreshes keys if cache is expired.
     */
    public PublicKey getKey(String kid) throws JwksException {
        if (isCacheExpired()) {
            synchronized (this) {
                if (isCacheExpired()) {
                    refreshKeys();
                }
            }
        }

        PublicKey key = cachedKeys.get(kid);
        if (key == null) {
            throw new JwksException("Key not found in JWKS: " + kid);
        }

        return key;
    }

    /**
     * Get all currently cached keys.
     */
    public Map<String, PublicKey> getAllKeys() {
        if (isCacheExpired()) {
            synchronized (this) {
                if (isCacheExpired()) {
                    try {
                        refreshKeys();
                    } catch (JwksException e) {
                        logger.warn("Failed to refresh JWKS keys, using stale cache", e);
                    }
                }
            }
        }
        return new HashMap<>(cachedKeys);
    }

    /**
     * Force refresh of JWKS keys.
     */
    public void refreshKeys() throws JwksException {
        try {
            logger.debug("Refreshing JWKS keys from: {}", jwksUri);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(jwksUri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new JwksException("JWKS endpoint returned status: " + response.statusCode());
            }

            JsonNode jwks = OBJECT_MAPPER.readTree(response.body());
            JsonNode keys = jwks.path("keys");

            if (!keys.isArray()) {
                throw new JwksException("JWKS response missing keys array");
            }

            Map<String, PublicKey> newKeys = new ConcurrentHashMap<>();
            int keyCount = 0;

            for (JsonNode keyNode : keys) {
                try {
                    String keyId = keyNode.path("kid").asText();
                    String keyType = keyNode.path("kty").asText();

                    if ("RSA".equals(keyType) && keyId != null && !keyId.isBlank()) {
                        PublicKey publicKey = parseJwk(keyNode);
                        newKeys.put(keyId, publicKey);
                        keyCount++;
                        logger.debug("Loaded JWK with kid: {}", keyId);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse JWK: {}", e.getMessage());
                }
            }

            if (keyCount == 0) {
                throw new JwksException("No valid RSA keys found in JWKS response");
            }

            // Check for key rotation
            if (!cachedKeys.isEmpty() && !newKeys.keySet().equals(cachedKeys.keySet())) {
                logger.warn("JWKS key rotation detected. Old keys: {}, New keys: {}",
                           cachedKeys.keySet(), newKeys.keySet());
                if (!rotationAlarmTriggered) {
                    rotationAlarmTriggered = true;
                    logger.info("JWKS key rotation alarm triggered - keys have changed");
                }
            }

            cachedKeys = newKeys;
            lastFetchTime = Instant.now();

            logger.info("Successfully refreshed JWKS keys. Loaded {} keys from {}", keyCount, jwksUri);

        } catch (JwksException e) {
            throw e;
        } catch (Exception e) {
            throw new JwksException("Failed to fetch JWKS keys", e);
        }
    }

    private boolean isCacheExpired() {
        return Instant.now().isAfter(lastFetchTime.plus(cacheTtl));
    }

    private PublicKey parseJwk(JsonNode jwk) throws Exception {
        String modulus = jwk.path("n").asText();
        String exponent = jwk.path("e").asText();

        if (modulus == null || exponent == null) {
            throw new IllegalArgumentException("JWK missing required RSA parameters");
        }

        // Convert JWK to PEM format
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);

        // Create RSA public key from modulus and exponent
        java.math.BigInteger n = new java.math.BigInteger(1, modulusBytes);
        java.math.BigInteger e = new java.math.BigInteger(1, exponentBytes);

        java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(n, e);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /**
     * Get cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                cachedKeys.size(),
                lastFetchTime,
                rotationAlarmTriggered
        );
    }

    public record CacheStats(int keyCount, Instant lastFetchTime, boolean rotationAlarmTriggered) {}

    public static class JwksException extends Exception {
        public JwksException(String message) {
            super(message);
        }

        public JwksException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}