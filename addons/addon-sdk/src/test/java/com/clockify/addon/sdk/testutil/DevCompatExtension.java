package com.clockify.addon.sdk.testutil;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Enables deterministic test defaults for the SDK test suite and
 * restores the original values afterwards.
 */
public final class DevCompatExtension implements BeforeAllCallback, AfterAllCallback {

    private final Map<String, String> original = new HashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        setProp("ENV", "dev");
        setProp("ADDON_AUTH_COMPAT", "HMAC");
        setProp("ADDON_ACCEPT_JWT_SIGNATURE", "true");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        restore("ENV");
        restore("ADDON_AUTH_COMPAT");
        restore("ADDON_ACCEPT_JWT_SIGNATURE");
    }

    private void setProp(String key, String value) {
        original.put(key, System.getProperty(key));
        System.setProperty(key, value);
    }

    private void restore(String key) {
        String prev = original.get(key);
        if (prev == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, prev);
        }
    }
}

