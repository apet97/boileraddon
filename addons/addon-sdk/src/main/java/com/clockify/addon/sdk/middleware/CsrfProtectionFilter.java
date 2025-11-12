package com.clockify.addon.sdk.middleware;

import com.clockify.addon.sdk.security.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

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
    private static final String CSRF_COOKIE_NAME = "clockify-addon-csrf";
    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String CSRF_PARAM = "__csrf";
    private static final int TOKEN_LENGTH = 32;  // 256 bits
    private static final boolean COOKIE_ATTRIBUTE_SUPPORTED = cookieAttributeSupported();

    /**
     * TEST-ONLY: Public static flag for integration tests to bypass CSRF validation.
     * Allows tests to bypass CSRF validation without timing issues from system properties.
     * This is volatile to ensure visibility across threads in embedded server tests.
     * MUST be false in production (default value).
     * Only set this to true in test @BeforeAll and reset to false in @AfterAll.
     */
    public static volatile boolean testModeDisabled = false;

    private final SecureRandom random = new SecureRandom();
    private final String sameSiteAttribute;

    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/webhook",
            "/lifecycle",
            "/health",
            "/actuator"
    );

    private static final String[] SIGNATURE_HEADERS = {
            "clockify-webhook-signature",
            "x-clockify-webhook-signature",
            "Clockify-Signature",
            "X-Clockify-Signature"
    };

    public CsrfProtectionFilter() {
        this(System.getenv().getOrDefault("ADDON_CSRF_SAMESITE", "None"));
    }

    public CsrfProtectionFilter(String sameSiteAttribute) {
        this.sameSiteAttribute = normalizeSameSite(sameSiteAttribute);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // TEST-ONLY: Allow disabling CSRF protection for integration tests
        // Static flag is checked first to avoid timing issues with system properties
        // This should NEVER be enabled in production environments
        if (testModeDisabled || Boolean.getBoolean("clockify.csrf.disabled")) {
            logger.debug("CSRF protection bypassed for testing");
            chain.doFilter(request, response);
            return;
        }

        logger.debug("CSRF filter processing request");

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String method = httpRequest.getMethod();
        String path = requestPath(httpRequest);

        logger.debug("CSRF filter processing {} {} from {}", method, path, getClientIp(httpRequest));

        if (isExempt(httpRequest, path, method)) {
            logger.debug("CSRF check exempted for path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        TokenState tokenState = getOrCreateCsrfToken(httpRequest);
        String csrfToken = tokenState.token();
        if (tokenState.generated() || isSafeMethod(method)) {
            logger.debug("Sending CSRF cookie for {} request - generated: {}, safe: {}",
                    method, tokenState.generated(), isSafeMethod(method));
            sendTokenCookie(httpResponse, httpRequest, csrfToken);
        }

        if (isSafeMethod(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Validate CSRF token on state-changing requests
        String providedToken = extractCsrfToken(httpRequest);
        if (!validateCsrfToken(csrfToken, providedToken)) {
            logger.warn("SECURITY: CSRF token validation failed for {} {} from {}",
                    method, path, getClientIp(httpRequest));

            // Audit log CSRF failure
            AuditLogger.log(AuditLogger.AuditEvent.CSRF_TOKEN_INVALID)
                    .clientIp(getClientIp(httpRequest))
                    .detail("path", path)
                    .detail("method", method)
                    .error();

            sendCsrfError(httpResponse);
            return;
        }

        AuditLogger.log(AuditLogger.AuditEvent.CSRF_TOKEN_VALIDATED)
                .clientIp(getClientIp(httpRequest))
                .detail("path", path)
                .detail("method", method)
                .info();
        chain.doFilter(request, response);
    }

    /**
     * Gets existing CSRF token from session, or creates a new one.
     */
    private TokenState getOrCreateCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(CSRF_TOKEN_ATTR);
        if (existing instanceof String token && !token.isBlank()) {
            return new TokenState(token, false);
        }

        byte[] randomBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        session.setAttribute(CSRF_TOKEN_ATTR, token);
        AuditLogger.log(AuditLogger.AuditEvent.CSRF_TOKEN_GENERATED)
                .clientIp(getClientIp(request))
                .detail("path", request.getRequestURI())
                .info();
        logger.debug("Generated new CSRF token for session");
        return new TokenState(token, true);
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

    private boolean isExempt(HttpServletRequest request, String path, String method) {
        if (path == null) return true;
        for (String prefix : EXEMPT_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        if (hasSignatureHeader(request)) {
            return true;
        }
        return isSafeMethod(method);
    }

    private boolean hasSignatureHeader(HttpServletRequest request) {
        for (String header : SIGNATURE_HEADERS) {
            if (request.getHeader(header) != null) {
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

    private void sendTokenCookie(HttpServletResponse response, HttpServletRequest request, String token) {
        boolean secure = isSecureRequest(request);
        if (COOKIE_ATTRIBUTE_SUPPORTED) {
            Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token);
            cookie.setPath("/");
            cookie.setHttpOnly(false); // exposed to JS for double-submit header
            cookie.setSecure(secure);
            cookie.setMaxAge(-1);
            cookie.setAttribute("SameSite", sameSiteAttribute);
            response.addCookie(cookie);
            logger.debug("Added CSRF cookie via Cookie object: {}", cookie.getValue());
            return;
        }

        // Fallback for Servlet containers that do not support Cookie#setAttribute (Servlet <= 5)
        StringBuilder sb = new StringBuilder();
        sb.append(CSRF_COOKIE_NAME).append("=").append(token).append("; Path=/");
        if (secure) {
            sb.append("; Secure");
        }
        sb.append("; SameSite=").append(sameSiteAttribute);
        response.addHeader("Set-Cookie", sb.toString());
        logger.debug("Added CSRF cookie via Set-Cookie header: {}", sb.toString());
    }

    private static String normalizeSameSite(String value) {
        if (value == null) {
            return "None";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "None";
        }
        String upper = trimmed.toUpperCase();
        if ("STRICT".equals(upper)) return "Strict";
        if ("LAX".equals(upper)) return "Lax";
        return "None";
    }

    private static boolean cookieAttributeSupported() {
        try {
            Cookie.class.getMethod("setAttribute", String.class, String.class);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

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

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (uri == null) {
            return null;
        }
        if (context != null && !context.isBlank() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    private record TokenState(String token, boolean generated) {}

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CSRF protection filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("CSRF protection filter destroyed");
    }
}
