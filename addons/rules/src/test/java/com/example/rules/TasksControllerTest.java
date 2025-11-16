package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.security.TokenStore;
import com.example.rules.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TasksControllerTest {
    private TasksController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new TasksController((baseUrl, token) -> null);
        request = mock(HttpServletRequest.class);
        TokenStore.clear();
        RequestContext.configureWorkspaceFallback(false);
        when(request.getAttribute(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
    }

    @Test
    void createTaskReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tasks");
        when(request.getParameter("projectId")).thenReturn("proj-1");

        HttpResponse response = controller.createTask().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void deleteTaskReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tasks");
        when(request.getParameter("id")).thenReturn("task-1");

        HttpResponse response = controller.deleteTask().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }
}
