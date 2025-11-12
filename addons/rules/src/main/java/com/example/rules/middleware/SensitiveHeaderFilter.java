package com.example.rules.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Filter that redacts sensitive headers to prevent token leakage in logs.
 *
 * <p><strong>Security Requirement:</strong> Per Clockify addon guide (line 1750),
 * sensitive headers like X-Addon-Token must be redacted in logs to prevent
 * installation token exposure.</p>
 *
 * <p>This filter wraps the HttpServletRequest to provide redacted values when
 * headers are accessed for logging purposes.</p>
 */
public class SensitiveHeaderFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveHeaderFilter.class);

    /**
     * Set of header names (lowercase) that contain sensitive data and must be redacted.
     */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "x-addon-token",
        "authorization",
        "clockify-signature",
        "x-addon-lifecycle-token",
        "cookie",
        "set-cookie"
    );

    private static final String REDACTED_VALUE = "[REDACTED]";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("SensitiveHeaderFilter initialized - protecting {} header types",
            SENSITIVE_HEADERS.size());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletRequest wrappedRequest = new SensitiveHeaderRequestWrapper(httpRequest);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        logger.debug("SensitiveHeaderFilter destroyed");
    }

    /**
     * Request wrapper that redacts sensitive header values when accessed.
     * This prevents tokens from appearing in logs if request headers are logged.
     */
    private static class SensitiveHeaderRequestWrapper extends HttpServletRequestWrapper {

        public SensitiveHeaderRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            if (value != null && isSensitiveHeader(name)) {
                return REDACTED_VALUE;
            }
            return value;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isSensitiveHeader(name)) {
                // Return single redacted value
                return Collections.enumeration(List.of(REDACTED_VALUE));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Don't redact header names, only values
            return super.getHeaderNames();
        }

        /**
         * Checks if a header name is sensitive and should be redacted.
         *
         * @param headerName The header name to check
         * @return true if the header should be redacted
         */
        private boolean isSensitiveHeader(String headerName) {
            if (headerName == null) {
                return false;
            }
            return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
        }
    }
}
