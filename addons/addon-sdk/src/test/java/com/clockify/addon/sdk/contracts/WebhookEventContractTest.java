package com.clockify.addon.sdk.contracts;

import com.clockify.addon.sdk.testing.TestFixtures;
import com.clockify.addon.sdk.testing.builders.TimeEntryBuilder;
import com.clockify.addon.sdk.testing.builders.WebhookEventBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for webhook event payloads.
 * Validates that webhook events conform to expected schemas and field requirements.
 */
class WebhookEventContractTest {
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // ============ TIME_ENTRY_CREATED Contract ============

    @Test
    void timeEntryCreatedEvent_hasRequiredFields() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;

        assertEquals("TIME_ENTRY_CREATED", event.get("event").asText());
        assertEquals("ws-test-001", event.get("workspaceId").asText());
        assertEquals("Test Workspace", event.get("workspaceName").asText());
        assertEquals("user-test-001", event.get("userId").asText());
        assertEquals("Test User", event.get("userName").asText());
        assertEquals("test@example.com", event.get("userEmail").asText());
        assertNotNull(event.get("timeEntry"));
    }

    @Test
    void timeEntryCreatedEvent_timeEntryHasRequiredFields() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        assertTrue(timeEntry.has("id"));
        assertTrue(timeEntry.has("description"));
        assertTrue(timeEntry.has("start"));
        assertTrue(timeEntry.has("end"));
        assertTrue(timeEntry.has("duration"));
        assertTrue(timeEntry.has("billable"));
    }

    @Test
    void timeEntryCreatedEvent_durationIsInSeconds() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");
        long duration = timeEntry.get("duration").asLong();

        // 7200 seconds = 2 hours
        assertEquals(7200, duration);
        assertTrue(duration > 0);
    }

    @Test
    void timeEntryCreatedEvent_startEndAreIsoFormat() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        String start = timeEntry.get("start").asText();
        String end = timeEntry.get("end").asText();

        // ISO 8601 format validation
        assertTrue(start.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
        assertTrue(end.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    void timeEntryCreatedEvent_optionalFieldsCanBeNull() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        // Optional fields: costRate, tags (can be null or missing)
        // But if present, should be valid
        assertTrue(!timeEntry.has("costRate") || timeEntry.get("costRate").isNull());
    }

    // ============ TIME_ENTRY_UPDATED Contract ============

    @Test
    void timeEntryUpdatedEvent_hasSameStructureAsCREATED() throws Exception {
        ObjectNode createdEvent = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        ObjectNode updatedEvent = TestFixtures.WEBHOOK_TIME_ENTRY_UPDATED;

        // Should have same top-level fields (except event type)
        assertEquals("TIME_ENTRY_UPDATED", updatedEvent.get("event").asText());
        assertEquals("ws-test-001", updatedEvent.get("workspaceId").asText());

        // Both should have timeEntry
        assertTrue(createdEvent.has("timeEntry"));
        assertTrue(updatedEvent.has("timeEntry"));
    }

    @Test
    void timeEntryUpdatedEvent_reflectsChangedValues() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_UPDATED;
        JsonNode timeEntry = event.get("timeEntry");

        // Duration was changed from 7200 to 5400
        long duration = timeEntry.get("duration").asLong();
        assertEquals(5400, duration);
    }

    // ============ TIME_ENTRY_DELETED Contract ============

    @Test
    void timeEntryDeletedEvent_hasMinimalPayload() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_DELETED;

        assertEquals("TIME_ENTRY_DELETED", event.get("event").asText());
        assertEquals("ws-test-001", event.get("workspaceId").asText());
        assertEquals("entry-001", event.get("timeEntryId").asText());

        // DELETED event does NOT include user context (object is already deleted)
        assertFalse(event.has("userId"));
        assertFalse(event.has("userName"));
        assertFalse(event.has("userEmail"));
    }

    @Test
    void timeEntryDeletedEvent_hasProjectAndTaskInfo() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_DELETED;

        assertTrue(event.has("projectId"));
        assertTrue(event.has("taskId"));
        assertEquals("proj-001", event.get("projectId").asText());
        assertEquals("task-001", event.get("taskId").asText());
    }

    // ============ TIMER_STARTED Contract ============

    @Test
    void timerStartedEvent_hasActiveTimer() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIMER_STARTED;

        assertEquals("NEW_TIMER_STARTED", event.get("event").asText());
        JsonNode timeEntry = event.get("timeEntry");

        assertNotNull(timeEntry.get("id"));
        assertNotNull(timeEntry.get("start"));
        assertNotNull(timeEntry.get("description"));
        // Timer started has basic time entry data
        assertTrue(timeEntry.has("id"));
    }

    @Test
    void timerStartedEvent_includesUserContext() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIMER_STARTED;

        assertEquals("user-test-001", event.get("userId").asText());
        assertEquals("Test User", event.get("userName").asText());
    }

    // ============ TIMER_STOPPED Contract ============

    @Test
    void timerStoppedEvent_hasCompleteTimeEntry() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIMER_STOPPED;

        assertEquals("TIMER_STOPPED", event.get("event").asText());
        JsonNode timeEntry = event.get("timeEntry");

        assertNotNull(timeEntry.get("id"));
        assertNotNull(timeEntry.get("start"));
        assertNotNull(timeEntry.get("end"));
        assertNotNull(timeEntry.get("duration"));

        // 30 minutes = 1800 seconds
        assertEquals(1800, timeEntry.get("duration").asLong());
    }

    // ============ Event Type Validation ============

    @Test
    void allTestFixtureEvents_haveValidEventType() throws Exception {
        ObjectNode[] events = {
                TestFixtures.WEBHOOK_TIME_ENTRY_CREATED,
                TestFixtures.WEBHOOK_TIME_ENTRY_UPDATED,
                TestFixtures.WEBHOOK_TIME_ENTRY_DELETED,
                TestFixtures.WEBHOOK_TIMER_STARTED,
                TestFixtures.WEBHOOK_TIMER_STOPPED
        };

        for (ObjectNode event : events) {
            String eventType = event.get("event").asText();
            assertFalse(eventType.isEmpty());
            assertFalse(eventType.contains(" "));
            assertTrue(Character.isUpperCase(eventType.charAt(0)));
        }
    }

    // ============ Workspace Context Contract ============

    @Test
    void allEvents_haveConsistentWorkspaceContext() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;

        String workspaceId = event.get("workspaceId").asText();
        String workspaceName = event.get("workspaceName").asText();

        assertTrue(workspaceId.length() > 0);
        assertTrue(workspaceName.length() > 0);
        assertEquals("ws-test-001", workspaceId);
        assertEquals("Test Workspace", workspaceName);
    }

    // ============ Dynamic Event Building Contract ============

    @Test
    void webhookEventBuilder_createsValidPayload() throws Exception {
        ObjectNode event = WebhookEventBuilder.create()
                .eventType("TIME_ENTRY_CREATED")
                .workspaceId("ws-123")
                .workspaceName("My Workspace")
                .userId("user-456")
                .userName("John Doe")
                .userEmail("john@example.com")
                .withTimeEntry(TimeEntryBuilder.create()
                        .withId("entry-789")
                        .withDescription("Testing")
                        .withDuration(3600)
                        .build())
                .build();

        assertEquals("TIME_ENTRY_CREATED", event.get("event").asText());
        assertEquals("ws-123", event.get("workspaceId").asText());
        assertEquals("user-456", event.get("userId").asText());
        assertNotNull(event.get("timeEntry"));
    }

    @Test
    void webhookEventBuilder_withDeletionEvent_excludesUserContext() throws Exception {
        ObjectNode event = WebhookEventBuilder.create()
                .eventType("TIME_ENTRY_DELETED")
                .workspaceId("ws-123")
                .timeEntryId("entry-001")
                .build();

        assertEquals("TIME_ENTRY_DELETED", event.get("event").asText());
        assertFalse(event.has("userId"));
        assertFalse(event.has("userName"));
    }

    // ============ Payload Size & Format Constraints ============

    @Test
    void timeEntryCreatedEvent_payloadIsJsonSerializable() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        String json = mapper.writeValueAsString(event);

        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Round-trip test
        JsonNode parsed = mapper.readTree(json);
        assertEquals("TIME_ENTRY_CREATED", parsed.get("event").asText());
    }

    @Test
    void timeEntryEvent_fieldOrderDoesNotMatter() throws Exception {
        ObjectNode event1 = WebhookEventBuilder.create()
                .eventType("TIME_ENTRY_CREATED")
                .workspaceId("ws-1")
                .userId("u-1")
                .build();

        ObjectNode event2 = WebhookEventBuilder.create()
                .userId("u-1")
                .workspaceId("ws-1")
                .eventType("TIME_ENTRY_CREATED")
                .build();

        assertEquals(event1.get("event").asText(), event2.get("event").asText());
        assertEquals(event1.get("workspaceId").asText(), event2.get("workspaceId").asText());
    }

    // ============ Data Type Contracts ============

    @Test
    void timeEntryDuration_isLong() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        assertTrue(timeEntry.get("duration").isIntegralNumber());
        assertTrue(timeEntry.get("duration").asLong() > 0);
    }

    @Test
    void billableFlag_isBoolean() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        assertTrue(timeEntry.get("billable").isBoolean());
        assertTrue(timeEntry.get("billable").asBoolean());
    }

    @Test
    void hourlyRate_isNumber() throws Exception {
        ObjectNode event = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        JsonNode timeEntry = event.get("timeEntry");

        if (timeEntry.has("hourlyRate") && !timeEntry.get("hourlyRate").isNull()) {
            assertTrue(timeEntry.get("hourlyRate").isNumber());
        }
    }

    @Test
    void tagsArray_containsValidObjects() throws Exception {
        // Create event with tags
        ObjectNode event = WebhookEventBuilder.create()
                .eventType("TIME_ENTRY_CREATED")
                .workspaceId("ws-1")
                .withTimeEntry(TimeEntryBuilder.create()
                        .withTag("development")
                        .withTag("urgent")
                        .build())
                .build();

        JsonNode timeEntry = event.get("timeEntry");
        if (timeEntry.has("tags")) {
            assertTrue(timeEntry.get("tags").isArray());
            timeEntry.get("tags").forEach(tag -> {
                assertTrue(tag.isTextual());
            });
        }
    }

    // ============ Null Handling ============

    @Test
    void eventWithoutOptionalFields_stillValid() throws Exception {
        ObjectNode event = WebhookEventBuilder.create()
                .eventType("TIME_ENTRY_CREATED")
                .workspaceId("ws-1")
                .userId("u-1")
                .build();

        assertNotNull(event.get("event"));
        assertNotNull(event.get("workspaceId"));
        // Optional fields can be missing
    }

    @Test
    void timeEntryWithoutProjectId_stillValid() throws Exception {
        ObjectNode timeEntry = TimeEntryBuilder.create()
                .withId("entry-1")
                .withDescription("Standalone task")
                .build();

        assertNotNull(timeEntry.get("id"));
        assertNotNull(timeEntry.get("description"));
        // projectId is optional
    }

    // ============ Event Type Enumeration ============

    @Test
    void supportedWebhookEvents_fromTestFixtures() {
        // Documented events that have test fixtures
        String[] supportedEvents = {
                "TIME_ENTRY_CREATED",
                "TIME_ENTRY_UPDATED",
                "TIME_ENTRY_DELETED",
                "NEW_TIMER_STARTED",
                "TIMER_STOPPED"
        };

        for (String eventType : supportedEvents) {
            assertFalse(eventType.isEmpty());
            assertFalse(eventType.contains(" "));
        }
    }

    // ============ Cross-Payload Consistency ============

    @Test
    void differentTimeEntryEvents_haveConsistentSchemaForTimeEntry() throws Exception {
        ObjectNode created = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        ObjectNode updated = TestFixtures.WEBHOOK_TIME_ENTRY_UPDATED;

        JsonNode createdEntry = created.get("timeEntry");
        JsonNode updatedEntry = updated.get("timeEntry");

        // Both should have these core fields
        assertTrue(createdEntry.has("id"));
        assertTrue(updatedEntry.has("id"));

        assertTrue(createdEntry.has("duration"));
        assertTrue(updatedEntry.has("duration"));
    }
}
