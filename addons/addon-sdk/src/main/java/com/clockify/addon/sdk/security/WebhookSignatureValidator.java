package com.clockify.addon.sdk.security;

import com.clockify.addon.sdk.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

/**
 * Webhook signature validation helpers.
 * - verify(request, workspaceId): high-level verification using stored TokenStore secret
 * - validate(signatureHeader, body, secret): low-level validator
 */
public final class WebhookSignatureValidator {
    private WebhookSignatureValidator() {}

    public static final String SIGNATURE_HEADER = "clockify-webhook-signature";
    private static final String[] ALT_HEADERS = new String[]{
            "x-clockify-webhook-signature",
            "Clockify-Webhook-Signature",
            "X-Clockify-Webhook-Signature",
            // Developer workspace often sends JWT under this header
            "Clockify-Signature"
    };

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
     * Validates the request using the stored installation token for the workspace.
     * Returns 401 if token/signature missing; 403 if signature mismatch.
     */
    public static VerificationResult verify(HttpServletRequest request, String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"workspaceId missing\"}", "application/json"));
        }
        var tokenOpt = TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"installation token not found\"}", "application/json"));
        }
        String sigHeader = request.getHeader(SIGNATURE_HEADER);
        if (sigHeader == null || sigHeader.isBlank()) {
            for (String h : ALT_HEADERS) {
                String v = request.getHeader(h);
                if (v != null && !v.isBlank()) { sigHeader = v; break; }
            }
        }
        if (sigHeader == null || sigHeader.isBlank()) {
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"signature header missing\"}", "application/json"));
        }
        byte[] body = readRawBody(request).getBytes(StandardCharsets.UTF_8);

        // Path 1: HMAC (sha256=<hex> or raw hex)
        if (looksLikeHmac(sigHeader)) {
            boolean ok = validate(sigHeader, body, tokenOpt.get().token());
            if (!ok) {
                return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
            }
            return VerificationResult.ok();
        }

        // Path 2: Developer JWT (Clockify-Signature) — optionally accept by inspecting payload
        if (looksLikeJwt(sigHeader) && acceptJwtDevSignature()) {
            try {
                String payload = decodeJwtPayload(sigHeader);
                // naive JSON parse to avoid dependencies
                String wsFromJwt = extractJsonString(payload, "workspaceId");
                if (wsFromJwt != null && wsFromJwt.equals(workspaceId)) {
                    return VerificationResult.ok();
                }
                // fallback: allow when workspaceId missing but header present
                if (wsFromJwt == null || wsFromJwt.isBlank()) {
                    return VerificationResult.ok();
                }
            } catch (Exception ignored) {}
            return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid jwt signature\"}", "application/json"));
        }

        // Unknown format — treat as invalid
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
        String v = System.getProperty("ADDON_ACCEPT_JWT_SIGNATURE");
        if (v == null || v.isBlank()) {
            v = System.getenv("ADDON_ACCEPT_JWT_SIGNATURE");
        }
        return "true".equalsIgnoreCase(v);
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
}
