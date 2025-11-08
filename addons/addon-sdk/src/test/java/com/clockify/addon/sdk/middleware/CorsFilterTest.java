package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CorsFilterTest {

    @Test
    void allowsPreflightForAllowedOrigin() throws Exception {
        CorsFilter filter = new CorsFilter("https://app.clockify.me", true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getMethod()).thenReturn("OPTIONS");
        when(req.getHeader("Access-Control-Request-Method")).thenReturn("POST");
        when(req.getHeader("Origin")).thenReturn("https://app.clockify.me");

        filter.doFilter(req, resp, chain);

        verify(resp).addHeader("Vary", "Origin");
        verify(resp).setHeader("Access-Control-Allow-Origin", "https://app.clockify.me");
        verify(resp).setHeader("Access-Control-Allow-Credentials", "true");
        verify(resp).setStatus(204);
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void rejectsUnknownOriginPreflight() throws Exception {
        CorsFilter filter = new CorsFilter("https://app.clockify.me", false);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getMethod()).thenReturn("OPTIONS");
        when(req.getHeader("Access-Control-Request-Method")).thenReturn("GET");
        when(req.getHeader("Origin")).thenReturn("https://evil.example");

        filter.doFilter(req, resp, chain);

        verify(resp).addHeader("Vary", "Origin");
        verify(resp).setStatus(403);
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void passesThroughForAllowedSimpleRequest() throws Exception {
        CorsFilter filter = new CorsFilter("https://app.clockify.me", false);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getMethod()).thenReturn("POST");
        when(req.getHeader("Origin")).thenReturn("https://app.clockify.me");

        filter.doFilter(req, resp, chain);

        verify(resp).setHeader("Access-Control-Allow-Origin", "https://app.clockify.me");
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}

