package com.clockify.addon.sdk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for sanitizing and validating URL paths to prevent security issues.
 * Handles path traversal, null bytes, double slashes, and invalid characters.
 */
public class PathSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(PathSanitizer.class);

    private PathSanitizer() {
        // Utility class
    }

    /**
     * Sanitizes a path to prevent security vulnerabilities.
     *
     * @param path The path to sanitize
     * @return Sanitized path starting with /
     * @throws IllegalArgumentException if path contains malicious patterns
     */
    public static String sanitize(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }

        String sanitized = path.trim();

        // Detect common null-byte representations
        boolean hasNullChar = sanitized.indexOf('\u0000') >= 0;
        boolean hasPercent00 = sanitized.toLowerCase().contains("%00"); // URL-encoded null byte
        boolean hasBackslashZero = false; // robust detection for "\\0" (backslash then zero)
        for (int i = 0; i + 1 < sanitized.length(); i++) {
            if (sanitized.charAt(i) == '\\' && sanitized.charAt(i + 1) == '0') {
                hasBackslashZero = true;
                break;
            }
        }

        // Check for null bytes (potential security issue) or common encodings
        if (hasNullChar || hasBackslashZero || hasPercent00) {
            logger.warn("Path contains null byte or encoding (nullChar?={}, \\0?={}, %00?={}): {}",
                    hasNullChar, hasBackslashZero, hasPercent00, path);
            throw new IllegalArgumentException("Path contains null bytes");
        }

        // Disallow backslash outright (only forward-slash path separators are permitted)
        if (sanitized.indexOf('\\') >= 0) {
            logger.warn("Path contains backslash character: {}", path);
            throw new IllegalArgumentException("Path contains invalid characters");
        }

        // Check for path traversal attempts
        if (sanitized.contains("..")) {
            logger.warn("Path contains directory traversal attempt: {}", path);
            throw new IllegalArgumentException("Path contains directory traversal patterns (..)");
        }

        // Remove duplicate slashes
        while (sanitized.contains("//")) {
            sanitized = sanitized.replace("//", "/");
        }

        // Ensure path starts with /
        if (!sanitized.startsWith("/")) {
            sanitized = "/" + sanitized;
        }

        // Remove trailing slash (except for root path)
        if (sanitized.length() > 1 && sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        // Validate characters (allow alphanumeric, -, _, /, ., and common URL chars)
        if (!sanitized.matches("^[/a-zA-Z0-9._~:?#\\[\\]@!$&'()*+,;=-]*$")) {
            logger.warn("Path contains invalid characters: {}", path);
            throw new IllegalArgumentException("Path contains invalid characters");
        }

        return sanitized;
    }

    /**
     * Normalizes a path without strict validation (for backward compatibility).
     * Use sanitize() for security-critical paths.
     *
     * @param path The path to normalize
     * @return Normalized path
     */
    public static String normalize(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }

        String normalized = path.trim();

        // Remove duplicate slashes
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }

        // Ensure path starts with /
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // Remove trailing slash (except for root path)
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * Validates a path for lifecycle endpoints.
     *
     * @param lifecycleType The lifecycle type
     * @param path The custom path (can be null)
     * @return Sanitized path
     */
    public static String sanitizeLifecyclePath(String lifecycleType, String path) {
        if (path == null || path.trim().isEmpty()) {
            // Generate safe default path
            String safeLcType = lifecycleType.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
            return "/lifecycle/" + safeLcType;
        }

        return sanitize(path);
    }

    /**
     * Validates a path for webhook endpoints.
     *
     * @param path The webhook path (can be null for default)
     * @return Sanitized path
     */
    public static String sanitizeWebhookPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/webhook";
        }

        return sanitize(path);
    }
}
