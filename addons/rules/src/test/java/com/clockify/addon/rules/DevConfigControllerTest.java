package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DevConfigControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void handleReportsBasicConfig() throws Exception {
        RulesConfiguration config = new RulesConfiguration(
                "rules",
                "http://localhost:8080/rules",
                8080,
                "dev",
                Optional.empty(),
                false
        );
        DevConfigController controller = new DevConfigController(config);

        HttpResponse response = controller.handle(null);
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());

        assertEquals("rules", root.get("addonKey").asText());
        assertEquals("dev", root.get("environment").asText());
        assertEquals("http://localhost:8080/rules", root.get("baseUrl").asText());
        assertEquals("memory", root.get("tokenStore").asText());
        assertFalse(root.get("applyChanges").asBoolean());
        assertEquals("disabled", root.get("jwtMode").asText());
        assertFalse(response.getBody().contains("jdbc"));
    }
}
