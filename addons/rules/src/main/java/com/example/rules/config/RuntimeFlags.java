package com.example.rules.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralizes environment-driven feature flags (apply changes, signature skip, etc.).
 * Reads {@link System#getenv(String)} directly so toggles can flip without recreating
 * {@link com.example.rules.config.RulesConfiguration}, but guards dangerous flags to dev environments only.
 */
public final class RuntimeFlags {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeFlags.class);
    private static final AtomicBoolean signatureWarned = new AtomicBoolean(false);

    private RuntimeFlags() {
    }

    public static boolean isDevEnvironment() {
        String env = System.getenv().getOrDefault("ENV", "prod");
        return "dev".equalsIgnoreCase(env) || "development".equalsIgnoreCase(env);
    }

    public static boolean applyChangesEnabled() {
        return boolEnvOrProp("RULES_APPLY_CHANGES");
    }

    public static boolean skipSignatureVerification() {
        boolean requested = boolEnvOrProp("ADDON_SKIP_SIGNATURE_VERIFY");
        if (!requested) {
            return false;
        }

        if (!isDevEnvironment()) {
            if (signatureWarned.compareAndSet(false, true)) {
                logger.warn("ADDON_SKIP_SIGNATURE_VERIFY is ignored because ENV!='dev'. Enable only for local development.");
            }
            return false;
        }
        return true;
    }

    public static String environmentLabel() {
        return System.getenv().getOrDefault("ENV", "prod").toLowerCase(Locale.ROOT);
    }

    private static boolean boolEnvOrProp(String key) {
        String envValue = System.getenv().getOrDefault(key, "false");
        if (Boolean.parseBoolean(envValue)) {
            return true;
        }
        return Boolean.parseBoolean(System.getProperty(key, "false"));
    }
}
