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

        // Pre-trim checks: examine the raw input to avoid trim() hiding trailing control chars
        String raw = path;
        boolean rawHasNullChar = raw.indexOf('\u0000') >= 0;
        boolean rawHasPercent00 = raw.toLowerCase().contains("%00");
        boolean rawHasBackslashZero = false; // "\\0"
        boolean rawHasUnicodeLiteralNull = raw.toLowerCase().contains("\\u0000"); // literal text "\u0000"
        for (int i = 0; i + 1 < raw.length(); i++) {
            if (raw.charAt(i) == '\\' && raw.charAt(i + 1) == '0') {
                rawHasBackslashZero = true;
                break;
            }
        }
        if (rawHasNullChar || rawHasBackslashZero || rawHasPercent00 || rawHasUnicodeLiteralNull) {
            logger.warn("Path contains null byte or encoding before trim (nullChar?={}, \\0?={}, %00?={}, \\u0000?={}): {}",
                rawHasNullChar, rawHasBackslashZero, rawHasPercent00, rawHasUnicodeLiteralNull, path);
            throw new IllegalArgumentException("Path contains null bytes");
        }

        // Check for control characters in the middle and end of the path BEFORE trim
        // This ensures control characters like \u0001, \u0007, \u001F are rejected even when not at the very beginning
        // We allow specific whitespace control characters (\t, \n, \r, \f, \u000B) at the beginning to be handled by trim()
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            // Only check characters that are not at the very beginning
            // This allows trim() to handle whitespace control characters at the start, but rejects them elsewhere
            if (i > 0 && ch <= 0x1F) {
                // Check if this is a whitespace control character that should be allowed at beginning
                boolean isWhitespaceControlChar = (ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f' || ch == '\u000B');
                // If it's a whitespace control character at the beginning, allow it to be handled by trim()
                // Otherwise, reject it
                if (!isWhitespaceControlChar) {
                    logger.warn("Path contains control characters (<=0x1F): {}", path);
                    throw new IllegalArgumentException("Path contains control characters");
                }
            }
        }

        // Now trim and continue normalization/validation
        String sanitized = path.trim();

        // Final check for any remaining control characters after trim
        // This catches any control characters that weren't at the edges
        for (int i = 0; i < sanitized.length(); i++) {
            char ch = sanitized.charAt(i);
            if (ch <= 0x1F) {
                logger.warn("Path contains control characters after trim (<=0x1F): {}", path);
                throw new IllegalArgumentException("Path contains control characters");
            }
        }

        // Detect common null-byte representations again after trim
        boolean hasNullChar = sanitized.indexOf('\u0000') >= 0;
        boolean hasPercent00 = sanitized.toLowerCase().contains("%00"); // URL-encoded null byte
        boolean hasUnicodeLiteralNull = sanitized.toLowerCase().contains("\\u0000"); // literal text "\u0000"
        boolean hasBackslashZero = false; // robust detection for "\\0" (backslash then zero)
        for (int i = 0; i + 1 < sanitized.length(); i++) {
            if (sanitized.charAt(i) == '\\' && sanitized.charAt(i + 1) == '0') {
                hasBackslashZero = true;
                break;
            }
        }

        // Check for null bytes (potential security issue) or common encodings
        if (hasNullChar || hasBackslashZero || hasPercent00 || hasUnicodeLiteralNull) {
            logger.warn("Path contains null byte or encoding (nullChar?={}, \\0?={}, %00?={}, \\u0000?={}): {}",
                    hasNullChar, hasBackslashZero, hasPercent00, hasUnicodeLiteralNull, path);
            throw new IllegalArgumentException("Path contains null bytes");
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

        // Validate characters - allow most Unicode characters, but explicitly reject dangerous ones
        // We'll check for specific dangerous characters rather than using a restrictive regex
        for (int i = 0; i < sanitized.length(); i++) {
            char ch = sanitized.charAt(i);
            // Reject specific dangerous characters
            if (ch == '<' || ch == '>' || ch == '\"' || ch == '\\' || ch == '`' || ch == '{' || ch == '}' || ch == '|' || ch == '^') {
                logger.warn("Path contains dangerous character '{}': {}", ch, path);
                throw new IllegalArgumentException("Path contains dangerous characters");
            }
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
        if (path == null) {
            return "/";
        }

        // Check if path is empty or contains only whitespace
        if (path.isEmpty() || path.trim().isEmpty()) {
            return "/";
        }

        String normalized = path;

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
