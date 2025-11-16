package com.example.rules.config;

import com.clockify.addon.sdk.config.EnvironmentInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralizes environment-driven feature flags (apply changes, signature skip, etc.).
 * Reads {@link System#getenv(String)} directly so toggles can flip without recreating
 * {@link com.example.rules.config.RulesConfiguration}. Dev-only toggles are ignored when ENV is not
 * "dev"/"development"/"local" so a mis-set environment variable cannot weaken production security.
 */
public final class RuntimeFlags {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeFlags.class);
    private static final AtomicBoolean signatureWarned = new AtomicBoolean(false);

    private RuntimeFlags() {
    }

    public static boolean isDevEnvironment() {
        return EnvironmentInspector.isDevEnvironment();
    }

    public static boolean applyChangesEnabled() {
        return boolEnvOrProp("RULES_APPLY_CHANGES");
    }

    public static boolean skipSignatureVerification() {
        return devOnlyToggleEnabled("ADDON_SKIP_SIGNATURE_VERIFY", signatureWarned);
    }

    public static String environmentLabel() {
        return EnvironmentInspector.environmentLabel().toLowerCase(Locale.ROOT);
    }

    private static boolean boolEnvOrProp(String key) {
        return EnvironmentInspector.booleanFlag(key);
    }

    private static boolean devOnlyToggleEnabled(String key, AtomicBoolean warnFlag) {
        boolean requested = boolEnvOrProp(key);
        if (!requested) {
            return false;
        }
        if (isDevEnvironment()) {
            return true;
        }
        if (warnFlag.compareAndSet(false, true)) {
            logger.warn("{} is ignored because ENV='{}'. Dev-only toggles never activate outside development.",
                    key, environmentLabel());
        }
        return false;
    }
}
