package com.clockify.addon.sdk.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies webhook signatures using HMAC-SHA256 over the raw body.
 * Expected header formats supported: "sha256=<hex>" or raw hex.
 */
public final class WebhookSignatureValidator {
    private WebhookSignatureValidator() {}

    public static boolean validate(String signatureHeader, byte[] body, String sharedSecret) {
        if (signatureHeader == null || sharedSecret == null || body == null) return false;
        String expected = hmacHex(sharedSecret.getBytes(StandardCharsets.UTF_8), body);
        String provided = normalize(signatureHeader);
        return constantTimeEquals(expected, provided);
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
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
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
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

