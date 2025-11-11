package com.clockify.addon.sdk.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates SLF4J MDC with request-scoped metadata (requestId, workspaceId, userId).
 * Also ensures the requestId is propagated via the {@code X-Request-Id} header.
 */
public class DiagnosticContextFilter implements Filter {
    public static final String REQUEST_ID_ATTR = "clockify.requestId";
    public static final String WORKSPACE_ID_ATTR = "clockify.workspaceId";
    public static final String USER_ID_ATTR = "clockify.userId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) ||
                !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = resolveRequestId(httpRequest);
        MDC.put("requestId", requestId);
        httpRequest.setAttribute(REQUEST_ID_ATTR, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("workspaceId");
            MDC.remove("userId");
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerId = request.getHeader(REQUEST_ID_HEADER);
        if (headerId != null && !headerId.isBlank()) {
            return headerId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
