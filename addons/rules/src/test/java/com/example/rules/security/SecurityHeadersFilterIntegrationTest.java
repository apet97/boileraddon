package com.example.rules.security;

import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityHeadersFilterIntegrationTest {

    @Test
    void cspAndNonceAreAttachedToResponses() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.isSecure()).thenReturn(true);

        FilterChain chain = (ServletRequest req, ServletResponse res) -> { };

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> cspCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Security-Policy"), cspCaptor.capture());
        String csp = cspCaptor.getValue();
        assertTrue(csp.contains("frame-ancestors"));
        assertTrue(csp.contains("nonce-"));

        verify(request).setAttribute(eq(SecurityHeadersFilter.CSP_NONCE_ATTR), any());
    }
}
