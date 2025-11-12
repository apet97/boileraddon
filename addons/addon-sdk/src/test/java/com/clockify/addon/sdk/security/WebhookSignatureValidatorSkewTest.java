package com.clockify.addon.sdk.security;

import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookSignatureValidatorSkewTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
        System.clearProperty("JWT_MAX_CLOCK_SKEW_SECONDS");
        TokenStore.clear();
    }

    @Test
    void expired_with_zero_skew_returns_401() {
        String workspaceId = "ws-1";
        TokenStore.save(workspaceId, "install-token", "https://api.clockify.me/api");

        var key = SignatureTestUtil.RsaFixture.generate("kid-skew");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");
        System.setProperty("JWT_MAX_CLOCK_SKEW_SECONDS", "0");

        var jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub("rules")
                        .workspaceId(workspaceId)
                        .exp(java.time.Instant.now().minusSeconds(2))
        );

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(jwt);
        try {
            when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));
        } catch (Exception ignored) {}

        var res = WebhookSignatureValidator.verify(req, workspaceId, "rules");
        assertFalse(res.isValid());
        assertEquals(401, res.response().getStatusCode());
    }
}

