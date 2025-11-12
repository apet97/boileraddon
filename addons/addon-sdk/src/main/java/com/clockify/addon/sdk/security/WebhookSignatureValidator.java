package com.clockify.addon.sdk.security;

import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import com.clockify.addon.sdk.metrics.MetricsHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Webhook signature validation helpers.
 * - verify(request, workspaceId): high-level verification using stored TokenStore secret
 * - validate(signatureHeader, body, secret): low-level validator
 */
public final class WebhookSignatureValidator {
    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // Clock skew configuration for temporal claim checks
    private static final long DEFAULT_SKEW = 30L;
    private static long skewSeconds() {
        String v = System.getenv("JWT_MAX_CLOCK_SKEW_SECONDS");
        if (v == null || v.isBlank()) v = System.getProperty("JWT_MAX_CLOCK_SKEW_SECONDS");
        try { return v == null ? DEFAULT_SKEW : Math.max(0, Long.parseLong(v)); }
        catch (NumberFormatException e) { return DEFAULT_SKEW; }
    }

    private WebhookSignatureValidator() {}

    public static final String SIGNATURE_HEADER = "clockify-webhook-signature";
    private static final String[] ALT_HEADERS = new String[]{
            "x-clockify-webhook-signature",
            "Clockify-Webhook-Signature",
            "X-Clockify-Webhook-Signature",
            // Developer workspace often sends JWT under this header
            "Clockify-Signature"
    };

    private static final String[] LIFECYCLE_HEADERS = new String[]{
            // Use the general Clockify signature header names for lifecycle as well
            "clockify-signature", "Clockify-Signature"
    };

    // Canonical signature header for all paths
    private static final String HDR_CANONICAL = "Clockify-Signature";
    private static final String[] HDR_ALTS = new String[]{
            "clockify-signature",
            "clockify-webhook-signature",
            "X-Clockify-Signature",
            "x-clockify-signature"
    };

    static String resolveSignatureHeader(HttpServletRequest req, Logger log, MeterRegistry mr) {
        String v = req.getHeader(HDR_CANONICAL);
        if (v != null && !v.isBlank()) return v;
        for (String h : HDR_ALTS) {
            v = req.getHeader(h);
            if (v != null && !v.isBlank()) {
                if (log != null) log.warn("Non-canonical signature header used: {}", h);
                if (mr != null) Counter.builder("addon.signature.header.noncanonical")
                        .tag("header", h)
                        .register(mr)
                        .increment();
                return v;
            }
        }
        return null;
    }

    public static class VerificationResult {
        private final boolean valid;
        private final HttpResponse response;
        public VerificationResult(boolean valid, HttpResponse response) {
            this.valid = valid;
            this.response = response;
        }
        public boolean isValid() { return valid; }
        public HttpResponse response() { return response; }
        public static VerificationResult ok() { return new VerificationResult(true, HttpResponse.ok("ok")); }
    }

    /**
     * Validates webhook request authenticity.
     * Prefers JWT signature verification (RSA256) per Clockify guide. Falls back to HMAC validation for backwards compatibility.
     * Returns 401 if signature missing; 403 if invalid.
     */
    public static VerificationResult verify(HttpServletRequest request, String workspaceId) {
        return verify(request, workspaceId, null);
    }

    /**
     * Validates webhook request authenticity with an expected add-on key for JWT 'sub' claim checking.
     */
    public static VerificationResult verify(HttpServletRequest request, String workspaceId, String expectedAddonKey) {
        if (workspaceId == null || workspaceId.isBlank()) {
            logger.warn("Webhook signature validation failed: workspaceId is missing or blank");
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"workspaceId missing\"}", "application/json"));
        }

        var tokenOpt = TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            logger.warn("Webhook signature validation failed for workspace '{}': no installation token found in TokenStore", workspaceId);
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"installation token not found\"}", "application/json"));
        }
        logger.debug("Webhook signature validation starting for workspace '{}'", workspaceId);

        String sigHeader = resolveSignatureHeader(request, logger, MetricsHandler.registry());

        if (sigHeader == null || sigHeader.isBlank()) {
            logger.warn("Webhook signature validation failed for workspace '{}': no signature header found", workspaceId);
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"signature header missing\"}", "application/json"));
        }

        byte[] body = readRawBody(request).getBytes(StandardCharsets.UTF_8);
        logger.debug("Webhook signature validation: payload size = {} bytes", body.length);

        // Preferred path: Clockify-Signature style JWT (RSA256)
        if (looksLikeJwt(sigHeader)) {
            VerificationResult jwtResult = verifyJwtSignature(sigHeader, expectedAddonKey, workspaceId);
            if (jwtResult != null) {
                return jwtResult;
            }
        }

        // Compatibility path: HMAC (sha256=<hex> or raw hex) using installation token
        boolean allowHmac = "HMAC".equalsIgnoreCase(System.getenv("ADDON_AUTH_COMPAT"))
                || "HMAC".equalsIgnoreCase(System.getProperty("ADDON_AUTH_COMPAT"));
        if (allowHmac && looksLikeHmac(sigHeader)) {
            logger.debug("Webhook signature validation: signature format is HMAC-SHA256 (compat mode)");
            boolean ok = validate(sigHeader, body, tokenOpt.get().token());
            if (!ok) {
                logger.warn("Webhook signature validation failed for workspace '{}': HMAC signature mismatch", workspaceId);
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
            }
            if ("prod".equalsIgnoreCase(System.getenv().getOrDefault("ENV", "prod"))) {
                logger.warn("HMAC compatibility path accepted in production for workspace {}. Migrate to JWT.", workspaceId);
            } else {
                logger.info("Webhook signature validation successful for workspace '{}': HMAC-SHA256 verified (compat)", workspaceId);
            }
            return VerificationResult.ok();
        }

        // Unknown format — treat as invalid
        logger.warn("Webhook signature validation failed for workspace '{}': unrecognized signature format", workspaceId);
        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
    }

    /**
     * Validates lifecycle request authenticity via JWT signature.
     */
    public static VerificationResult verifyLifecycle(HttpServletRequest request, String expectedAddonKey) {
        String sigHeader = resolveSignatureHeader(request, logger, MetricsHandler.registry());
        if (sigHeader == null || sigHeader.isBlank()) {
            logger.warn("Lifecycle signature missing (checked {} headers)", LIFECYCLE_HEADERS.length);
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"signature header missing\"}", "application/json"));
        }
        if (!looksLikeJwt(sigHeader)) {
            logger.warn("Lifecycle signature is not a JWT");
            return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
        }
        VerificationResult jwt = verifyJwtSignature(sigHeader, expectedAddonKey, null);
        if (jwt != null) return jwt;
        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
    }

    /** Low-level validator using HMAC-SHA256 hex. */
    public static boolean validate(String signatureHeader, byte[] body, String sharedSecret) {
        if (signatureHeader == null || sharedSecret == null || body == null) return false;
        String expected = hmacHex(sharedSecret.getBytes(StandardCharsets.UTF_8), body);
        String provided = normalize(signatureHeader);
        return constantTimeEquals(expected, provided);
    }

    /** Utility for tests and tooling: returns header-style signature ("sha256=<hex>"). */
    public static String computeSignature(String sharedSecret, String body) {
        if (sharedSecret == null) throw new IllegalArgumentException("sharedSecret is required");
        byte[] key = sharedSecret.getBytes(StandardCharsets.UTF_8);
        byte[] data = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        String hex = hmacHex(key, data);
        return "sha256=" + hex;
    }

    private static String readRawBody(HttpServletRequest request) {
        Object cached = request.getAttribute("clockify.rawBody");
        if (cached instanceof String s) return s;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) { sb.append(line); }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private static String hmacHex(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal(data);
            return toHex(out);
        } catch (Exception e) {
            throw new RuntimeException("HMAC failure", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String normalize(String header) {
        String v = header.trim();
        int idx = v.indexOf('=');
        return (idx > 0) ? v.substring(idx + 1) : v;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    private static boolean looksLikeHmac(String sig) {
        String s = sig != null ? sig.trim() : "";
        if (s.startsWith("sha256=")) s = s.substring("sha256=".length());
        return s.matches("[0-9a-fA-F]{64}");
    }

    private static boolean looksLikeJwt(String sig) {
        return sig != null && sig.contains(".") && sig.split("\\.").length >= 2;
    }

    /**
     * Controls whether to accept developer JWT signatures (dev workspaces only).
     * SECURITY: Defaults to FALSE for safety. Requires explicit opt-in for development.
     * Environment variable: ADDON_ACCEPT_JWT_SIGNATURE (set to "true" to enable)
     */
    private static boolean acceptJwtDevSignature() {
        String v = Optional.ofNullable(System.getProperty("ADDON_ACCEPT_JWT_SIGNATURE"))
                .orElse(System.getenv("ADDON_ACCEPT_JWT_SIGNATURE"));
        String env = Optional.ofNullable(System.getProperty("ENV"))
                .orElse(Optional.ofNullable(System.getenv("ENV")).orElse("prod"));
        return "true".equalsIgnoreCase(v) && ("dev".equalsIgnoreCase(env) || "development".equalsIgnoreCase(env));
    }

    private static String decodeJwtPayload(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return "";
        String b64 = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .trim();
        // pad base64url if needed
        while (b64.length() % 4 != 0) b64 += "=";
        byte[] bytes = java.util.Base64.getDecoder().decode(b64);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String extractJsonString(String json, String key) {
        // very small parser: looks for "key":"value"
        if (json == null || key == null) return null;
        String pattern = "\"" + key + "\"\s*:\s*\""; // "key":"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            int start = m.end();
            int end = json.indexOf('"', start);
            if (end > start) return json.substring(start, end);
        }
        return null;
    }

    /** Extracts numeric (seconds) claim value; returns null if not found. */
    private static Long extractJsonLong(String json, String key) {
        if (json == null || key == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*"; // "key":
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            int start = m.end();
            StringBuilder sb = new StringBuilder();
            while (start < json.length()) {
                char c = json.charAt(start);
                if ((c >= '0' && c <= '9')) { sb.append(c); start++; continue; }
                break;
            }
            if (sb.length() > 0) return Long.parseLong(sb.toString());
        }
        return null;
    }

    /**
     * Verifies a JWT using RSA256 and required claims per Clockify guide.
     * Returns null when JWT format invalid so caller can fallback to other strategies.
     */
    private static VerificationResult verifyJwtSignature(String jwt, String expectedAddonKey, String workspaceIdParam) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return null; // not a JWT

            JsonNode header = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(parts[0]));
            String alg = header.path("alg").asText(null);
            String kid = header.path("kid").asText(null);
            if (alg == null || !"RS256".equalsIgnoreCase(alg)) {
                logger.warn("JWT alg is not RS256: {}", alg);
                if (acceptJwtDevSignature()) {
                    // validate workspaceId constraint if present
                    JsonNode payloadNode = safeDecodePayload(parts[1]);
                    if (workspaceMismatch(payloadNode, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt algorithm\"}", "application/json"));
            }

            Optional<PublicKey> keyOpt = resolveVerificationKey(kid);
            if (keyOpt.isEmpty()) {
                logger.warn("No public key configured or found for kid={}", kid);
                if (acceptJwtDevSignature()) {
                    JsonNode payloadNode = safeDecodePayload(parts[1]);
                    if (workspaceMismatch(payloadNode, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(500, "{\"error\":\"jwt key not configured\"}", "application/json"));
            }
            PublicKey key = keyOpt.get();

            String signingInput = parts[0] + "." + parts[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(key);
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            if (!sig.verify(signatureBytes)) {
                logger.warn("JWT signature verification failed");
                if (acceptJwtDevSignature()) {
                    JsonNode payloadNode = safeDecodePayload(parts[1]);
                    if (workspaceMismatch(payloadNode, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt signature\"}", "application/json"));
            }

            JsonNode payload = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
            String iss = normalize(payload.path("iss").asText(null));
            String typ = normalize(payload.path("type").asText(null));
            String sub = normalize(payload.path("sub").asText(null));
            long exp = payload.path("exp").asLong(0L);

            String expectedIss = Optional
                    .ofNullable(System.getProperty("CLOCKIFY_JWT_EXPECTED_ISS"))
                    .orElse(Optional.ofNullable(System.getenv("CLOCKIFY_JWT_EXPECTED_ISS")).orElse(null));
            if (expectedIss == null) {
                expectedIss = Optional.ofNullable(System.getProperty("CLOCKIFY_JWT_EXPECT_ISS"))
                        .orElse(Optional.ofNullable(System.getenv("CLOCKIFY_JWT_EXPECT_ISS")).orElse("clockify"));
                if (LegacyFlags.ISSUER_WARNED.compareAndSet(false, true)) {
                    logger.warn("Using legacy issuer env key CLOCKIFY_JWT_EXPECT_ISS; prefer CLOCKIFY_JWT_EXPECTED_ISS");
                }
                try {
                    Counter.builder("addon.jwt.issuer.env.fallback").register(MetricsHandler.registry()).increment();
                } catch (Exception ignored) {}
            }

            if (expectedIss != null && !expectedIss.equalsIgnoreCase(Optional.ofNullable(iss).orElse(""))) {
                logger.warn("Unexpected JWT issuer: {}", iss);
                if (acceptJwtDevSignature()) {
                    if (workspaceMismatch(payload, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt issuer\"}", "application/json"));
            }
            if (typ == null || !"addon".equalsIgnoreCase(typ)) {
                logger.warn("Unexpected JWT type: {}", typ);
                if (acceptJwtDevSignature()) {
                    if (workspaceMismatch(payload, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt type\"}", "application/json"));
            }
            if (expectedAddonKey != null && !expectedAddonKey.equals(sub)) {
                logger.warn("JWT sub mismatch (expected {}, got {})", expectedAddonKey, sub);
                if (acceptJwtDevSignature()) {
                    if (workspaceMismatch(payload, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                    return VerificationResult.ok();
                }
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt subject\"}", "application/json"));
            }
            // Strict workspaceId enforcement (if claim is present)
            if (workspaceMismatch(payload, workspaceIdParam)) {
                logger.warn("JWT workspaceId mismatch: expected={}, got={}", workspaceIdParam, payload.path("workspaceId").asText(""));
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"workspace mismatch\"}", "application/json"));
            }

            // Temporal verification:
            // - Webhook path (workspaceIdParam != null): exp is optional; if present and expired => 401
            // - Lifecycle path (workspaceIdParam == null): exp required and must not be expired (legacy behavior)
            long now = Instant.now().getEpochSecond();
            if (workspaceIdParam != null) {
                if (exp > 0 && now > (exp + skewSeconds())) {
                    logger.warn("JWT expired for webhook path (exp present)");
                    return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"token expired\"}", "application/json"));
                }
            } else {
                if (exp <= 0 || now > (exp + skewSeconds())) {
                    logger.warn("JWT expired or missing exp for lifecycle path");
                    if (acceptJwtDevSignature()) {
                        return VerificationResult.ok();
                    }
                    return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"jwt expired\"}", "application/json"));
                }
            }

            logger.info("JWT signature validation successful");
            return VerificationResult.ok();
        } catch (Exception e) {
            if (acceptJwtDevSignature()) {
                logger.warn("DEV MODE: Accepting JWT without strict verification due to error: {}", e.getMessage());
                // Best-effort workspace check
                try {
                    String[] parts = jwt.split("\\.");
                    JsonNode payload = safeDecodePayload(parts.length > 1 ? parts[1] : "");
                    if (workspaceMismatch(payload, workspaceIdParam)) {
                        return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid workspace\"}", "application/json"));
                    }
                } catch (Exception ignored) {}
                return VerificationResult.ok();
            }
            logger.warn("JWT verification error: {}", e.getMessage());
            return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt signature\"}", "application/json"));
        }
    }

    // Util: once-per-process warning guards
    private static final class LegacyFlags {
        private static final java.util.concurrent.atomic.AtomicBoolean ISSUER_WARNED = new java.util.concurrent.atomic.AtomicBoolean(false);
    }

    private static JsonNode safeDecodePayload(String payloadPartB64Url) {
        try {
            return OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(payloadPartB64Url));
        } catch (Exception e) { return OBJECT_MAPPER.createObjectNode(); }
    }

    private static boolean workspaceMismatch(JsonNode payload, String expectedWorkspaceId) {
        if (expectedWorkspaceId == null || expectedWorkspaceId.isBlank()) return false;
        String tokenWs = payload.path("workspaceId").asText(null);
        return tokenWs != null && !tokenWs.isBlank() && !expectedWorkspaceId.equals(tokenWs);
    }

    private static Optional<PublicKey> resolveVerificationKey(String kid) {
        // 1) PEM override
        PublicKey pemKey = parsePemPublicKey(Optional.ofNullable(System.getProperty("CLOCKIFY_JWT_PUBLIC_KEY"))
                .orElse(Optional.ofNullable(System.getenv("CLOCKIFY_JWT_PUBLIC_KEY")).orElse(
                        Optional.ofNullable(System.getProperty("CLOCKIFY_JWT_PUBLIC_KEY_PEM"))
                                .orElse(System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_PEM"))
                )));
        if (pemKey != null) return Optional.of(pemKey);

        // 2) JWKS by kid (or default)
        String jwksUrl = System.getenv("CLOCKIFY_JWKS_URL");
        if (jwksUrl == null || jwksUrl.isBlank()) return Optional.empty();
        return JwksCache.get(jwksUrl).lookup(kid);
    }

    private static PublicKey parsePemPublicKey(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try {
            String normalized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            logger.warn("Failed to parse public key: {}", e.getMessage());
            return null;
        }
    }

    // Minimal JWKS cache (TTL-based)
    private static final class JwksCache {
        private final String url;
        private volatile long expiresAt;
        private volatile java.util.Map<String, PublicKey> byKid = new java.util.concurrent.ConcurrentHashMap<>();
        private static final long TTL_MILLIS = 10 * 60 * 1000L;

        private JwksCache(String url) { this.url = url; }
        static JwksCache get(String url) { return Holder.instances.computeIfAbsent(url, JwksCache::new); }

        Optional<PublicKey> lookup(String kid) {
            ensureFresh();
            if (kid != null && byKid.containsKey(kid)) return Optional.ofNullable(byKid.get(kid));
            return byKid.values().stream().findFirst();
        }

        private void ensureFresh() {
            long now = System.currentTimeMillis();
            if (now < expiresAt) return;
            synchronized (this) {
                if (now < expiresAt) return;
                try {
                    var client = java.net.http.HttpClient.newHttpClient();
                    var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
                    var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() / 100 != 2) { expiresAt = now + 30_000L; return; }
                    var json = OBJECT_MAPPER.readTree(resp.body());
                    var arr = json.path("keys");
                    java.util.Map<String, PublicKey> next = new java.util.HashMap<>();
                    if (arr.isArray()) {
                        for (JsonNode k : arr) {
                            if (!"RSA".equalsIgnoreCase(k.path("kty").asText(""))) continue;
                            String kid = k.path("kid").asText(java.util.UUID.randomUUID().toString());
                            String n = k.path("n").asText(null);
                            String e = k.path("e").asText(null);
                            PublicKey pk = rsaFromModExp(n, e);
                            if (pk != null) next.put(kid, pk);
                        }
                    }
                    if (!next.isEmpty()) byKid = next;
                    expiresAt = now + TTL_MILLIS;
                } catch (Exception ex) {
                    expiresAt = now + 30_000L;
                }
            }
        }

        private static PublicKey rsaFromModExp(String nB64Url, String eB64Url) {
            try {
                if (nB64Url == null || eB64Url == null) return null;
                java.math.BigInteger n = new java.math.BigInteger(1, Base64.getUrlDecoder().decode(nB64Url));
                java.math.BigInteger e = new java.math.BigInteger(1, Base64.getUrlDecoder().decode(eB64Url));
                java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(n, e);
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (Exception ignored) { return null; }
        }

        private static final class Holder { static final java.util.concurrent.ConcurrentHashMap<String, JwksCache> instances = new java.util.concurrent.ConcurrentHashMap<>(); }
    }

    private static PublicKey resolvePublicKey() {
        String pem = Optional.ofNullable(System.getProperty("CLOCKIFY_JWT_PUBLIC_KEY"))
                .orElse(Optional.ofNullable(System.getenv("CLOCKIFY_JWT_PUBLIC_KEY")).orElse(null));
        if (pem == null || pem.isBlank()) {
            pem = Optional.ofNullable(System.getProperty("CLOCKIFY_JWT_PUBLIC_KEY_PEM"))
                    .orElse(System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_PEM"));
        }
        if (pem == null || pem.isBlank()) return null;
        try {
            String normalized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            logger.warn("Failed to parse public key: {}", e.getMessage());
            return null;
        }
    }
}
