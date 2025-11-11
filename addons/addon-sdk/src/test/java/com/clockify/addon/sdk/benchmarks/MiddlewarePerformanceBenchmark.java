package com.clockify.addon.sdk.benchmarks;

import com.clockify.addon.sdk.middleware.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.mockito.Mockito;

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

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private FilterChain mockFilterChain;

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

        // Create mock request/response objects using Mockito
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        mockFilterChain = Mockito.mock(FilterChain.class);

        // Configure mock request for typical scenarios
        Mockito.when(mockRequest.getMethod()).thenReturn("POST");
        Mockito.when(mockRequest.getRequestURI()).thenReturn("/webhook");
        Mockito.when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        Mockito.when(mockRequest.getHeader("Content-Type")).thenReturn("application/json");
        Mockito.when(mockRequest.getHeader("User-Agent")).thenReturn("Clockify-Webhook/1.0");
        Mockito.when(mockRequest.getHeader("Origin")).thenReturn("https://app.clockify.me");
        Mockito.when(mockRequest.getScheme()).thenReturn("http");
        Mockito.when(mockRequest.getServerName()).thenReturn("localhost");
        Mockito.when(mockRequest.getServerPort()).thenReturn(8080);
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

}