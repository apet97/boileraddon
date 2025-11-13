package com.example.rules.security;

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
 * JWT verifier for Clockify marketplace tokens (RS256).
 *
 * <p><strong>Architecture Note:</strong> This verifier is implemented at the addon level rather than
 * in the shared SDK to allow each addon to customize its JWT verification requirements independently.
 * Different addons may have different:
 * <ul>
 *   <li>Trust domains and expected issuers/audiences</li>
 *   <li>Key management strategies (JWKS, static keys, rotation schedules)</li>
 *   <li>Security policies (algorithm restrictions, TTL limits, clock skew tolerance)</li>
 * </ul>
 *
 * <p>If all addons in your deployment share identical JWT verification requirements, consider
 * moving this to the SDK for reusability. Otherwise, maintain addon-specific implementations.
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li><strong>Strict kid handling:</strong> When JWT header includes "kid", that specific key
 *       is used with no fallback to default keys (prevents key confusion attacks)</li>
 *   <li><strong>Algorithm intersection:</strong> Enforces intersection of configured algorithms
 *       with safe built-in set (RS256, ES256) to prevent algorithm substitution</li>
 *   <li><strong>Temporal validation:</strong> Enforces iat/nbf/exp with configurable clock skew
 *       and maximum TTL (24h default) to limit token lifetime</li>
 *   <li><strong>Audience any-of:</strong> Supports both string and array audience claims with
 *       any-of semantics per RFC 7519</li>
 * </ul>
 *
 * @see Constraints#fromEnvironment() for environment-based configuration
 */
public final class JwtVerifier {
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

    static JwtVerifier forTesting(PublicKey defaultKey, Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, Map.of(), constraints, null, clock);
    }

    static JwtVerifier forTesting(Map<String, PublicKey> kidKeys, PublicKey defaultKey,
                                  Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, kidKeys, constraints, null, clock);
    }

    static JwtVerifier forTesting(Map<String, PublicKey> kidKeys, PublicKey defaultKey,
                                  String defaultKid, Constraints constraints, Clock clock) {
        return new JwtVerifier(defaultKey, kidKeys, constraints, defaultKid, clock);
    }

    public static JwtVerifier fromKeySource(JwksKeySource keySource, Constraints constraints) throws Exception {
        Map<String, PublicKey> allKeys = keySource.getAllKeys();
        if (allKeys.isEmpty()) {
            throw new IllegalArgumentException("Key source returned no keys");
        }
        // Use the first key as default if no default kid is specified
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

    // Intersect constraints with a safe built-in set
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
            String trimmedKid = kid.trim();
            PublicKey key = kidKeys.get(trimmedKid);
            if (key != null) return key;
            // No fallback when kid is present
            throw new JwtVerificationException("Unknown JWT kid: " + trimmedKid);
        }
        if (defaultKey != null) return defaultKey;
        if (!kidKeys.isEmpty()) {
            if (defaultKid != null && !defaultKid.isBlank()) {
                PublicKey key = kidKeys.get(defaultKid);
                if (key == null) throw new JwtVerificationException("Configured default kid not found: " + defaultKid);
                return key;
            }
            throw new JwtVerificationException("JWT missing kid header");
        }
        throw new JwtVerificationException("No public key configured for JWT verification");
    }

    private JsonNode decodeSegment(String segment, String description) throws JwtVerificationException {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segment);
            return OBJECT_MAPPER.readTree(decoded);
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
                throw new JwtVerificationException("JWT signature verification failed");
            }
        } catch (JwtVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtVerificationException("JWT signature validation error", e);
        }
    }

    private void validateClaims(JsonNode payload) throws JwtVerificationException {
        enforceTemporal(payload);

        if (constraints.expectedIssuer() != null) {
            String iss = normalize(payload.path("iss").asText(null));
            if (!constraints.expectedIssuer().equals(iss)) {
                throw new JwtVerificationException("Unexpected issuer");
            }
        }

        if (constraints.expectedAudience() != null) {
            if (!audMatches(payload.get("aud"), constraints.expectedAudience())) {
                throw new JwtVerificationException("Unexpected audience");
            }
        }

        if (expectedSubject != null) {
            String sub = normalize(payload.path("sub").asText(null));
            if (!Objects.equals(expectedSubject, sub)) {
                throw new JwtVerificationException("Unexpected subject");
            }
        }
    }

    private void enforceTemporal(JsonNode payload) throws JwtVerificationException {
        Instant now = Instant.now(clock);
        long leeway = Optional.ofNullable(constraints.clockSkewSeconds()).orElse(60L);
        Instant iat = getInstant(payload.get("iat"));
        Instant nbf = getInstant(payload.get("nbf"));
        Instant exp = getInstant(payload.get("exp"));

        if (iat != null && iat.isAfter(now.plusSeconds(leeway))) {
            throw new JwtVerificationException("JWT issued in the future");
        }
        if (nbf != null && now.isBefore(nbf.minusSeconds(leeway))) {
            throw new JwtVerificationException("JWT not yet valid");
        }
        if (exp == null) {
            throw new JwtVerificationException("JWT missing exp");
        }
        if (now.isAfter(exp.plusSeconds(leeway))) {
            throw new JwtVerificationException("JWT expired");
        }
        if (iat != null && Duration.between(iat, exp).compareTo(MAX_TTL) > 0) {
            throw new JwtVerificationException("JWT lifetime exceeds policy");
        }
    }

    private Instant getInstant(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return Instant.ofEpochSecond(n.asLong());
        if (n.isTextual()) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(n.asText().trim()));
            } catch (Exception ignore) {}
        }
        return null;
    }

    private boolean audMatches(JsonNode audNode, String expected) {
        if (audNode == null || audNode.isNull()) {
            return false;
        }
        if (audNode.isTextual()) {
            return expected.equals(normalize(audNode.asText()));
        }
        if (audNode.isArray()) {
            for (JsonNode element : audNode) {
                if (element.isTextual() && expected.equals(normalize(element.asText()))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static PublicKey parsePem(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("Public key PEM is required");
        }
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public record DecodedJwt(JsonNode header, JsonNode payload) {}

    public record Constraints(String expectedIssuer,
                              String expectedAudience,
                              long clockSkewSeconds,
                              Set<String> allowedAlgorithms) {

        public static Constraints defaults() {
            return new Constraints(null, null, 60, Set.of("RS256"));
        }

    }

    public static class JwtVerificationException extends Exception {
        public JwtVerificationException(String message) {
            super(message);
        }

        public JwtVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
