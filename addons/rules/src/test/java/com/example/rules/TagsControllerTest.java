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

class TagsControllerTest {
    private TagsController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new TagsController((baseUrl, token) -> null);
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
    void listTagsReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tags");

        HttpResponse response = controller.listTags().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void createTagReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tags");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"name\":\"demo\"}")));

        HttpResponse response = controller.createTag().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void updateTagReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tags");
        when(request.getParameter("id")).thenReturn("tag-1");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"name\":\"demo\"}")));

        HttpResponse response = controller.updateTag().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void deleteTagReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-tags");
        when(request.getParameter("id")).thenReturn("tag-2");

        HttpResponse response = controller.deleteTag().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }
}
