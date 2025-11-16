package com.example.templateaddon;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsControllerTest {

    @Test
    void returnsUnauthorizedWhenWorkspaceMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR)).thenReturn(null);

        HttpResponse response = new SettingsController().handle(request);
        assertEquals(401, response.getStatusCode());
    }

    @Test
    void returnsHtmlWhenWorkspacePresent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR)).thenReturn("ws1");

        HttpResponse response = new SettingsController().handle(request);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("<html"), "Response should include HTML");
    }
}
