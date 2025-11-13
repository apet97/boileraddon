package com.example.rules.web;

import com.clockify.addon.sdk.logging.LoggingContext;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestContextTest {

    @Test
    void attachWorkspaceAndUser_setsRequestAttributes() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        LoggingContext ctx = LoggingContext.create();

        RequestContext.attachWorkspace(req, ctx, "ws-123");
        RequestContext.attachUser(req, ctx, "user-9");

        verify(req).setAttribute(DiagnosticContextFilter.WORKSPACE_ID_ATTR, "ws-123");
        verify(req).setAttribute(DiagnosticContextFilter.USER_ID_ATTR, "user-9");

        // requestId() returns empty when not set
        assertEquals("", RequestContext.requestId(req));
    }

    @Test
    void requestId_readsAttributeOrReturnsEmpty() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("rid-1");
        assertEquals("rid-1", RequestContext.requestId(req));

        assertEquals("", RequestContext.requestId(null));
    }
}

