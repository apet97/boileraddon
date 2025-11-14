package com.example.overtime;

import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapLoader;

import java.util.Map;
import java.util.Optional;

public record OvertimeConfiguration(
        String addonKey,
        String baseUrl,
        int port,
        String environment,
        Optional<JwtBootstrapConfig> jwtBootstrap
) {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/overtime";
    private static final int DEFAULT_PORT = 8080;

    public static OvertimeConfiguration fromEnvironment() {
        Map<String, String> env = System.getenv();
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
                .orElse("overtime");
        String environment = Optional.ofNullable(env.get("ENV"))
                .filter(value -> !value.isBlank())
                .orElse("dev");
        Optional<JwtBootstrapConfig> jwtBootstrap = JwtBootstrapLoader.fromEnvironment(env);
        return new OvertimeConfiguration(addonKey, baseUrl, port, environment, jwtBootstrap);
    }

    public boolean isDev() {
        return "dev".equalsIgnoreCase(environment);
    }
}
