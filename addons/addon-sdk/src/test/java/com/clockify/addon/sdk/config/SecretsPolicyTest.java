package com.clockify.addon.sdk.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretsPolicyTest {

    @Test
    void enforceFailsWhenSecretMissing() {
        Map<String, String> env = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> SecretsPolicy.enforce(env));
    }

    @Test
    void enforceFailsWhenSecretTooShortOrPlaceholder() {
        Map<String, String> env = new HashMap<>();
        env.put("ADDON_WEBHOOK_SECRET", "changeme");
        assertThrows(IllegalStateException.class, () -> SecretsPolicy.enforce(env));

        env.put("ADDON_WEBHOOK_SECRET", "short-secret-value");
        assertThrows(IllegalStateException.class, () -> SecretsPolicy.enforce(env));
    }

    @Test
    void enforceRequiresDbCredsWhenUrlProvided() {
        Map<String, String> env = new HashMap<>();
        env.put("ADDON_WEBHOOK_SECRET", "a".repeat(32));
        env.put("DB_URL", "jdbc:postgresql://localhost/test");
        assertThrows(IllegalStateException.class, () -> SecretsPolicy.enforce(env));

        env.put("DB_USERNAME", "clockify");
        env.put("DB_PASSWORD", "strong-password-value");
        assertDoesNotThrow(() -> SecretsPolicy.enforce(env));
    }

    @Test
    void enforcePassesWithValidSecret() {
        Map<String, String> env = new HashMap<>();
        env.put("ADDON_WEBHOOK_SECRET", "this_is_a_valid_secret_value_with_length");
        assertDoesNotThrow(() -> SecretsPolicy.enforce(env));
    }
}
