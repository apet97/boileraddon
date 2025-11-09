package com.clockify.addon.sdk.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests JWT signature validation path in WebhookSignatureValidator.
 * Covers the Developer JWT flow (Clockify-Signature header).
 */
class WebhookSignatureValidatorJwtTest {

    private static final String WORKSPACE_ID = "ws123";
    private static final String TOKEN = "test-token-secret";

    @BeforeEach
    void setUp() {
        // Clear any existing tokens
        TokenStore.clear();
        System.setProperty("ADDON_ACCEPT_JWT_SIGNATURE", "true");
        // Store a test token for the workspace
        TokenStore.save(WORKSPACE_ID, TOKEN, "https://api.clockify.me");
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
        System.clearProperty("ADDON_ACCEPT_JWT_SIGNATURE");
    }

    @Test
    void jwtWithMatchingWorkspaceId_shouldPass() throws Exception {
        String jwt = createJwt("{\"workspaceId\":\"" + WORKSPACE_ID + "\"}");
        HttpServletRequest req = mockRequest(jwt, "{}");

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertTrue(result.isValid(), "JWT with matching workspaceId should be valid");
    }

    @Test
    void jwtWithMismatchedWorkspaceId_shouldFail() throws Exception {
        String jwt = createJwt("{\"workspaceId\":\"other-workspace\"}");
        HttpServletRequest req = mockRequest(jwt, "{}");

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertFalse(result.isValid(), "JWT with mismatched workspaceId should be invalid");
        assertEquals(403, result.response().getStatusCode());
    }

    @Test
    void jwtWithoutWorkspaceId_shouldPassAsDefault() throws Exception {
        String jwt = createJwt("{\"userId\":\"user123\"}");
        HttpServletRequest req = mockRequest(jwt, "{}");

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertTrue(result.isValid(), "JWT without workspaceId should pass (fallback behavior)");
    }

    @Test
    void malformedJwt_shouldFail() throws Exception {
        String malformedJwt = "not.a.valid.jwt";
        HttpServletRequest req = mockRequest(malformedJwt, "{}");

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertFalse(result.isValid(), "Malformed JWT should be invalid");
        assertEquals(403, result.response().getStatusCode());
    }

    @Test
    void jwtAcceptedViaAlternativeHeader() throws Exception {
        String jwt = createJwt("{\"workspaceId\":\"" + WORKSPACE_ID + "\"}");
        HttpServletRequest req = mock(HttpServletRequest.class);

        // Set JWT under alternative header "Clockify-Signature"
        when(req.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(req.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertTrue(result.isValid(), "JWT should be accepted via Clockify-Signature header");
    }

    @Test
    void hmacPath_shouldStillWork() throws Exception {
        String body = "{\"event\":\"NEW_TIME_ENTRY\"}";
        String hmacSig = WebhookSignatureValidator.computeSignature(TOKEN, body);
        HttpServletRequest req = mockRequest(hmacSig, body);

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertTrue(result.isValid(), "HMAC signature should still be validated correctly");
    }

    @Test
    void hmacPathWithWrongSecret_shouldFail() throws Exception {
        String body = "{\"event\":\"NEW_TIME_ENTRY\"}";
        String hmacSig = WebhookSignatureValidator.computeSignature("wrong-secret", body);
        HttpServletRequest req = mockRequest(hmacSig, body);

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertFalse(result.isValid(), "HMAC with wrong secret should fail");
        assertEquals(403, result.response().getStatusCode());
    }

    @Test
    void missingSignatureHeader_shouldReturn401() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(anyString())).thenReturn(null);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID);

        assertFalse(result.isValid(), "Missing signature should return 401");
        assertEquals(401, result.response().getStatusCode());
    }

    @Test
    void missingWorkspaceToken_shouldReturn401() throws Exception {
        String jwt = createJwt("{\"workspaceId\":\"unknown-workspace\"}");
        HttpServletRequest req = mockRequest(jwt, "{}");

        var result = WebhookSignatureValidator.verify(req, "unknown-workspace");

        assertFalse(result.isValid(), "Missing workspace token should return 401");
        assertEquals(401, result.response().getStatusCode());
    }

    // Helper methods

    private HttpServletRequest mockRequest(String signature, String body) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("clockify-webhook-signature")).thenReturn(signature);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        return req;
    }

    /**
     * Creates a minimal JWT with the given payload.
     * Format: header.payload.signature (we skip signature for testing)
     */
    private String createJwt(String payload) {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = base64UrlEncode(header);
        String encodedPayload = base64UrlEncode(payload);
        return encodedHeader + "." + encodedPayload + ".fake-signature";
    }

    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input.getBytes());
    }
}
