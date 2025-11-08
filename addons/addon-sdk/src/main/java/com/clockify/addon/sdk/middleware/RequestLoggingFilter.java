package com.clockify.addon.sdk.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Minimal request logging filter with header scrubbing.
 * Off by default; wire it in your server/app only when needed.
 */
public class RequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Case-insensitive set of header names to scrub
    private static final Set<String> SENSITIVE_HEADERS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        Collections.addAll(SENSITIVE_HEADERS,
                "authorization",
                "proxy-authorization",
                "x-addon-token",
                "clockify-webhook-signature",
                "cookie",
                "set-cookie"
        );
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest req) {
            String method = req.getMethod();
            String path = req.getRequestURI();
            Map<String, String> headers = collectHeaders(req);
            Map<String, String> sanitized = sanitizeHeaders(headers);
            if (logger.isInfoEnabled()) {
                logger.info("{} {} headers={}", method, path, sanitized);
            }
        }
        chain.doFilter(request, response);
    }

    private Map<String, String> collectHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = req.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = req.getHeader(name);
                headers.put(name, value);
            }
        }
        return headers;
    }

    /**
     * Return a copy of headers with sensitive values redacted.
     */
    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            if (name != null && SENSITIVE_HEADERS.contains(name)) {
                out.put(name, "[REDACTED]");
            } else {
                out.put(name, value);
            }
        }
        return out;
    }
}

