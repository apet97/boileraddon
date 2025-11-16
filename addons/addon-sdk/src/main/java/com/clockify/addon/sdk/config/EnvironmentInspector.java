package com.clockify.addon.sdk.config;

import java.util.Locale;
import java.util.Set;

/**
 * Central helper for environment-aware feature flags. Reads {@code ENV} from
 * system properties (preferred) or process environment variables, defaulting to {@code prod}.
 * Provides shared helpers for dev-only toggles (signature bypass, JWT acceptance, etc.).
 */
public final class EnvironmentInspector {
    private static final Set<String> DEV_ENVIRONMENTS = Set.of("dev", "development", "local");

    private EnvironmentInspector() {
    }

    /**
     * Returns the normalized environment label derived from {@code ENV} system property/env.
     * Defaults to {@code prod} when unset.
     */
    public static String environmentLabel() {
        String prop = System.getProperty("ENV");
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String env = System.getenv("ENV");
        if (env == null || env.isBlank()) {
            return "prod";
        }
        return env.trim();
    }

    /**
     * @return {@code true} if {@link #environmentLabel()} resolves to a dev-only value
     * ({@code dev}, {@code development}, or {@code local}, case-insensitive).
     */
    public static boolean isDevEnvironment() {
        String raw = environmentLabel();
        return DEV_ENVIRONMENTS.contains(raw.toLowerCase(Locale.ROOT));
    }

    /**
     * Reads a boolean flag from system properties first, then environment variables.
     * Absent values default to {@code false}.
     */
    public static boolean booleanFlag(String key) {
        String prop = System.getProperty(key);
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        String env = System.getenv(key);
        if (env == null) {
            return false;
        }
        return Boolean.parseBoolean(env);
    }
}
