package com.example.autotagassistant;

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
    void handleReturnsSnapshotWithoutSecrets() throws Exception {
        AutoTagConfiguration config = new AutoTagConfiguration(
                "auto-tag-assistant",
                "http://localhost:8080/auto-tag-assistant",
                8080,
                "dev",
                Optional.empty()
        );
        DevConfigController controller = new DevConfigController(config, true);

        HttpResponse response = controller.handle(null);
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());

        assertEquals("dev", root.get("environment").asText());
        assertEquals("database", root.get("tokenStore").asText());
        assertEquals("disabled", root.get("jwtMode").asText());
        assertFalse(response.getBody().contains("jdbc"), "Response should not leak connection strings");
    }
}
