package com.clockify.addon.sdk.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SECURITY: CSRF (Cross-Site Request Forgery) protection filter.
 *
 * Clockify addon webhooks use webhook signatures (HMAC-based) which naturally provide
 * CSRF protection since attackers cannot forge valid signatures without the secret.
 *
 * However, this filter adds defense-in-depth CSRF protection for:
 * - Custom endpoints that accept form data or cookies
 * - State-changing operations (POST/PUT/DELETE/PATCH)
 * - Browser-based interactions (where applicable)
 *
 * Implementation: Token-based CSRF protection
 * - Generates a unique CSRF token per session
 * - Validates token on state-changing requests (POST, PUT, DELETE, PATCH)
 * - Tokens must be provided via header (X-CSRF-Token) or parameter (__csrf)
 * - Webhook endpoints automatically bypass CSRF (they use signature validation)
 *
 * Note: Clockify webhooks are NOT vulnerable to CSRF because they:
 * 1. Use HMAC-SHA256 signatures (cryptographic authentication)
 * 2. Require specific content-type headers
 * 3. Cannot be triggered by browser form submissions
 */
public class CsrfProtectionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CsrfProtectionFilter.class);

    private static final String CSRF_TOKEN_ATTR = "_csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String CSRF_PARAM = "__csrf";
    private static final int TOKEN_LENGTH = 32;  // 256 bits

    private final SecureRandom random = new SecureRandom();

    /**
     * List of paths that bypass CSRF checks.
     * These use alternative security mechanisms (e.g., webhook signatures).
     */
    private static final String[] CSRF_EXEMPT_PATHS = {
            "/webhook",
            "/lifecycle",
            "/health",
            "/metrics",
            "/manifest.json"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Get or create CSRF token for this session
        String csrfToken = getOrCreateCsrfToken(httpRequest);

        // For safe methods (GET, HEAD, OPTIONS, TRACE), just continue
        String method = httpRequest.getMethod();
        if (isSafeMethod(method)) {
            chain.doFilter(request, response);
            return;
        }

        // For state-changing methods (POST, PUT, DELETE, PATCH), validate CSRF token
        String path = httpRequest.getRequestURI();
        if (shouldExemptFromCsrf(path)) {
            logger.debug("CSRF check exempted for path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // Validate CSRF token on state-changing requests
        String providedToken = extractCsrfToken(httpRequest);
        if (!validateCsrfToken(csrfToken, providedToken)) {
            logger.warn("SECURITY: CSRF token validation failed for {} {} from {}",
                    method, path, httpRequest.getRemoteAddr());

            sendCsrfError(httpResponse);
            return;
        }

        logger.debug("CSRF validation passed for {} {}", method, path);
        chain.doFilter(request, response);
    }

    /**
     * Gets existing CSRF token from session, or creates a new one.
     */
    private String getOrCreateCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);

        if (token == null) {
            // Generate new token: secure random bytes encoded as Base64
            byte[] randomBytes = new byte[TOKEN_LENGTH];
            random.nextBytes(randomBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            session.setAttribute(CSRF_TOKEN_ATTR, token);
            logger.debug("Generated new CSRF token for session");
        }

        return token;
    }

    /**
     * Extracts CSRF token from request (header preferred, then parameter).
     */
    private String extractCsrfToken(HttpServletRequest request) {
        // Try header first (preferred, XHR/API friendly)
        String token = request.getHeader(CSRF_HEADER);
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // Fall back to parameter (form submission)
        token = request.getParameter(CSRF_PARAM);
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return null;
    }

    /**
     * Validates CSRF token using constant-time comparison.
     */
    private boolean validateCsrfToken(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }

        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(expected, provided);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Checks if a path should be exempt from CSRF checks.
     */
    private boolean shouldExemptFromCsrf(String path) {
        if (path == null) return false;

        for (String exemptPath : CSRF_EXEMPT_PATHS) {
            if (path.startsWith(exemptPath) || path.equals(exemptPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if HTTP method is safe (doesn't change state).
     */
    private boolean isSafeMethod(String method) {
        if (method == null) return true;

        switch (method.toUpperCase()) {
            case "GET":
            case "HEAD":
            case "OPTIONS":
            case "TRACE":
                return true;
            default:
                return false;
        }
    }

    /**
     * Sends 403 Forbidden response for CSRF token validation failure.
     */
    private void sendCsrfError(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");

        String json = "{\"error\":\"csrf_token_invalid\",\"message\":\"CSRF token validation failed. Please provide a valid " +
                CSRF_HEADER + " header or " + CSRF_PARAM + " parameter.\"}";

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CSRF protection filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("CSRF protection filter destroyed");
    }
}
