package com.clockify.addon.sdk.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceContextFilterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void extractsWorkspaceAndUserFromValidJwt() throws Exception {
        // Given: Valid JWT payload
        JsonNode payload = MAPPER.readTree("{\"workspaceId\":\"ws123\",\"userId\":\"user456\"}");
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> payload;

        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn("valid.jwt.token");

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: Request attributes are set
        verify(request).setAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR, "ws123");
        verify(request).setAttribute(WorkspaceContextFilter.USER_ID_ATTR, "user456");
        verify(chain).doFilter(request, response);
    }

    @Test
    void extractsFromAuthorizationHeader() throws Exception {
        // Given: JWT in Authorization header
        JsonNode payload = MAPPER.readTree("{\"workspaceId\":\"ws789\"}");
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> payload;

        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: Workspace ID extracted
        verify(request).setAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR, "ws789");
        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesWithoutAttributesWhenNoJwt() throws Exception {
        // Given: No JWT present
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> null;
        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: No attributes set, chain continues
        verify(request, never()).setAttribute(anyString(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesWithoutAttributesWhenVerificationFails() throws Exception {
        // Given: JWT verification returns null (failure)
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> null;
        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn("invalid.jwt.token");

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: No attributes set, chain continues
        verify(request, never()).setAttribute(anyString(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void handlesExceptionGracefully() throws Exception {
        // Given: Verifier throws exception
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> {
            throw new RuntimeException("Verification failed");
        };
        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn("bad.jwt.token");

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: Exception caught, chain continues without attributes
        verify(request, never()).setAttribute(anyString(), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsBlankWorkspaceId() throws Exception {
        // Given: JWT with blank workspaceId
        JsonNode payload = MAPPER.readTree("{\"workspaceId\":\"\",\"userId\":\"user123\"}");
        WorkspaceContextFilter.JwtVerifierFunction verifier = jwt -> payload;

        WorkspaceContextFilter filter = new WorkspaceContextFilter(verifier);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getParameter("auth_token")).thenReturn("jwt.token");

        // When: Filter processes request
        filter.doFilter(request, response, chain);

        // Then: Only userId set, workspaceId skipped
        verify(request, never()).setAttribute(eq(WorkspaceContextFilter.WORKSPACE_ID_ATTR), anyString());
        verify(request).setAttribute(WorkspaceContextFilter.USER_ID_ATTR, "user123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void requiresVerifierFunction() {
        // When/Then: Constructor rejects null verifier
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceContextFilter(null));
    }
}
