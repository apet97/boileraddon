package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimiterTest {

    @Test
    void ipMode_allowsFirst_thenReturns429() throws Exception {
        RateLimiter limiter = new RateLimiter(1.0, "ip");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("203.0.113.5");

        // First request: allowed
        limiter.doFilter(req, resp, chain);
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Second immediate request: should exceed rate and return 429
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        limiter.doFilter(req, resp, chain);

        verify(resp).setStatus(429);
        verify(resp).setHeader(eq("Retry-After"), eq("1"));
        assertTrue(sw.toString().contains("rate_limit_exceeded"));
    }

    @Test
    void workspaceMode_usesHeaderIdentifier() throws Exception {
        RateLimiter limiter = new RateLimiter(1.0, "workspace");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("X-Workspace-Id")).thenReturn("ws-abc");
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("198.51.100.7");

        // First allowed
        limiter.doFilter(req, resp, chain);
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Next blocked
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        limiter.doFilter(req, resp, chain);
        verify(resp).setStatus(429);
        assertTrue(sw.toString().contains("\"identifier\":\"ws-abc\""));
    }

    @Test
    void workspaceMode_extractsIdFromPathWhenHeaderMissing() throws Exception {
        RateLimiter limiter = new RateLimiter(1.0, "workspace");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("X-Workspace-Id")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/workspace/ws-path/time-entries");
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.0.2.50");

        // First allowed
        limiter.doFilter(req, resp, chain);
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Next blocked and echo identifier
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        limiter.doFilter(req, resp, chain);
        verify(resp).setStatus(429);
        assertTrue(sw.toString().contains("\"identifier\":\"ws-path\""));
    }
}

