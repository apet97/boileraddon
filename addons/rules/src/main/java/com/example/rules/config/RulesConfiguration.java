package com.example.rules.config;

import com.clockify.addon.sdk.ConfigValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized, immutable view of all environment-driven configuration for the Rules add-on.
 * Ensures we validate base URL/port consistently and expose typed accessors for downstream components.
 */
public record RulesConfiguration(
        String addonKey,
        String baseUrl,
        int port,
        String clockifyApiBaseUrl,
        Optional<DatabaseSettings> rulesDatabase,
        Optional<DatabaseSettings> sharedDatabase,
        boolean persistentTokenStoreEnabled,
        Optional<RateLimitConfig> rateLimit,
        Optional<CorsConfig> cors,
        boolean requestLoggingEnabled,
        long webhookDeduplicationTtlMillis,
        String environment
) {
    private static final Logger logger = LoggerFactory.getLogger(RulesConfiguration.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/rules";
    private static final String DEFAULT_API_BASE = "https://api.clockify.me/api";
    private static final long DEFAULT_DEDUP_SECONDS = 600L;

    public static RulesConfiguration fromEnvironment() {
        Map<String, String> env = System.getenv();

        String baseUrl = ConfigValidator.validateUrl(
                env.get("ADDON_BASE_URL"),
                DEFAULT_BASE_URL,
                "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
                env.get("ADDON_PORT"),
                8080,
                "ADDON_PORT"
        );
        String clockifyApiBaseUrl = ConfigValidator.validateUrl(
                env.get("CLOCKIFY_API_BASE_URL"),
                DEFAULT_API_BASE,
                "CLOCKIFY_API_BASE_URL"
        );

        Optional<DatabaseSettings> rulesDb = databaseSettings(
                env.get("RULES_DB_URL"),
                coalesce(env.get("RULES_DB_USERNAME"), env.get("RULES_DB_USER")),
                env.get("RULES_DB_PASSWORD")
        );
        Optional<DatabaseSettings> sharedDb = databaseSettings(
                env.get("DB_URL"),
                coalesce(env.get("DB_USER"), env.get("DB_USERNAME")),
                env.get("DB_PASSWORD")
        );

        Optional<RateLimitConfig> rateLimit = parseRateLimit(env.get("ADDON_RATE_LIMIT"), env.get("ADDON_LIMIT_BY"));
        Optional<CorsConfig> cors = parseCors(
                env.get("ADDON_CORS_ORIGINS"),
                env.getOrDefault("ADDON_CORS_ALLOW_CREDENTIALS", "false")
        );
        boolean requestLogging = isTruthy(env.get("ADDON_REQUEST_LOGGING"));

        long dedupSeconds = parseLong(env.get("RULES_WEBHOOK_DEDUP_SECONDS"), DEFAULT_DEDUP_SECONDS);
        long dedupMillis = Math.max(60L, dedupSeconds) * 1000L;

        String envLabel = normalizeEnv(env.get("ENV"));
        boolean persistentTokenStore = resolvePersistenceFlag(env.get("ENABLE_DB_TOKEN_STORE"), envLabel);

        return new RulesConfiguration(
                "rules",
                baseUrl,
                port,
                clockifyApiBaseUrl,
                rulesDb,
                sharedDb,
                persistentTokenStore,
                rateLimit,
                cors,
                requestLogging,
                dedupMillis,
                envLabel
        );
    }

    public record DatabaseSettings(String url, String username, String password) {}
    public record RateLimitConfig(double permitsPerSecond, String limitBy) {}
    public record CorsConfig(String originsCsv, boolean allowCredentials) {}

    private static Optional<DatabaseSettings> databaseSettings(String url, String username, String password) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new DatabaseSettings(url, username, password));
    }

    private static Optional<RateLimitConfig> parseRateLimit(String rawPermits, String rawLimitBy) {
        if (rawPermits == null || rawPermits.isBlank()) {
            return Optional.empty();
        }
        try {
            double permits = Double.parseDouble(rawPermits.trim());
            if (permits <= 0) {
                logger.warn("ADDON_RATE_LIMIT must be positive; ignoring value {}", rawPermits);
                return Optional.empty();
            }
            String limitBy = (rawLimitBy == null || rawLimitBy.isBlank()) ? "ip" : rawLimitBy.trim();
            return Optional.of(new RateLimitConfig(permits, limitBy));
        } catch (NumberFormatException nfe) {
            logger.warn("Invalid ADDON_RATE_LIMIT '{}'. Expected numeric value.", rawPermits);
            return Optional.empty();
        }
    }

    private static Optional<CorsConfig> parseCors(String originsCsv, String allowCredentialsRaw) {
        if (originsCsv == null || originsCsv.isBlank()) {
            return Optional.empty();
        }
        boolean allowCredentials = isTruthy(allowCredentialsRaw);
        return Optional.of(new CorsConfig(originsCsv, allowCredentials));
    }

    private static long parseLong(String raw, long defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException nfe) {
            logger.warn("Invalid numeric value '{}'; falling back to {}", raw, defaultValue);
            return defaultValue;
        }
    }

    private static boolean resolvePersistenceFlag(String explicitFlag, String envLabel) {
        if (explicitFlag != null && !explicitFlag.isBlank()) {
            return Boolean.parseBoolean(explicitFlag.trim());
        }
        return "prod".equalsIgnoreCase(envLabel);
    }

    private static boolean isTruthy(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) ||
                "true".equals(normalized) ||
                "yes".equals(normalized) ||
                "on".equals(normalized);
    }

    private static String coalesce(String primary, String secondary) {
        return (primary != null && !primary.isBlank()) ? primary : secondary;
    }

    private static String normalizeEnv(String rawEnv) {
        return rawEnv == null ? "prod" : rawEnv.trim().toLowerCase(Locale.ROOT);
    }
}
