package com.clockify.addon.sdk.middleware;

import com.clockify.addon.sdk.logging.RedactingLogger;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal request logging filter with header scrubbing.
 * Off by default; wire it in your server/app only when needed.
 */
public class RequestLoggingFilter implements Filter {
    private static final RedactingLogger redactingLogger = RedactingLogger.get(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest req) {
            String method = req.getMethod();
            String path = req.getRequestURI();
            Map<String, String> headers = collectHeaders(req);
            redactingLogger.infoRequest(method, path, headers);
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

    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        return RedactingLogger.redactHeaders(headers);
    }
}
