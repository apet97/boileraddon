package com.clockify.addon.sdk.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wraps an SLF4J logger and redacts sensitive headers before emitting structured messages.
 */
public final class RedactingLogger {
    public static final String REDACTED_VALUE = "‹redacted›";
    private static final Set<String> SECRET_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "x-addon-token",
            "clockify-webhook-signature",
            "cookie",
            "set-cookie"
    );

    private final Logger logger;

    private RedactingLogger(Class<?> owner) {
        this.logger = LoggerFactory.getLogger(owner);
    }

    public static RedactingLogger get(Class<?> owner) {
        return new RedactingLogger(owner);
    }

    /**
     * Logs an HTTP request with sensitive headers redacted.
     */
    public void infoRequest(String method, String path, Map<String, String> headers) {
        if (logger.isInfoEnabled()) {
            logger.info("{} {} headers={}", method, path, redactInternal(headers));
        }
    }

    public static Map<String, String> redactHeaders(Map<String, String> headers) {
        return redactInternal(headers);
    }

    private static Map<String, String> redactInternal(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> sanitized = new HashMap<>();
        headers.forEach((name, value) -> {
            String key = name == null ? "" : name;
            String normalized = key.toLowerCase(Locale.ROOT);
            sanitized.put(key, SECRET_HEADERS.contains(normalized) ? REDACTED_VALUE : value);
        });
        return sanitized;
    }
}
