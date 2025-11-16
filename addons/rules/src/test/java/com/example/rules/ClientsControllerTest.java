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

class ClientsControllerTest {
    private ClientsController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new ClientsController((baseUrl, token) -> null);
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
    void createClientReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-clients");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"name\":\"Demo\"}")));

        HttpResponse response = controller.createClient().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void updateClientReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-clients");
        when(request.getParameter("id")).thenReturn("client-1");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"name\":\"New\"}")));

        HttpResponse response = controller.updateClient().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }

    @Test
    void deleteClientReturns412WhenTokenMissing() throws Exception {
        when(request.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn("ws-clients");
        when(request.getParameter("id")).thenReturn("client-2");

        HttpResponse response = controller.deleteClient().handle(request);

        assertEquals(412, response.getStatusCode());
        assertTrue(response.getBody().contains("RULES.MISSING_TOKEN"));
    }
}
