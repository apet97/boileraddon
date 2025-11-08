package com.example.rules;

import com.example.rules.spec.OpenAPISpecLoader;
import com.example.rules.spec.TriggersCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for catalog endpoints
 */
public class CatalogEndpointsTest {

    @Test
    public void testTriggersToJsonReturnsNonEmpty() {
        JsonNode result = TriggersCatalog.triggersToJson();
        assertNotNull(result);
        assertTrue(result.has("triggers"));
        assertTrue(result.has("count"));
    }

    @Test
    public void testEndpointsToJsonReturnsNonEmpty() {
        JsonNode result = OpenAPISpecLoader.endpointsToJson();
        assertNotNull(result);
        assertTrue(result.has("tags"));
        assertTrue(result.has("count"));
    }
}
