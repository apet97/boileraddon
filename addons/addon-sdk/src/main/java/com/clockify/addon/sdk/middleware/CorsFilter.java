package com.clockify.addon.sdk.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.*;

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
    private final List<AllowedWildcard> wildcardOrigins;

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
            this.wildcardOrigins = List.of();
            logger.info("CorsFilter initialized with empty allowlist; CORS disabled");
        } else {
            Set<String> s = new HashSet<>();
            List<AllowedWildcard> wildcards = new ArrayList<>();
            Arrays.stream(originsCsv.split(","))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .forEach(v -> {
                        if (v.contains("*")) {
                            AllowedWildcard aw = AllowedWildcard.parse(v);
                            if (aw != null) wildcards.add(aw);
                            else s.add(v); // fallback to exact if unparsable
                        } else {
                            s.add(v);
                        }
                    });
            this.allowedOrigins = Set.copyOf(s);
            this.wildcardOrigins = Collections.unmodifiableList(wildcards);
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

        if (origin != null && (allowedOrigins.contains(origin) || isWildcardAllowed(origin))) {
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

    private boolean isWildcardAllowed(String origin) {
        if (wildcardOrigins.isEmpty() || origin == null) return false;
        try {
            URI o = URI.create(origin);
            String scheme = Optional.ofNullable(o.getScheme()).orElse("");
            String host = Optional.ofNullable(o.getHost()).orElse("").toLowerCase(Locale.ROOT);
            for (AllowedWildcard aw : wildcardOrigins) {
                if (aw.matches(scheme, host)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static final class AllowedWildcard {
        final String scheme; // e.g., https
        final String suffix; // e.g., clockify.me

        private AllowedWildcard(String scheme, String suffix) {
            this.scheme = scheme;
            this.suffix = suffix;
        }

        static AllowedWildcard parse(String pattern) {
            // Expect formats like: https://*.example.com or http://*.example.com
            try {
                int schemeEnd = pattern.indexOf("://");
                String scheme = schemeEnd > 0 ? pattern.substring(0, schemeEnd).toLowerCase(Locale.ROOT) : "";
                String host = schemeEnd > 0 ? pattern.substring(schemeEnd + 3) : pattern;
                host = host.trim().toLowerCase(Locale.ROOT);
                if (!host.startsWith("*.") || host.length() < 3) return null;
                String suffix = host.substring(2); // drop '*.'
                return new AllowedWildcard(scheme, suffix);
            } catch (Exception e) {
                return null;
            }
        }

        boolean matches(String originScheme, String originHost) {
            if (scheme != null && !scheme.isBlank() && !scheme.equalsIgnoreCase(originScheme)) return false;
            return originHost.endsWith(suffix) && !originHost.equals(suffix); // must be a subdomain
        }
    }
}
