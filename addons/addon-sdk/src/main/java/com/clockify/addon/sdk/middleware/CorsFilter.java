package com.clockify.addon.sdk.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal CORS filter with explicit origin allowlist via ADDON_CORS_ORIGINS env var.
 *
 * ADDON_CORS_ORIGINS: comma-separated list of exact origins to allow, e.g.:
 *   https://app.clockify.me,https://example.com
 */
public class CorsFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    private final Set<String> allowedOrigins;
    private final boolean allowCredentials;

    public CorsFilter() {
        this(System.getenv("ADDON_CORS_ORIGINS"),
             Boolean.parseBoolean(System.getenv().getOrDefault("ADDON_CORS_ALLOW_CREDENTIALS", "false")));
    }

    public CorsFilter(String originsCsv) {
        this(originsCsv, false);
    }

    public CorsFilter(String originsCsv, boolean allowCredentials) {
        if (originsCsv == null || originsCsv.isBlank()) {
            this.allowedOrigins = Set.of();
            logger.info("CorsFilter initialized with empty allowlist; CORS disabled");
        } else {
            Set<String> s = new HashSet<>();
            Arrays.stream(originsCsv.split(","))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .forEach(s::add);
            this.allowedOrigins = Set.copyOf(s);
            logger.info("CorsFilter allowlist: {}", this.allowedOrigins);
        }
        this.allowCredentials = allowCredentials;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse resp)) {
            chain.doFilter(request, response);
            return;
        }

        String origin = req.getHeader("Origin");
        boolean preflight = "OPTIONS".equalsIgnoreCase(req.getMethod()) && req.getHeader("Access-Control-Request-Method") != null;

        // Always vary on Origin for caches
        resp.addHeader("Vary", "Origin");

        if (origin != null && allowedOrigins.contains(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            // Allow typical headers used in add-ons
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, clockify-webhook-signature");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Max-Age", "600");
            if (allowCredentials) {
                resp.setHeader("Access-Control-Allow-Credentials", "true");
            }

            if (preflight) {
                resp.setStatus(204);
                return;
            }
        } else if (preflight) {
            // Unknown origin preflight → reject quickly
            resp.setStatus(403);
            return;
        }

        chain.doFilter(request, response);
    }
}
