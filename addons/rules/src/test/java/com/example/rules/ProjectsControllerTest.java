package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.PlatformAuthFilter;
import com.clockify.addon.sdk.security.TokenStore;
import com.example.rules.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectsControllerTest {
    private ProjectsController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new ProjectsController((baseUrl, token) -> null);
        request = mock(HttpServletRequest.class);
        RequestContext.configureWorkspaceFallback(false);
        TokenStore.clear();
        when(request.getAttribute(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
    }

    @Test
    void createProjectReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-test");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"name\":\"demo\"}")));

        HttpResponse response = controller.createProject().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("token"));
    }
}
