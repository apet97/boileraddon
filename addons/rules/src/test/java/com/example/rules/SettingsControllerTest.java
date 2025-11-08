package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SettingsControllerTest {

    @Test
    void returnsHtmlWithExpectedTitle() throws Exception {
        SettingsController controller = new SettingsController();
        HttpServletRequest req = mock(HttpServletRequest.class);

        HttpResponse resp = controller.handle(req);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.getContentType().startsWith("text/html"));
        assertTrue(resp.getBody().contains("<title>Rules Add-on</title>"));
        assertTrue(resp.getBody().contains("GET /api/rules"));
    }
}

