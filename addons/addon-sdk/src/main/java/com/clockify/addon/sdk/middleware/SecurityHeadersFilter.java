package com.clockify.addon.sdk.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Simple security headers filter.
 * - X-Content-Type-Options: nosniff
 * - Referrer-Policy: no-referrer
 * - Strict-Transport-Security: only when the request is secure or forwarded as https
 * - Content-Security-Policy: set frame-ancestors if ADDON_FRAME_ANCESTORS env is present
 */
public class SecurityHeadersFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    private final String frameAncestors;

    public SecurityHeadersFilter() {
        this(System.getenv("ADDON_FRAME_ANCESTORS"));
    }

    public SecurityHeadersFilter(String frameAncestors) {
        this.frameAncestors = frameAncestors;
        if (frameAncestors == null || frameAncestors.isBlank()) {
            logger.info("SecurityHeadersFilter initialized without frame-ancestors (CSP not set). Use ADDON_FRAME_ANCESTORS to enable.");
        } else {
            logger.info("SecurityHeadersFilter will set CSP frame-ancestors: {}", frameAncestors);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse resp && request instanceof HttpServletRequest req) {
            // Basic security headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Referrer-Policy", "no-referrer");

            // HSTS only if secure (or forwarded as https)
            boolean secure = req.isSecure();
            String forwardedProto = req.getHeader("X-Forwarded-Proto");
            if (secure || (forwardedProto != null && forwardedProto.equalsIgnoreCase("https"))) {
                resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }

            // Optional CSP frame-ancestors (avoid breaking embedding unless explicitly configured)
            if (frameAncestors != null && !frameAncestors.isBlank()) {
                resp.setHeader("Content-Security-Policy", "frame-ancestors " + frameAncestors);
            }
        }

        chain.doFilter(request, response);
    }
}

