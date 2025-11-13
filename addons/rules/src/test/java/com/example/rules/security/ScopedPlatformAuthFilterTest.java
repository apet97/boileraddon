package com.example.rules.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class ScopedPlatformAuthFilterTest {

    @Test
    void delegatesToPlatformFilterForProtectedPrefixes() throws Exception {
        Filter delegate = mock(Filter.class);
        ScopedPlatformAuthFilter filter = new ScopedPlatformAuthFilter(delegate, Set.of("/status"), List.of("/api"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/rules/api/rules");
        when(request.getContextPath()).thenReturn("/rules");

        filter.doFilter(request, response, chain);

        verify(delegate).doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void bypassesDelegateForUnprotectedPaths() throws Exception {
        Filter delegate = mock(Filter.class);
        ScopedPlatformAuthFilter filter = new ScopedPlatformAuthFilter(delegate, Set.of("/status"), List.of("/api"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/rules/health");
        when(request.getContextPath()).thenReturn("/rules");

        filter.doFilter(request, response, chain);

        verify(delegate, never()).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
