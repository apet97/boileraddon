package com.clockify.addon.sdk.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentInspectorTest {

    @AfterEach
    void reset() {
        System.clearProperty("ENV");
        System.clearProperty("TEST_FLAG");
    }

    @Test
    void defaultsToProdWhenEnvUnset() {
        System.clearProperty("ENV");
        assertEquals("prod", EnvironmentInspector.environmentLabel());
        assertFalse(EnvironmentInspector.isDevEnvironment());
    }

    @Test
    void detectsDevEnvironmentFromProperty() {
        System.setProperty("ENV", "Development");
        assertEquals("Development", EnvironmentInspector.environmentLabel());
        assertTrue(EnvironmentInspector.isDevEnvironment());
    }

    @Test
    void booleanFlagPrefersSystemProperty() {
        System.setProperty("TEST_FLAG", "true");
        assertTrue(EnvironmentInspector.booleanFlag("TEST_FLAG"));
        System.setProperty("TEST_FLAG", "false");
        assertFalse(EnvironmentInspector.booleanFlag("TEST_FLAG"));
    }
}
