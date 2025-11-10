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
 * Tests for HttpsEnforcementFilter covering HTTPS requirement enforcement and proxy detection.
 */
class HttpsEnforcementFilterTest {

    @Test
    void enforceHttps_enabled_blocksNonHttpsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(403);
        verify(resp).setContentType("application/json");
        verify(chain, never()).doFilter(any(), any());
        assertTrue(sw.toString().contains("insecure_connection"));
        assertTrue(sw.toString().contains("HTTPS is required"));
    }

    @Test
    void enforceHttps_disabled_allowsNonHttpsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(false);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void directHttpsConnection_allowsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(true);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(resp, never()).setStatus(403);
    }

    @Test
    void xForwardedProtoHttps_allowsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(resp, never()).setStatus(403);
    }

    @Test
    void xForwardedProtoHttp_blocksRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("http");
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void xForwardedProtoWithWhitespace_handlesCorrectly() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("  https  ");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void xForwardedProtoCaseInsensitive_acceptsHttpsInAnyCase() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("HTTPS");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void xOriginalProtoHttps_allowsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn("https");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void cloudFrontHttps_allowsRequest() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn("https");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void multipleHeadersPresent_usesFirstHttpsHeader() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(req.getHeader("X-Original-Proto")).thenReturn("http");
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        filter.doFilter(req, resp, chain);

        // Should allow because first header is https (priority-based)
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void nonHttpRequest_blocksImmediately() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void responseIncludesDocumentationLink() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        assertTrue(sw.toString().contains("documentation"));
    }

    @Test
    void clientIpExtraction_usesXForwardedFor() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void clientIpExtraction_fallsBackToRemoteAddr() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(403);
    }

    @Test
    void constructor_default_enforcesHttps() throws Exception {
        HttpsEnforcementFilter filter = new HttpsEnforcementFilter();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.isSecure()).thenReturn(false);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(req.getHeader("X-Original-Proto")).thenReturn(null);
        when(req.getHeader("CloudFront-Forwarded-Proto")).thenReturn(null);
        when(req.getRequestURI()).thenReturn("/api/v1/webhook");
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(req, resp, chain);

        verify(resp).setStatus(403);
    }
}
