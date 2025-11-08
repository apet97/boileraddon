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
        SecurityHeadersFilter filter = new SecurityHeadersFilter("'self' https://*.clockify.me");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(true);

        filter.doFilter(req, resp, chain);

        verify(resp).setHeader(eq("X-Content-Type-Options"), eq("nosniff"));
        verify(resp).setHeader(eq("Referrer-Policy"), eq("no-referrer"));
        verify(resp).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(resp).setHeader(eq("Content-Security-Policy"), contains("frame-ancestors"));
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void setsHstsWhenForwardedHttps() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("https");

        filter.doFilter(req, resp, chain);

        verify(resp).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}

