package com.clockify.addon.sdk.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter that extracts workspace context from JWT tokens in settings/UI requests.
 *
 * <p>Reads JWT from:
 * <ul>
 *   <li>Query parameter: {@code ?auth_token=<jwt>}</li>
 *   <li>Authorization header: {@code Authorization: Bearer <jwt>}</li>
 * </ul>
 *
 * <p>On successful JWT verification, sets request attributes:
 * <ul>
 *   <li>{@code clockify.workspaceId} - The workspace identifier</li>
 *   <li>{@code clockify.userId} - The user identifier</li>
 * </ul>
 *
 * <p>If JWT verification fails or no token is present, the filter continues
 * without setting attributes. Controllers should handle missing context gracefully.
 *
 * <p><strong>Architecture:</strong> This filter is intentionally simple and delegates
 * JWT verification to addon-specific verifiers. The SDK provides the extraction
 * mechanism, but addons control verification policy.
 */
public class WorkspaceContextFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceContextFilter.class);

    public static final String WORKSPACE_ID_ATTR = "clockify.workspaceId";
    public static final String USER_ID_ATTR = "clockify.userId";

    private final JwtVerifierFunction verifierFunction;

    /**
     * Functional interface for JWT verification to avoid coupling SDK to specific verifier implementations.
     */
    @FunctionalInterface
    public interface JwtVerifierFunction {
        /**
         * Verifies JWT and returns decoded payload, or null if verification fails.
         */
        JsonNode verify(String jwt);
    }

    public WorkspaceContextFilter(JwtVerifierFunction verifierFunction) {
        if (verifierFunction == null) {
            throw new IllegalArgumentException("JWT verifier function is required");
        }
        this.verifierFunction = verifierFunction;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String jwt = extractJwt(httpRequest);

        if (jwt != null && !jwt.isBlank()) {
            try {
                JsonNode payload = verifierFunction.verify(jwt);
                if (payload != null) {
                    String workspaceId = payload.path("workspaceId").asText(null);
                    String userId = payload.path("userId").asText(null);

                    if (workspaceId != null && !workspaceId.isBlank()) {
                        request.setAttribute(WORKSPACE_ID_ATTR, workspaceId);
                        logger.debug("Set workspace context: {}", workspaceId);
                    }

                    if (userId != null && !userId.isBlank()) {
                        request.setAttribute(USER_ID_ATTR, userId);
                        logger.debug("Set user context: {}", userId);
                    }
                }
            } catch (Exception e) {
                logger.debug("JWT verification failed: {}", e.getMessage());
                // Continue without setting attributes - controllers handle missing context
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Extracts JWT from query parameter or Authorization header.
     */
    private String extractJwt(HttpServletRequest request) {
        // Try query parameter first (Clockify settings pattern)
        String token = firstNonBlank(
                request.getParameter("auth_token"),
                request.getParameter("token"),
                request.getParameter("jwt"));
        if (token != null) {
            return token;
        }

        // Try Authorization header (standard Bearer pattern)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("WorkspaceContextFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("WorkspaceContextFilter destroyed");
    }
}
