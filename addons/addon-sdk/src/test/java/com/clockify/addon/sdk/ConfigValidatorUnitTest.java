package com.clockify.addon.sdk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidatorUnitTest {
    private static final String VAR_NAME = "ADDON_BASE_URL";
    private static final String DEFAULT_URL = "http://localhost:8080/app";

    @AfterEach
    void tearDown() {
        System.clearProperty("env.ENV");
    }

    @Test
    void acceptsHttpWhenNotProd() {
        ConfigValidator.validateUrl("http://localhost:8080/app", DEFAULT_URL, VAR_NAME);
    }

    @Test
    void rejectsHttpWhenProd() {
        System.setProperty("env.ENV", "prod");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                ConfigValidator.validateUrl("http://localhost:8080/app", DEFAULT_URL, VAR_NAME)
        );
        assertEquals("ADDON_BASE_URL must use HTTPS when ENV=prod for secure deployments", error.getMessage());
    }

    @Test
    void acceptsHttpsWhenProd() {
        System.setProperty("env.ENV", "prod");
        String url = ConfigValidator.validateUrl("https://secure.example.com", DEFAULT_URL, VAR_NAME);
        assertEquals("https://secure.example.com", url);
    }
}
