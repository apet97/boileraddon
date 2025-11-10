package com.clockify.addon.sdk.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * Centralized error handling for addon servlet.
 *
 * Provides safe error responses while logging full details server-side.
 * Prevents information disclosure and ensures consistent error formatting.
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Error categories for classification and safe client responses.
     */
    public enum ErrorCategory {
        VALIDATION("Validation failed", 400),
        NOT_FOUND("Resource not found", 404),
        AUTHENTICATION("Authentication required", 401),
        RATE_LIMIT("Rate limit exceeded", 429),
        SERVER_ERROR("Internal server error", 500),
        DEPENDENCY_ERROR("Service dependency error", 502);

        private final String clientMessage;
        private final int httpStatus;

        ErrorCategory(String clientMessage, int httpStatus) {
            this.clientMessage = clientMessage;
            this.httpStatus = httpStatus;
        }

        public String getClientMessage() {
            return clientMessage;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    /**
     * Creates a safe error response from an exception.
     *
     * Full exception details are logged server-side with correlation ID.
     * Client receives generic message with error code for tracking.
     *
     * @param exception the exception that occurred
     * @param category the error category for classification
     * @param context optional context (e.g., "webhook handler", "token lookup")
     * @return JSON error response safe for client
     */
    public static ErrorResponse handleException(Exception exception, ErrorCategory category, String context) {
        String errorId = UUID.randomUUID().toString();

        // Log full details server-side with correlation ID
        if (category == ErrorCategory.SERVER_ERROR) {
            logger.error("Error [{}] in {}: {}", errorId, context, exception.getMessage(), exception);
        } else {
            logger.warn("Error [{}] in {}: {}", errorId, context, exception.getMessage(), exception);
        }

        ObjectNode response = mapper.createObjectNode();
        response.put("error", category.name().toLowerCase());
        response.put("message", category.getClientMessage());
        response.put("errorId", errorId);  // For support/debugging

        // Add specific error details for validation errors only
        if (category == ErrorCategory.VALIDATION && exception instanceof ValidationException) {
            response.put("details", ((ValidationException) exception).getValidationMessage());
        }

        return new ErrorResponse(
            category.getHttpStatus(),
            response.toString(),
            "application/json"
        );
    }

    /**
     * Creates a validation error response.
     *
     * @param message the validation error message (safe for client)
     * @param context the operation context for logging
     * @return JSON error response
     */
    public static ErrorResponse validationError(String message, String context) {
        logger.warn("Validation error in {}: {}", context, message);

        ObjectNode response = mapper.createObjectNode();
        response.put("error", "validation_failed");
        response.put("message", message);

        return new ErrorResponse(400, response.toString(), "application/json");
    }

    /**
     * Creates a safe error response for unknown exceptions.
     *
     * Hides implementation details while logging everything.
     *
     * @param exception the unexpected exception
     * @param context the operation context
     * @return generic error response
     */
    public static ErrorResponse unknownError(Exception exception, String context) {
        return handleException(exception, ErrorCategory.SERVER_ERROR, context);
    }

    /**
     * Creates a JSON error response.
     *
     * @param statusCode HTTP status code
     * @param message client-safe error message
     * @return JSON error response
     */
    public static ErrorResponse jsonError(int statusCode, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("message", message);
        return new ErrorResponse(statusCode, response.toString(), "application/json");
    }

    /**
     * Safely extracts error message from exception, hiding sensitive details.
     *
     * @param exception the exception
     * @return safe error message
     */
    public static String getSafeMessage(Exception exception) {
        if (exception instanceof ValidationException) {
            return ((ValidationException) exception).getValidationMessage();
        }

        String message = exception.getMessage();
        if (message == null) {
            return "An error occurred";
        }

        // Hide sensitive details in message
        message = message.replace("SQLException", "Database error")
                        .replace("Connection refused", "Service unavailable")
                        .replace("password", "***");

        // Truncate if too long
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }

        return message;
    }

    /**
     * Custom validation exception for clearer error classification.
     */
    public static class ValidationException extends Exception {
        private final String validationMessage;

        public ValidationException(String validationMessage) {
            super(validationMessage);
            this.validationMessage = validationMessage;
        }

        public String getValidationMessage() {
            return validationMessage;
        }
    }

    /**
     * Error response data transfer object.
     */
    public static class ErrorResponse {
        private final int statusCode;
        private final String body;
        private final String contentType;

        public ErrorResponse(int statusCode, String body, String contentType) {
            this.statusCode = statusCode;
            this.body = body;
            this.contentType = contentType;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
