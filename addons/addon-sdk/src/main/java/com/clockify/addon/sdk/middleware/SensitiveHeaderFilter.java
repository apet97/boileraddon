package com.clockify.addon.sdk.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
 */
public class SensitiveHeaderFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveHeaderFilter.class);

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
    public void init(FilterConfig filterConfig) {
        logger.info("SensitiveHeaderFilter initialized - protecting {} header types", SENSITIVE_HEADERS.size());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
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

    private static class SensitiveHeaderRequestWrapper extends HttpServletRequestWrapper {

        SensitiveHeaderRequestWrapper(HttpServletRequest request) {
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
                return Collections.enumeration(List.of(REDACTED_VALUE));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return super.getHeaderNames();
        }

        private boolean isSensitiveHeader(String headerName) {
            if (headerName == null) {
                return false;
            }
            return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
        }
    }
}
