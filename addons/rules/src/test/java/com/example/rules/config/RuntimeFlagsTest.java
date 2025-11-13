package com.example.rules.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeFlagsTest {

    @AfterEach
    void clearProps() {
        System.clearProperty("RULES_APPLY_CHANGES");
        System.clearProperty("ADDON_SKIP_SIGNATURE_VERIFY");
    }

    @Test
    void applyChangesEnabled_readsSystemProperty() {
        System.setProperty("RULES_APPLY_CHANGES", "true");
        assertTrue(RuntimeFlags.applyChangesEnabled());
        System.setProperty("RULES_APPLY_CHANGES", "false");
        assertFalse(RuntimeFlags.applyChangesEnabled());
    }

    @Test
    void skipSignatureVerification_requiresDevEnv_soFalseInProd() {
        System.setProperty("ADDON_SKIP_SIGNATURE_VERIFY", "true");
        // ENV is read from System.getenv (prod by default in test env), so this should be false
        assertFalse(RuntimeFlags.skipSignatureVerification());
        // Second call still false; once-per-process warning path covered implicitly
        assertFalse(RuntimeFlags.skipSignatureVerification());
    }

    @Test
    void environmentLabel_defaultsToProd() {
        assertEquals("prod", RuntimeFlags.environmentLabel());
    }
}

