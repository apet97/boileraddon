package com.clockify.addon.sdk.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JWT verifier for Clockify marketplace tokens (RS256/ES256).
 */
public final class JwtVerifier implements AuthTokenVerifier {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PublicKey defaultKey;
    private final Map<String, PublicKey> kidKeys;
    private final Constraints constraints;
    private final String defaultKid;
    private final Clock clock;
    private final String expectedSubject;

    private JwtVerifier(PublicKey defaultKey,
                       Map<String, PublicKey> kidKeys,
                       Constraints constraints,
                       String defaultKid,
                       Clock clock) {
        this(defaultKey, kidKeys, constraints, defaultKid, clock, null);
    }

    private JwtVerifier(PublicKey defaultKey,
                        Map<String, PublicKey> kidKeys,
                        Constraints constraints,
                        String defaultKid,
                        Clock clock,
                        String expectedSubject) {
        this.defaultKey = defaultKey;
        this.kidKeys = kidKeys == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(kidKeys));
        this.constraints = constraints == null ? Constraints.defaults() : constraints;
        this.defaultKid = defaultKid == null ? null : defaultKid.trim();
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.expectedSubject = normalize(expectedSubject);
    }

    public static JwtVerifier fromPem(String pem) throws Exception {
        return fromPem(pem, Constraints.defaults());
    }

    public static JwtVerifier fromPem(String pem, Constraints constraints) throws Exception {
        return new JwtVerifier(parsePem(pem), Map.of(), constraints, null, Clock.systemUTC());
    }

    public static JwtVerifier fromPem(String pem, Constraints constraints, String expectedSubject) throws Exception {
        return new JwtVerifier(parsePem(pem), Map.of(), constraints, null, Clock.systemUTC(), expectedSubject);
    }

    public static JwtVerifier fromPemMap(Map<String, String> pemByKid,
                                         String defaultKid,
                                         Constraints constraints) throws Exception {
        if (pemByKid == null || pemByKid.isEmpty()) {
            throw new IllegalArgumentException("pemByKid must contain at least one entry");
        }
        Map<String, PublicKey> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : pemByKid.entrySet()) {
            resolved.put(entry.getKey(), parsePem(entry.getValue()));
        }
        PublicKey fallback = defaultKid == null ? null : resolved.get(defaultKid);
        return new JwtVerifier(fallback, resolved, constraints, defaultKid, Clock.systemUTC());
    }

    public static JwtVerifier fromPemMap(Map<String, String> pemByKid,
                                         String defaultKid,
                                         Constraints constraints,
                                         String expectedSubject) throws Exception {
        if (pemByKid == null || pemByKid.isEmpty()) {
            throw new IllegalArgumentException("pemByKid must contain at least one entry");
        }
        Map<String, PublicKey> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : pemByKid.entrySet()) {
            resolved.put(entry.getKey(), parsePem(entry.getValue()));
        }
        PublicKey fallback = defaultKid == null ? null : resolved.get(defaultKid);
        return new JwtVerifier(fallback, resolved, constraints, defaultKid, Clock.systemUTC(), expectedSubject);
    }

    public static JwtVerifier forTesting(PublicKey defaultKey, Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, Map.of(), constraints, null, clock);
    }

    public static JwtVerifier forTesting(PublicKey defaultKey,
                                         Constraints constraints,
                                         Clock clock,
                                         String expectedSubject) {
        return new JwtVerifier(defaultKey, Map.of(), constraints, null, clock, expectedSubject);
    }

    public static JwtVerifier forTesting(Map<String, PublicKey> kidKeys, PublicKey defaultKey,
                                         Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, kidKeys, constraints, null, clock);
    }

    public static JwtVerifier forTesting(Map<String, PublicKey> kidKeys, PublicKey defaultKey,
                                         String defaultKid, Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, kidKeys, constraints, defaultKid, clock);
    }

    public static JwtVerifier fromKeySource(JwksKeySource keySource, Constraints constraints) throws Exception {
        Map<String, PublicKey> allKeys = keySource.getAllKeys();
        if (allKeys.isEmpty()) {
            throw new IllegalArgumentException("Key source returned no keys");
        }
        String firstKid = allKeys.keySet().iterator().next();
        return new JwtVerifier(allKeys.get(firstKid), allKeys, constraints, firstKid, Clock.systemUTC());
    }

    public static JwtVerifier fromKeySource(JwksKeySource keySource, Constraints constraints, String expectedSubject) throws Exception {
        Map<String, PublicKey> allKeys = keySource.getAllKeys();
        if (allKeys.isEmpty()) {
            throw new IllegalArgumentException("Key source returned no keys");
        }
        String firstKid = allKeys.keySet().iterator().next();
        return new JwtVerifier(allKeys.get(firstKid), allKeys, constraints, firstKid, Clock.systemUTC(), expectedSubject);
    }

    public DecodedJwt verify(String token) throws JwtVerificationException {
        if (token == null || token.isBlank()) {
            throw new JwtVerificationException("JWT token is required");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtVerificationException("Malformed JWT token");
        }

        JsonNode header = decodeSegment(parts[0], "header");
        enforceAlgorithm(header);
        PublicKey keyToUse = selectKey(header.path("kid").asText(null));
        JsonNode payload = decodeSegment(parts[1], "payload");
        verifySignature(parts[0] + "." + parts[1], parts[2], keyToUse);
        validateClaims(payload);
        return new DecodedJwt(header, payload);
    }

    private static final Set<String> SAFE_ALGS = Set.of("RS256", "ES256");
    private static final Duration MAX_TTL = Duration.ofHours(24);

    private void enforceAlgorithm(JsonNode header) throws JwtVerificationException {
        String alg = header.hasNonNull("alg") ? header.get("alg").asText() : null;
        if (alg == null || alg.isBlank()) {
            throw new JwtVerificationException("Missing JWT algorithm");
        }
        String normalized = alg.toUpperCase(Locale.ROOT);
        if (!constraints.allowedAlgorithms().contains(normalized) || !SAFE_ALGS.contains(normalized)) {
            throw new JwtVerificationException("Unsupported JWT algorithm");
        }
    }

    private PublicKey selectKey(String kid) throws JwtVerificationException {
        if (kid != null && !kid.isBlank()) {
            PublicKey key = kidKeys.get(kid);
            if (key == null) {
                throw new JwtVerificationException("Unknown JWT kid: " + kid);
            }
            return key;
        }
        if (defaultKey != null) {
            return defaultKey;
        }
        if (!kidKeys.isEmpty() && defaultKid != null) {
            PublicKey fallback = kidKeys.get(defaultKid);
            if (fallback != null) {
                return fallback;
            }
        }
        throw new JwtVerificationException("No default JWT key configured");
    }

    private JsonNode decodeSegment(String segment, String description) throws JwtVerificationException {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segment);
            return OBJECT_MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new JwtVerificationException("Failed to decode JWT " + description, e);
        }
    }

    private void verifySignature(String signedData, String signaturePart, PublicKey key) throws JwtVerificationException {
        try {
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signaturePart);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(signedData.getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(signatureBytes)) {
                throw new JwtVerificationException("Invalid JWT signature");
            }
        } catch (JwtVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtVerificationException("Failed to verify JWT signature", e);
        }
    }

    private void validateClaims(JsonNode payload) throws JwtVerificationException {
        long now = clock.instant().getEpochSecond();

        if (payload.has("exp")) {
            long exp = payload.get("exp").asLong();
            long adjustedExp = exp + constraints.clockSkewSeconds();
            if (adjustedExp < now) {
                throw new JwtVerificationException("JWT expired");
            }
            long issuedAt = payload.has("iat") ? payload.get("iat").asLong() : now;
            long ttl = exp - issuedAt;
            if (ttl > MAX_TTL.toSeconds()) {
                throw new JwtVerificationException("JWT TTL exceeds maximum allowed duration");
            }
        } else if (constraints.requireExp()) {
            throw new JwtVerificationException("JWT missing exp claim");
        }

        if (payload.has("nbf")) {
            long nbf = payload.get("nbf").asLong();
            long adjustedNbf = nbf - constraints.clockSkewSeconds();
            if (adjustedNbf > now) {
                throw new JwtVerificationException("JWT not yet valid");
            }
        }

        if (payload.has("iss")) {
            String iss = payload.get("iss").asText();
            if (!constraints.allowedIssuers().isEmpty() && !constraints.allowedIssuers().contains(iss)) {
                throw new JwtVerificationException("Unexpected JWT issuer");
            }
        } else if (!constraints.allowedIssuers().isEmpty()) {
            throw new JwtVerificationException("JWT missing iss claim");
        }

        if (!constraints.allowedAudiences().isEmpty()) {
            if (payload.has("aud")) {
                if (!audienceMatches(payload.get("aud"), constraints.allowedAudiences())) {
                    throw new JwtVerificationException("JWT audience mismatch");
                }
            } else {
                throw new JwtVerificationException("JWT missing aud claim");
            }
        }

        if (expectedSubject != null && !expectedSubject.isBlank()) {
            String sub = payload.has("sub") ? payload.get("sub").asText() : null;
            if (!expectedSubject.equals(sub)) {
                throw new JwtVerificationException("JWT subject mismatch");
            }
        }
    }

    private static boolean audienceMatches(JsonNode aud, Set<String> allowedAud) {
        if (aud == null) {
            return false;
        }
        if (aud.isTextual()) {
            return allowedAud.contains(aud.asText());
        }
        if (aud.isArray()) {
            for (JsonNode value : aud) {
                if (value.isTextual() && allowedAud.contains(value.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    public record DecodedJwt(JsonNode header, JsonNode payload) {}

    public static final class Constraints {
        private final Set<String> allowedIssuers;
        private final Set<String> allowedAudiences;
        private final Set<String> allowedAlgorithms;
        private final long clockSkewSeconds;
        private final boolean requireExp;

        public Constraints(String expectedIss, String expectedAud, long clockSkewSeconds, Set<String> allowedAlgorithms) {
            this(expectedIss == null ? Set.of() : Set.of(expectedIss),
                    expectedAud == null ? Set.of() : Set.of(expectedAud),
                    allowedAlgorithms == null || allowedAlgorithms.isEmpty() ? SAFE_ALGS : allowedAlgorithms,
                    clockSkewSeconds,
                    true);
        }

        public Constraints(Set<String> allowedIssuers,
                           Set<String> allowedAudiences,
                           Set<String> allowedAlgorithms,
                           long clockSkewSeconds,
                           boolean requireExp) {
            this.allowedIssuers = allowedIssuers == null ? Set.of() : Set.copyOf(allowedIssuers);
            this.allowedAudiences = allowedAudiences == null ? Set.of() : Set.copyOf(allowedAudiences);
            this.allowedAlgorithms = allowedAlgorithms == null ? SAFE_ALGS : Set.copyOf(allowedAlgorithms);
            this.clockSkewSeconds = Math.max(0, clockSkewSeconds);
            this.requireExp = requireExp;
        }

        public static Constraints defaults() {
            return new Constraints(Set.of(), Set.of(), SAFE_ALGS, 60, true);
        }

        public Set<String> allowedIssuers() {
            return allowedIssuers;
        }

        public Set<String> allowedAudiences() {
            return allowedAudiences;
        }

        public Set<String> allowedAlgorithms() {
            return allowedAlgorithms;
        }

        public long clockSkewSeconds() {
            return clockSkewSeconds;
        }

        public boolean requireExp() {
            return requireExp;
        }
    }

    public static final class JwtVerificationException extends Exception {
        public JwtVerificationException(String message) {
            super(message);
        }

        public JwtVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static PublicKey parsePem(String pem) throws Exception {
        String sanitized = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static Map<String, String> parsePemMap(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
    }

    public Constraints constraints() {
        return constraints;
    }
}
