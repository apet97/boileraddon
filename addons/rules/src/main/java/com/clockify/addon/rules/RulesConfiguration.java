package com.clockify.addon.rules;

import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record RulesConfiguration(
        String addonKey,
        String baseUrl,
        int port,
        String environment,
        Optional<JwtBootstrapConfig> jwtBootstrap,
        boolean applyChanges
) {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/rules";
    private static final int DEFAULT_PORT = 8080;

    public static RulesConfiguration fromEnvironment() {
        Map<String, String> env = mergedEnvironment();
        String baseUrl = ConfigValidator.validateUrl(
                env.get("ADDON_BASE_URL"),
                DEFAULT_BASE_URL,
                "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
                env.get("ADDON_PORT"),
                DEFAULT_PORT,
                "ADDON_PORT"
        );
        String addonKey = Optional.ofNullable(env.get("ADDON_KEY"))
                .filter(value -> !value.isBlank())
                .orElse("rules");
        String environment = Optional.ofNullable(env.get("ENV"))
                .filter(value -> !value.isBlank())
                .orElse("dev");
        Optional<JwtBootstrapConfig> jwtBootstrap = JwtBootstrapLoader.fromEnvironment(env);
        boolean applyChanges = Optional.ofNullable(env.get("RULES_APPLY_CHANGES"))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(Boolean::parseBoolean)
                .orElse(false);
        return new RulesConfiguration(addonKey, baseUrl, port, environment, jwtBootstrap, applyChanges);
    }

    public boolean isDev() {
        return "dev".equalsIgnoreCase(environment);
    }

    private static Map<String, String> mergedEnvironment() {
        Map<String, String> merged = new HashMap<>(EnvConfig.asMap());
        System.getenv().forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.put(key, value);
            }
        });
        return merged;
    }
}
