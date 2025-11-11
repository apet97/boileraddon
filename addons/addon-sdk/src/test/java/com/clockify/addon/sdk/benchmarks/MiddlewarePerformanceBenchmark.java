package com.clockify.addon.sdk.benchmarks;

import com.clockify.addon.sdk.middleware.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for middleware filter chain performance.
 *
 * Critical paths:
 * - Filter chain execution time
 * - Rate limiting overhead
 * - Request size validation
 * - CSRF protection overhead
 * - Security header processing
 *
 * These operations happen for every HTTP request processed by the addon.
 *
 * Run with: mvn test -Dtest=MiddlewarePerformanceBenchmark -pl addons/addon-sdk
 * Or: java -jar target/benchmarks.jar MiddlewarePerformanceBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = "-Xmx2g")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class MiddlewarePerformanceBenchmark {

    private RequestLoggingFilter requestLoggingFilter;
    private RequestSizeLimitFilter requestSizeLimitFilter;
    private HttpsEnforcementFilter httpsEnforcementFilter;
    private CsrfProtectionFilter csrfProtectionFilter;
    private RateLimiter rateLimiter;
    private SecurityHeadersFilter securityHeadersFilter;
    private CriticalEndpointRateLimiter criticalEndpointRateLimiter;
    private CorsFilter corsFilter;
    private DiagnosticContextFilter diagnosticContextFilter;
    private RequestIdPropagationFilter requestIdPropagationFilter;

    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;
    private MockFilterChain mockFilterChain;

    @Setup
    public void setup() throws Exception {
        // Initialize all middleware filters
        requestLoggingFilter = new RequestLoggingFilter();
        requestSizeLimitFilter = new RequestSizeLimitFilter(10L); // 10MB - use long constructor
        httpsEnforcementFilter = new HttpsEnforcementFilter(true);
        csrfProtectionFilter = new CsrfProtectionFilter();
        rateLimiter = new RateLimiter(100.0, "ip"); // 100 requests/sec per IP
        securityHeadersFilter = new SecurityHeadersFilter();
        criticalEndpointRateLimiter = new CriticalEndpointRateLimiter(true);
        corsFilter = new CorsFilter("https://app.clockify.me", false); // origin, allowCredentials
        diagnosticContextFilter = new DiagnosticContextFilter();
        requestIdPropagationFilter = new RequestIdPropagationFilter();

        // Create mock request/response objects
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        mockFilterChain = new MockFilterChain();

        // Configure mock request for typical scenarios
        mockRequest.setMethod("POST");
        mockRequest.setRequestURI("/webhook");
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("Content-Type", "application/json");
        mockRequest.addHeader("User-Agent", "Clockify-Webhook/1.0");
        mockRequest.addHeader("Origin", "https://app.clockify.me");
    }

    /**
     * Benchmark: Request logging filter performance
     * Measures overhead of request logging for each request.
     */
    @Benchmark
    public void requestLoggingFilterPerformance(Blackhole bh) throws Exception {
        requestLoggingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Request size limit filter performance
     * Measures overhead of size validation for small requests.
     */
    @Benchmark
    public void requestSizeLimitFilterPerformance(Blackhole bh) throws Exception {
        requestSizeLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: HTTPS enforcement filter performance
     * Measures overhead of HTTPS validation.
     */
    @Benchmark
    public void httpsEnforcementFilterPerformance(Blackhole bh) throws Exception {
        httpsEnforcementFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: CSRF protection filter performance (GET request)
     * Measures overhead for safe requests that don't require CSRF validation.
     */
    @Benchmark
    public void csrfProtectionFilterSafeRequest(Blackhole bh) throws Exception {
        mockRequest.setMethod("GET");
        csrfProtectionFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Rate limiter performance
     * Measures overhead of rate limiting checks.
     */
    @Benchmark
    public void rateLimiterPerformance(Blackhole bh) throws Exception {
        rateLimiter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Security headers filter performance
     * Measures overhead of adding security headers.
     */
    @Benchmark
    public void securityHeadersFilterPerformance(Blackhole bh) throws Exception {
        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: CORS filter performance
     * Measures overhead of CORS header processing.
     */
    @Benchmark
    public void corsFilterPerformance(Blackhole bh) throws Exception {
        corsFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Diagnostic context filter performance
     * Measures overhead of MDC context management.
     */
    @Benchmark
    public void diagnosticContextFilterPerformance(Blackhole bh) throws Exception {
        diagnosticContextFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Request ID propagation filter performance
     * Measures overhead of request ID generation and propagation.
     */
    @Benchmark
    public void requestIdPropagationFilterPerformance(Blackhole bh) throws Exception {
        requestIdPropagationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Complete filter chain performance
     * Measures total overhead of all middleware filters combined.
     */
    @Benchmark
    public void completeFilterChainPerformance(Blackhole bh) throws Exception {
        // Execute filters in typical order
        diagnosticContextFilter.doFilter(mockRequest, mockResponse,
            (request, response) -> requestIdPropagationFilter.doFilter(request, response,
            (req, res) -> requestLoggingFilter.doFilter(req, res,
            (rq, rs) -> requestSizeLimitFilter.doFilter(rq, rs,
            (r, s) -> httpsEnforcementFilter.doFilter(r, s,
            (req1, res1) -> corsFilter.doFilter(req1, res1,
            (req2, res2) -> securityHeadersFilter.doFilter(req2, res2,
            (req3, res3) -> rateLimiter.doFilter(req3, res3,
            (req4, res4) -> csrfProtectionFilter.doFilter(req4, res4,
            (req5, res5) -> criticalEndpointRateLimiter.doFilter(req5, res5, mockFilterChain)
            )))))))));
        bh.consume(mockResponse);
    }

    /**
     * Benchmark: Critical endpoint rate limiter performance
     * Measures overhead of critical endpoint rate limiting.
     */
    @Benchmark
    public void criticalEndpointRateLimiterPerformance(Blackhole bh) throws Exception {
        criticalEndpointRateLimiter.doFilter(mockRequest, mockResponse, mockFilterChain);
        bh.consume(mockResponse);
    }

    // ============ Mock Classes ============

    /**
     * Mock HttpServletRequest implementation for benchmarks
     */
    public static class MockHttpServletRequest implements HttpServletRequest {
        private String method = "GET";
        private String requestURI = "/";
        private String remoteAddr = "127.0.0.1";
        private java.util.Map<String, String> headers = new java.util.HashMap<>();

        public void setMethod(String method) { this.method = method; }
        public void setRequestURI(String uri) { this.requestURI = uri; }
        public void setRemoteAddr(String addr) { this.remoteAddr = addr; }
        public void addHeader(String name, String value) { headers.put(name, value); }

        @Override
        public String getMethod() { return method; }

        @Override
        public String getRequestURI() { return requestURI; }

        @Override
        public String getRemoteAddr() { return remoteAddr; }

        @Override
        public String getHeader(String name) { return headers.get(name); }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            return java.util.Collections.enumeration(headers.keySet());
        }

        // Implement other required methods with default values
        @Override public String getAuthType() { return null; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getServletPath() { return ""; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean isRequestedSessionIdFromUrl() { return false; }
        @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return null; }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public java.util.Enumeration<String> getParameterNames() { return null; }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public java.util.Map<String, String[]> getParameterMap() { return null; }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 8080; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteHost() { return "localhost"; }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public String getRealPath(String path) { return null; }
        @Override public int getRemotePort() { return 8080; }
        @Override public String getLocalName() { return "localhost"; }
        @Override public String getLocalAddr() { return "127.0.0.1"; }
        @Override public int getLocalPort() { return 8080; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return jakarta.servlet.DispatcherType.REQUEST; }
    }

    /**
     * Mock HttpServletResponse implementation for benchmarks
     */
    public static class MockHttpServletResponse implements HttpServletResponse {
        private int status = 200;
        private java.util.Map<String, String> headers = new java.util.HashMap<>();

        @Override
        public void addHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
        }

        @Override
        public int getStatus() {
            return status;
        }

        // Implement other required methods with default implementations
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public String encodeUrl(String url) { return url; }
        @Override public String encodeRedirectUrl(String url) { return url; }
        @Override public void sendError(int sc, String msg) {}
        @Override public void sendError(int sc) {}
        @Override public void sendRedirect(String location) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public void setStatus(int sc, String sm) {}
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public java.util.Collection<String> getHeaders(String name) { return null; }
        @Override public java.util.Collection<String> getHeaderNames() { return headers.keySet(); }
        @Override public String getCharacterEncoding() { return null; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
        @Override public java.io.PrintWriter getWriter() { return null; }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setContentType(String type) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(java.util.Locale loc) {}
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
    }

    /**
     * Mock FilterChain implementation for benchmarks
     */
    public static class MockFilterChain implements FilterChain {
        @Override
        public void doFilter(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            // Simulate minimal processing time
            try {
                Thread.sleep(1); // 1ms delay to simulate backend processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}