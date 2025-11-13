package com.example.rules.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
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
    private static final Set<String> DEV_ENVIRONMENTS = Set.of("dev", "development", "local");

    private RuntimeFlags() {
    }

    public static boolean isDevEnvironment() {
        return DEV_ENVIRONMENTS.contains(environmentLabel());
    }

    public static boolean applyChangesEnabled() {
        return boolEnvOrProp("RULES_APPLY_CHANGES");
    }

    public static boolean skipSignatureVerification() {
        return devOnlyToggleEnabled("ADDON_SKIP_SIGNATURE_VERIFY", signatureWarned);
    }

    public static String environmentLabel() {
        return resolveEnvironment().toLowerCase(Locale.ROOT);
    }

    private static boolean boolEnvOrProp(String key) {
        String sysValue = System.getProperty(key);
        if (sysValue != null) {
            return Boolean.parseBoolean(sysValue);
        }
        String envValue = System.getenv().getOrDefault(key, "false");
        return Boolean.parseBoolean(envValue);
    }

    private static String resolveEnvironment() {
        String prop = System.getProperty("ENV");
        String raw = (prop != null && !prop.isBlank())
                ? prop
                : System.getenv().getOrDefault("ENV", "prod");
        return raw.trim().isEmpty() ? "prod" : raw.trim();
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
