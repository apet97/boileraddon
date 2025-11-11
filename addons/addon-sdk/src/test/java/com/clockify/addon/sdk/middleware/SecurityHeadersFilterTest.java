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

        // Should set comprehensive Permissions-Policy header
        verify(resp).setHeader(eq("Permissions-Policy"), anyString());
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
