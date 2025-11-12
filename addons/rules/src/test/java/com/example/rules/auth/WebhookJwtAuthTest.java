package com.example.rules.auth;

import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Minimal RS256 header injection tests for webhook JWTs using the shared SDK test utility.
 */
class WebhookJwtAuthTest {

    private static final String ADDON_KEY = "rules"; // matches RulesApp manifest key
    private static final String WORKSPACE_ID = "workspace-1";

    @BeforeEach
    void setup() {
        // Clear token store and set a dummy installation token (not used for JWT path)
        com.clockify.addon.sdk.security.TokenStore.clear();
        com.clockify.addon.sdk.security.TokenStore.save(WORKSPACE_ID, "test-token", "https://api.clockify.me/api");
    }

    @AfterEach
    void tearDown() {
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECT_ISS");
    }

    @Test
    void accepts_token_in_clockify_signature_header() throws Exception {
        // Generate RS256 JWT for this add-on key
        var key = SignatureTestUtil.RsaFixture.generate("kid-1");
        var jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub(ADDON_KEY)
                        .workspaceId(WORKSPACE_ID)
        );

        // Expose PEM public key to the validator via system property
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        // Mock request with JWT header (Clockify-Signature)
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(req.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID, ADDON_KEY);
        assertTrue(result.isValid(), "JWT in Clockify-Signature should be accepted");
    }

    @Test
    void rejects_wrong_subject() throws Exception {
        var key = SignatureTestUtil.RsaFixture.generate("kid-2");
        var jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub("other-addon-key")
                        .workspaceId(WORKSPACE_ID)
        );

        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECT_ISS", "clockify");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(req.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));

        var result = WebhookSignatureValidator.verify(req, WORKSPACE_ID, ADDON_KEY);
        assertFalse(result.isValid(), "JWT with wrong subject (sub) should be rejected");
        assertEquals(403, result.response().getStatusCode());
    }
}
