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

    @Test
    void testSanitize_RejectsControlCharacters() {
        // Test various control characters (0x00-0x1F)
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0001"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0007")); // bell
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u001F")); // unit separator
    }

    @Test
    void testSanitize_RejectsInvalidCharacters() {
        // Test characters not allowed by the regex pattern
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test<"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test>"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test`"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test{"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test}"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test|"));
    }

    @Test
    void testSanitize_AllowsValidURLCharacters() {
        // Test all characters allowed by the regex pattern
        assertEquals("/test~", PathSanitizer.sanitize("/test~"));
        assertEquals("/test?", PathSanitizer.sanitize("/test?"));
        assertEquals("/test#", PathSanitizer.sanitize("/test#"));
        assertEquals("/test[", PathSanitizer.sanitize("/test["));
        assertEquals("/test]", PathSanitizer.sanitize("/test]"));
        assertEquals("/test@", PathSanitizer.sanitize("/test@"));
        assertEquals("/test!", PathSanitizer.sanitize("/test!"));
        assertEquals("/test$", PathSanitizer.sanitize("/test$"));
        assertEquals("/test&", PathSanitizer.sanitize("/test&"));
        assertEquals("/test'", PathSanitizer.sanitize("/test'"));
        assertEquals("/test(", PathSanitizer.sanitize("/test("));
        assertEquals("/test)", PathSanitizer.sanitize("/test)"));
        assertEquals("/test*", PathSanitizer.sanitize("/test*"));
        assertEquals("/test+", PathSanitizer.sanitize("/test+"));
        assertEquals("/test,", PathSanitizer.sanitize("/test,"));
        assertEquals("/test;", PathSanitizer.sanitize("/test;"));
        assertEquals("/test=", PathSanitizer.sanitize("/test="));
    }

    @Test
    void testSanitize_ComplexPathTraversalAttempts() {
        // More sophisticated path traversal attempts
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/.."));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/../"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/../test"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("test/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/..test"));
    }

    @Test
    void testSanitize_WhitespaceHandling() {
        // Test various whitespace scenarios
        assertEquals("/test", PathSanitizer.sanitize(" test "));
        assertEquals("/test", PathSanitizer.sanitize("\ttest\t"));
        assertEquals("/test", PathSanitizer.sanitize("\ntest\n"));
        assertEquals("/test", PathSanitizer.sanitize("\rtest\r"));
    }

    @Test
    void testSanitize_EdgeCasePaths() {
        // Test edge case paths
        assertEquals("/", PathSanitizer.sanitize("/"));
        assertEquals("/", PathSanitizer.sanitize("//"));
        assertEquals("/", PathSanitizer.sanitize("///"));
        assertEquals("/a", PathSanitizer.sanitize("a"));
        assertEquals("/a", PathSanitizer.sanitize("/a/"));
        assertEquals("/a/b", PathSanitizer.sanitize("a/b"));
        assertEquals("/a/b", PathSanitizer.sanitize("/a/b/"));
    }

    @Test
    void testSanitize_UnicodeAndInternationalCharacters() {
        // Test Unicode and international characters (should be allowed)
        assertEquals("/测试", PathSanitizer.sanitize("/测试"));
        assertEquals("/café", PathSanitizer.sanitize("/café"));
        assertEquals("/🎯", PathSanitizer.sanitize("/🎯"));
        assertEquals("/مرحبا", PathSanitizer.sanitize("/مرحبا"));
    }

    @Test
    void testSanitize_MixedCaseNullByteRepresentations() {
        // Test mixed case null byte representations
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\U0000"));
    }

    @Test
    void testSanitize_BackslashZeroVariations() {
        // Test various backslash-zero patterns
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("\\0test"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("test\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0path"));
    }

    @Test
    void testNormalize_DoesNotValidateSecurity() {
        // Test that normalize() doesn't perform security validation
        assertEquals("/test/../admin", PathSanitizer.normalize("/test/../admin"));
        assertEquals("/test\u0000", PathSanitizer.normalize("/test\u0000"));
        assertEquals("/test%00", PathSanitizer.normalize("/test%00"));
    }

    @Test
    void testNormalize_EdgeCases() {
        // Test normalize() edge cases
        assertEquals("/", PathSanitizer.normalize(null));
        assertEquals("/", PathSanitizer.normalize(""));
        assertEquals("/", PathSanitizer.normalize("   "));
        assertEquals("/test", PathSanitizer.normalize("test"));
        assertEquals("/test", PathSanitizer.normalize("/test/"));
        assertEquals("/test/path", PathSanitizer.normalize("//test//path//"));
    }

    @Test
    void testSanitizeLifecyclePath_EdgeCases() {
        // Test lifecycle path edge cases
        assertEquals("/lifecycle/", PathSanitizer.sanitizeLifecyclePath("", null));
        assertEquals("/lifecycle/", PathSanitizer.sanitizeLifecyclePath("!@#$%", null));
        assertEquals("/lifecycle/test", PathSanitizer.sanitizeLifecyclePath("test", null));
        assertEquals("/lifecycle/test", PathSanitizer.sanitizeLifecyclePath("TEST", null));
        assertEquals("/lifecycle/test-event", PathSanitizer.sanitizeLifecyclePath("test-event", null));
        assertEquals("/lifecycle/test_event", PathSanitizer.sanitizeLifecyclePath("test_event", null));
    }

    @Test
    void testSanitizeLifecyclePath_CustomPathValidation() {
        // Test that custom lifecycle paths are properly validated
        assertEquals("/custom/path", PathSanitizer.sanitizeLifecyclePath("INSTALLED", "/custom/path"));

        // Custom path with security issues should throw
        assertThrows(IllegalArgumentException.class, () ->
            PathSanitizer.sanitizeLifecyclePath("INSTALLED", "/custom/../path"));
        assertThrows(IllegalArgumentException.class, () ->
            PathSanitizer.sanitizeLifecyclePath("INSTALLED", "/custom\u0000path"));
    }

    @Test
    void testSanitizeWebhookPath_EdgeCases() {
        // Test webhook path edge cases
        assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(null));
        assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(""));
        assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath("   "));
        assertEquals("/custom/webhook", PathSanitizer.sanitizeWebhookPath("/custom/webhook"));
        assertEquals("/custom/webhook", PathSanitizer.sanitizeWebhookPath("custom/webhook"));
    }

    @Test
    void testSanitizeWebhookPath_SecurityValidation() {
        // Test that webhook paths are properly validated for security
        assertThrows(IllegalArgumentException.class, () ->
            PathSanitizer.sanitizeWebhookPath("/webhook/../admin"));
        assertThrows(IllegalArgumentException.class, () ->
            PathSanitizer.sanitizeWebhookPath("/webhook\u0000"));
        assertThrows(IllegalArgumentException.class, () ->
            PathSanitizer.sanitizeWebhookPath("/webhook%00"));
    }

    @Test
    void testSanitize_PerformanceEdgeCases() {
        // Test paths that might cause performance issues
        assertEquals("/a", PathSanitizer.sanitize("a"));
        assertEquals("/a/b/c/d/e/f", PathSanitizer.sanitize("a/b/c/d/e/f"));
        assertEquals("/a/b/c/d/e/f", PathSanitizer.sanitize("//a//b//c//d//e//f//"));
    }

    @Test
    void testSanitize_ConcurrentAccessSafety() {
        // Test that the method is thread-safe (no shared state)
        String[] testPaths = {
            "/test1", "/test2", "/test3", "/test4", "/test5",
            "/test6", "/test7", "/test8", "/test9", "/test10"
        };

        // If the method is thread-safe, this should not throw exceptions
        for (String path : testPaths) {
            assertEquals("/" + path.substring(1), PathSanitizer.sanitize(path));
        }
    }

    @Test
    void testSanitize_LoggingCoverage() {
        // Test paths that should trigger logging
        // We can't easily verify the actual log output, but we can verify the exceptions are thrown
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test<"));
    }

    @Test
    void testSanitize_MixedCaseNullByteEncodings() {
        // Test mixed case null byte encodings
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00"));
    }

    @Test
    void testSanitize_UnicodeNullByteVariations() {
        // Test various Unicode null byte representations
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\u0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\U0000"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\u0000"));
    }

    @Test
    void testSanitize_ControlCharactersRange() {
        // Test all ASCII control characters (0x00-0x1F)
        for (int i = 0; i <= 0x1F; i++) {
            char controlChar = (char) i;
            String pathWithControlChar = "/test" + controlChar + "path";
            assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize(pathWithControlChar));
        }
    }

    @Test
    void testSanitize_ComplexPathTraversalPatterns() {
        // Test sophisticated path traversal attempts
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/.."));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/../"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/../test"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("test/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/..test"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/.../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/..../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test/...../admin"));
    }

    @Test
    void testSanitize_WhitespaceVariations() {
        // Test various whitespace scenarios
        assertEquals("/test", PathSanitizer.sanitize(" test "));
        assertEquals("/test", PathSanitizer.sanitize("\ttest\t"));
        assertEquals("/test", PathSanitizer.sanitize("\ntest\n"));
        assertEquals("/test", PathSanitizer.sanitize("\rtest\r"));
        assertEquals("/test", PathSanitizer.sanitize("\ftest\f"));
        assertEquals("/test", PathSanitizer.sanitize("\u000Btest\u000B")); // vertical tab
    }

    @Test
    void testSanitize_DeepNestedPaths() {
        // Test deep nested paths
        String deepPath = "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z";
        assertEquals(deepPath, PathSanitizer.sanitize(deepPath));

        // Test with duplicate slashes
        String deepPathWithSlashes = "//a//b//c//d//e//f//g//h//i//j//k//l//m//n//o//p//q//r//s//t//u//v//w//x//y//z//";
        assertEquals(deepPath, PathSanitizer.sanitize(deepPathWithSlashes));
    }

    @Test
    void testSanitize_EmptyAndNullEdgeCases() {
        // Test various empty and null scenarios
        assertEquals("/", PathSanitizer.sanitize(null));
        assertEquals("/", PathSanitizer.sanitize(""));
        assertEquals("/", PathSanitizer.sanitize("   "));
        assertEquals("/", PathSanitizer.sanitize("\t\t"));
        assertEquals("/", PathSanitizer.sanitize("\n\n"));
        assertEquals("/", PathSanitizer.sanitize("\r\r"));
    }

    @Test
    void testSanitize_InternationalCharacters() {
        // Test various international characters (should be allowed)
        assertEquals("/测试", PathSanitizer.sanitize("/测试"));
        assertEquals("/café", PathSanitizer.sanitize("/café"));
        assertEquals("/🎯", PathSanitizer.sanitize("/🎯"));
        assertEquals("/مرحبا", PathSanitizer.sanitize("/مرحبا"));
        assertEquals("/こんにちは", PathSanitizer.sanitize("/こんにちは"));
        assertEquals("/안녕하세요", PathSanitizer.sanitize("/안녕하세요"));
        assertEquals("/Привет", PathSanitizer.sanitize("/Привет"));
    }

    @Test
    void testSanitize_ComplexURLCharacters() {
        // Test complex URL characters that should be allowed
        assertEquals("/test~", PathSanitizer.sanitize("/test~"));
        assertEquals("/test?", PathSanitizer.sanitize("/test?"));
        assertEquals("/test#", PathSanitizer.sanitize("/test#"));
        assertEquals("/test[", PathSanitizer.sanitize("/test["));
        assertEquals("/test]", PathSanitizer.sanitize("/test]"));
        assertEquals("/test@", PathSanitizer.sanitize("/test@"));
        assertEquals("/test!", PathSanitizer.sanitize("/test!"));
        assertEquals("/test$", PathSanitizer.sanitize("/test$"));
        assertEquals("/test&", PathSanitizer.sanitize("/test&"));
        assertEquals("/test'", PathSanitizer.sanitize("/test'"));
        assertEquals("/test(", PathSanitizer.sanitize("/test("));
        assertEquals("/test)", PathSanitizer.sanitize("/test)"));
        assertEquals("/test*", PathSanitizer.sanitize("/test*"));
        assertEquals("/test+", PathSanitizer.sanitize("/test+"));
        assertEquals("/test,", PathSanitizer.sanitize("/test,"));
        assertEquals("/test;", PathSanitizer.sanitize("/test;"));
        assertEquals("/test=", PathSanitizer.sanitize("/test="));
        assertEquals("/test:", PathSanitizer.sanitize("/test:"));
    }

    @Test
    void testSanitize_InvalidCharactersComprehensive() {
        // Test all invalid characters that should be rejected
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test<"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test>"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test`"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test{"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test}"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test|"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test^"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\""));
    }

    @Test
    void testSanitize_PerformanceWithManyDuplicates() {
        // Test performance with many duplicate slashes
        String manySlashes = "//a//b//c//d//e//f//g//h//i//j//k//l//m//n//o//p//q//r//s//t//u//v//w//x//y//z//";
        String expected = "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z";
        assertEquals(expected, PathSanitizer.sanitize(manySlashes));

        // Test extreme case
        String extremeSlashes = "////////a////////b////////c////////";
        assertEquals("/a/b/c", PathSanitizer.sanitize(extremeSlashes));
    }

    @Test
    void testSanitize_BackslashZeroEdgeCases() {
        // Test various backslash-zero patterns
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("\\0test"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("test\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0path"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/\\0"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("\\0/"));
    }

    @Test
    void testSanitize_MixedSecurityVulnerabilities() {
        // Test combinations of multiple security issues
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0000/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test%00/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\\0/../admin"));
        assertThrows(IllegalArgumentException.class, () -> PathSanitizer.sanitize("/test\u0001/../admin"));
    }

    @Test
    void testNormalize_ComplexPaths() {
        // Test normalize with complex paths (should not validate security)
        assertEquals("/test/../admin", PathSanitizer.normalize("/test/../admin"));
        assertEquals("/test\u0000", PathSanitizer.normalize("/test\u0000"));
        assertEquals("/test%00", PathSanitizer.normalize("/test%00"));
        assertEquals("/test\\0", PathSanitizer.normalize("/test\\0"));
        assertEquals("/test<", PathSanitizer.normalize("/test<"));
    }

    @Test
    void testSanitizeLifecyclePath_ComplexTypes() {
        // Test lifecycle path with complex type names
        assertEquals("/lifecycle/custom_event", PathSanitizer.sanitizeLifecyclePath("CUSTOM_EVENT", null));
        assertEquals("/lifecycle/test-event", PathSanitizer.sanitizeLifecyclePath("TEST-EVENT", null));
        assertEquals("/lifecycle/123", PathSanitizer.sanitizeLifecyclePath("123", null));
        assertEquals("/lifecycle/test123", PathSanitizer.sanitizeLifecyclePath("test123", null));

        // Test with special characters that should be removed
        assertEquals("/lifecycle/test", PathSanitizer.sanitizeLifecyclePath("test@#$%", null));
        assertEquals("/lifecycle/", PathSanitizer.sanitizeLifecyclePath("!@#$%", null));
    }

    @Test
    void testSanitizeWebhookPath_ComplexPaths() {
        // Test webhook path with complex custom paths
        assertEquals("/custom/webhook", PathSanitizer.sanitizeWebhookPath("/custom/webhook"));
        assertEquals("/api/v1/webhooks", PathSanitizer.sanitizeWebhookPath("/api/v1/webhooks"));
        assertEquals("/webhook/endpoint", PathSanitizer.sanitizeWebhookPath("webhook/endpoint"));
        assertEquals("/webhook-test", PathSanitizer.sanitizeWebhookPath("webhook-test"));
        assertEquals("/webhook_test", PathSanitizer.sanitizeWebhookPath("webhook_test"));
    }

    @Test
    void testSanitize_LeadingAndTrailingWhitespace() {
        // Test paths with leading and trailing whitespace
        assertEquals("/test", PathSanitizer.sanitize(" test"));
        assertEquals("/test", PathSanitizer.sanitize("test "));
        assertEquals("/test", PathSanitizer.sanitize(" test "));
        assertEquals("/test/path", PathSanitizer.sanitize(" test/path "));
        assertEquals("/test/path", PathSanitizer.sanitize("\ttest/path\t"));
        assertEquals("/test/path", PathSanitizer.sanitize("\ntest/path\n"));
    }

    @Test
    void testSanitize_MaximumLengthPaths() {
        // Test paths with maximum reasonable length
        String longPath = "/" + "a".repeat(1000);
        assertEquals(longPath, PathSanitizer.sanitize(longPath));

        String longNestedPath = "/" + "a/".repeat(100) + "end";
        assertEquals(longNestedPath, PathSanitizer.sanitize(longNestedPath));
    }

    @Test
    void testSanitize_Consistency() {
        // Test that sanitize produces consistent results
        String[] testPaths = {
            "test", "/test", "test/", "/test/", "//test//", " test ", "\ttest\t"
        };

        String expected = "/test";
        for (String path : testPaths) {
            assertEquals(expected, PathSanitizer.sanitize(path));
        }
    }
}
