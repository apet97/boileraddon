package com.example.templateaddon;

import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record TemplateAddonConfiguration(
        String addonKey,
        String baseUrl,
        int port,
        String environment,
        Optional<JwtBootstrapConfig> jwtBootstrap
) {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/_template-addon";
    private static final int DEFAULT_PORT = 8080;

    public static TemplateAddonConfiguration fromEnvironment() {
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
                .orElse("_template-addon");
        String environment = Optional.ofNullable(env.get("ENV"))
                .filter(value -> !value.isBlank())
                .orElse("dev");
        Optional<JwtBootstrapConfig> jwtBootstrap = JwtBootstrapLoader.fromEnvironment(env);
        return new TemplateAddonConfiguration(addonKey, baseUrl, port, environment, jwtBootstrap);
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
