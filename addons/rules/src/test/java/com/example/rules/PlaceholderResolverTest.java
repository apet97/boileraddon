package com.example.rules;

import com.example.rules.engine.PlaceholderResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlaceholderResolver
 */
public class PlaceholderResolverTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSimplePlaceholderResolution() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", "123");
        payload.put("description", "Test entry");
        payload.put("workspaceId", "ws-456");

        String template = "Entry {{id}} in workspace {{workspaceId}}: {{description}}";
        String result = PlaceholderResolver.resolve(template, payload);

        assertEquals("Entry 123 in workspace ws-456: Test entry", result);
    }

    @Test
    public void testNestedPlaceholderResolution() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode project = mapper.createObjectNode();
        project.put("id", "proj-1");
        project.put("name", "Test Project");
        payload.set("project", project);

        String template = "Project: {{project.name}} (ID: {{project.id}})";
        String result = PlaceholderResolver.resolve(template, payload);

        assertEquals("Project: Test Project (ID: proj-1)", result);
    }

    @Test
    public void testMissingPlaceholderReturnsEmpty() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", "123");

        String template = "Entry {{id}}: {{nonexistent}}";
        String result = PlaceholderResolver.resolve(template, payload);

        assertEquals("Entry 123: ", result);
    }

    @Test
    public void testNoPlaceholdersReturnsOriginal() {
        ObjectNode payload = mapper.createObjectNode();

        String template = "This has no placeholders";
        String result = PlaceholderResolver.resolve(template, payload);

        assertEquals("This has no placeholders", result);
    }

    @Test
    public void testResolveForPathEncodesSegments() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("workspaceId", "ws 123");
        payload.put("project", mapper.createObjectNode().put("id", "proj/42"));

        String template = "/workspaces/{{workspaceId}}/projects/{{project.id}}";
        String resolved = PlaceholderResolver.resolveForPath(template, payload);

        assertEquals("/workspaces/ws+123/projects/proj%2F42", resolved);
    }

    @Test
    public void testResolveInJsonHandlesArraysAndBooleans() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("flag", true);
        payload.put("firstValue", 1);
        payload.put("secondValue", 2);

        ObjectNode template = mapper.createObjectNode();
        template.put("enabled", "{{flag}}");
        template.putArray("ids").add("{{firstValue}}").add("{{secondValue}}");

        ObjectNode resolved = (ObjectNode) PlaceholderResolver.resolveInJson(template, payload);

        assertEquals("true", resolved.get("enabled").asText());
        assertEquals("1", resolved.get("ids").get(0).asText());
        assertEquals("2", resolved.get("ids").get(1).asText());
    }
}
