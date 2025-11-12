package com.clockify.addon.sdk.security;

import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookSignatureAltHeaderMetricTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
        TokenStore.clear();
    }

    @Test
    void alt_header_increments_noncanonical_metric() {
        String alt = "clockify-webhook-signature";
        String workspaceId = "ws-telemetry";
        TokenStore.save(workspaceId, "install-token", "https://api.clockify.me/api");

        var key = SignatureTestUtil.RsaFixture.generate("kid-metric");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        var jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub("rules")
                        .workspaceId(workspaceId)
        );

        double before = counterValue("addon.signature.header.noncanonical", "header", alt);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Clockify-Signature")).thenReturn(null);
        when(req.getHeader(alt)).thenReturn(jwt);
        try { when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}"))); } catch (Exception ignored) {}

        var res = WebhookSignatureValidator.verify(req, workspaceId, "rules");
        assertTrue(res.isValid());

        double after = counterValue("addon.signature.header.noncanonical", "header", alt);
        assertEquals(before + 1.0, after, 0.0001);
    }

    private static double counterValue(String name, String tagKey, String tagValue) {
        Counter c = MetricsHandler.registry().find(name).tag(tagKey, tagValue).counter();
        return c == null ? 0.0 : c.count();
    }
}

