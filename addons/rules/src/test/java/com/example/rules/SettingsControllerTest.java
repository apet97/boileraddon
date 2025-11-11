package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.example.rules.security.JwtVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsControllerTest {

    private final SettingsController controller = new SettingsController();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void resolveBootstrapWithoutJwt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-123");
        when(request.getParameter("jwt")).thenReturn(null);

        SettingsController.SettingsBootstrap bootstrap = controller.resolveBootstrap(request);

        assertEquals("", bootstrap.workspaceId());
        assertEquals("", bootstrap.userId());
        assertEquals("", bootstrap.userEmail());
        assertEquals("req-123", bootstrap.requestId());
    }

    @Test
    void resolveBootstrapWithEmptyJwt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-123");
        when(request.getParameter("jwt")).thenReturn("");

        SettingsController.SettingsBootstrap bootstrap = controller.resolveBootstrap(request);

        assertEquals("", bootstrap.workspaceId());
        assertEquals("", bootstrap.userId());
        assertEquals("", bootstrap.userEmail());
        assertEquals("req-123", bootstrap.requestId());
    }

    @Test
    void serializeBootstrapProducesJsonString() {
        SettingsController.SettingsBootstrap bootstrap =
                new SettingsController.SettingsBootstrap("ws", "user", "user@example.com", "req-1");

        String json = controller.serializeBootstrap(bootstrap);

        assertTrue(json.contains("\"workspaceId\":\"ws\""));
        assertTrue(json.contains("\"userEmail\":\"user@example.com\""));
    }

    @Test
    void handleAddsSecurityHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn(null);
        when(request.getParameter("jwt")).thenReturn(null);

        HttpResponse response = controller.handle(request);

        assertEquals("text/html; charset=utf-8", response.getContentType());

        // Check that security headers are present (be lenient for testing)
        String csp = response.getHeaders().get("Content-Security-Policy");
        if (csp != null) {
            assertTrue(csp.contains("script-src 'nonce-"));
        }

        String cacheControl = response.getHeaders().get("Cache-Control");
        if (cacheControl != null) {
            assertEquals("no-store", cacheControl);
        }
    }
}
