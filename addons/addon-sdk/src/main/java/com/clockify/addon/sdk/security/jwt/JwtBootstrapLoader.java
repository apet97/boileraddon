package com.clockify.addon.sdk.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig.JwtKeySource;

/**
 * Utility that resolves JWT verification inputs from environment variables shared across add-ons.
 */
public final class JwtBootstrapLoader {
    private static final Logger logger = LoggerFactory.getLogger(JwtBootstrapLoader.class);

    private JwtBootstrapLoader() {}

    public static Optional<JwtBootstrapConfig> fromEnvironment(Map<String, String> env) {
        String pem = firstNonBlank(
                value(env, "CLOCKIFY_JWT_PUBLIC_KEY"),
                value(env, "CLOCKIFY_JWT_PUBLIC_KEY_PEM"));
        String keyMap = trimToNull(value(env, "CLOCKIFY_JWT_PUBLIC_KEY_MAP"));
        String jwksUri = trimToNull(value(env, "CLOCKIFY_JWT_JWKS_URI"));
        if (pem == null && keyMap == null && jwksUri == null) {
            return Optional.empty();
        }
        JwtKeySource source;
        if (jwksUri != null) {
            source = JwtKeySource.JWKS_URI;
            if (keyMap != null || pem != null) {
                logger.warn("CLOCKIFY_JWT_JWKS_URI is set; ignoring CLOCKIFY_JWT_PUBLIC_KEY_MAP and CLOCKIFY_JWT_PUBLIC_KEY(_PEM).");
            }
            keyMap = null;
            pem = null;
        } else if (keyMap != null) {
            source = JwtKeySource.KEY_MAP;
            if (pem != null) {
                logger.warn("CLOCKIFY_JWT_PUBLIC_KEY_MAP is set; ignoring CLOCKIFY_JWT_PUBLIC_KEY(_PEM).");
            }
            jwksUri = null;
            pem = null;
        } else {
            source = JwtKeySource.PUBLIC_KEY;
            jwksUri = null;
            keyMap = null;
        }
        String defaultKid = trimToNull(value(env, "CLOCKIFY_JWT_DEFAULT_KID"));
        String expectedIssuer = trimToNull(firstNonBlank(
                value(env, "CLOCKIFY_JWT_EXPECTED_ISS"),
                value(env, "CLOCKIFY_JWT_EXPECT_ISS")));
        String expectedAudience = trimToNull(value(env, "CLOCKIFY_JWT_EXPECT_AUD"));
        long leewaySeconds = parseLong(value(env, "CLOCKIFY_JWT_LEEWAY_SECONDS"), 60L);
        return Optional.of(new JwtBootstrapConfig(
                Optional.ofNullable(pem),
                Optional.ofNullable(keyMap),
                Optional.ofNullable(jwksUri),
                Optional.ofNullable(defaultKid),
                Optional.ofNullable(expectedIssuer),
                Optional.ofNullable(expectedAudience),
                leewaySeconds,
                source
        ));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String primary, String fallback) {
        String first = trimToNull(primary);
        if (first != null) {
            return first;
        }
        return trimToNull(fallback);
    }

    private static String value(Map<String, String> env, String key) {
        String fromEnv = env.get(key);
        if (fromEnv != null) {
            return fromEnv;
        }
        return System.getProperty(key);
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
}
