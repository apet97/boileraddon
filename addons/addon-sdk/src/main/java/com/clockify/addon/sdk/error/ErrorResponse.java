package com.clockify.addon.sdk.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Standardized JSON error response format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String error;
    private String message;
    private String errorCode;
    private Integer statusCode;
    private String path;
    private Long timestamp;
    private Map<String, Object> details;

    private ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an error response.
     *
     * @param error Short error identifier (e.g., "validation_failed")
     * @param message Human-readable error message
     * @return Builder for additional fields
     */
    public static ErrorResponse create(String error, String message) {
        ErrorResponse response = new ErrorResponse();
        response.error = error;
        response.message = message;
        return response;
    }

    /**
     * Creates a validation error.
     */
    public static ErrorResponse validationError(String message) {
        return create("validation_failed", message)
                .withStatusCode(400);
    }

    /**
     * Creates an authentication error.
     */
    public static ErrorResponse authenticationError(String message) {
        return create("authentication_failed", message)
                .withStatusCode(401);
    }

    /**
     * Creates an authorization error.
     */
    public static ErrorResponse authorizationError(String message) {
        return create("authorization_failed", message)
                .withStatusCode(403);
    }

    /**
     * Creates a not found error.
     */
    public static ErrorResponse notFound(String message) {
        return create("not_found", message)
                .withStatusCode(404);
    }

    /**
     * Creates a server error.
     */
    public static ErrorResponse serverError(String message) {
        return create("internal_server_error", message)
                .withStatusCode(500);
    }

    /**
     * Creates a bad request error.
     */
    public static ErrorResponse badRequest(String message) {
        return create("bad_request", message)
                .withStatusCode(400);
    }

    public ErrorResponse withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ErrorResponse withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ErrorResponse withPath(String path) {
        this.path = path;
        return this;
    }

    public ErrorResponse withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }

    // Getters
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public Integer getStatusCode() { return statusCode; }
    public String getPath() { return path; }
    public Long getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }

    /**
     * Converts to JSON string.
     */
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Fallback to simple JSON if serialization fails
            return String.format("{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                    escapeJson(error), escapeJson(message), timestamp);
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return toJson();
    }
}
