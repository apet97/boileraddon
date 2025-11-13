package com.example.rules.web;

import com.clockify.addon.sdk.logging.LoggingContext;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.example.rules.security.PlatformAuthFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilities for working with request-scoped context (requestId/workspaceId) and MDC wiring.
 */
public final class RequestContext {
    private static volatile boolean workspaceFallbackAllowed = false;

    private RequestContext() {
    }

    public static void configureWorkspaceFallback(boolean allowFallback) {
        workspaceFallbackAllowed = allowFallback;
    }

    public static boolean workspaceFallbackAllowed() {
        return workspaceFallbackAllowed;
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

    public static String resolveWorkspaceId(HttpServletRequest request) {
        return resolveWorkspaceId(request, workspaceFallbackAllowed);
    }

    public static String resolveWorkspaceId(HttpServletRequest request, boolean allowFallback) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID);
        if (attr instanceof String workspace && !workspace.isBlank()) {
            return workspace;
        }
        Object contextAttr = request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
        if (contextAttr instanceof String workspace && !workspace.isBlank()) {
            return workspace;
        }
        if (!allowFallback) {
            return null;
        }
        String param = request.getParameter("workspaceId");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }
        String header = request.getHeader("X-Workspace-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        Object diagnostic = request.getAttribute(DiagnosticContextFilter.WORKSPACE_ID_ATTR);
        if (diagnostic instanceof String workspace && !workspace.isBlank()) {
            return workspace;
        }
        return null;
    }
}
