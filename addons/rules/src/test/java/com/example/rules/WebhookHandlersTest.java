package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class WebhookHandlersTest {

    private RulesStore store;
    private ObjectMapper mapper;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        store = new RulesStore();
        mapper = new ObjectMapper();
        request = Mockito.mock(HttpServletRequest.class);

        // Clear token store
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.setProperty("ADDON_ACCEPT_JWT_SIGNATURE", "true");
    }

    @AfterEach
    void tearDown() {
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.clearProperty("ADDON_ACCEPT_JWT_SIGNATURE");
    }

    @Test
    void testWebhook_noRules() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Test entry",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequest(payload, authToken);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("no_rules", json.get("status").asText());
    }

    @Test
    void testWebhook_withMatchingRule() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        // Create a rule
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Action action = new Action("add_tag", Collections.singletonMap("tag", "billable"));
        Rule rule = new Rule("rule-1", "Tag meetings", true, "AND",
                Collections.singletonList(condition),
                Collections.singletonList(action));
        store.save(workspaceId, rule);

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Client meeting",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequest(payload, authToken);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("actions_logged", json.get("status").asText());
        assertEquals(1, json.get("actionsCount").asInt());
    }

    @Test
    void testWebhook_invalidSignature() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Test entry",
                    "tagIds": []
                }
            }
            """;

        // Setup request with WRONG signature
        setupWebhookRequest(payload, "wrong-token");

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(403, response.getStatusCode());
    }

    @Test
    void testWebhook_noMatchingRules() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        // Create a rule that won't match
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Action action = new Action("add_tag", Collections.singletonMap("tag", "billable"));
        Rule rule = new Rule("rule-1", "Tag meetings", true, "AND",
                Collections.singletonList(condition),
                Collections.singletonList(action));
        store.save(workspaceId, rule);

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Development work",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequest(payload, authToken);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("no_match", json.get("status").asText());
        assertEquals(0, json.get("actionsCount").asInt());
    }

    private void setupWebhookRequest(String payload, String authToken) throws Exception {
        String signature = WebhookSignatureValidator.computeSignature(authToken, payload);

        when(request.getHeader("clockify-webhook-signature")).thenReturn(signature);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);

        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            @Override
            public int read() {
                return bis.read();
            }

            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
            }
        };

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }

    @Test
    void testWebhook_acceptsDeveloperJwtSignature() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Provide stored token (required by validator even when accepting JWT header)
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": { "id": "e1", "description": "hello", "tagIds": [] }
            }
            """;

        // Craft a minimal JWT with base64url payload containing the workspaceId; signature ignored by validator
        String headerJson = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"workspaceId\":\"" + workspaceId + "\"}";
        java.util.Base64.Encoder urlEnc = java.util.Base64.getUrlEncoder().withoutPadding();
        String jwt = urlEnc.encodeToString(headerJson.getBytes()) + "." + urlEnc.encodeToString(payloadJson.getBytes()) + ".x";

        // Mock request with JWT header instead of HMAC header
        when(request.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(request.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);
        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(jakarta.servlet.ReadListener readListener) { }
        };
        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));

        // Wire add-on and handler
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules").description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        assertEquals(200, response.getStatusCode());
        JsonNode out = mapper.readTree(response.getBody());
        assertTrue(out.has("status"));
    }
}
