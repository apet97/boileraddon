package com.clockify.addon.sdk.middleware;

import com.clockify.addon.sdk.security.AuditLogger;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * SECURITY: Mandatory rate limiting for critical endpoints (lifecycle, webhooks, token operations).
 *
 * Critical endpoints are sensitive to abuse:
 * - /lifecycle: Installation/deletion events (could trigger cleanup logic)
 * - /webhook: User event processing
 *
 * This filter applies strict rate limits to prevent:
 * - Spam/DoS attacks targeting sensitive operations
 * - Rapid installation/deletion cycles
 * - Resource exhaustion from event floods
 *
 * Fail-closed: If rate limiter initialization fails, requests are BLOCKED (security-first approach).
 */
public class CriticalEndpointRateLimiter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CriticalEndpointRateLimiter.class);

    // Lifecycle endpoints: very sensitive, should be rare (1 install/delete per workspace)
    private static final double LIFECYCLE_PERMITS_PER_SECOND = 0.1;  // 1 per 10 seconds

    // Webhook endpoints: user events, moderate frequency
    private static final double WEBHOOK_PERMITS_PER_SECOND = 1.0;    // 1 per second

    // Default catch-all for other sensitive paths
    private static final double DEFAULT_PERMITS_PER_SECOND = 0.5;    // 1 per 2 seconds

    private final LoadingCache<LimiterKey, com.google.common.util.concurrent.RateLimiter> limiters;
    private final boolean failClosed;

    /**
     * Creates a critical endpoint rate limiter.
     *
     * @param failClosed If true, block requests when rate limiter fails (security-first).
     *                   If false, allow requests (fail-open, less secure).
     */
    public CriticalEndpointRateLimiter(boolean failClosed) {
        this.failClosed = failClosed;
        this.limiters = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(5000)  // Smaller cache for critical paths
                .build(new CacheLoader<LimiterKey, com.google.common.util.concurrent.RateLimiter>() {
                    @Override
                    public com.google.common.util.concurrent.RateLimiter load(LimiterKey key) {
                        double rate = getPermitRate(key.scope());
                        logger.debug("Creating critical endpoint rate limiter for: {} ({}) -> {} permits/sec",
                                key.identifier(), key.scope(), rate);
                        return com.google.common.util.concurrent.RateLimiter.create(rate);
                    }
                });
        logger.info("Critical endpoint rate limiter initialized (fail-closed: {})", failClosed);
    }

    /**
     * Default: fail-closed for security
     */
    public CriticalEndpointRateLimiter() {
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
        String path = httpRequest.getRequestURI();
        Scope scope = resolveScope(path);

        // SECURITY: Only apply to critical paths
        if (scope == null) {
            chain.doFilter(request, response);
            return;
        }

        String identifier = getIdentifier(httpRequest);

        try {
            LimiterKey limiterKey = new LimiterKey(scope, identifier);
            com.google.common.util.concurrent.RateLimiter limiter = limiters.get(limiterKey);
            double permitsPerSecond = getPermitRate(scope);

            if (limiter.tryAcquire()) {
                // Request allowed
                chain.doFilter(request, response);
            } else {
                // Rate limit exceeded - log audit event
                logger.warn("CRITICAL: Rate limit exceeded for path: {} identifier: {}", path, identifier);
                AuditLogger.log(AuditLogger.AuditEvent.RATE_LIMIT_EXCEEDED)
                        .clientIp(identifier)
                        .detail("path", path)
                        .detail("scope", scope.name())
                        .detail("limit_permits_sec", permitsPerSecond)
                        .error();
                sendRateLimitError(httpResponse, permitsPerSecond);
            }

        } catch (ExecutionException e) {
            logger.error("CRITICAL: Error in rate limiter for path: {} identifier: {}", path, identifier, e);

            if (failClosed) {
                // SECURITY: Block request if rate limiter fails
                logger.warn("SECURITY: Blocking request due to rate limiter failure (fail-closed mode)");
                sendRateLimitError(httpResponse, 0.0);
            } else {
                // Fail open: allow the request
                logger.warn("Allowing request despite rate limiter failure (fail-open mode)");
                try {
                    chain.doFilter(request, response);
                } catch (Exception ex) {
                    logger.error("Error processing request after rate limiter failure", ex);
                }
            }
        }
    }

    private Scope resolveScope(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/lifecycle")) {
            return Scope.LIFECYCLE;
        }
        if (path.startsWith("/webhook") || path.endsWith("/webhook")) {
            return Scope.WEBHOOK;
        }
        return null;
    }

    /**
     * Gets appropriate permit rate for a given identifier/path.
     */
    private double getPermitRate(Scope scope) {
        if (scope == Scope.LIFECYCLE) {
            return LIFECYCLE_PERMITS_PER_SECOND;
        }
        if (scope == Scope.WEBHOOK) {
            return WEBHOOK_PERMITS_PER_SECOND;
        }
        return DEFAULT_PERMITS_PER_SECOND;
    }

    /**
     * Gets the identifier for rate limiting (workspace ID preferred).
     */
    private String getIdentifier(HttpServletRequest request) {
        // Try to get workspace ID from header (preferred)
        String workspaceId = request.getHeader("X-Workspace-Id");
        if (workspaceId != null && !workspaceId.isEmpty()) {
            return workspaceId;
        }

        // Try to extract from path
        String path = request.getRequestURI();
        if (path.contains("workspace/")) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("workspace".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }

        // Fallback to IP address
        return getClientIp(request);
    }

    /**
     * Gets client IP, accounting for proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Sends a 429 Too Many Requests error response.
     */
    private void sendRateLimitError(HttpServletResponse response, double permitsPerSecond) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");  // Longer retry for critical endpoints

        String json;
        if (permitsPerSecond > 0) {
            json = String.format(
                    "{\"error\":\"rate_limit_exceeded\",\"message\":\"Critical endpoint rate limit exceeded. Please retry after some time.\",\"retry_after\":60}");
        } else {
            json = "{\"error\":\"rate_limit_exceeded\",\"message\":\"Critical endpoint temporarily unavailable due to rate limiting.\",\"retry_after\":60}";
        }

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Critical endpoint rate limiter filter initialized");
    }

    @Override
    public void destroy() {
        limiters.invalidateAll();
        logger.info("Critical endpoint rate limiter destroyed");
    }

    private record LimiterKey(Scope scope, String identifier) { }

    private enum Scope {
        LIFECYCLE,
        WEBHOOK
    }

    /**
     * Returns cache statistics for monitoring.
     */
    public String getStats() {
        return String.format("Critical rate limiter - Size: %d, Hits: %d, Misses: %d",
                limiters.size(),
                limiters.stats().hitCount(),
                limiters.stats().missCount());
    }
}
