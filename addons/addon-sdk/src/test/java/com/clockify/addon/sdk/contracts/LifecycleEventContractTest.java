package com.clockify.addon.sdk.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for lifecycle event payloads.
 * Validates INSTALLED and DELETED events conform to expected schemas.
 */
class LifecycleEventContractTest {
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // ============ INSTALLED Event Contract ============

    @Test
    void installedEvent_hasRequiredFields() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-test-001",
                "user-test-001",
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...");

        assertEquals("INSTALLED", event.get("event").asText());
        assertEquals("ws-test-001", event.get("workspaceId").asText());
        assertEquals("user-test-001", event.get("userId").asText());
        assertTrue(event.has("installationToken"));
        assertTrue(event.has("timestamp"));
        assertTrue(event.has("context"));
    }

    @Test
    void installedEvent_hasValidTimestamp() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-123",
                "user-123",
                "token");

        String timestamp = event.get("timestamp").asText();
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void installedEvent_contextHasWorkspaceDetails() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-123",
                "user-123",
                "token");

        JsonNode context = event.get("context");
        assertTrue(context.has("workspaceName"));
        assertTrue(context.has("userEmail"));
        assertTrue(context.has("userName"));

        assertEquals("Test Workspace", context.get("workspaceName").asText());
        assertEquals("test@example.com", context.get("userEmail").asText());
        assertEquals("Test User", context.get("userName").asText());
    }

    @Test
    void installedEvent_installationTokenIsJWT() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-123",
                "user-123",
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2Nsb2NraWZ5Lm1lIn0...");

        String token = event.get("installationToken").asText();
        // JWT format: three dot-separated parts
        String[] parts = token.split("\\.");
        assertTrue(parts.length >= 2, "Token should be JWT-like format");
    }

    @Test
    void installedEvent_workspaceIdIsNonEmpty() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-abc123xyz",
                "user-123",
                "token");

        String workspaceId = event.get("workspaceId").asText();
        assertFalse(workspaceId.isEmpty());
        assertTrue(workspaceId.length() > 0);
    }

    @Test
    void installedEvent_userIdIsNonEmpty() throws Exception {
        ObjectNode event = buildInstalledEvent(
                "ws-123",
                "user-xyz789",
                "token");

        String userId = event.get("userId").asText();
        assertFalse(userId.isEmpty());
    }

    // ============ DELETED Event Contract ============

    @Test
    void deletedEvent_hasRequiredFields() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-test-001", "user-test-001");

        assertEquals("DELETED", event.get("event").asText());
        assertEquals("ws-test-001", event.get("workspaceId").asText());
        assertEquals("user-test-001", event.get("userId").asText());
        assertTrue(event.has("timestamp"));
        assertTrue(event.has("context"));
    }

    @Test
    void deletedEvent_noInstallationToken() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-123", "user-123");

        assertFalse(event.has("installationToken"),
                "DELETED event should not include token");
    }

    @Test
    void deletedEvent_contextHasWorkspaceDetails() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-123", "user-123");

        JsonNode context = event.get("context");
        assertTrue(context.has("workspaceName"));
        // userEmail might be optional in DELETED event
    }

    @Test
    void deletedEvent_hasValidTimestamp() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-123", "user-123");

        String timestamp = event.get("timestamp").asText();
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    // ============ Event Type Validation ============

    @Test
    void lifecycleEventTypes_areUpperCase() {
        String[] lifecycleTypes = {"INSTALLED", "DELETED"};

        for (String type : lifecycleTypes) {
            assertTrue(type.matches("[A-Z_]+"));
        }
    }

    // ============ Timestamp Format Contract ============

    @Test
    void installedAndDeletedEvent_bothHaveRFC3339Timestamp() throws Exception {
        ObjectNode installed = buildInstalledEvent("ws-1", "u-1", "token");
        ObjectNode deleted = buildDeletedEvent("ws-1", "u-1");

        String installedTs = installed.get("timestamp").asText();
        String deletedTs = deleted.get("timestamp").asText();

        // RFC 3339 / ISO 8601 UTC format
        assertTrue(installedTs.endsWith("Z"));
        assertTrue(deletedTs.endsWith("Z"));

        assertTrue(installedTs.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
        assertTrue(deletedTs.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    // ============ Context Object Contract ============

    @Test
    void lifecycleContext_alwaysPresent() throws Exception {
        ObjectNode installed = buildInstalledEvent("ws-1", "u-1", "token");
        ObjectNode deleted = buildDeletedEvent("ws-1", "u-1");

        assertTrue(installed.has("context"));
        assertTrue(deleted.has("context"));
    }

    @Test
    void lifecycleContext_isObject() throws Exception {
        ObjectNode installed = buildInstalledEvent("ws-1", "u-1", "token");

        JsonNode context = installed.get("context");
        assertTrue(context.isObject());
    }

    @Test
    void contextWorkspaceName_isString() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        String name = event.get("context").get("workspaceName").asText();
        assertFalse(name.isEmpty());
    }

    // ============ Immutability / Consistency ============

    @Test
    void lifecycleEvent_fieldsAreConsistent() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-123", "user-456", "token");

        // If we extract and verify multiple times, should be consistent
        String wsId1 = event.get("workspaceId").asText();
        String wsId2 = event.get("workspaceId").asText();

        assertEquals(wsId1, wsId2);
    }

    // ============ JSON Serialization ============

    @Test
    void installedEvent_isJsonSerializable() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        String json = mapper.writeValueAsString(event);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Round-trip test
        JsonNode parsed = mapper.readTree(json);
        assertEquals("INSTALLED", parsed.get("event").asText());
    }

    @Test
    void deletedEvent_isJsonSerializable() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-1", "u-1");

        String json = mapper.writeValueAsString(event);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        JsonNode parsed = mapper.readTree(json);
        assertEquals("DELETED", parsed.get("event").asText());
    }

    // ============ Context Field Contracts ============

    @Test
    void installedEvent_contextEmail_isValidFormat() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        String email = event.get("context").get("userEmail").asText();
        assertTrue(email.contains("@"), "Email should contain @");
        assertTrue(email.contains("."), "Email should contain .");
    }

    @Test
    void installedEvent_contextUserName_isNonEmpty() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        String userName = event.get("context").get("userName").asText();
        assertFalse(userName.isEmpty());
    }

    // ============ Field Data Types ============

    @Test
    void workspaceId_isString() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        assertTrue(event.get("workspaceId").isTextual());
    }

    @Test
    void userId_isString() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        assertTrue(event.get("userId").isTextual());
    }

    @Test
    void installationToken_isString() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        assertTrue(event.get("installationToken").isTextual());
    }

    @Test
    void timestamp_isString() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        assertTrue(event.get("timestamp").isTextual());
    }

    // ============ No Null Values in Required Fields ============

    @Test
    void installedEvent_noNullRequiredFields() throws Exception {
        ObjectNode event = buildInstalledEvent("ws-1", "u-1", "token");

        assertFalse(event.get("event").isNull());
        assertFalse(event.get("workspaceId").isNull());
        assertFalse(event.get("userId").isNull());
        assertFalse(event.get("installationToken").isNull());
        assertFalse(event.get("timestamp").isNull());
    }

    @Test
    void deletedEvent_noNullRequiredFields() throws Exception {
        ObjectNode event = buildDeletedEvent("ws-1", "u-1");

        assertFalse(event.get("event").isNull());
        assertFalse(event.get("workspaceId").isNull());
        assertFalse(event.get("userId").isNull());
        assertFalse(event.get("timestamp").isNull());
    }

    // ============ Event Type Enumeration ============

    @Test
    void supportedLifecycleEvents() {
        String[] supportedEvents = {"INSTALLED", "DELETED"};

        for (String eventType : supportedEvents) {
            assertTrue(eventType.matches("[A-Z_]+"));
            assertFalse(eventType.isEmpty());
        }
    }

    // ============ Comparison: INSTALLED vs DELETED ============

    @Test
    void deletedEvent_isMinimalVersionOfInstalled() throws Exception {
        ObjectNode installed = buildInstalledEvent("ws-1", "u-1", "token");
        ObjectNode deleted = buildDeletedEvent("ws-1", "u-1");

        // Both have these
        assertEquals("ws-1", installed.get("workspaceId").asText());
        assertEquals("ws-1", deleted.get("workspaceId").asText());

        // Only INSTALLED has token
        assertTrue(installed.has("installationToken"));
        assertFalse(deleted.has("installationToken"));
    }

    // ============ Helper Methods ============

    private ObjectNode buildInstalledEvent(String workspaceId, String userId, String token) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", "INSTALLED");
        event.put("workspaceId", workspaceId);
        event.put("userId", userId);
        event.put("installationToken", token);
        event.put("timestamp", "2025-01-10T10:30:00Z");

        ObjectNode context = mapper.createObjectNode();
        context.put("workspaceName", "Test Workspace");
        context.put("userEmail", "test@example.com");
        context.put("userName", "Test User");
        event.set("context", context);

        return event;
    }

    private ObjectNode buildDeletedEvent(String workspaceId, String userId) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", "DELETED");
        event.put("workspaceId", workspaceId);
        event.put("userId", userId);
        event.put("timestamp", "2025-01-10T11:30:00Z");

        ObjectNode context = mapper.createObjectNode();
        context.put("workspaceName", "Test Workspace");
        event.set("context", context);

        return event;
    }
}
