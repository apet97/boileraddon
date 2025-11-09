package com.clockify.addon.sdk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Utility class for validating configuration values from environment variables.
 * <p>
 * Provides helpful error messages to aid in troubleshooting configuration issues.
 * All validation methods throw {@link IllegalArgumentException} with descriptive
 * messages when validation fails.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * String baseUrl = ConfigValidator.validateUrl(
 *     System.getenv("ADDON_BASE_URL"),
 *     "http://localhost:8080/addon",
 *     "ADDON_BASE_URL"
 * );
 * int port = ConfigValidator.validatePort(
 *     System.getenv("ADDON_PORT"),
 *     8080,
 *     "ADDON_PORT"
 * );
 * }</pre>
 *
 * @since 0.1.0
 */
public class ConfigValidator {

    /**
     * Validates and parses a port number from a string.
     *
     * @param portStr the port string to parse
     * @param defaultPort the default port to use if portStr is null or empty
     * @param varName the environment variable name for error messages
     * @return validated port number
     * @throws IllegalArgumentException if port is invalid
     */
    public static int validatePort(String portStr, int defaultPort, String varName) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return defaultPort;
        }

        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                    String.format("Invalid %s: %d. Port must be between 1 and 65535.", varName, port)
                );
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s: '%s'. Must be a valid integer between 1 and 65535.", varName, portStr),
                e
            );
        }
    }

    /**
     * Validates a base URL string.
     *
     * @param urlStr the URL string to validate
     * @param defaultUrl the default URL to use if urlStr is null or empty
     * @param varName the environment variable name for error messages
     * @return validated URL string
     * @throws IllegalArgumentException if URL is malformed
     */
    public static String validateUrl(String urlStr, String defaultUrl, String varName) {
        String candidate = (urlStr == null || urlStr.trim().isEmpty()) ? defaultUrl : urlStr.trim();
        String url = candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;

        try {
            new URL(url); // Validate URL format
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s: '%s'. Must be a valid URL (e.g., http://localhost:8080/addon)", varName, url),
                e
            );
        }

        if (requiresHttpsInProduction()) {
            if (!url.toLowerCase(Locale.ROOT).startsWith("https://")) {
                throw new IllegalArgumentException(
                    String.format("%s must use HTTPS when ENV=prod for secure deployments", varName)
                );
            }
        }

        return url;
    }

    /**
     * Gets an environment variable with a default value.
     *
     * @param varName the environment variable name
     * @param defaultValue the default value if not set
     * @return the environment variable value or default
     */
    public static String getEnv(String varName, String defaultValue) {
        String value = System.getenv(varName);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    private static boolean requiresHttpsInProduction() {
        String envValue = resolveProperty("ENV");
        return envValue != null && envValue.equalsIgnoreCase("prod");
    }

    private static String resolveProperty(String key) {
        String override = System.getProperty("env." + key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return System.getenv(key);
    }
}
