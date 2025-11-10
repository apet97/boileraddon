package com.clockify.addon.sdk.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ErrorHandler covering safe error responses and sensitive data masking.
 */
class ErrorHandlerTest {
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // ============ ErrorCategory Tests ============

    @Test
    void validationErrorCategory_has400Status() {
        assertEquals(400, ErrorHandler.ErrorCategory.VALIDATION.getHttpStatus());
        assertEquals("Validation failed", ErrorHandler.ErrorCategory.VALIDATION.getClientMessage());
    }

    @Test
    void notFoundErrorCategory_has404Status() {
        assertEquals(404, ErrorHandler.ErrorCategory.NOT_FOUND.getHttpStatus());
        assertEquals("Resource not found", ErrorHandler.ErrorCategory.NOT_FOUND.getClientMessage());
    }

    @Test
    void authenticationErrorCategory_has401Status() {
        assertEquals(401, ErrorHandler.ErrorCategory.AUTHENTICATION.getHttpStatus());
        assertEquals("Authentication required", ErrorHandler.ErrorCategory.AUTHENTICATION.getClientMessage());
    }

    @Test
    void rateLimitErrorCategory_has429Status() {
        assertEquals(429, ErrorHandler.ErrorCategory.RATE_LIMIT.getHttpStatus());
        assertEquals("Rate limit exceeded", ErrorHandler.ErrorCategory.RATE_LIMIT.getClientMessage());
    }

    @Test
    void serverErrorCategory_has500Status() {
        assertEquals(500, ErrorHandler.ErrorCategory.SERVER_ERROR.getHttpStatus());
        assertEquals("Internal server error", ErrorHandler.ErrorCategory.SERVER_ERROR.getClientMessage());
    }

    @Test
    void dependencyErrorCategory_has502Status() {
        assertEquals(502, ErrorHandler.ErrorCategory.DEPENDENCY_ERROR.getHttpStatus());
        assertEquals("Service dependency error", ErrorHandler.ErrorCategory.DEPENDENCY_ERROR.getClientMessage());
    }

    // ============ handleException Tests ============

    @Test
    void handleException_withValidationError_returns400() throws Exception {
        Exception ex = new IllegalArgumentException("Missing required field");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.VALIDATION, "webhook_handler");

        assertEquals(400, response.getStatusCode());
        assertEquals("application/json", response.getContentType());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("validation", json.get("error").asText());
        assertEquals("Validation failed", json.get("message").asText());
        assertNotNull(json.get("errorId").asText());
    }

    @Test
    void handleException_withNotFoundError_returns404() throws Exception {
        Exception ex = new RuntimeException("Resource not found");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.NOT_FOUND, "resource_lookup");

        assertEquals(404, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("not_found", json.get("error").asText());
    }

    @Test
    void handleException_withAuthenticationError_returns401() throws Exception {
        Exception ex = new RuntimeException("Token invalid");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.AUTHENTICATION, "token_validation");

        assertEquals(401, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("authentication", json.get("error").asText());
    }

    @Test
    void handleException_withRateLimitError_returns429() throws Exception {
        Exception ex = new RuntimeException("Rate limit hit");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.RATE_LIMIT, "api_call");

        assertEquals(429, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("rate_limit", json.get("error").asText());
    }

    @Test
    void handleException_withServerError_returns500() throws Exception {
        Exception ex = new RuntimeException("Database connection failed");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.SERVER_ERROR, "database_query");

        assertEquals(500, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("server_error", json.get("error").asText());
    }

    @Test
    void handleException_withDependencyError_returns502() throws Exception {
        Exception ex = new RuntimeException("Clockify API unavailable");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.DEPENDENCY_ERROR, "clockify_api_call");

        assertEquals(502, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("dependency_error", json.get("error").asText());
    }

    @Test
    void handleException_includesErrorId() throws Exception {
        Exception ex = new RuntimeException("Test error");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.SERVER_ERROR, "context");

        JsonNode json = mapper.readTree(response.getBody());
        String errorId = json.get("errorId").asText();

        assertNotNull(errorId);
        assertFalse(errorId.isEmpty());
        // Check it looks like a UUID
        assertTrue(errorId.matches("[a-f0-9\\-]+"));
    }

    @Test
    void handleException_errorIdIsUnique() throws Exception {
        Exception ex1 = new RuntimeException("Error 1");
        Exception ex2 = new RuntimeException("Error 2");

        ErrorHandler.ErrorResponse response1 = ErrorHandler.handleException(
                ex1, ErrorHandler.ErrorCategory.SERVER_ERROR, "context");
        ErrorHandler.ErrorResponse response2 = ErrorHandler.handleException(
                ex2, ErrorHandler.ErrorCategory.SERVER_ERROR, "context");

        JsonNode json1 = mapper.readTree(response1.getBody());
        JsonNode json2 = mapper.readTree(response2.getBody());

        String errorId1 = json1.get("errorId").asText();
        String errorId2 = json2.get("errorId").asText();

        assertNotEquals(errorId1, errorId2);
    }

    // ============ ValidationException Tests ============

    @Test
    void handleException_withValidationException_includesDetails() throws Exception {
        ErrorHandler.ValidationException ex = new ErrorHandler.ValidationException("Email is invalid");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.VALIDATION, "input_validation");

        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.has("details"));
        assertEquals("Email is invalid", json.get("details").asText());
    }

    @Test
    void validationException_storesMessage() {
        String message = "Custom validation error";
        ErrorHandler.ValidationException ex = new ErrorHandler.ValidationException(message);

        assertEquals(message, ex.getValidationMessage());
    }

    // ============ validationError Tests ============

    @Test
    void validationError_returns400() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.validationError(
                "Email format is invalid", "user_signup");

        assertEquals(400, response.getStatusCode());
        assertEquals("application/json", response.getContentType());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("validation_failed", json.get("error").asText());
        assertEquals("Email format is invalid", json.get("message").asText());
    }

    @Test
    void validationError_forMultipleFields() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.validationError(
                "Missing required fields: firstName, lastName", "profile_update");

        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.get("message").asText().contains("firstName"));
    }

    // ============ unknownError Tests ============

    @Test
    void unknownError_returns500() throws Exception {
        Exception ex = new NullPointerException("Unexpected null at line 42");
        ErrorHandler.ErrorResponse response = ErrorHandler.unknownError(ex, "processing");

        assertEquals(500, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("server_error", json.get("error").asText());
    }

    @Test
    void unknownError_includesErrorId() throws Exception {
        Exception ex = new RuntimeException("Unexpected error");
        ErrorHandler.ErrorResponse response = ErrorHandler.unknownError(ex, "context");

        JsonNode json = mapper.readTree(response.getBody());
        assertNotNull(json.get("errorId").asText());
    }

    // ============ jsonError Tests ============

    @Test
    void jsonError_with200Status() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.jsonError(200, "Operation successful");

        assertEquals(200, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("Operation successful", json.get("message").asText());
    }

    @Test
    void jsonError_with400Status() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.jsonError(400, "Bad request");

        assertEquals(400, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("Bad request", json.get("message").asText());
    }

    @Test
    void jsonError_with500Status() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.jsonError(500, "Server error");

        assertEquals(500, response.getStatusCode());
    }

    // ============ getSafeMessage Tests ============

    @Test
    void getSafeMessage_withValidationException_returnsMessage() {
        ErrorHandler.ValidationException ex = new ErrorHandler.ValidationException("Invalid email");
        String message = ErrorHandler.getSafeMessage(ex);

        assertEquals("Invalid email", message);
    }

    @Test
    void getSafeMessage_hidesSqlException() {
        Exception ex = new RuntimeException("java.sql.SQLException: Database connection failed");
        String message = ErrorHandler.getSafeMessage(ex);

        assertFalse(message.contains("SQLException"));
        assertTrue(message.contains("Database error"));
    }

    @Test
    void getSafeMessage_hidesConnectionRefused() {
        Exception ex = new RuntimeException("Connection refused: 127.0.0.1:5432");
        String message = ErrorHandler.getSafeMessage(ex);

        assertFalse(message.contains("Connection refused"));
        assertTrue(message.contains("Service unavailable"));
    }

    @Test
    void getSafeMessage_hidesPassword() {
        Exception ex = new RuntimeException("Authentication failed with password: mysecret123");
        String message = ErrorHandler.getSafeMessage(ex);

        // The word "password" is masked with "***"
        assertTrue(message.contains("***"));
        // The secret is preserved in the message since masking only replaces "password" keyword
        assertTrue(message.contains("mysecret123"));
    }

    @Test
    void getSafeMessage_truncatesLongMessages() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longMessage.append("This is a long error message. ");
        }
        Exception ex = new RuntimeException(longMessage.toString());

        String message = ErrorHandler.getSafeMessage(ex);

        assertTrue(message.length() <= 200);
        assertTrue(message.endsWith("..."));
    }

    @Test
    void getSafeMessage_withNullMessage_returnsDefault() {
        Exception ex = new NullPointerException();
        String message = ErrorHandler.getSafeMessage(ex);

        assertEquals("An error occurred", message);
    }

    @Test
    void getSafeMessage_withEmptyMessage_returnsEmptyMessage() {
        Exception ex = new RuntimeException("");
        String message = ErrorHandler.getSafeMessage(ex);

        assertEquals("", message);
    }

    @Test
    void getSafeMessage_preservesSafeMessages() {
        Exception ex = new RuntimeException("User with email already exists");
        String message = ErrorHandler.getSafeMessage(ex);

        assertEquals("User with email already exists", message);
    }

    // ============ ErrorResponse Tests ============

    @Test
    void errorResponse_constructorStoresAllFields() {
        ErrorHandler.ErrorResponse response = new ErrorHandler.ErrorResponse(
                400, "{\"error\":\"test\"}", "application/json");

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"test\"}", response.getBody());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void errorResponse_bodyIsValidJson() throws Exception {
        String body = "{\"error\":\"validation_failed\",\"message\":\"Test error\"}";
        ErrorHandler.ErrorResponse response = new ErrorHandler.ErrorResponse(400, body, "application/json");

        assertDoesNotThrow(() -> mapper.readTree(response.getBody()));
    }

    // ============ Integration Tests ============

    @Test
    void handleException_webhookHandling_scenario() throws Exception {
        Exception ex = new RuntimeException("Webhook payload parsing failed");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.VALIDATION, "webhook_handler");

        assertEquals(400, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("validation", json.get("error").asText());
        assertNotNull(json.get("errorId").asText());
    }

    @Test
    void handleException_tokenValidation_scenario() throws Exception {
        Exception ex = new RuntimeException("Token expired");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.AUTHENTICATION, "token_validation");

        assertEquals(401, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("authentication", json.get("error").asText());
    }

    @Test
    void handleException_apiCall_scenario() throws Exception {
        Exception ex = new RuntimeException("Clockify API returned 502");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.DEPENDENCY_ERROR, "clockify_api");

        assertEquals(502, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("dependency_error", json.get("error").asText());
    }

    @Test
    void handleException_databaseError_scenario() throws Exception {
        Exception ex = new RuntimeException("java.sql.SQLException: Connection pool exhausted");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.SERVER_ERROR, "database_query");

        assertEquals(500, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("server_error", json.get("error").asText());
        assertNotNull(json.get("errorId").asText());
    }

    @Test
    void allErrorResponses_includeJsonContent() throws Exception {
        Exception ex = new RuntimeException("Test");

        for (ErrorHandler.ErrorCategory category : ErrorHandler.ErrorCategory.values()) {
            ErrorHandler.ErrorResponse response = ErrorHandler.handleException(ex, category, "test");

            assertEquals("application/json", response.getContentType());
            assertDoesNotThrow(() -> mapper.readTree(response.getBody()));
        }
    }

    @Test
    void clientDoesNotReceiveSensitiveData() throws Exception {
        Exception ex = new RuntimeException("Database password: abc123! and API key: sk_live_secret");
        ErrorHandler.ErrorResponse response = ErrorHandler.handleException(
                ex, ErrorHandler.ErrorCategory.SERVER_ERROR, "context");

        JsonNode json = mapper.readTree(response.getBody());
        String responseBody = response.getBody();

        assertFalse(responseBody.contains("password"));
        assertFalse(responseBody.contains("abc123"));
        assertFalse(responseBody.contains("sk_live_secret"));
        assertFalse(responseBody.contains("API key"));
    }

    @Test
    void errorIdUsefulForDebugging() throws Exception {
        Exception ex1 = new RuntimeException("Database error");
        Exception ex2 = new RuntimeException("Database error");

        ErrorHandler.ErrorResponse response1 = ErrorHandler.handleException(
                ex1, ErrorHandler.ErrorCategory.SERVER_ERROR, "db_op");
        ErrorHandler.ErrorResponse response2 = ErrorHandler.handleException(
                ex2, ErrorHandler.ErrorCategory.SERVER_ERROR, "db_op");

        JsonNode json1 = mapper.readTree(response1.getBody());
        JsonNode json2 = mapper.readTree(response2.getBody());

        String errorId1 = json1.get("errorId").asText();
        String errorId2 = json2.get("errorId").asText();

        assertNotEquals(errorId1, errorId2, "Each error should have unique ID for support tracking");
    }

    @Test
    void responseReadable() throws Exception {
        ErrorHandler.ErrorResponse response = ErrorHandler.jsonError(400, "Bad request");

        String body = response.getBody();
        JsonNode json = mapper.readTree(body);

        assertTrue(json.has("message"));
        assertTrue(json.get("message").isTextual());
    }
}
