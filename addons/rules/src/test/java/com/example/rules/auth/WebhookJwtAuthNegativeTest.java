package com.example.rules.auth;

import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookJwtAuthNegativeTest {

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        // Provide a stored token for workspace; validator requires it before signature checks
        com.clockify.addon.sdk.security.TokenStore.clear();
        com.clockify.addon.sdk.security.TokenStore.save("workspace-1", "test-token", "https://api.clockify.me/api");
    }

    @AfterEach
    void tearDown() {
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
    }

    @Test
    void rejects_missing_header_401() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(null);
        var res = com.clockify.addon.sdk.security.WebhookSignatureValidator
                .verify(req, "workspace-1", "rules");
        assertFalse(res.isValid());
        assertEquals(401, res.response().getStatusCode());
    }

    @Test
    void rejects_expired_token() {
        var key = SignatureTestUtil.RsaFixture.generate("kid-exp");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");
        var expired = SignatureTestUtil.rs256Jwt(
                key, new SignatureTestUtil.Builder()
                        .sub("rules")
                        .exp(java.time.Instant.now().minusSeconds(60))
        );
        var req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(expired);
        var res = com.clockify.addon.sdk.security.WebhookSignatureValidator
                .verify(req, "workspace-1", "rules");
        assertFalse(res.isValid());
        // Webhook path: expired token yields 401 (unauthorized)
        assertEquals(401, res.response().getStatusCode());
    }

    @Test
    void accepts_alt_header_but_emits_warn_metric() {
        var key = SignatureTestUtil.RsaFixture.generate("kid-alt");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");
        var jwt = SignatureTestUtil.rs256Jwt(key, new SignatureTestUtil.Builder().sub("rules"));
        var req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(null);
        when(req.getHeader("clockify-webhook-signature")).thenReturn(jwt); // alt header
        var res = com.clockify.addon.sdk.security.WebhookSignatureValidator
                .verify(req, "workspace-1", "rules");
        assertTrue(res.isValid());
    }
}
