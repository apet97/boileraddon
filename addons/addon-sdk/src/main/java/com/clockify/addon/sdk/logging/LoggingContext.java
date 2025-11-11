package com.clockify.addon.sdk.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for temporarily attaching structured context (workspaceId, userId, etc.) to MDC.
 * Usage:
 * <pre>
 * try (LoggingContext ctx = LoggingContext.create(request).workspace(workspaceId).user(userId)) {
 *     // handler logic
 * }
 * </pre>
 */
public final class LoggingContext implements AutoCloseable {
    private final List<String> keys = new ArrayList<>();

    private LoggingContext() {
    }

    public static LoggingContext create() {
        return new LoggingContext();
    }

    public static LoggingContext create(HttpServletRequest request) {
        LoggingContext ctx = new LoggingContext();
        if (request != null) {
            Object workspaceId = request.getAttribute(com.clockify.addon.sdk.middleware.DiagnosticContextFilter.WORKSPACE_ID_ATTR);
            Object userId = request.getAttribute(com.clockify.addon.sdk.middleware.DiagnosticContextFilter.USER_ID_ATTR);
            ctx.workspace(workspaceId == null ? null : workspaceId.toString());
            ctx.user(userId == null ? null : userId.toString());
        }
        return ctx;
    }

    public LoggingContext workspace(String workspaceId) {
        return put("workspaceId", workspaceId);
    }

    public LoggingContext user(String userId) {
        return put("userId", userId);
    }

    public LoggingContext request(String requestId) {
        return put("requestId", requestId);
    }

    private LoggingContext put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
            keys.add(key);
        }
        return this;
    }

    @Override
    public void close() {
        for (String key : keys) {
            MDC.remove(key);
        }
    }
}
