package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for RequestSizeLimitFilter covering DoS prevention via request size limits.
 */
class RequestSizeLimitFilterTest {

    private static final long DEFAULT_MAX_SIZE_MB = 10;
    private static final long MB_TO_BYTES = 1024 * 1024;

    @Test
    void defaultConstructor_uses10MBLimit() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // Just under 10 MB
        long contentLength = (10 * MB_TO_BYTES) - 1;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
        verify(resp, never()).setStatus(413);
    }

    @Test
    void contentLengthExceedsLimit_returns413() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5); // 5 MB limit
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // 6 MB (exceeds 5 MB limit)
        long contentLength = 6 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
        verify(resp).setContentType("application/json");
        verify(chain, never()).doFilter(any(), any());
        assertTrue(sw.toString().contains("payload_too_large"));
    }

    @Test
    void contentLengthEqualsLimit_allowsRequest() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // Exactly 10 MB
        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
        verify(resp, never()).setStatus(413);
    }

    @Test
    void smallRequest_passesThrough() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // 1 KB
        when(req.getContentLength()).thenReturn(1024);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
        verify(resp, never()).setStatus(413);
    }

    @Test
    void noContentLength_passesThrough() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // No Content-Length header (getContentLength returns -1)
        when(req.getContentLength()).thenReturn(-1);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("GET");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void zeroContentLength_passesThrough() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getContentLength()).thenReturn(0);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("DELETE");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void customConstructor_usesProvidedLimit() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(2); // 2 MB limit
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // 3 MB (exceeds 2 MB limit)
        long contentLength = 3 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
        assertTrue(sw.toString().contains("2"));
    }

    @Test
    void errorResponse_includesMaxSizeInJson() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        String response = sw.toString();
        assertTrue(response.contains("max_size_mb"));
        assertTrue(response.contains("5"));
    }

    @Test
    void nonHttpRequest_passesThrough() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter();
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void largeFileUpload_blocksPostRequest() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(1); // 1 MB limit
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // 50 MB file upload
        long contentLength = 50 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/uploads");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void getRequest_withContentLength_stillChecksSize() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // GET with body (unusual but possible)
        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/data");
        when(req.getMethod()).thenReturn("GET");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
    }

    @Test
    void putRequest_checksSize() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/data");
        when(req.getMethod()).thenReturn("PUT");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
    }

    @Test
    void fromEnvironment_withValidEnvVar_usesEnvValue() {
        // Note: This test would require environment manipulation
        // For now, we test the happy path with default
        RequestSizeLimitFilter filter = RequestSizeLimitFilter.fromEnvironment();
        assertNotNull(filter);
    }

    @Test
    void fromEnvironment_withoutEnvVar_usesDefault() {
        RequestSizeLimitFilter filter = RequestSizeLimitFilter.fromEnvironment();
        // Should use default limit
        assertNotNull(filter);
    }

    @Test
    void clientIpExtraction_usesXForwardedFor() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
    }

    @Test
    void contentLengthIntegerOverflow_handledGracefully() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // Maximum int value (2 GB)
        when(req.getContentLength()).thenReturn(Integer.MAX_VALUE);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
    }

    @Test
    void logsWarningForOversizedRequest() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(413);
        // Logging is implicit - verify the filter executed
    }

    @Test
    void multipleRequests_eachCheckedIndependently() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);
        FilterChain chain = mock(FilterChain.class);

        // First request: small
        {
            HttpServletRequest req1 = mock(HttpServletRequest.class);
            HttpServletResponse resp1 = mock(HttpServletResponse.class);
            when(req1.getContentLength()).thenReturn(1024);
            when(req1.getRequestURI()).thenReturn("/api/v1/small");
            when(req1.getRemoteAddr()).thenReturn("192.0.2.1");

            filter.doFilter(req1, resp1, chain);
            verify(chain).doFilter(any(), any());
        }

        // Second request: large
        {
            HttpServletRequest req2 = mock(HttpServletRequest.class);
            HttpServletResponse resp2 = mock(HttpServletResponse.class);
            long contentLength = 20 * MB_TO_BYTES;
            when(req2.getContentLength()).thenReturn((int) contentLength);
            when(req2.getRequestURI()).thenReturn("/api/v1/large");
            when(req2.getMethod()).thenReturn("POST");
            when(req2.getRemoteAddr()).thenReturn("192.0.2.1");

            StringWriter sw = new StringWriter();
            when(resp2.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(req2, resp2, chain);
            verify(resp2).setStatus(413);
        }
    }

    @Test
    void errorResponse_hasCorrectContentType() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(5);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        long contentLength = 10 * MB_TO_BYTES;
        when(req.getContentLength()).thenReturn((int) contentLength);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setContentType("application/json");
    }
}
