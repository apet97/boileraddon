package com.clockify.addon.sdk.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SECURITY: HTTPS enforcement filter.
 *
 * In production, all addon communication MUST use HTTPS to:
 * - Prevent man-in-the-middle attacks
 * - Protect webhook signatures and tokens in transit
 * - Comply with security best practices
 *
 * This filter:
 * 1. Blocks non-HTTPS requests with 403 Forbidden
 * 2. Checks X-Forwarded-Proto header (for proxies/load balancers)
 * 3. Is automatically enabled in production
 * 4. Can be disabled for local development (not recommended)
 *
 * Environment variable: ENFORCE_HTTPS=true (default: true in production)
 */
public class HttpsEnforcementFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(HttpsEnforcementFilter.class);

    private final boolean enforceHttps;

    /**
     * Creates HTTPS enforcement filter with configurable strictness.
     *
     * @param enforceHttps if true, reject non-HTTPS requests. If false, log warning but allow.
     */
    public HttpsEnforcementFilter(boolean enforceHttps) {
        this.enforceHttps = enforceHttps;
        logger.info("HTTPS enforcement filter initialized (enforce: {})", enforceHttps);
    }

    /**
     * Default: enforce HTTPS (security-first approach).
     */
    public HttpsEnforcementFilter() {
        this(true);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Check if request is HTTPS
        if (!isSecureConnection(httpRequest)) {
            String clientIp = getClientIp(httpRequest);
            String path = httpRequest.getRequestURI();

            if (enforceHttps) {
                // SECURITY: Reject non-HTTPS in strict mode
                logger.warn("SECURITY: Rejecting non-HTTPS request from {} to {}. Use HTTPS in production.",
                        clientIp, path);

                sendHttpsEnforcementError(httpResponse);
                return;
            } else {
                // Development mode: warn but allow
                logger.warn("Non-HTTPS request from {} to {} (allowed in development mode)", clientIp, path);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Checks if connection is secure (HTTPS or equivalent).
     * Accounts for reverse proxies and load balancers.
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        // Direct HTTPS connection
        if (request.isSecure()) {
            return true;
        }

        // Check X-Forwarded-Proto (for proxies/load balancers)
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && "https".equalsIgnoreCase(forwardedProto.trim())) {
            return true;
        }

        // Check X-Original-Proto (Cloudflare)
        String originalProto = request.getHeader("X-Original-Proto");
        if (originalProto != null && "https".equalsIgnoreCase(originalProto.trim())) {
            return true;
        }

        // Check CloudFront header
        String cloudFrontProto = request.getHeader("CloudFront-Forwarded-Proto");
        if (cloudFrontProto != null && "https".equalsIgnoreCase(cloudFrontProto.trim())) {
            return true;
        }

        return false;
    }

    /**
     * Gets client IP address, accounting for proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Sends 403 Forbidden response for non-HTTPS requests.
     */
    private void sendHttpsEnforcementError(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");

        String json = "{\"error\":\"insecure_connection\"," +
                "\"message\":\"HTTPS is required. Please use a secure connection.\"," +
                "\"documentation\":\"https://clockify.me/help/security/https-requirement\"}";

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("HTTPS enforcement filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("HTTPS enforcement filter destroyed");
    }
}
