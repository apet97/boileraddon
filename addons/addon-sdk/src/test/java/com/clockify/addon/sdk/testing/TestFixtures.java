package com.clockify.addon.sdk.testing;

import com.clockify.addon.sdk.testing.builders.ManifestBuilder;
import com.clockify.addon.sdk.testing.builders.TimeEntryBuilder;
import com.clockify.addon.sdk.testing.builders.WebhookEventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Pre-configured test fixtures for common testing scenarios.
 * Provides realistic test data for webhooks, API responses, and addon configuration.
 *
 * Usage:
 * <pre>
 * ObjectNode webhook = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
 * ClockifyManifest manifest = TestFixtures.BASIC_MANIFEST;
 * </pre>
 */
public class TestFixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ============ Common Workspace/Addon Fixtures ============

    /**
     * Basic addon manifest with minimal configuration
     */
    public static final com.clockify.addon.sdk.ClockifyManifest BASIC_MANIFEST = ManifestBuilder.create()
            .key("test-addon")
            .name("Test Addon")
            .description("Test addon for integration testing")
            .baseUrl("http://localhost:8080/test-addon")
            .minimalSubscriptionPlan("FREE")
            .withScope("TIME_ENTRY_READ")
            .build();

    /**
     * Full-featured addon manifest with all scopes and webhooks
     */
    public static final com.clockify.addon.sdk.ClockifyManifest FULL_MANIFEST = ManifestBuilder.create()
            .key("full-test-addon")
            .name("Full Test Addon")
            .description("Test addon with all features")
            .baseUrl("http://localhost:8080/full-test-addon")
            .minimalSubscriptionPlan("FREE")
            .withScopes("TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "PROJECT_READ", "TAG_READ", "TAG_WRITE")
            .withWebhook("TIME_ENTRY_CREATED", "/webhook")
            .withWebhook("TIME_ENTRY_UPDATED", "/webhook")
            .withWebhook("TIME_ENTRY_DELETED", "/webhook")
            .build();

    // ============ Webhook Event Fixtures ============

    /**
     * TIME_ENTRY_CREATED webhook event
     */
    public static final ObjectNode WEBHOOK_TIME_ENTRY_CREATED =
            WebhookEventBuilder.create()
                    .eventType(WebhookEventBuilder.EventType.TIME_ENTRY_CREATED)
                    .workspaceId("ws-test-001")
                    .workspaceName("Test Workspace")
                    .userId("user-test-001")
                    .userName("Test User")
                    .userEmail("test@example.com")
                    .withTimeEntry(TimeEntryBuilder.create()
                            .withId("entry-001")
                            .withWorkspaceId("ws-test-001")
                            .withUserId("user-test-001")
                            .withDescription("Worked on feature implementation")
                            .withProjectId("proj-001")
                            .withProjectName("Feature Development")
                            .withTaskId("task-001")
                            .withTaskName("Backend API Development")
                            .withDuration(7200)
                            .billable()
                            .withHourlyRate(75.0)
                            .build())
                    .build();

    /**
     * TIME_ENTRY_UPDATED webhook event
     */
    public static final ObjectNode WEBHOOK_TIME_ENTRY_UPDATED =
            WebhookEventBuilder.create()
                    .eventType(WebhookEventBuilder.EventType.TIME_ENTRY_UPDATED)
                    .workspaceId("ws-test-001")
                    .workspaceName("Test Workspace")
                    .userId("user-test-001")
                    .userName("Test User")
                    .userEmail("test@example.com")
                    .withTimeEntry(TimeEntryBuilder.create()
                            .withId("entry-001")
                            .withDuration(5400) // Changed from 7200 to 5400
                            .build())
                    .build();

    /**
     * TIME_ENTRY_DELETED webhook event
     */
    public static final ObjectNode WEBHOOK_TIME_ENTRY_DELETED =
            WebhookEventBuilder.create()
                    .eventType(WebhookEventBuilder.EventType.TIME_ENTRY_DELETED)
                    .workspaceId("ws-test-001")
                    .workspaceName("Test Workspace")
                    .timeEntryId("entry-001")
                    .projectId("proj-001")
                    .taskId("task-001")
                    .build();

    /**
     * NEW_TIMER_STARTED webhook event (timer is running, no end time)
     */
    public static final ObjectNode WEBHOOK_TIMER_STARTED =
            WebhookEventBuilder.create()
                    .eventType(WebhookEventBuilder.EventType.TIMER_STARTED)
                    .workspaceId("ws-test-001")
                    .workspaceName("Test Workspace")
                    .userId("user-test-001")
                    .userName("Test User")
                    .userEmail("test@example.com")
                    .withTimeEntry(TimeEntryBuilder.create()
                            .withId("entry-002")
                            .withDescription("Working on urgent task")
                            .withProjectId("proj-001")
                            .withProjectName("Urgent Work")
                            .build())
                    .build();

    /**
     * TIMER_STOPPED webhook event
     */
    public static final ObjectNode WEBHOOK_TIMER_STOPPED =
            WebhookEventBuilder.create()
                    .eventType(WebhookEventBuilder.EventType.TIMER_STOPPED)
                    .workspaceId("ws-test-001")
                    .workspaceName("Test Workspace")
                    .userId("user-test-001")
                    .userName("Test User")
                    .userEmail("test@example.com")
                    .withTimeEntry(TimeEntryBuilder.create()
                            .withId("entry-002")
                            .withDuration(1800) // 30 minutes
                            .build())
                    .build();

    // ============ Time Entry Fixtures ============

    /**
     * Standard billable time entry
     */
    public static final ObjectNode TIME_ENTRY_BILLABLE = TimeEntryBuilder.create()
            .withId("entry-billable-001")
            .withWorkspaceId("ws-test-001")
            .withUserId("user-test-001")
            .withDescription("Billable work")
            .withProjectId("proj-001")
            .withProjectName("Client Project")
            .withDuration(3600)
            .billable()
            .withHourlyRate(100.0)
            .build();

    /**
     * Non-billable time entry (internal work)
     */
    public static final ObjectNode TIME_ENTRY_NON_BILLABLE = TimeEntryBuilder.create()
            .withId("entry-non-billable-001")
            .withWorkspaceId("ws-test-001")
            .withUserId("user-test-001")
            .withDescription("Internal meeting")
            .withProjectId("proj-002")
            .withProjectName("Internal")
            .withDuration(1800)
            .notBillable()
            .build();

    /**
     * Time entry with multiple tags
     */
    public static final ObjectNode TIME_ENTRY_WITH_TAGS = TimeEntryBuilder.create()
            .withId("entry-tagged-001")
            .withWorkspaceId("ws-test-001")
            .withDescription("Feature implementation")
            .withDuration(5400)
            .withTag("development")
            .withTag("sprint-1")
            .withTag("backend")
            .billable()
            .build();

    // ============ Error Response Fixtures ============

    /**
     * 401 Unauthorized response (invalid token)
     */
    public static final ObjectNode ERROR_401_UNAUTHORIZED = MAPPER.createObjectNode()
            .put("message", "Invalid token")
            .put("statusCode", 401)
            .put("errorCode", "INVALID_TOKEN");

    /**
     * 403 Forbidden response (insufficient permissions)
     */
    public static final ObjectNode ERROR_403_FORBIDDEN = MAPPER.createObjectNode()
            .put("message", "Insufficient permissions")
            .put("statusCode", 403)
            .put("errorCode", "INSUFFICIENT_PERMISSIONS");

    /**
     * 404 Not Found response (resource not found)
     */
    public static final ObjectNode ERROR_404_NOT_FOUND = MAPPER.createObjectNode()
            .put("message", "Resource not found")
            .put("statusCode", 404)
            .put("errorCode", "NOT_FOUND");

    /**
     * 429 Rate Limited response (too many requests)
     */
    public static final ObjectNode ERROR_429_RATE_LIMITED = MAPPER.createObjectNode()
            .put("message", "Rate limit exceeded")
            .put("statusCode", 429)
            .put("errorCode", "RATE_LIMITED")
            .put("retryAfter", 60);

    /**
     * 500 Internal Server Error response
     */
    public static final ObjectNode ERROR_500_SERVER_ERROR = MAPPER.createObjectNode()
            .put("message", "Internal server error")
            .put("statusCode", 500)
            .put("errorCode", "INTERNAL_ERROR");

    // ============ Helper Methods ============

    /**
     * Create a copy of a fixture (to avoid modifying shared instances)
     */
    public static ObjectNode copy(ObjectNode node) {
        return node.deepCopy();
    }

    /**
     * Create a webhook event with custom parameters
     */
    public static ObjectNode createWebhookEvent(String eventType, String workspaceId, ObjectNode payload) {
        ObjectNode event = MAPPER.createObjectNode();
        event.put("event", eventType);
        event.put("workspaceId", workspaceId);
        event.put("workspaceName", "Test Workspace");
        if (payload != null) {
            event.setAll((ObjectNode) payload);
        }
        return event;
    }

    /**
     * Create a time entry with custom parameters
     */
    public static ObjectNode createTimeEntry(String description, long duration) {
        return TimeEntryBuilder.create()
                .withDescription(description)
                .withDuration(duration)
                .build();
    }
}
