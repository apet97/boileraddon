package com.clockify.addon.sdk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates configuration parameters with helpful error messages.
 * Provides early validation to fail fast with clear diagnostics.
 */
public class ConfigValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    private final List<String> errors = new ArrayList<>();

    /**
     * Validates a required environment variable exists and is not empty.
     *
     * @param key The environment variable name
     * @param value The environment variable value
     * @return The validated value
     */
    public String validateRequired(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            String error = String.format("Required environment variable '%s' is missing or empty. " +
                    "Please set it in your .env file or environment.", key);
            errors.add(error);
            return null;
        }
        return value.trim();
    }

    /**
     * Validates a port number is valid (1-65535).
     *
     * @param key The environment variable name
     * @param value The port value as string
     * @param defaultValue The default port if value is null
     * @return The validated port number
     */
    public int validatePort(String key, String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            logger.info("Port '{}' not specified, using default: {}", key, defaultValue);
            return defaultValue;
        }

        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                errors.add(String.format("Environment variable '%s' must be a valid port (1-65535), got: %d",
                        key, port));
                return defaultValue;
            }
            return port;
        } catch (NumberFormatException e) {
            errors.add(String.format("Environment variable '%s' must be a valid integer port number, got: '%s'",
                    key, value));
            return defaultValue;
        }
    }

    /**
     * Validates a URL is well-formed.
     *
     * @param key The environment variable name
     * @param value The URL value
     * @return The validated URL string with trailing slash removed
     */
    public String validateUrl(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(String.format("Required URL environment variable '%s' is missing or empty", key));
            return null;
        }

        String trimmed = value.trim();
        try {
            new URL(trimmed);
            // Remove trailing slash for consistency
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        } catch (MalformedURLException e) {
            errors.add(String.format("Environment variable '%s' must be a valid URL, got: '%s'. Error: %s",
                    key, value, e.getMessage()));
            return null;
        }
    }

    /**
     * Validates a webhook secret is strong enough.
     *
     * @param key The environment variable name
     * @param value The secret value
     * @return The validated secret
     */
    public String validateWebhookSecret(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(String.format("Required webhook secret '%s' is missing", key));
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() < 32) {
            errors.add(String.format("Webhook secret '%s' is too weak (< 32 characters). " +
                    "Generate a strong secret: openssl rand -hex 32", key));
            return null;
        }

        return trimmed;
    }

    /**
     * Validates a boolean environment variable.
     *
     * @param key The environment variable name
     * @param value The value as string
     * @param defaultValue The default value if not specified
     * @return The validated boolean
     */
    public boolean validateBoolean(String key, String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        String lower = value.trim().toLowerCase();
        if (lower.equals("true") || lower.equals("1") || lower.equals("yes")) {
            return true;
        } else if (lower.equals("false") || lower.equals("0") || lower.equals("no")) {
            return false;
        } else {
            errors.add(String.format("Environment variable '%s' must be true/false, got: '%s'", key, value));
            return defaultValue;
        }
    }

    /**
     * Checks if validation has errors.
     *
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets all validation errors.
     *
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Throws a ConfigValidationException if there are any errors.
     *
     * @throws ConfigValidationException if validation failed
     */
    public void throwIfInvalid() throws ConfigValidationException {
        if (hasErrors()) {
            StringBuilder message = new StringBuilder("Configuration validation failed:\n");
            for (int i = 0; i < errors.size(); i++) {
                message.append(String.format("  %d. %s\n", i + 1, errors.get(i)));
            }
            throw new ConfigValidationException(message.toString(), errors);
        }
    }

    /**
     * Validates all common addon configuration parameters.
     *
     * @param env The environment variables map
     * @return A validated AddonConfig object
     * @throws ConfigValidationException if validation fails
     */
    public static AddonConfig validateAddonConfig(Map<String, String> env) throws ConfigValidationException {
        ConfigValidator validator = new ConfigValidator();

        String baseUrl = validator.validateUrl("ADDON_BASE_URL", env.get("ADDON_BASE_URL"));
        String webhookSecret = validator.validateWebhookSecret("ADDON_WEBHOOK_SECRET", env.get("ADDON_WEBHOOK_SECRET"));
        int port = validator.validatePort("ADDON_PORT", env.get("ADDON_PORT"), 8080);
        boolean debugMode = validator.validateBoolean("DEBUG", env.get("DEBUG"), false);

        validator.throwIfInvalid();

        return new AddonConfig(baseUrl, webhookSecret, port, debugMode);
    }

    /**
     * Simple configuration holder class.
     */
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

    /**
     * Exception thrown when configuration validation fails.
     */
    public static class ConfigValidationException extends Exception {
        private final List<String> errors;

        public ConfigValidationException(String message, List<String> errors) {
            super(message);
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
