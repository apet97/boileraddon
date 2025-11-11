package com.clockify.addon.sdk.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive RFC-7807 test assertions for ErrorResponse class.
 * Tests compliance with Problem Details for HTTP APIs specification.
 */
class ErrorResponseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsValidationErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.validationError("Invalid input data");

        // RFC-7807 required fields
        assertNotNull(response.getType(), "type field should not be null");
        assertTrue(response.getType().startsWith("https://developer.clockify.me/addons/errors/"),
                "type should be a URI");
        assertNotNull(response.getTitle(), "title field should not be null");
        assertNotNull(response.getDetail(), "detail field should not be null");
        assertEquals("Invalid input data", response.getDetail());
        assertEquals(400, response.getStatus(), "validation error should have 400 status");
        assertNotNull(response.getTimestamp(), "timestamp should be set");

        // Additional fields
        assertEquals("validation_failed", response.getError());
        assertEquals("Invalid input data", response.getMessage());
    }

    @Test
    void createsAuthenticationErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.authenticationError("Invalid credentials");

        assertEquals("https://developer.clockify.me/addons/errors/authentication_failed", response.getType());
        assertEquals("Authentication Failed", response.getTitle());
        assertEquals("Invalid credentials", response.getDetail());
        assertEquals(401, response.getStatus());
        assertEquals("authentication_failed", response.getError());
    }

    @Test
    void createsAuthorizationErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.authorizationError("Insufficient permissions");

        assertEquals("https://developer.clockify.me/addons/errors/authorization_failed", response.getType());
        assertEquals("Authorization Failed", response.getTitle());
        assertEquals("Insufficient permissions", response.getDetail());
        assertEquals(403, response.getStatus());
        assertEquals("authorization_failed", response.getError());
    }

    @Test
    void createsNotFoundErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.notFound("Resource not found");

        assertEquals("https://developer.clockify.me/addons/errors/not_found", response.getType());
        assertEquals("Not Found", response.getTitle());
        assertEquals("Resource not found", response.getDetail());
        assertEquals(404, response.getStatus());
        assertEquals("not_found", response.getError());
    }

    @Test
    void createsServerErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.serverError("Internal server error");

        assertEquals("https://developer.clockify.me/addons/errors/internal_server_error", response.getType());
        assertEquals("Internal Server Error", response.getTitle());
        assertEquals("Internal server error", response.getDetail());
        assertEquals(500, response.getStatus());
        assertEquals("internal_server_error", response.getError());
    }

    @Test
    void createsBadRequestErrorWithRfc7807Fields() {
        ErrorResponse response = ErrorResponse.badRequest("Malformed request");

        assertEquals("https://developer.clockify.me/addons/errors/bad_request", response.getType());
        assertEquals("Bad Request", response.getTitle());
        assertEquals("Malformed request", response.getDetail());
        assertEquals(400, response.getStatus());
        assertEquals("bad_request", response.getError());
    }

    @Test
    void supportsCustomErrorCode() {
        ErrorResponse response = ErrorResponse.validationError("Invalid input")
                .withErrorCode("VALIDATION.001");

        assertEquals("VALIDATION.001", response.getErrorCode());
    }

    @Test
    void supportsInstanceField() {
        ErrorResponse response = ErrorResponse.validationError("Invalid input")
                .withInstance("/api/rules/123");

        assertEquals("/api/rules/123", response.getInstance());
    }

    @Test
    void supportsAdditionalDetails() {
        Map<String, Object> details = Map.of(
                "field", "email",
                "reason", "must be a valid email address",
                "value", "invalid-email"
        );

        ErrorResponse response = ErrorResponse.validationError("Validation failed")
                .withDetails(details);

        assertEquals(details, response.getDetails());
    }

    @Test
    void producesValidJsonOutput() throws Exception {
        ErrorResponse response = ErrorResponse.validationError("Invalid input")
                .withErrorCode("VALIDATION.001")
                .withInstance("/api/test");

        String json = response.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Parse JSON to verify structure
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        // RFC-7807 required fields
        assertTrue(parsed.containsKey("type"));
        assertTrue(parsed.containsKey("title"));
        assertTrue(parsed.containsKey("detail"));
        assertTrue(parsed.containsKey("status"));
        assertTrue(parsed.containsKey("timestamp"));

        // Additional fields
        assertTrue(parsed.containsKey("error"));
        assertTrue(parsed.containsKey("message"));
        assertTrue(parsed.containsKey("errorCode"));
        assertTrue(parsed.containsKey("instance"));

        assertEquals("Invalid input", parsed.get("detail"));
        assertEquals(400, parsed.get("status"));
        assertEquals("VALIDATION.001", parsed.get("errorCode"));
        assertEquals("/api/test", parsed.get("instance"));
    }

    @Test
    void handlesJsonSerializationFailureGracefully() {
        // Create a response with problematic data that might cause serialization issues
        ErrorResponse response = ErrorResponse.validationError("Test error")
                .withDetails(Map.of("problematic", new Object() {
                    @Override
                    public String toString() {
                        throw new RuntimeException("Serialization error");
                    }
                }));

        // Should not throw exception
        String json = response.toJson();
        assertNotNull(json);
        assertTrue(json.contains("type"));
        assertTrue(json.contains("title"));
        assertTrue(json.contains("detail"));
    }

    @Test
    void generatesAppropriateTitlesForUnknownErrorTypes() {
        ErrorResponse response = ErrorResponse.create("custom_error_type", "Custom error message");

        assertEquals("https://developer.clockify.me/addons/errors/custom_error_type", response.getType());
        assertEquals("Custom Error Type", response.getTitle());
        assertEquals("Custom error message", response.getDetail());
    }

    @Test
    void maintainsBackwardCompatibility() {
        ErrorResponse response = ErrorResponse.validationError("Test error")
                .withStatusCode(400)
                .withPath("/api/test");

        assertEquals(400, response.getStatusCode());
        assertEquals("/api/test", response.getPath());
        assertEquals(400, response.getStatus());
        assertEquals("/api/test", response.getInstance());
    }

    @Test
    void jsonOutputIncludesProblemMediaTypeIndicators() throws Exception {
        ErrorResponse response = ErrorResponse.validationError("Test error");
        String json = response.toJson();

        // Should contain RFC-7807 problem detail structure
        assertTrue(json.contains("\"type\":"));
        assertTrue(json.contains("\"title\":"));
        assertTrue(json.contains("\"detail\":"));
        assertTrue(json.contains("\"status\":"));

        // Should be parseable as JSON
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertNotNull(parsed);
    }
}