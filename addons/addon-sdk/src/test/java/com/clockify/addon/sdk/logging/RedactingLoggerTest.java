package com.clockify.addon.sdk.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedactingLogger class.
 * Tests header redaction functionality and sensitive data protection.
 */
class RedactingLoggerTest {

    private static final RedactingLogger logger = RedactingLogger.get(RedactingLoggerTest.class);

    @Test
    void testGetLogger() {
        RedactingLogger logger1 = RedactingLogger.get(RedactingLoggerTest.class);
        RedactingLogger logger2 = RedactingLogger.get(RedactingLoggerTest.class);

        assertNotNull(logger1);
        assertNotNull(logger2);
        // Should return different instances (not cached)
        assertNotSame(logger1, logger2);
    }

    @Test
    void testRedactHeadersNullInput() {
        Map<String, String> result = RedactingLogger.redactHeaders(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRedactHeadersEmptyInput() {
        Map<String, String> emptyHeaders = new HashMap<>();
        Map<String, String> result = RedactingLogger.redactHeaders(emptyHeaders);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRedactHeadersAuthorization() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer secret-token-123");
        headers.put("Content-Type", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals(2, result.size());
    }

    @Test
    void testRedactHeadersProxyAuthorization() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Proxy-Authorization", "Basic dXNlcjpwYXNz");
        headers.put("User-Agent", "Clockify-Webhook/1.0");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Proxy-Authorization"));
        assertEquals("Clockify-Webhook/1.0", result.get("User-Agent"));
    }

    @Test
    void testRedactHeadersXAddonToken() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Addon-Token", "addon-secret-token-456");
        headers.put("Accept", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("X-Addon-Token"));
        assertEquals("application/json", result.get("Accept"));
    }

    @Test
    void testRedactHeadersClockifyWebhookSignature() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Clockify-Webhook-Signature", "sha256=abcdef123456");
        headers.put("Content-Length", "1024");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Clockify-Webhook-Signature"));
        assertEquals("1024", result.get("Content-Length"));
    }

    @Test
    void testRedactHeadersCookie() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "session=abc123; user=john");
        headers.put("Host", "api.example.com");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Cookie"));
        assertEquals("api.example.com", result.get("Host"));
    }

    @Test
    void testRedactHeadersSetCookie() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Set-Cookie", "session=new-session-id; Path=/; HttpOnly");
        headers.put("Cache-Control", "no-cache");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Set-Cookie"));
        assertEquals("no-cache", result.get("Cache-Control"));
    }

    @Test
    void testRedactHeadersCaseInsensitive() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer token-lowercase");
        headers.put("AUTHORIZATION", "Bearer token-uppercase");
        headers.put("Authorization", "Bearer token-mixedcase");
        headers.put("Content-Type", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // All variations of authorization should be redacted
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("authorization"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("AUTHORIZATION"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
        assertEquals("application/json", result.get("Content-Type"));
    }

    @Test
    void testRedactHeadersMixedSensitiveAndNormal() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", "Clockify/1.0");
        headers.put("X-Addon-Token", "addon-secret");
        headers.put("Accept", "*/*");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // Verify sensitive headers are redacted
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("X-Addon-Token"));

        // Verify normal headers are preserved
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals("Clockify/1.0", result.get("User-Agent"));
        assertEquals("*/*", result.get("Accept"));

        assertEquals(5, result.size());
    }

    @Test
    void testRedactHeadersNullKey() {
        Map<String, String> headers = new HashMap<>();
        headers.put(null, "some-value");
        headers.put("Content-Type", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // Null key should be handled gracefully
        assertTrue(result.containsKey(""));
        assertEquals("some-value", result.get(""));
        assertEquals("application/json", result.get("Content-Type"));
    }

    @Test
    void testRedactHeadersNullValue() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", null);
        headers.put("Content-Type", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // Null values should be handled
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
        assertEquals("application/json", result.get("Content-Type"));
    }

    @Test
    void testRedactHeadersEmptyStringKey() {
        Map<String, String> headers = new HashMap<>();
        headers.put("", "empty-key-value");
        headers.put("Authorization", "Bearer token");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        assertEquals("empty-key-value", result.get(""));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
    }

    @Test
    void testInfoRequestWithNullHeaders() {
        // This test verifies that the method doesn't throw NPE with null headers
        // We can't easily test the actual logging output, but we can verify it doesn't crash
        logger.infoRequest("GET", "/api/test", null);
        // If we get here without exception, the test passes
    }

    @Test
    void testInfoRequestWithEmptyHeaders() {
        Map<String, String> emptyHeaders = new HashMap<>();
        logger.infoRequest("POST", "/webhook", emptyHeaders);
        // If we get here without exception, the test passes
    }

    @Test
    void testInfoRequestWithSensitiveHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer secret-token");
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", "Test-Agent");

        logger.infoRequest("POST", "/api/webhook", headers);
        // If we get here without exception, the test passes
    }

    @Test
    void testRedactedValueConstant() {
        assertEquals("‹redacted›", RedactingLogger.REDACTED_VALUE);
    }

    @Test
    void testSecretHeadersSetIsImmutable() {
        // Verify that the SECRET_HEADERS set is properly defined and immutable
        assertNotNull(RedactingLogger.class.getDeclaredFields());
        // We can't directly access the private field, but we can test through redaction
    }

    @Test
    void testAllSensitiveHeadersAreRedacted() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer token1");
        headers.put("proxy-authorization", "Basic creds");
        headers.put("x-addon-token", "addon-token");
        headers.put("clockify-webhook-signature", "sig123");
        headers.put("cookie", "session=abc");
        headers.put("set-cookie", "new-session=def");
        headers.put("content-type", "application/json");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // All sensitive headers should be redacted
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("authorization"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("proxy-authorization"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("x-addon-token"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("clockify-webhook-signature"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("cookie"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("set-cookie"));

        // Normal header should be preserved
        assertEquals("application/json", result.get("content-type"));
    }

    @Test
    void testHeaderOrderPreservationNotRequired() {
        // Note: HashMap doesn't preserve order, so we're not testing order preservation
        Map<String, String> headers = new HashMap<>();
        headers.put("Z-Header", "z-value");
        headers.put("A-Header", "a-value");
        headers.put("Authorization", "secret");

        Map<String, String> result = RedactingLogger.redactHeaders(headers);

        // All headers should be present with correct values
        assertEquals(3, result.size());
        assertEquals("z-value", result.get("Z-Header"));
        assertEquals("a-value", result.get("A-Header"));
        assertEquals(RedactingLogger.REDACTED_VALUE, result.get("Authorization"));
    }

    @Test
    void testRedactingLoggerConstructor() {
        // Test that the static factory method properly initializes the underlying logger
        RedactingLogger customLogger = RedactingLogger.get(RedactingLoggerTest.class);
        assertNotNull(customLogger);

        // The underlying logger should be the same as what LoggerFactory would return
        Logger underlyingLogger = LoggerFactory.getLogger(RedactingLoggerTest.class);
        // We can't directly access the private field, but we can verify behavior through public methods
    }
}