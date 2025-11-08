package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestLoggingFilterDoFilterTest {

    @Test
    void collectsHeadersAndPassesThrough() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/path");

        // Simulate three headers returned by enumeration
        Vector<String> names = new Vector<>();
        names.add("Content-Type");
        names.add("Authorization");
        names.add("clockify-webhook-signature");
        Enumeration<String> en = names.elements();
        when(req.getHeaderNames()).thenReturn(en);
        when(req.getHeader("Content-Type")).thenReturn("application/json");
        when(req.getHeader("Authorization")).thenReturn("Bearer abc");
        when(req.getHeader("clockify-webhook-signature")).thenReturn("deadbeef");

        filter.doFilter(req, mock(ServletResponse.class), chain);

        // Always forwards request
        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void handlesNullHeaderEnumeration() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/path");
        when(req.getHeaderNames()).thenReturn(null);

        filter.doFilter(req, mock(ServletResponse.class), mock(FilterChain.class));
    }
}

