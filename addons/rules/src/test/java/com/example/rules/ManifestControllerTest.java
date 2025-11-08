package com.example.rules;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ManifestControllerTest {

    private ManifestController controller;
    private ObjectMapper mapper;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test description")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE"})
                .build();

        controller = new ManifestController(manifest);
        mapper = new ObjectMapper();
        request = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    void testManifestResponse() throws Exception {
        HttpResponse response = controller.handle(request);

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.contentType());

        JsonNode json = mapper.readTree(response.body());
        assertEquals("1.3", json.get("schemaVersion").asText());
        assertEquals("rules", json.get("key").asText());
        assertEquals("Rules", json.get("name").asText());
        assertFalse(json.has("$schema")); // Runtime manifest should NOT have $schema
    }

    @Test
    void testManifestWithForwardedHeaders() throws Exception {
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("X-Forwarded-Host")).thenReturn("example.ngrok.io");
        when(request.getHeader("X-Forwarded-Prefix")).thenReturn("/rules");

        HttpResponse response = controller.handle(request);

        JsonNode json = mapper.readTree(response.body());
        String baseUrl = json.get("baseUrl").asText();

        assertTrue(baseUrl.startsWith("https://example.ngrok.io"));
    }
}
