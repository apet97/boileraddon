package com.example.rules.api;

import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.config.RulesConfiguration;
import com.example.rules.config.RuntimeFlags;
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
        System.setProperty("ADDON_SKIP_SIGNATURE_VERIFY", "false");
        assertEquals("false", System.getProperty("ADDON_SKIP_SIGNATURE_VERIFY"));
        boolean before = RuntimeFlags.skipSignatureVerification();
        assertFalse(before, "Dev flag should be false when not requested");

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
        boolean after = RuntimeFlags.skipSignatureVerification();
        assertEquals(before, after, "Runtime flag mutated unexpectedly during controller invocation");
        assertEquals(200, response.getStatusCode());
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
        assertEquals("dev", root.get("environment").asText());
        assertEquals("in_memory", root.get("idempotencyBackend").asText());
        assertEquals("memory", root.get("tokenStore").asText());
        assertEquals("disabled", root.get("jwtMode").asText());
        JsonNode runtime = root.get("runtimeFlags");
        assertEquals(true, runtime.get("applyChanges").asBoolean());
        boolean skipFlag = runtime.get("skipSignatureVerify").asBoolean();
        assertFalse(skipFlag, "Skip flag should be false. Payload: " + response.getBody());
        assertFalse(response.getBody().contains("jdbc"), "Response should not leak database URLs");
    }
}
