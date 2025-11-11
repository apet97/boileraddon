package com.clockify.addon.sdk.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoggingContext class.
 * Tests MDC context management and request-based context creation.
 */
class LoggingContextTest {

    @BeforeEach
    @AfterEach
    void clearMDC() {
        MDC.clear();
    }

    @Test
    void testCreateEmptyContext() {
        try (LoggingContext context = LoggingContext.create()) {
            assertNotNull(context);
            // No MDC values should be set for empty context
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
    }

    @Test
    void testWorkspaceContext() {
        try (LoggingContext context = LoggingContext.create().workspace("ws-test-123")) {
            assertEquals("ws-test-123", MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
        // Verify cleanup after close
        assertNull(MDC.get("workspaceId"));
    }

    @Test
    void testUserContext() {
        try (LoggingContext context = LoggingContext.create().user("user-test-456")) {
            assertEquals("user-test-456", MDC.get("userId"));
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("requestId"));
        }
        // Verify cleanup after close
        assertNull(MDC.get("userId"));
    }

    @Test
    void testRequestIdContext() {
        try (LoggingContext context = LoggingContext.create().request("req-test-789")) {
            assertEquals("req-test-789", MDC.get("requestId"));
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
        }
        // Verify cleanup after close
        assertNull(MDC.get("requestId"));
    }

    @Test
    void testMultipleContextValues() {
        try (LoggingContext context = LoggingContext.create()
                .workspace("ws-multi-123")
                .user("user-multi-456")
                .request("req-multi-789")) {

            assertEquals("ws-multi-123", MDC.get("workspaceId"));
            assertEquals("user-multi-456", MDC.get("userId"));
            assertEquals("req-multi-789", MDC.get("requestId"));
        }
        // Verify all values are cleaned up
        assertNull(MDC.get("workspaceId"));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void testNullValuesAreIgnored() {
        try (LoggingContext context = LoggingContext.create()
                .workspace(null)
                .user("")
                .request("   ")) {

            // Null, empty, and blank values should not be set in MDC
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
    }

    @Test
    void testCreateFromRequestWithAttributes() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute("clockify.workspaceId")).thenReturn("ws-from-req-123");
        when(mockRequest.getAttribute("clockify.userId")).thenReturn("user-from-req-456");

        try (LoggingContext context = LoggingContext.create(mockRequest)) {
            assertEquals("ws-from-req-123", MDC.get("workspaceId"));
            assertEquals("user-from-req-456", MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
        // Verify cleanup
        assertNull(MDC.get("workspaceId"));
        assertNull(MDC.get("userId"));
    }

    @Test
    void testCreateFromRequestWithNullAttributes() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute("clockify.workspaceId")).thenReturn(null);
        when(mockRequest.getAttribute("clockify.userId")).thenReturn(null);

        try (LoggingContext context = LoggingContext.create(mockRequest)) {
            // Null attributes should not be set in MDC
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
    }

    @Test
    void testCreateFromNullRequest() {
        try (LoggingContext context = LoggingContext.create((HttpServletRequest) null)) {
            // Should handle null request gracefully
            assertNull(MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
            assertNull(MDC.get("requestId"));
        }
    }

    @Test
    void testChainedOperations() {
        // Test that chaining operations returns the same instance
        LoggingContext context1 = LoggingContext.create();
        LoggingContext context2 = context1.workspace("ws-chain-123");
        LoggingContext context3 = context2.user("user-chain-456");

        assertSame(context1, context2);
        assertSame(context2, context3);

        try (LoggingContext context = context3) {
            assertEquals("ws-chain-123", MDC.get("workspaceId"));
            assertEquals("user-chain-456", MDC.get("userId"));
        }
    }

    @Test
    void testMultipleContextsDoNotInterfere() {
        // Test that multiple contexts can exist independently
        try (LoggingContext outer = LoggingContext.create().workspace("ws-outer")) {
            assertEquals("ws-outer", MDC.get("workspaceId"));

            try (LoggingContext inner = LoggingContext.create().user("user-inner")) {
                assertEquals("ws-outer", MDC.get("workspaceId"));
                assertEquals("user-inner", MDC.get("userId"));
            }

            // Outer context should still be active
            assertEquals("ws-outer", MDC.get("workspaceId"));
            assertNull(MDC.get("userId"));
        }
        // Everything should be cleaned up
        assertNull(MDC.get("workspaceId"));
        assertNull(MDC.get("userId"));
    }

    @Test
    void testExceptionInContextDoesNotPreventCleanup() {
        // Test that exceptions don't prevent MDC cleanup
        try (LoggingContext context = LoggingContext.create().workspace("ws-exception")) {
            assertEquals("ws-exception", MDC.get("workspaceId"));
            throw new RuntimeException("Test exception");
        } catch (RuntimeException e) {
            // Exception should be caught and rethrown, but MDC should still be cleaned up
            assertEquals("Test exception", e.getMessage());
        }
        // Verify cleanup happened despite exception
        assertNull(MDC.get("workspaceId"));
    }

    @Test
    void testContextWithSpecialCharacters() {
        String workspaceWithSpecialChars = "ws-测试-123";
        String userWithSpecialChars = "user-тест-456";
        String requestWithSpecialChars = "req-🎯-789";

        try (LoggingContext context = LoggingContext.create()
                .workspace(workspaceWithSpecialChars)
                .user(userWithSpecialChars)
                .request(requestWithSpecialChars)) {

            assertEquals(workspaceWithSpecialChars, MDC.get("workspaceId"));
            assertEquals(userWithSpecialChars, MDC.get("userId"));
            assertEquals(requestWithSpecialChars, MDC.get("requestId"));
        }
        // Verify cleanup
        assertNull(MDC.get("workspaceId"));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void testContextWithVeryLongValues() {
        String longWorkspaceId = "ws-" + "x".repeat(1000);
        String longUserId = "user-" + "y".repeat(1000);

        try (LoggingContext context = LoggingContext.create()
                .workspace(longWorkspaceId)
                .user(longUserId)) {

            assertEquals(longWorkspaceId, MDC.get("workspaceId"));
            assertEquals(longUserId, MDC.get("userId"));
        }
        // Verify cleanup
        assertNull(MDC.get("workspaceId"));
        assertNull(MDC.get("userId"));
    }

    @Test
    void testContextReuseAfterClose() {
        LoggingContext context = LoggingContext.create().workspace("ws-reuse");

        // First usage
        try (context) {
            assertEquals("ws-reuse", MDC.get("workspaceId"));
        }

        // Context should be cleaned up
        assertNull(MDC.get("workspaceId"));

        // Attempting to reuse closed context should not affect MDC
        // (This tests the behavior but doesn't verify it throws exceptions)
        context.workspace("ws-reuse-again");
        assertNull(MDC.get("workspaceId"));
    }

    @Test
    void testRequestAttributeNames() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // Verify the actual attribute names used
        when(mockRequest.getAttribute("clockify.workspaceId")).thenReturn("ws-attr-test");
        when(mockRequest.getAttribute("clockify.userId")).thenReturn("user-attr-test");

        try (LoggingContext context = LoggingContext.create(mockRequest)) {
            assertEquals("ws-attr-test", MDC.get("workspaceId"));
            assertEquals("user-attr-test", MDC.get("userId"));
        }

        // Verify the correct attribute names were requested
        verify(mockRequest).getAttribute("clockify.workspaceId");
        verify(mockRequest).getAttribute("clockify.userId");
    }
}