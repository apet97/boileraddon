package com.clockify.addon.sdk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Backward-compatible configuration validator kept under the historic
 * package com.clockify.addon.sdk.config for tests and examples.
 *
 * New code should prefer com.clockify.addon.sdk.ConfigValidator (static helpers).
 */
public class ConfigValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    private final List<String> errors = new ArrayList<>();

    public String validateRequired(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(String.format("Required environment variable '%s' is missing or empty.", key));
            return null;
        }
        return value.trim();
    }

    public int validatePort(String key, String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            logger.info("Port '{}' not specified, using default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                errors.add(String.format("%s must be 1-65535, got: %d", key, port));
                return defaultValue;
            }
            return port;
        } catch (NumberFormatException e) {
            errors.add(String.format("%s must be an integer port, got: '%s'", key, value));
            return defaultValue;
        }
    }

    public String validateUrl(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(String.format("Required URL '%s' missing", key));
            return null;
        }
        String trimmed = value.trim();
        try {
            new URL(trimmed);
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        } catch (MalformedURLException e) {
            errors.add(String.format("%s must be a valid URL, got '%s'", key, value));
            return null;
        }
    }

    public String validateWebhookSecret(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(String.format("Required webhook secret '%s' missing", key));
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 32) {
            errors.add(String.format("Webhook secret '%s' too weak (<32 chars)", key));
            return null;
        }
        return trimmed;
    }

    public boolean validateBoolean(String key, String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        String lower = value.trim().toLowerCase();
        if (lower.equals("true") || lower.equals("1") || lower.equals("yes")) return true;
        if (lower.equals("false") || lower.equals("0") || lower.equals("no")) return false;
        errors.add(String.format("%s must be boolean (true/false/1/0/yes/no), got '%s'", key, value));
        return defaultValue;
    }

    public void throwIfInvalid() throws ConfigValidationException {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Configuration errors:\n");
            for (String e : errors) sb.append(" - ").append(e).append('\n');
            throw new ConfigValidationException(sb.toString(), new ArrayList<>(errors));
        }
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public List<String> getErrors() { return new ArrayList<>(errors); }

    public static AddonConfig validateAddonConfig(Map<String, String> env) throws ConfigValidationException {
        ConfigValidator v = new ConfigValidator();
        String baseUrl = v.validateUrl("ADDON_BASE_URL", env.get("ADDON_BASE_URL"));
        String webhookSecret = v.validateWebhookSecret("ADDON_WEBHOOK_SECRET", env.get("ADDON_WEBHOOK_SECRET"));
        int port = v.validatePort("ADDON_PORT", env.get("ADDON_PORT"), 8080);
        boolean debugMode = v.validateBoolean("DEBUG", env.get("DEBUG"), false);
        v.throwIfInvalid();
        return new AddonConfig(baseUrl, webhookSecret, port, debugMode);
    }

    public static class AddonConfig {
        private final String baseUrl;
        private final String webhookSecret;
        private final int port;
        private final boolean debugMode;

        public AddonConfig(String baseUrl, String webhookSecret, int port, boolean debugMode) {
            this.baseUrl = baseUrl;
            this.webhookSecret = webhookSecret;
            this.port = port;
            this.debugMode = debugMode;
        }
        public String getBaseUrl() { return baseUrl; }
        public String getWebhookSecret() { return webhookSecret; }
        public int getPort() { return port; }
        public boolean isDebugMode() { return debugMode; }
    }

    public static class ConfigValidationException extends RuntimeException {
        private final List<String> errors;
        public ConfigValidationException(String msg, List<String> errors) { super(msg); this.errors = errors; }
        public List<String> getErrors() { return errors; }
    }
}
