package com.clockify.addon.sdk.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathSanitizerTest {

    @Test
    void testSanitize_Basic() {
        assertEquals("/test", PathSanitizer.sanitize("/test"));
        assertEquals("/test", PathSanitizer.sanitize("test"));
        assertEquals("/test/path", PathSanitizer.sanitize("/test/path"));
    }

    @Test
    void testSanitize_RemovesDuplicateSlashes() {
        assertEquals("/test/path", PathSanitizer.sanitize("//test//path"));
        assertEquals("/test/path", PathSanitizer.sanitize("/test////path"));
    }

    @Test
    void testSanitize_RemovesTrailingSlash() {
        assertEquals("/test", PathSanitizer.sanitize("/test/"));
        assertEquals("/", PathSanitizer.sanitize("/"));
    }

    @Test
    void testSanitize_NullOrEmpty() {
        assertEquals("/", PathSanitizer.sanitize(null));
        assertEquals("/", PathSanitizer.sanitize(""));
        assertEquals("/", PathSanitizer.sanitize("   "));
    }

    @Test
    void testSanitize_RejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                PathSanitizer.sanitize("/test/../admin"));
        assertThrows(IllegalArgumentException.class, () ->
                PathSanitizer.sanitize(".."));
    }

    @Test
    void testSanitize_RejectsNullBytes() {
        // Real null byte and URL-encoded must both be rejected
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
    }

    @Test
    void testSanitize_RejectsLiteralUnicodeEscapeAndBackslashZero() {
        // Literal backslash-u-0000 sequence should be rejected
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\u0000"));
        // Backslash followed by zero ("\\0") should be rejected
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0"));
    }

    @Test
    void testSanitize_AllowsValidCharacters() {
        assertEquals("/test_file-123.json", PathSanitizer.sanitize("/test_file-123.json"));
        assertEquals("/api/v1/users", PathSanitizer.sanitize("/api/v1/users"));
        assertEquals("/path/with-dashes_and_underscores",
                PathSanitizer.sanitize("/path/with-dashes_and_underscores"));
    }

    @Test
    void testNormalize_Basic() {
        assertEquals("/test", PathSanitizer.normalize("/test"));
        assertEquals("/test/path", PathSanitizer.normalize("test/path"));
    }

    @Test
    void testNormalize_HandlesDuplicateSlashes() {
        assertEquals("/test/path", PathSanitizer.normalize("//test//path"));
    }

    @Test
    void testSanitizeLifecyclePath_DefaultPath() {
        assertEquals("/lifecycle/installed",
                PathSanitizer.sanitizeLifecyclePath("INSTALLED", null));
        assertEquals("/lifecycle/deleted",
                PathSanitizer.sanitizeLifecyclePath("DELETED", ""));
    }

    @Test
    void testSanitizeLifecyclePath_CustomPath() {
        assertEquals("/custom/lifecycle",
                PathSanitizer.sanitizeLifecyclePath("INSTALLED", "/custom/lifecycle"));
    }

    @Test
    void testSanitizeLifecyclePath_InvalidCharactersInType() {
        String result = PathSanitizer.sanitizeLifecyclePath("CUSTOM@EVENT!", null);
        assertTrue(result.startsWith("/lifecycle/"));
        assertFalse(result.contains("@"));
        assertFalse(result.contains("!"));
    }

    @Test
    void testSanitizeWebhookPath_Default() {
        assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(null));
        assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(""));
    }

    @Test
    void testSanitizeWebhookPath_Custom() {
        assertEquals("/custom/webhook",
                PathSanitizer.sanitizeWebhookPath("/custom/webhook"));
    }
}
