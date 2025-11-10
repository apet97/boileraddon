package com.clockify.addon.sdk.contracts;

import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for HTTP response objects.
 * Validates response format, content types, and status codes.
 */
class HttpResponseContractTest {
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // ============ Success Response Contract ============

    @Test
    void okResponse_returns200Status() throws Exception {
        HttpResponse response = HttpResponse.ok("{\"status\":\"ok\"}", "application/json");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void okResponse_withBody() throws Exception {
        String body = "{\"message\":\"Success\"}";
        HttpResponse response = HttpResponse.ok(body, "application/json");

        assertEquals(body, response.getBody());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void okResponse_defaultContentType() throws Exception {
        HttpResponse response = HttpResponse.ok("plain text");

        assertNotNull(response.getContentType());
    }

    @Test
    void okResponseWithJson_isValidJson() throws Exception {
        HttpResponse response = HttpResponse.ok(
                "{\"success\":true,\"id\":\"123\"}",
                "application/json");

        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.get("success").asBoolean());
        assertEquals("123", json.get("id").asText());
    }

    // ============ Error Response Contract ============

    @Test
    void errorResponse_returns4xxStatus() throws Exception {
        HttpResponse response = HttpResponse.error(400, "Bad request");

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getStatusCode() >= 400);
    }

    @Test
    void errorResponse_with404() throws Exception {
        HttpResponse response = HttpResponse.error(404, "Not found");

        assertEquals(404, response.getStatusCode());
    }

    @Test
    void errorResponse_with500() throws Exception {
        HttpResponse response = HttpResponse.error(500, "Server error");

        assertEquals(500, response.getStatusCode());
    }

    @Test
    void errorResponse_with401() throws Exception {
        HttpResponse response = HttpResponse.error(401, "Unauthorized");

        assertEquals(401, response.getStatusCode());
    }

    @Test
    void errorResponse_with403() throws Exception {
        HttpResponse response = HttpResponse.error(403, "Forbidden");

        assertEquals(403, response.getStatusCode());
    }

    @Test
    void errorResponse_withMessage() throws Exception {
        String message = "Resource not found";
        HttpResponse response = HttpResponse.error(404, message);

        assertEquals(message, response.getBody());
    }

    @Test
    void errorResponseWithJson_isValidJson() throws Exception {
        HttpResponse response = HttpResponse.error(
                400,
                "{\"error\":\"invalid_input\",\"message\":\"Missing field: name\"}",
                "application/json");

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("invalid_input", json.get("error").asText());
    }

    // ============ Content Type Contract ============

    @Test
    void applicationJsonContentType_isCorrect() throws Exception {
        HttpResponse response = HttpResponse.ok("{}", "application/json");

        assertEquals("application/json", response.getContentType());
    }

    @Test
    void textPlainContentType_isCorrect() throws Exception {
        HttpResponse response = HttpResponse.ok("plain text", "text/plain");

        assertEquals("text/plain", response.getContentType());
    }

    @Test
    void textHtmlContentType_isCorrect() throws Exception {
        HttpResponse response = HttpResponse.ok("<html></html>", "text/html");

        assertEquals("text/html", response.getContentType());
    }

    @Test
    void responseContentType_preserved() throws Exception {
        String contentType = "application/json; charset=utf-8";
        HttpResponse response = HttpResponse.ok("{}", contentType);

        assertEquals(contentType, response.getContentType());
    }

    // ============ Body Content Contract ============

    @Test
    void responseBody_isPreserved() throws Exception {
        String body = "{\"key\":\"value\"}";
        HttpResponse response = HttpResponse.ok(body, "application/json");

        assertEquals(body, response.getBody());
    }

    @Test
    void responseBody_canBeEmpty() throws Exception {
        HttpResponse response = HttpResponse.ok("", "text/plain");

        assertEquals("", response.getBody());
    }

    @Test
    void errorResponseBody_canBeEmpty() throws Exception {
        HttpResponse response = HttpResponse.error(204, "");

        assertEquals("", response.getBody());
    }

    @Test
    void responseBody_canBeMultiline() throws Exception {
        String body = "Line 1\nLine 2\nLine 3";
        HttpResponse response = HttpResponse.ok(body, "text/plain");

        assertEquals(body, response.getBody());
        assertTrue(response.getBody().contains("\n"));
    }

    @Test
    void responseBody_preservesSpecialCharacters() throws Exception {
        String body = "{\"text\":\"Special chars: @#$%^&*()\"}";
        HttpResponse response = HttpResponse.ok(body, "application/json");

        assertEquals(body, response.getBody());
    }

    // ============ Status Code Contract ============

    @Test
    void statusCode_is200ForSuccess() throws Exception {
        HttpResponse response = HttpResponse.ok("success", "text/plain");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void statusCode_isValidHttpStatus() throws Exception {
        HttpResponse response = HttpResponse.error(400, "bad");

        assertTrue(response.getStatusCode() >= 100);
        assertTrue(response.getStatusCode() < 600);
    }

    @Test
    void statusCode_variedErrorCodes() throws Exception {
        int[] errorCodes = {400, 401, 403, 404, 429, 500, 502, 503};

        for (int code : errorCodes) {
            HttpResponse response = HttpResponse.error(code, "Error");
            assertEquals(code, response.getStatusCode());
        }
    }

    // ============ Immutability Contract ============

    @Test
    void response_fieldValuesAreConsistent() throws Exception {
        HttpResponse response = HttpResponse.ok("{\"id\":\"123\"}", "application/json");

        int status1 = response.getStatusCode();
        int status2 = response.getStatusCode();

        assertEquals(status1, status2);
    }

    @Test
    void response_bodyDoesNotChange() throws Exception {
        String body = "{\"data\":\"test\"}";
        HttpResponse response = HttpResponse.ok(body, "application/json");

        String body1 = response.getBody();
        String body2 = response.getBody();

        assertEquals(body1, body2);
    }

    // ============ Webhook Success Response Contract ============

    @Test
    void webhookSuccessResponse_returns200() throws Exception {
        HttpResponse response = HttpResponse.ok(
                "{\"processed\":true}",
                "application/json");

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getContentType());

        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.get("processed").asBoolean());
    }

    @Test
    void webhookErrorResponse_returns401() throws Exception {
        HttpResponse response = HttpResponse.error(401,
                "{\"error\":\"invalid_signature\",\"message\":\"Webhook signature invalid\"}",
                "application/json");

        assertEquals(401, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("invalid_signature", json.get("error").asText());
    }

    // ============ Lifecycle Handler Response Contract ============

    @Test
    void installedHandlerResponse_returns200() throws Exception {
        HttpResponse response = HttpResponse.ok(
                "{\"success\":true,\"message\":\"Addon installed\"}",
                "application/json");

        assertEquals(200, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.get("success").asBoolean());
    }

    @Test
    void deletedHandlerResponse_returns200() throws Exception {
        HttpResponse response = HttpResponse.ok(
                "{\"success\":true}",
                "application/json");

        assertEquals(200, response.getStatusCode());
    }

    // ============ Manifest Response Contract ============

    @Test
    void manifestResponse_returns200() throws Exception {
        String manifest = "{\"schemaVersion\":\"1.3\",\"key\":\"test-addon\"}";
        HttpResponse response = HttpResponse.ok(manifest, "application/json");

        assertEquals(200, response.getStatusCode());

        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("1.3", json.get("schemaVersion").asText());
        assertEquals("test-addon", json.get("key").asText());
    }

    // ============ Custom Endpoint Response Contract ============

    @Test
    void customEndpoint_canReturnJson() throws Exception {
        String body = "{\"settings\":{\"theme\":\"dark\",\"notifications\":true}}";
        HttpResponse response = HttpResponse.ok(body, "application/json");

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("dark", json.get("settings").get("theme").asText());
    }

    @Test
    void customEndpoint_canReturnHtml() throws Exception {
        String body = "<html><body><h1>Settings</h1></body></html>";
        HttpResponse response = HttpResponse.ok(body, "text/html");

        assertEquals(200, response.getStatusCode());
        assertEquals("text/html", response.getContentType());
        assertTrue(response.getBody().contains("<html>"));
    }

    @Test
    void customEndpoint_canReturnPlainText() throws Exception {
        String body = "Server is healthy";
        HttpResponse response = HttpResponse.ok(body, "text/plain");

        assertEquals(200, response.getStatusCode());
        assertEquals("text/plain", response.getContentType());
    }

    // ============ Error Response Variations ============

    @Test
    void validationErrorResponse() throws Exception {
        HttpResponse response = HttpResponse.error(400,
                "{\"error\":\"validation_error\",\"details\":\"Invalid email format\"}",
                "application/json");

        assertEquals(400, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("validation_error", json.get("error").asText());
    }

    @Test
    void authenticationErrorResponse() throws Exception {
        HttpResponse response = HttpResponse.error(401,
                "{\"error\":\"authentication_required\",\"message\":\"Missing token\"}",
                "application/json");

        assertEquals(401, response.getStatusCode());
    }

    @Test
    void permissionErrorResponse() throws Exception {
        HttpResponse response = HttpResponse.error(403,
                "{\"error\":\"insufficient_permissions\",\"message\":\"Admin required\"}",
                "application/json");

        assertEquals(403, response.getStatusCode());
    }

    @Test
    void serverErrorResponse() throws Exception {
        HttpResponse response = HttpResponse.error(500,
                "{\"error\":\"server_error\",\"errorId\":\"abc-123\"}",
                "application/json");

        assertEquals(500, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertNotNull(json.get("errorId"));
    }

    // ============ Content Encoding ============

    @Test
    void responseBody_supportsUtf8Characters() throws Exception {
        String body = "{\"message\":\"Hello 世界 🌍\"}";
        HttpResponse response = HttpResponse.ok(body, "application/json; charset=utf-8");

        assertEquals(body, response.getBody());
        assertTrue(response.getBody().contains("世界"));
    }

    // ============ Response Factory Methods ============

    @Test
    void okResponseFactory_createsValidResponse() throws Exception {
        HttpResponse response = HttpResponse.ok("{\"test\":true}", "application/json");

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getContentType());
    }

    @Test
    void errorResponseFactory_createsValidResponse() throws Exception {
        HttpResponse response = HttpResponse.error(404, "Not found");

        assertEquals(404, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void errorResponseFactory_withContentType() throws Exception {
        HttpResponse response = HttpResponse.error(400,
                "{\"error\":\"invalid\"}",
                "application/json");

        assertEquals(400, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
    }
}
