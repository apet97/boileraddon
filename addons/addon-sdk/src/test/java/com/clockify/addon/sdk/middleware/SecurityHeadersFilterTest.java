package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityHeadersFilterTest {

    @Test
    void setsBasicHeadersAndHstsWhenSecure() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(true);

        filter.doFilter(req, resp, chain);

        verify(resp).setHeader(eq("X-Content-Type-Options"), eq("nosniff"));
        verify(resp).setHeader(eq("Referrer-Policy"), eq("strict-origin-when-cross-origin"));
        verify(resp).setHeader(eq("Cache-Control"), eq("no-store"));
        verify(resp).setHeader(eq("Permissions-Policy"), anyString());
        verify(resp).setHeader(eq("Content-Security-Policy"), contains("frame-ancestors"));
        verify(resp).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsCspWithNonce() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        // Capture the nonce stored in request attribute
        ArgumentCaptor<String> nonceCaptor = ArgumentCaptor.forClass(String.class);
        verify(req).setAttribute(eq(SecurityHeadersFilter.CSP_NONCE_ATTR), nonceCaptor.capture());
        String nonce = nonceCaptor.getValue();
        assertNotNull(nonce, "Nonce should be generated");
        assertFalse(nonce.isEmpty(), "Nonce should not be empty");

        // Capture the CSP header
        ArgumentCaptor<String> cspCaptor = ArgumentCaptor.forClass(String.class);
        verify(resp).setHeader(eq("Content-Security-Policy"), cspCaptor.capture());
        String csp = cspCaptor.getValue();

        // Verify nonce is used in script-src and style-src
        assertTrue(csp.contains("script-src 'nonce-" + nonce + "'"), "CSP should include nonce in script-src");
        assertTrue(csp.contains("style-src 'nonce-" + nonce + "'"), "CSP should include nonce in style-src");
        assertTrue(csp.contains("frame-ancestors"), "CSP should include frame-ancestors");

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsHstsWhenForwardedHttps() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("https");

        filter.doFilter(req, resp, chain);

        verify(resp).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsDefaultCspWhenFrameAncestorsUnset() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);

        filter.doFilter(req, resp, chain);

        // Should set comprehensive CSP even when frame-ancestors is unset
        verify(resp).setHeader(eq("Content-Security-Policy"), anyString());
        // Should not set HSTS when not secure
        verify(resp, never()).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsPermissionsPolicyHeader() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        // Capture the actual Permissions-Policy header value
        ArgumentCaptor<String> ppCaptor = ArgumentCaptor.forClass(String.class);
        verify(resp).setHeader(eq("Permissions-Policy"), ppCaptor.capture());

        // Verify all required policies are present
        String ppValue = ppCaptor.getValue();
        assertTrue(ppValue.contains("browsing-topics=()"), "Should block browsing-topics");
        assertTrue(ppValue.contains("geolocation=()"), "Should block geolocation");
        assertTrue(ppValue.contains("microphone=()"), "Should block microphone");
        assertTrue(ppValue.contains("camera=()"), "Should block camera");
        assertTrue(ppValue.contains("payment=()"), "Should block payment");

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsAllCacheControlHeaders() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        // Should set all cache control headers
        verify(resp).setHeader(eq("Cache-Control"), eq("no-store"));
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}
