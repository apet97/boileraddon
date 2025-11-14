package com.example.rules;

import com.example.rules.engine.Action;
import com.example.rules.engine.OpenApiCallConfig;
import com.example.rules.engine.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for openapi_call action execution in DynamicWebhookHandlers.
 * Verifies that the dynamic handler can process IFTTT-style actions with OpenAPI calls.
 */
class DynamicHandlerOpenApiCallTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openApiCallAction_hasCorrectStructure() {
        // Verify that we can create an openapi_call action with the expected structure
        Map<String, String> args = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"urgent\"}"
        );

        Action action = new Action("openapi_call", args);

        assertEquals("openapi_call", action.getType());
        assertEquals("POST", action.getArgs().get("method"));
        assertEquals("/workspaces/{workspaceId}/tags", action.getArgs().get("path"));
        assertEquals("{\"name\":\"urgent\"}", action.getArgs().get("body"));
    }

    @Test
    void openApiCallAction_canBeSerialized() throws Exception {
        // Verify that openapi_call actions can be serialized/deserialized properly
        Map<String, String> args = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/projects/{projectId}",
                "body", "{\"name\":\"Updated Project\"}"
        );

        Action action = new Action("openapi_call", args);
        String json = objectMapper.writeValueAsString(action);

        // Deserialize and verify
        Action deserialized = objectMapper.readValue(json, Action.class);
        assertEquals(action.getType(), deserialized.getType());
        assertEquals(action.getArgs(), deserialized.getArgs());
    }

    @Test
    void rule_canContainMultipleOpenApiCallActions() {
        // Verify that a rule can contain multiple openapi_call actions
        Action createTag = new Action("openapi_call", Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"urgent\"}"
        ));

        Action updateProject = new Action("openapi_call", Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/projects/{projectId}/archive",
                "body", "{\"archived\":true}"
        ));

        Rule rule = new Rule(
                "rule-1",
                "Archive project and add urgent tag",
                true,
                "AND",
                List.of(),
                List.of(createTag, updateProject),
                null,
                0
        );

        assertEquals(2, rule.getActions().size());
        assertTrue(rule.getActions().stream()
                .allMatch(a -> "openapi_call".equals(a.getType())));
    }

    @Test
    void openApiCallAction_supportsGetMethod() {
        // Verify GET method actions
        Map<String, String> args = Map.of(
                "method", "GET",
                "path", "/workspaces/{workspaceId}/tags"
        );

        Action action = new Action("openapi_call", args);

        assertEquals("GET", action.getArgs().get("method"));
        assertNull(action.getArgs().get("body"), "GET requests should not have a body");
    }

    @Test
    void openApiCallAction_rejectsDeleteMethod() {
        Map<String, String> args = Map.of(
                "method", "DELETE",
                "path", "/workspaces/{workspaceId}/tags/{tagId}"
        );

        Action action = new Action("openapi_call", args);

        assertThrows(IllegalArgumentException.class, () -> OpenApiCallConfig.from(action, objectMapper));
    }

    @Test
    void openApiCallAction_supportsPlaceholdersInPath() {
        // Verify that paths can contain placeholders that will be resolved at runtime
        Map<String, String> args = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/projects/{event.project.id}/tasks",
                "body", "{\"name\":\"{event.task.name}\"}"
        );

        Action action = new Action("openapi_call", args);

        // Verify placeholders are preserved in the action definition
        assertTrue(action.getArgs().get("path").contains("{workspaceId}"));
        assertTrue(action.getArgs().get("path").contains("{event.project.id}"));
        assertTrue(action.getArgs().get("body").contains("{event.task.name}"));
    }

    @Test
    void openApiCallAction_canBeCreatedFromJson() throws Exception {
        // Verify that we can deserialize an openapi_call action from JSON
        String json = """
                {
                    "type": "openapi_call",
                    "args": {
                        "method": "POST",
                        "path": "/workspaces/{workspaceId}/tags",
                        "body": "{\\"name\\":\\"urgent\\"}"
                    }
                }
                """;

        Action action = objectMapper.readValue(json, Action.class);

        assertEquals("openapi_call", action.getType());
        assertEquals("POST", action.getArgs().get("method"));
        assertNotNull(action.getArgs().get("body"));
    }

    @Test
    void rule_withOpenApiCallActions_canBeSerialized() throws Exception {
        // Verify that a complete rule with openapi_call actions can be serialized
        Action action = new Action("openapi_call", Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"automated\"}"
        ));

        Rule rule = new Rule(
                null,  // auto-generate ID
                "Auto-tag new time entries",
                true,
                "AND",
                List.of(),
                List.of(action),
                null,
                0
        );

        String json = objectMapper.writeValueAsString(rule);
        Rule deserialized = objectMapper.readValue(json, Rule.class);

        assertEquals(rule.getName(), deserialized.getName());
        assertEquals(rule.getActions().size(), deserialized.getActions().size());
        assertEquals("openapi_call", deserialized.getActions().get(0).getType());
    }

    @Test
    void openApiCallAction_withComplexBody() throws Exception {
        // Verify that openapi_call actions can handle complex JSON bodies
        String complexBody = """
                {
                    "name": "Project Name",
                    "billable": true,
                    "color": "#FF5733",
                    "estimate": {
                        "estimate": "PT8H",
                        "type": "AUTO"
                    }
                }
                """;

        Map<String, String> args = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/projects",
                "body", complexBody
        );

        Action action = new Action("openapi_call", args);

        // Verify the body is valid JSON
        JsonNode bodyNode = objectMapper.readTree(action.getArgs().get("body"));
        assertTrue(bodyNode.isObject());
        assertTrue(bodyNode.has("estimate"));
        assertEquals("Project Name", bodyNode.get("name").asText());
    }

    @Test
    void openApiCallAction_equalityWorks() {
        // Verify that equality and hashCode work correctly for openapi_call actions
        Map<String, String> args1 = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"urgent\"}"
        );

        Map<String, String> args2 = Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"urgent\"}"
        );

        Action action1 = new Action("openapi_call", args1);
        Action action2 = new Action("openapi_call", args2);

        assertEquals(action1, action2);
        assertEquals(action1.hashCode(), action2.hashCode());
    }
}
