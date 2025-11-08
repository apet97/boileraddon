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
        String sigHeader = request.getHeader("clockify-webhook-signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            return new VerificationResult(false, HttpResponse.error(401, "{\"error\":\"signature header missing\"}", "application/json"));
        }
        byte[] body = readRawBody(request).getBytes(StandardCharsets.UTF_8);
        boolean ok = validate(sigHeader, body, tokenOpt.get().token());
        if (!ok) {
            return new VerificationResult(false, HttpResponse.error(403, "{\"error\":\"invalid signature\"}", "application/json"));
        }
        return VerificationResult.ok();
    }

    /** Low-level validator using HMAC-SHA256 hex. */
    public static boolean validate(String signatureHeader, byte[] body, String sharedSecret) {
        if (signatureHeader == null || sharedSecret == null || body == null) return false;
        String expected = hmacHex(sharedSecret.getBytes(StandardCharsets.UTF_8), body);
        String provided = normalize(signatureHeader);
        return constantTimeEquals(expected, provided);
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
}
