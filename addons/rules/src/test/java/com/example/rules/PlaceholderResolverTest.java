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
}
