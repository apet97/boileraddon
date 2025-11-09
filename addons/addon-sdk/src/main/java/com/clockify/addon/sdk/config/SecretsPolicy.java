package com.clockify.addon.sdk.config;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SECURITY: Enforces baseline rules for secrets and sensitive environment variables.
 * <p>
 * Applications should invoke {@link #enforce()} during startup to fail-fast when
 * required secrets are missing, blank, or using placeholder defaults.
 * </p>
 */
public final class SecretsPolicy {
    private static final Set<String> DISALLOWED_SECRETS = Set.of(
            "changeme",
            "secret",
            "default",
            "placeholder",
            "sample",
            "password",
            "123456",
            "123456789",
            "letmein"
    );

    private SecretsPolicy() {}

    /** Enforces the policy using {@link System#getenv()}. */
    public static void enforce() {
        enforce(System.getenv());
    }

    /** Enforces the policy using the provided environment map (used by tests). */
    public static void enforce(Map<String, String> env) {
        Objects.requireNonNull(env, "env");
        requireStrongSecret(env, "ADDON_WEBHOOK_SECRET");

        // Optional database credentials: if URL is provided, ensure username/password are not blank
        String dbUrl = env.get("DB_URL");
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            requirePresent(env, "DB_USERNAME", "DB_USERNAME is required when DB_URL is set");
            requirePresent(env, "DB_PASSWORD", "DB_PASSWORD is required when DB_URL is set");
        }
    }

    private static void requireStrongSecret(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(key + " is required and cannot be empty");
        }
        String trimmed = value.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.length() < 32) {
            throw new IllegalStateException(key + " must be at least 32 characters long");
        }
        if (DISALLOWED_SECRETS.contains(normalized)) {
            throw new IllegalStateException(key + " cannot use placeholder values such as '" + trimmed + "'");
        }
    }

    private static void requirePresent(Map<String, String> env, String key, String message) {
        String value = env.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
    }
}
