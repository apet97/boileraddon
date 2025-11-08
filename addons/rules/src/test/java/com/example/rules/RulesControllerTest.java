package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RulesControllerTest {

    private RulesController controller;
    private RulesStore store;
    private ObjectMapper mapper;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        store = new RulesStore();
        controller = new RulesController(store);
        mapper = new ObjectMapper();
        request = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    void testListRules_empty() throws Exception {
        when(request.getParameter("workspaceId")).thenReturn("workspace-1");

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertTrue(json.isArray());
        assertEquals(0, json.size());
    }

    @Test
    void testListRules_withRules() throws Exception {
        Rule rule = createTestRule("rule-1", "Test Rule");
        store.save("workspace-1", rule);

        when(request.getParameter("workspaceId")).thenReturn("workspace-1");

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("Test Rule", json.get(0).get("name").asText());
    }

    @Test
    void testListRules_missingWorkspaceId() throws Exception {
        when(request.getParameter("workspaceId")).thenReturn(null);

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("workspaceId is required"));
    }

    @Test
    void testSaveRule_create() throws Exception {
        String ruleJson = """
            {
                "name": "Test Rule",
                "enabled": true,
                "combinator": "AND",
                "conditions": [
                    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"}
                ],
                "actions": [
                    {"type": "add_tag", "args": {"tag": "billable"}}
                ]
            }
            """;

        setupRequestWithBody(ruleJson);
        when(request.getParameter("workspaceId")).thenReturn("workspace-1");

        HttpResponse response = controller.saveRule().handle(request);

        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertEquals("Test Rule", json.get("name").asText());
        assertNotNull(json.get("id").asText());

        // Verify it was saved
        assertEquals(1, store.count("workspace-1"));
    }

    @Test
    void testSaveRule_invalidJson() throws Exception {
        String ruleJson = """
            {
                "name": "Missing conditions",
                "enabled": true,
                "combinator": "AND"
            }
            """;

        setupRequestWithBody(ruleJson);
        when(request.getParameter("workspaceId")).thenReturn("workspace-1");

        HttpResponse response = controller.saveRule().handle(request);

        assertEquals(400, response.statusCode());
    }

    @Test
    void testDeleteRule() throws Exception {
        Rule rule = createTestRule("rule-1", "Test Rule");
        store.save("workspace-1", rule);

        when(request.getParameter("workspaceId")).thenReturn("workspace-1");
        when(request.getPathInfo()).thenReturn("/api/rules/rule-1");

        HttpResponse response = controller.deleteRule().handle(request);

        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertTrue(json.get("deleted").asBoolean());

        // Verify it was deleted
        assertEquals(0, store.count("workspace-1"));
    }

    @Test
    void testDeleteRule_notFound() throws Exception {
        when(request.getParameter("workspaceId")).thenReturn("workspace-1");
        when(request.getPathInfo()).thenReturn("/api/rules/non-existent");

        HttpResponse response = controller.deleteRule().handle(request);

        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertFalse(json.get("deleted").asBoolean());
    }

    private void setupRequestWithBody(String json) throws Exception {
        JsonNode jsonNode = mapper.readTree(json);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);
        when(request.getAttribute("clockify.rawBody")).thenReturn(json);

        byte[] bytes = json.getBytes();
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

    private Rule createTestRule(String id, String name) {
        return new Rule(id, name, true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))));
    }
}
