package com.clockify.addon.sdk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SECURITY: Audit logging for security-relevant events.
 *
 * Logs all sensitive operations for compliance and debugging:
 * - Authentication events (token validation, rotation)
 * - Authorization failures (rate limit exceeded, CSRF failures)
 * - Data access (token lookups, storage)
 * - Suspicious activity (invalid event types, oversized requests)
 *
 * Output format: JSON for easy parsing by log aggregation systems.
 * Example:
 * <pre>{@code
 * {
 *   "timestamp":"2025-11-09T10:30:45.123Z",
 *   "event":"TOKEN_SAVED",
 *   "workspace":"ws-123",
 *   "status":"SUCCESS",
 *   "details":{"workspaceId":"ws-123","source":"lifecycle_event"}
 * }
 * }</pre>
 *
 * Usage:
 * <pre>{@code
 * AuditLogger.log(AuditEvent.TOKEN_VALIDATION_FAILURE)
 *     .workspace("ws-123")
 *     .detail("reason", "invalid_signature")
 *     .detail("ip", "192.168.1.1")
 *     .error();
 * }</pre>
 */
public class AuditLogger {
    private static final Logger auditLog = LoggerFactory.getLogger("com.clockify.addon.audit");

    /**
     * Audit events with security significance.
     */
    public enum AuditEvent {
        // Authentication/Authorization
        TOKEN_VALIDATION_SUCCESS("Token validation succeeded"),
        TOKEN_VALIDATION_FAILURE("Token validation failed"),
        TOKEN_SAVED("Token saved to storage"),
        TOKEN_ROTATED("Token rotated"),
        TOKEN_REMOVED("Token removed from storage"),
        TOKEN_LOOKUP_FAILURE("Token lookup failed"),

        // Rate Limiting
        RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
        RATE_LIMIT_ENFORCED("Rate limit enforcement triggered"),

        // CSRF Protection
        CSRF_TOKEN_GENERATED("CSRF token generated"),
        CSRF_TOKEN_VALIDATED("CSRF token validated successfully"),
        CSRF_TOKEN_INVALID("CSRF token validation failed"),

        // Input Validation
        INVALID_EVENT_TYPE("Invalid webhook event type received"),
        INVALID_PAYLOAD_SIZE("Payload size exceeds limit"),
        INVALID_JSON("Malformed JSON payload"),

        // HTTPS Enforcement
        INSECURE_CONNECTION_REJECTED("Non-HTTPS request rejected"),

        // Database Operations
        DATABASE_CONNECTION_ERROR("Database connection failed"),
        DATABASE_QUERY_ERROR("Database query failed"),

        // Suspicious Activity
        SUSPICIOUS_REQUEST("Suspicious request detected"),
        MULTIPLE_AUTH_FAILURES("Multiple authentication failures from IP");

        public final String description;

        AuditEvent(String description) {
            this.description = description;
        }
    }

    /**
     * Creates a new audit log entry builder.
     *
     * @param event the audit event
     * @return builder for fluent API
     */
    public static AuditEntry log(AuditEvent event) {
        return new AuditEntry(event);
    }

    /**
     * Builder for audit log entries with fluent API.
     */
    public static class AuditEntry {
        private final AuditEvent event;
        private final long timestamp;
        private String workspace;
        private String clientIp;
        private String userId;
        private String status = "INFO";
        private Map<String, Object> details = new HashMap<>();

        AuditEntry(AuditEvent event) {
            this.event = Objects.requireNonNull(event, "event is required");
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Sets workspace ID for this audit event.
         */
        public AuditEntry workspace(String workspaceId) {
            this.workspace = workspaceId;
            return this;
        }

        /**
         * Sets client IP address for this audit event.
         */
        public AuditEntry clientIp(String ip) {
            this.clientIp = ip;
            return this;
        }

        /**
         * Sets user ID for this audit event.
         */
        public AuditEntry userId(String id) {
            this.userId = id;
            return this;
        }

        /**
         * Adds a detail to the audit log.
         *
         * @param key the detail key
         * @param value the detail value
         */
        public AuditEntry detail(String key, Object value) {
            if (key != null && value != null) {
                details.put(key, value);
            }
            return this;
        }

        /**
         * Logs this entry as INFO level (default).
         */
        public void info() {
            if (auditLog.isInfoEnabled()) {
                auditLog.info("{}", toJson("INFO"));
            }
        }

        /**
         * Logs this entry as WARN level (security issue detected).
         */
        public void warn() {
            if (auditLog.isWarnEnabled()) {
                auditLog.warn("{}", toJson("WARN"));
            }
        }

        /**
         * Logs this entry as ERROR level (security violation detected).
         */
        public void error() {
            if (auditLog.isErrorEnabled()) {
                auditLog.error("{}", toJson("ERROR"));
            }
        }

        /**
         * Converts this entry to JSON format for log aggregation.
         */
        private String toJson(String level) {
            StringBuilder json = new StringBuilder();
            json.append("{\"timestamp\":\"").append(Instant.ofEpochMilli(timestamp).toString()).append("\"");
            json.append(",\"event\":\"").append(event.name()).append("\"");
            json.append(",\"level\":\"").append(level).append("\"");

            if (workspace != null) {
                json.append(",\"workspace\":\"").append(escapeJson(workspace)).append("\"");
            }
            if (clientIp != null) {
                json.append(",\"clientIp\":\"").append(escapeJson(clientIp)).append("\"");
            }
            if (userId != null) {
                json.append(",\"userId\":\"").append(escapeJson(userId)).append("\"");
            }

            if (!details.isEmpty()) {
                json.append(",\"details\":{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : details.entrySet()) {
                    if (!first) json.append(",");
                    json.append("\"").append(escapeJson(entry.getKey())).append("\":");

                    Object value = entry.getValue();
                    if (value instanceof String) {
                        json.append("\"").append(escapeJson((String) value)).append("\"");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        json.append(value);
                    } else {
                        json.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
                    }

                    first = false;
                }
                json.append("}");
            }

            json.append("}");
            return json.toString();
        }

        /**
         * Escapes special characters in JSON strings.
         */
        private static String escapeJson(String input) {
            if (input == null) return "";

            return input
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
