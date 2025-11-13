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
        TriggersCatalog.clearCache();
        JsonNode result = TriggersCatalog.triggersToJson();
        assertNotNull(result);
        assertTrue(result.has("triggers"));
        assertTrue(result.has("count"));
        assertTrue(result.get("count").asInt() > 0, "Expected packaged triggers catalog to contain entries");
    }

    @Test
    public void testEndpointsToJsonReturnsNonEmpty() {
        OpenAPISpecLoader.clearCache();
        JsonNode result = OpenAPISpecLoader.endpointsToJson();
        assertNotNull(result);
        assertTrue(result.has("tags"));
        assertTrue(result.has("count"));
        assertTrue(result.get("count").asInt() > 0, "Expected packaged actions catalog to contain endpoints");
    }
}
