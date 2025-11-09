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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CriticalEndpointRateLimiterTest {

    @Test
    void blocksExcessiveWebhookRequestsAndSetsRetryAfter() throws Exception {
        CriticalEndpointRateLimiter limiter = new CriticalEndpointRateLimiter(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/webhook");
        when(request.getHeader("X-Workspace-Id")).thenReturn("ws-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");

        limiter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        limiter.doFilter(request, response, chain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        assertTrue(sw.toString().contains("rate_limit_exceeded"));
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}
