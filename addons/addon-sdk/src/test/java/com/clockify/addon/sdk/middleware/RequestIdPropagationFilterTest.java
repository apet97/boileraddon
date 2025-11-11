package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.io.IOException;

import static org.mockito.Mockito.*;

class RequestIdPropagationFilterTest {

    @Test
    void setsHeaderFromRequestAttribute() throws IOException, ServletException {
        RequestIdPropagationFilter filter = new RequestIdPropagationFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-123");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("X-Request-Id", "req-123");
    }

    @Test
    void fallsBackToMdcValue() throws IOException, ServletException {
        RequestIdPropagationFilter filter = new RequestIdPropagationFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = (ServletRequest req, ServletResponse res) -> {
            MDC.put("requestId", "mdc-456");
        };

        try {
            filter.doFilter(request, response, chain);
        } finally {
            MDC.remove("requestId");
        }

        verify(response).setHeader("X-Request-Id", "mdc-456");
    }

    @Test
    void stillSetsHeaderWhenChainReturnsError() throws IOException, ServletException {
        RequestIdPropagationFilter filter = new RequestIdPropagationFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = (req, res) -> ((HttpServletResponse) res).sendError(400, "bad");
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("test-789");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), anyString());
    }
}
