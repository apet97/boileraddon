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

/**
 * Ensures {@code X-Request-Id} is included on every HTTP response.
 * <p>
 * DiagnosticContextFilter generates/attaches a request id early in the chain; this filter
 * simply copies the attribute (or current MDC value) onto the response headers after
 * the downstream handlers finish.
 * </p>
 */
public class RequestIdPropagationFilter implements Filter {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Try to set request ID from attribute first (available early in the chain)
        if (request instanceof HttpServletRequest httpRequest &&
                response instanceof HttpServletResponse httpResponse) {
            Object attr = httpRequest.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR);
            if (attr != null) {
                httpResponse.setHeader(REQUEST_ID_HEADER, attr.toString());
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // After chain completes, check if we need to set/update from MDC
            if (request instanceof HttpServletRequest httpRequest &&
                    response instanceof HttpServletResponse httpResponse) {
                // If header not already set, try MDC
                if (httpResponse.getHeader(REQUEST_ID_HEADER) == null) {
                    String requestId = MDC.get("requestId");
                    if (requestId != null && !requestId.isBlank()) {
                        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
                    }
                }
            }
        }
    }
}
