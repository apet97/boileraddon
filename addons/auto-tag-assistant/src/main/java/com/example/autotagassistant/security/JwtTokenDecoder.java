package com.example.autotagassistant.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal JWT decoder tailored for Clockify marketplace tokens.
 *
 * <p>The helper does not perform signature verification. Use the
 * public keys published in the Marketplace docs before trusting any
 * claims for production workloads.</p>
 */
public final class JwtTokenDecoder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtTokenDecoder() {
        // Utility class
    }

    /**
     * Decode a JWT string into structured header and payload JSON nodes.
     *
     * @param token the raw JWT value
     * @return a decoded representation containing header, payload, and signature
     */
    public static DecodedJwt decode(String token) {
        if (token == null) {
            throw new IllegalArgumentException("JWT token is required");
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("JWT token must have at least header and payload parts");
        }

        JsonNode header = parseBase64Json(parts[0], "header");
        JsonNode payload = parseBase64Json(parts[1], "payload");
        String signature = parts.length >= 3 ? parts[2] : "";

        return new DecodedJwt(header, payload, signature);
    }

    /**
     * Extract environment related claims (API base URLs, etc.) from a Clockify JWT token.
     *
     * @param token the raw JWT value
     * @return populated {@link EnvironmentClaims}
     */
    public static EnvironmentClaims extractEnvironmentClaims(String token) {
        DecodedJwt decoded = decode(token);
        return extractEnvironmentClaims(decoded.payload());
    }

    /**
     * Extract environment related claims (API base URLs, etc.) from a previously decoded payload.
     *
     * @param payload decoded JWT payload
     * @return populated {@link EnvironmentClaims}
     */
    public static EnvironmentClaims extractEnvironmentClaims(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return EnvironmentClaims.empty();
        }

        String backendUrl = getText(payload, "backendUrl");
        String apiUrl = getText(payload, "apiUrl");
        String reportsUrl = getText(payload, "reportsUrl");
        String locationsUrl = getText(payload, "locationsUrl");
        String screenshotsUrl = getText(payload, "screenshotsUrl");

        return new EnvironmentClaims(backendUrl, apiUrl, reportsUrl, locationsUrl, screenshotsUrl);
    }

    private static JsonNode parseBase64Json(String part, String description) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(part);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode JWT " + description + " segment", e);
        }
    }

    private static String getText(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    /**
     * Representation of a decoded JWT token.
     */
    public record DecodedJwt(JsonNode header, JsonNode payload, String signature) {
        public DecodedJwt {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(payload, "payload");
            signature = signature == null ? "" : signature;
        }
    }

    /**
     * Simple value object carrying Clockify environment specific claims.
     */
    public static final class EnvironmentClaims {
        private final String backendUrl;
        private final String apiUrl;
        private final String reportsUrl;
        private final String locationsUrl;
        private final String screenshotsUrl;

        private EnvironmentClaims(String backendUrl, String apiUrl, String reportsUrl, String locationsUrl, String screenshotsUrl) {
            this.backendUrl = normalize(backendUrl);
            this.apiUrl = normalize(apiUrl);
            this.reportsUrl = normalize(reportsUrl);
            this.locationsUrl = normalize(locationsUrl);
            this.screenshotsUrl = normalize(screenshotsUrl);
        }

        private static String normalize(String value) {
            return value != null && value.isBlank() ? null : value;
        }

        public static EnvironmentClaims empty() {
            return new EnvironmentClaims(null, null, null, null, null);
        }

        public String backendUrl() {
            return backendUrl;
        }

        public String apiUrl() {
            return apiUrl;
        }

        public String reportsUrl() {
            return reportsUrl;
        }

        public String locationsUrl() {
            return locationsUrl;
        }

        public String screenshotsUrl() {
            return screenshotsUrl;
        }

        public Map<String, String> asMap() {
            Map<String, String> claims = new java.util.LinkedHashMap<>();
            if (backendUrl != null) {
                claims.put("backendUrl", backendUrl);
            }
            if (apiUrl != null) {
                claims.put("apiUrl", apiUrl);
            }
            if (reportsUrl != null) {
                claims.put("reportsUrl", reportsUrl);
            }
            if (locationsUrl != null) {
                claims.put("locationsUrl", locationsUrl);
            }
            if (screenshotsUrl != null) {
                claims.put("screenshotsUrl", screenshotsUrl);
            }
            return Collections.unmodifiableMap(claims);
        }
    }
}
