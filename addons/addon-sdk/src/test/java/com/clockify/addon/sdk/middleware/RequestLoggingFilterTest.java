package com.clockify.addon.sdk.middleware;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {
    @Test
    void sanitizeHeaders_redactsSensitiveOnes() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer abc");
        headers.put("clockify-webhook-signature", "deadbeef");
        headers.put("X-Addon-Token", "s3cr3t");
        headers.put("Content-Type", "application/json");

        Map<String, String> out = RequestLoggingFilter.sanitizeHeaders(headers);
        assertEquals("[REDACTED]", out.get("Authorization"));
        assertEquals("[REDACTED]", out.get("clockify-webhook-signature"));
        assertEquals("[REDACTED]", out.get("X-Addon-Token"));
        assertEquals("application/json", out.get("Content-Type"));
    }
}

