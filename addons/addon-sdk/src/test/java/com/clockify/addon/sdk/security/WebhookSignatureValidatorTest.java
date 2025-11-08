package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureValidatorTest {

    @Test
    void normalizeStripsPrefixBeforeEquals() {
        assertEquals("abcd", callNormalize("sha256=abcd"));
        assertEquals("abcd", callNormalize("  sha256=abcd  "));
        assertEquals("", callNormalize("sha256="));
        // No equals → value returned as-is
        assertEquals("abcd", callNormalize("abcd"));
    }

    @Test
    void validateMatchesComputedSignature() {
        String secret = "s3cr3t";
        String body = "{\"x\":1}";
        String header = WebhookSignatureValidator.computeSignature(secret, body);
        assertTrue(WebhookSignatureValidator.validate(header, body.getBytes(), secret));

        // Wrong secret
        assertFalse(WebhookSignatureValidator.validate(header, body.getBytes(), "other"));
        // Wrong body
        assertFalse(WebhookSignatureValidator.validate(header, "other".getBytes(), secret));
        // Missing header
        assertFalse(WebhookSignatureValidator.validate(null, body.getBytes(), secret));
    }

    private static String callNormalize(String h) {
        try {
            var m = WebhookSignatureValidator.class.getDeclaredMethod("normalize", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

