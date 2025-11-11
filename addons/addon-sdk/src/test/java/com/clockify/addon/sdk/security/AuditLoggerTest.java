package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLogger class.
 * Tests audit logging functionality, JSON formatting, and security event tracking.
 */
class AuditLoggerTest {

    private static final Logger auditLog = LoggerFactory.getLogger("com.clockify.addon.audit");

    @Test
    void testAuditEventEnumValues() {
        // Verify all audit events have descriptions
        for (AuditLogger.AuditEvent event : AuditLogger.AuditEvent.values()) {
            assertNotNull(event.description);
            assertFalse(event.description.isBlank());
        }

        // Test specific event descriptions
        assertEquals("Token validation succeeded", AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS.description);
        assertEquals("Token validation failed", AuditLogger.AuditEvent.TOKEN_VALIDATION_FAILURE.description);
        assertEquals("Rate limit exceeded", AuditLogger.AuditEvent.RATE_LIMIT_EXCEEDED.description);
    }

    @Test
    void testLogMethodReturnsBuilder() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS);
        assertNotNull(entry);
        // Can't test internal event field directly, but we can test the builder works
    }

    @Test
    void testLogWithNullEvent() {
        assertThrows(NullPointerException.class, () -> {
            AuditLogger.log(null);
        });
    }

    @Test
    void testAuditEntryWorkspace() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.TOKEN_SAVED)
                .workspace("ws-test-123");
        assertNotNull(entry);
        // Can't test internal state directly, but we can test it doesn't throw
    }

    @Test
    void testAuditEntryClientIp() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.SUSPICIOUS_REQUEST)
                .clientIp("192.168.1.100");
        assertNotNull(entry);
    }

    @Test
    void testAuditEntryUserId() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.TOKEN_LOOKUP_FAILURE)
                .userId("user-test-456");
        assertNotNull(entry);
    }

    @Test
    void testAuditEntryDetail() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.RATE_LIMIT_EXCEEDED)
                .detail("limit", 100)
                .detail("current", 150)
                .detail("window", "1 minute");
        assertNotNull(entry);
    }

    @Test
    void testAuditEntryDetailWithNullKey() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.INVALID_JSON)
                .detail(null, "value");
        assertNotNull(entry);
        // Should handle null key gracefully
    }

    @Test
    void testAuditEntryDetailWithNullValue() {
        AuditLogger.AuditEntry entry = AuditLogger.log(AuditLogger.AuditEvent.INVALID_PAYLOAD_SIZE)
                .detail("size", null);
        assertNotNull(entry);
        // Should handle null value gracefully
    }

    @Test
    void testInfoLogging() {
        // This test verifies that info logging doesn't throw exceptions
        // We can't easily test the actual log output, but we can verify it doesn't crash
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS)
                .workspace("ws-test-123")
                .clientIp("192.168.1.1")
                .userId("user-456")
                .detail("validationTimeMs", 25)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testWarnLogging() {
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_FAILURE)
                .workspace("ws-test-456")
                .clientIp("10.0.0.1")
                .detail("reason", "invalid_signature")
                .detail("attempts", 3)
                .warn();
        // If we get here without exception, the test passes
    }

    @Test
    void testErrorLogging() {
        AuditLogger.log(AuditLogger.AuditEvent.MULTIPLE_AUTH_FAILURES)
                .workspace("ws-test-789")
                .clientIp("203.0.113.1")
                .detail("failures", 10)
                .detail("timeWindow", "5 minutes")
                .error();
        // If we get here without exception, the test passes
    }

    @Test
    void testJsonEscapeQuotes() {
        // Test JSON escaping for quotes
        String input = "test\"quoted\"string";
        String escaped = escapeJsonForTest(input);
        assertFalse(escaped.contains("\"")); // Should not contain unescaped quotes
    }

    @Test
    void testJsonEscapeBackslashes() {
        // Test JSON escaping for backslashes
        String input = "test\\backslash";
        String escaped = escapeJsonForTest(input);
        assertFalse(escaped.contains("\\")); // Should not contain unescaped backslashes
    }

    @Test
    void testJsonEscapeControlCharacters() {
        // Test JSON escaping for control characters
        String input = "test\nnewline\ttab";
        String escaped = escapeJsonForTest(input);
        assertFalse(escaped.contains("\n")); // Should not contain unescaped newlines
        assertFalse(escaped.contains("\t")); // Should not contain unescaped tabs
    }

    @Test
    void testJsonEscapeNullInput() {
        String escaped = escapeJsonForTest(null);
        assertEquals("", escaped); // Should return empty string for null
    }

    @Test
    void testMultipleDetails() {
        AuditLogger.log(AuditLogger.AuditEvent.DATABASE_QUERY_ERROR)
                .workspace("ws-multi-123")
                .detail("query", "SELECT * FROM tokens")
                .detail("error", "Connection timeout")
                .detail("retryCount", 3)
                .detail("success", false)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testChainedOperations() {
        // Test that chaining operations works correctly
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_ROTATED)
                .workspace("ws-chain-123")
                .clientIp("192.168.1.50")
                .userId("user-chain-456")
                .detail("rotationTimeMs", 150)
                .detail("oldTokenHash", "sha256-old")
                .detail("newTokenHash", "sha256-new")
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testAllAuditEventsCanBeLogged() {
        // Test that all audit events can be logged without exceptions
        for (AuditLogger.AuditEvent event : AuditLogger.AuditEvent.values()) {
            AuditLogger.log(event)
                    .workspace("ws-all-events")
                    .detail("test", true)
                    .info();
        }
        // If we get here without exception, all events can be logged
    }

    @Test
    void testEmptyDetails() {
        // Test logging with no details
        AuditLogger.log(AuditLogger.AuditEvent.CSRF_TOKEN_GENERATED)
                .workspace("ws-empty-details")
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testNullWorkspace() {
        // Test that null workspace is handled gracefully
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_REMOVED)
                .workspace(null)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testNullClientIp() {
        // Test that null client IP is handled gracefully
        AuditLogger.log(AuditLogger.AuditEvent.INSECURE_CONNECTION_REJECTED)
                .clientIp(null)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testNullUserId() {
        // Test that null user ID is handled gracefully
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_LOOKUP_FAILURE)
                .userId(null)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testSpecialCharactersInDetails() {
        // Test that special characters in details are handled correctly
        AuditLogger.log(AuditLogger.AuditEvent.INVALID_EVENT_TYPE)
                .workspace("ws-special-测试-123")
                .detail("eventType", "event-with-\"quotes\"-and-\\backslashes")
                .detail("unicode", "🎯 emoji and 测试 chinese")
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testLongValues() {
        // Test that long values are handled correctly
        String longWorkspaceId = "ws-" + "x".repeat(1000);
        String longDetailValue = "detail-" + "y".repeat(1000);

        AuditLogger.log(AuditLogger.AuditEvent.SUSPICIOUS_REQUEST)
                .workspace(longWorkspaceId)
                .detail("longValue", longDetailValue)
                .info();
        // If we get here without exception, the test passes
    }

    @Test
    void testMixedDetailTypes() {
        // Test different detail value types
        AuditLogger.log(AuditLogger.AuditEvent.DATABASE_CONNECTION_ERROR)
                .workspace("ws-mixed-123")
                .detail("string", "error message")
                .detail("number", 42)
                .detail("boolean", true)
                .detail("float", 3.14)
                .info();
        // If we get here without exception, the test passes
    }

    // Helper method to test JSON escaping (using reflection to access private method)
    private String escapeJsonForTest(String input) {
        try {
            var method = AuditLogger.AuditEntry.class.getDeclaredMethod("escapeJson", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to test JSON escaping", e);
        }
    }
}