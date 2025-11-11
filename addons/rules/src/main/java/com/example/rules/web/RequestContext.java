package com.example.rules.web;

import com.clockify.addon.sdk.logging.LoggingContext;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilities for working with request-scoped context (requestId/workspaceId) and MDC wiring.
 */
public final class RequestContext {
    private RequestContext() {
    }

    public static LoggingContext logging(HttpServletRequest request) {
        return LoggingContext.create().request(requestId(request));
    }

    public static void attachWorkspace(HttpServletRequest request, String workspaceId) {
        if (request == null || workspaceId == null || workspaceId.isBlank()) {
            return;
        }
        request.setAttribute(DiagnosticContextFilter.WORKSPACE_ID_ATTR, workspaceId);
    }

    public static void attachWorkspace(HttpServletRequest request, LoggingContext ctx, String workspaceId) {
        attachWorkspace(request, workspaceId);
        if (ctx != null && workspaceId != null && !workspaceId.isBlank()) {
            ctx.workspace(workspaceId);
        }
    }

    public static void attachUser(HttpServletRequest request, LoggingContext ctx, String userId) {
        if (request == null || userId == null || userId.isBlank()) {
            return;
        }
        request.setAttribute(DiagnosticContextFilter.USER_ID_ATTR, userId);
        if (ctx != null) {
            ctx.user(userId);
        }
    }

    public static String requestId(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        Object attr = request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
