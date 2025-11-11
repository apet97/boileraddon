package com.clockify.addon.sdk.middleware;

import com.clockify.addon.sdk.logging.RedactingLogger;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestLoggingFilterTest {
    @Test
    void sanitizeHeaders_redactsSensitiveOnes() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer abc");
        headers.put("clockify-webhook-signature", "deadbeef");
        headers.put("Clockify-Webhook-Signature", "deadbeef");
        headers.put("X-Addon-Token", "s3cr3t");
        headers.put("Content-Type", "application/json");

        Map<String, String> out = RequestLoggingFilter.sanitizeHeaders(headers);
        assertEquals(RedactingLogger.REDACTED_VALUE, out.get("Authorization"));
        assertEquals(RedactingLogger.REDACTED_VALUE, out.get("clockify-webhook-signature"));
        assertEquals(RedactingLogger.REDACTED_VALUE, out.get("Clockify-Webhook-Signature"));
        assertEquals(RedactingLogger.REDACTED_VALUE, out.get("X-Addon-Token"));
        assertEquals("application/json", out.get("Content-Type"));
    }

    @Test
    void sanitizeHeaders_preservesNonSensitiveHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Custom-Header", "value123");

        Map<String, String> out = RequestLoggingFilter.sanitizeHeaders(headers);
        assertEquals("value123", out.get("X-Custom-Header"));
    }
}
