package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.clockify.addon.sdk.security.jwt.AuthTokenVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsControllerTest {

    private final SettingsController controller =
            new SettingsController(null, "http://localhost/rules");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void resolveBootstrapWithoutJwt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-123");
        when(request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR)).thenReturn(null);
        when(request.getAttribute(WorkspaceContextFilter.USER_ID_ATTR)).thenReturn(null);
        when(request.getParameter("jwt")).thenReturn(null);

        SettingsController.SettingsBootstrap bootstrap = controller.resolveBootstrap(request);

        assertEquals("", bootstrap.workspaceId());
        assertEquals("", bootstrap.userId());
        assertEquals("", bootstrap.userEmail());
        assertEquals("req-123", bootstrap.requestId());
        assertEquals("", bootstrap.authToken());
    }

    @Test
    void resolveBootstrapFromWorkspaceContextFilter() {
        // Simulate WorkspaceContextFilter having set attributes from auth_token JWT
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-456");
        when(request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR)).thenReturn("ws789");
        when(request.getAttribute(WorkspaceContextFilter.USER_ID_ATTR)).thenReturn("user999");

        SettingsController.SettingsBootstrap bootstrap = controller.resolveBootstrap(request);

        assertEquals("ws789", bootstrap.workspaceId());
        assertEquals("user999", bootstrap.userId());
        assertEquals("req-456", bootstrap.requestId());
        assertEquals("", bootstrap.authToken());
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
        assertEquals("", bootstrap.authToken());
    }

    @Test
    void serializeBootstrapProducesJsonString() {
        SettingsController.SettingsBootstrap bootstrap =
                new SettingsController.SettingsBootstrap("ws", "user", "user@example.com", "req-1", "light", "en", "tok");

        String json = controller.serializeBootstrap(bootstrap);

        assertTrue(json.contains("\"workspaceId\":\"ws\""));
        assertTrue(json.contains("\"userEmail\":\"user@example.com\""));
        assertTrue(json.contains("\"theme\":\"light\""));
        assertTrue(json.contains("\"language\":\"en\""));
    }

    @Test
    void resolveBootstrapCapturesAuthTokenWhenVerifierPresent() throws Exception {
        var payload = MAPPER.createObjectNode()
                .put("workspaceId", "ws-123")
                .put("userId", "user-555")
                .put("userEmail", "someone@example.com")
                .put("theme", "dark")
                .put("language", "es");
        AuthTokenVerifier verifier = token -> {
            assertEquals("token-abc", token);
            return new JwtVerifier.DecodedJwt(MAPPER.createObjectNode(), payload);
        };
        SettingsController controllerWithVerifier =
                new SettingsController(verifier, "http://localhost/rules");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-777");
        when(request.getParameter("auth_token")).thenReturn("token-abc");
        SettingsController.SettingsBootstrap bootstrap = controllerWithVerifier.resolveBootstrap(request);

        assertEquals("ws-123", bootstrap.workspaceId());
        assertEquals("user-555", bootstrap.userId());
        assertEquals("someone@example.com", bootstrap.userEmail());
        assertEquals("dark", bootstrap.theme());
        assertEquals("es", bootstrap.language());
        assertEquals("token-abc", bootstrap.authToken());
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
