package com.clockify.addon.sdk.security;

import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookSignatureWorkspaceMismatchTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
        TokenStore.clear();
    }

    @Test
    void workspace_mismatch_returns_403() {
        String wsClaim = "ws-A";
        String wsTarget = "ws-B";
        TokenStore.save(wsTarget, "install-token", "https://api.clockify.me/api");

        var key = SignatureTestUtil.RsaFixture.generate("kid-ws");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        var jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub("rules")
                        .workspaceId(wsClaim)
        );

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(jwt);
        try { when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}"))); } catch (Exception ignored) {}

        var res = WebhookSignatureValidator.verify(req, wsTarget, "rules");
        assertFalse(res.isValid());
        assertEquals(403, res.response().getStatusCode());
    }
}

