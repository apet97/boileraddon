package com.clockify.addon.sdk.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Standardized JSON error response format compliant with RFC-7807 (Problem Details).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ERROR_TYPE_BASE_URI = "https://developer.clockify.me/addons/errors/";

    private String type;
    private String title;
    private String detail;
    private String error;
    private String message;
    private String errorCode;
    private Integer status;
    private String instance;
    private Long timestamp;
    private Map<String, Object> details;

    private ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an error response compliant with RFC-7807.
     *
     * @param error Short error identifier (e.g., "validation_failed")
     * @param message Human-readable error message (used as detail)
     * @return Builder for additional fields
     */
    public static ErrorResponse create(String error, String message) {
        ErrorResponse response = new ErrorResponse();
        response.error = error;
        response.message = message;
        response.detail = message;
        response.type = ERROR_TYPE_BASE_URI + error;
        response.title = getTitleForError(error);
        return response;
    }

    /**
     * Creates a validation error.
     */
    public static ErrorResponse validationError(String message) {
        return create("validation_failed", message)
                .withStatus(400);
    }

    /**
     * Creates an authentication error.
     */
    public static ErrorResponse authenticationError(String message) {
        return create("authentication_failed", message)
                .withStatus(401);
    }

    /**
     * Creates an authorization error.
     */
    public static ErrorResponse authorizationError(String message) {
        return create("authorization_failed", message)
                .withStatus(403);
    }

    /**
     * Creates a not found error.
     */
    public static ErrorResponse notFound(String message) {
        return create("not_found", message)
                .withStatus(404);
    }

    /**
     * Creates a server error.
     */
    public static ErrorResponse serverError(String message) {
        return create("internal_server_error", message)
                .withStatus(500);
    }

    /**
     * Creates a bad request error.
     */
    public static ErrorResponse badRequest(String message) {
        return create("bad_request", message)
                .withStatus(400);
    }

    public ErrorResponse withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ErrorResponse withStatus(int status) {
        this.status = status;
        return this;
    }

    public ErrorResponse withInstance(String instance) {
        this.instance = instance;
        return this;
    }

    public ErrorResponse withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }

    // Backward compatibility method
    public ErrorResponse withStatusCode(int statusCode) {
        return withStatus(statusCode);
    }

    // Backward compatibility method
    public ErrorResponse withPath(String path) {
        return withInstance(path);
    }

    // Getters
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public Integer getStatus() { return status; }
    public String getInstance() { return instance; }
    public Long getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }

    // Backward compatibility getters
    public Integer getStatusCode() { return status; }
    public String getPath() { return instance; }

    /**
     * Converts to JSON string.
     */
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Fallback to simple JSON if serialization fails
            return String.format("{\"type\":\"%s\",\"title\":\"%s\",\"detail\":\"%s\",\"status\":%d,\"timestamp\":%d}",
                    escapeJson(type), escapeJson(title), escapeJson(detail), status, timestamp);
        }
    }

    /**
     * Returns human-readable title for error type.
     */
    private static String getTitleForError(String error) {
        switch (error) {
            case "validation_failed":
                return "Validation Failed";
            case "authentication_failed":
                return "Authentication Failed";
            case "authorization_failed":
                return "Authorization Failed";
            case "not_found":
                return "Not Found";
            case "internal_server_error":
                return "Internal Server Error";
            case "bad_request":
                return "Bad Request";
            default:
                // Capitalize and format unknown error types
                String[] words = error.replace("_", " ").split("\\s+");
                StringBuilder title = new StringBuilder();
                for (String word : words) {
                    if (!word.isEmpty()) {
                        title.append(Character.toUpperCase(word.charAt(0)))
                             .append(word.substring(1).toLowerCase())
                             .append(" ");
                    }
                }
                return title.toString().trim();
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
