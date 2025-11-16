package com.example.rules.api;

import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.config.RulesConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DebugConfigControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AfterEach
    void clearProps() {
        System.clearProperty("RULES_APPLY_CHANGES");
        System.clearProperty("ADDON_SKIP_SIGNATURE_VERIFY");
        System.clearProperty("ENV");
    }

    @Test
    void handleReturnsSnapshotWithoutSecrets() throws Exception {
        System.setProperty("ENV", "dev");
        System.setProperty("RULES_APPLY_CHANGES", "true");

        RulesConfiguration config = new RulesConfiguration(
                "rules",
                "http://localhost:8080/rules",
                8080,
                "https://api.clockify.me/api",
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                false,
                90_000L,
                "dev",
                Optional.empty(),
                Optional.empty()
        );
        DebugConfigController controller = new DebugConfigController(
                config,
                () -> "in_memory",
                "memory"
        );

        HttpResponse response = controller.handle(null);
        assertEquals(200, response.getStatusCode());
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
        assertEquals("dev", root.get("environment").asText());
        assertEquals("in_memory", root.get("idempotencyBackend").asText());
        assertEquals("memory", root.get("tokenStore").asText());
        assertEquals("disabled", root.get("jwtMode").asText());
        JsonNode runtime = root.get("runtimeFlags");
        assertEquals(true, runtime.get("applyChanges").asBoolean());
        assertEquals(false, runtime.get("skipSignatureVerify").asBoolean());
        assertFalse(response.getBody().contains("jdbc"), "Response should not leak database URLs");
    }
}
