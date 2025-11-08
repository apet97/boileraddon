package com.clockify.addon.sdk.middleware;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
// Do not import Guava RateLimiter by simple name to avoid clashing with this class name
// Use the fully-qualified name com.google.common.util.concurrent.RateLimiter instead
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting middleware using token bucket algorithm.
 * Prevents abuse by limiting requests per IP address or workspace.
 */
public class RateLimiter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    private final LoadingCache<String, com.google.common.util.concurrent.RateLimiter> limiters;
    private final double permitsPerSecond;
    private final String limitBy;

    /**
     * Creates a rate limiter.
     *
     * @param permitsPerSecond Maximum requests per second per identifier
     * @param limitBy How to identify clients: "ip" or "workspace"
     */
    public RateLimiter(double permitsPerSecond, String limitBy) {
        this.permitsPerSecond = permitsPerSecond;
        this.limitBy = limitBy;

        // Cache of rate limiters, one per unique identifier
        // Expires after 5 minutes of inactivity to prevent memory leaks
        this.limiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(10000) // Max 10k unique identifiers
                .build(new CacheLoader<String, com.google.common.util.concurrent.RateLimiter>() {
                    @Override
                    public com.google.common.util.concurrent.RateLimiter load(String key) {
                        logger.debug("Creating new rate limiter for: {}", key);
                        return com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond);
                    }
                });

        logger.info("Rate limiter initialized: {} permits/sec, limit by: {}", permitsPerSecond, limitBy);
    }

    /**
     * Creates a default rate limiter: 10 requests/sec per IP.
     */
    public RateLimiter() {
        this(10.0, "ip");
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

        String identifier = getIdentifier(httpRequest);

        try {
            com.google.common.util.concurrent.RateLimiter limiter = limiters.get(identifier);

            // Try to acquire a permit (non-blocking)
            if (limiter.tryAcquire()) {
                // Request allowed
                chain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                logger.warn("Rate limit exceeded for: {} ({})", identifier, limitBy);
                sendRateLimitError(httpResponse, identifier);
            }

        } catch (ExecutionException e) {
            logger.error("Error getting rate limiter for: " + identifier, e);
            // Fail open: allow the request if rate limiter fails
            chain.doFilter(request, response);
        }
    }

    /**
     * Gets the identifier for rate limiting.
     */
    private String getIdentifier(HttpServletRequest request) {
        if ("workspace".equalsIgnoreCase(limitBy)) {
            // Extract workspace ID from path or header
            String workspaceId = extractWorkspaceId(request);
            return workspaceId != null ? workspaceId : getClientIp(request);
        } else {
            // Default: limit by IP
            return getClientIp(request);
        }
    }

    /**
     * Extracts workspace ID from request.
     */
    private String extractWorkspaceId(HttpServletRequest request) {
        // Try header first
        String workspaceId = request.getHeader("X-Workspace-Id");
        if (workspaceId != null && !workspaceId.isEmpty()) {
            return workspaceId;
        }

        // Try path: /workspace/{id}/...
        String path = request.getRequestURI();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("workspace".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }

        return null;
    }

    /**
     * Gets the client IP address, accounting for proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, take the first
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Sends a 429 Too Many Requests error response.
     */
    private void sendRateLimitError(HttpServletResponse response, String identifier) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "1"); // Retry after 1 second

        String json = String.format(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests. Please slow down.\",\"identifier\":\"%s\",\"limit\":%.1f}",
                identifier, permitsPerSecond);

        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // Clean up cache
        limiters.invalidateAll();
        logger.info("Rate limiter destroyed");
    }

    /**
     * Gets current cache statistics for monitoring.
     */
    public String getStats() {
        return String.format("Rate limiter stats - Size: %d, Hits: %d, Misses: %d",
                limiters.size(),
                limiters.stats().hitCount(),
                limiters.stats().missCount());
    }
}
