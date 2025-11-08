package com.clockify.addon.sdk.config;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    @Test
    void testValidateRequired_Success() {
        ConfigValidator validator = new ConfigValidator();
        String result = validator.validateRequired("TEST_VAR", "value");
        assertEquals("value", result);
        assertFalse(validator.hasErrors());
    }

    @Test
    void testValidateRequired_Missing() {
        ConfigValidator validator = new ConfigValidator();
        String result = validator.validateRequired("TEST_VAR", null);
        assertNull(result);
        assertTrue(validator.hasErrors());
        assertTrue(validator.getErrors().get(0).contains("TEST_VAR"));
    }

    @Test
    void testValidatePort_Valid() {
        ConfigValidator validator = new ConfigValidator();
        int port = validator.validatePort("PORT", "8080", 3000);
        assertEquals(8080, port);
        assertFalse(validator.hasErrors());
    }

    @Test
    void testValidatePort_Invalid() {
        ConfigValidator validator = new ConfigValidator();
        int port = validator.validatePort("PORT", "70000", 3000);
        assertEquals(3000, port);
        assertTrue(validator.hasErrors());
    }

    @Test
    void testValidatePort_NotANumber() {
        ConfigValidator validator = new ConfigValidator();
        int port = validator.validatePort("PORT", "abc", 3000);
        assertEquals(3000, port);
        assertTrue(validator.hasErrors());
    }

    @Test
    void testValidateUrl_Valid() {
        ConfigValidator validator = new ConfigValidator();
        String url = validator.validateUrl("BASE_URL", "https://example.com/");
        assertEquals("https://example.com", url);
        assertFalse(validator.hasErrors());
    }

    @Test
    void testValidateUrl_Invalid() {
        ConfigValidator validator = new ConfigValidator();
        String url = validator.validateUrl("BASE_URL", "not-a-url");
        assertNull(url);
        assertTrue(validator.hasErrors());
    }

    @Test
    void testValidateWebhookSecret_Valid() {
        ConfigValidator validator = new ConfigValidator();
        String secret = validator.validateWebhookSecret("SECRET", "a".repeat(32));
        assertNotNull(secret);
        assertFalse(validator.hasErrors());
    }

    @Test
    void testValidateWebhookSecret_TooShort() {
        ConfigValidator validator = new ConfigValidator();
        String secret = validator.validateWebhookSecret("SECRET", "short");
        assertNull(secret);
        assertTrue(validator.hasErrors());
    }

    @Test
    void testValidateBoolean_True() {
        ConfigValidator validator = new ConfigValidator();
        assertTrue(validator.validateBoolean("DEBUG", "true", false));
        assertTrue(validator.validateBoolean("DEBUG", "1", false));
        assertTrue(validator.validateBoolean("DEBUG", "yes", false));
    }

    @Test
    void testValidateBoolean_False() {
        ConfigValidator validator = new ConfigValidator();
        assertFalse(validator.validateBoolean("DEBUG", "false", true));
        assertFalse(validator.validateBoolean("DEBUG", "0", true));
        assertFalse(validator.validateBoolean("DEBUG", "no", true));
    }

    @Test
    void testValidateAddonConfig_Success() throws ConfigValidator.ConfigValidationException {
        Map<String, String> env = new HashMap<>();
        env.put("ADDON_BASE_URL", "https://example.com");
        env.put("ADDON_WEBHOOK_SECRET", "a".repeat(32));
        env.put("ADDON_PORT", "8080");
        env.put("DEBUG", "true");

        ConfigValidator.AddonConfig config = ConfigValidator.validateAddonConfig(env);
        assertEquals("https://example.com", config.getBaseUrl());
        assertEquals(8080, config.getPort());
        assertTrue(config.isDebugMode());
    }

    @Test
    void testValidateAddonConfig_Failure() {
        Map<String, String> env = new HashMap<>();
        env.put("ADDON_BASE_URL", "not-a-url");

        assertThrows(ConfigValidator.ConfigValidationException.class, () -> {
            ConfigValidator.validateAddonConfig(env);
        });
    }

    @Test
    void testThrowIfInvalid() {
        ConfigValidator validator = new ConfigValidator();
        validator.validateRequired("TEST", null);

        ConfigValidator.ConfigValidationException exception = assertThrows(
                ConfigValidator.ConfigValidationException.class,
                validator::throwIfInvalid
        );

        assertTrue(exception.getMessage().contains("Configuration validation failed"));
        assertFalse(exception.getErrors().isEmpty());
    }
}
