package com.clockify.addon.sdk.testing.builders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

/**
 * Fluent builder for creating test webhook event payloads.
 * Supports all Clockify webhook event types.
 *
 * Usage:
 * <pre>
 * ObjectNode webhook = WebhookEventBuilder.create()
 *     .eventType("TIME_ENTRY_CREATED")
 *     .workspaceId("ws-123")
 *     .withTimeEntry(timeEntryBuilder.build())
 *     .build();
 * </pre>
 */
public class WebhookEventBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum EventType {
        TIME_ENTRY_CREATED("TIME_ENTRY_CREATED"),
        TIME_ENTRY_UPDATED("TIME_ENTRY_UPDATED"),
        TIME_ENTRY_DELETED("TIME_ENTRY_DELETED"),
        TIMER_STARTED("NEW_TIMER_STARTED"),
        TIMER_STOPPED("TIMER_STOPPED"),
        PROJECT_CREATED("PROJECT_CREATED"),
        PROJECT_UPDATED("PROJECT_UPDATED"),
        PROJECT_DELETED("PROJECT_DELETED"),
        TASK_CREATED("TASK_CREATED"),
        TASK_UPDATED("TASK_UPDATED"),
        TASK_DELETED("TASK_DELETED"),
        CLIENT_CREATED("CLIENT_CREATED"),
        CLIENT_UPDATED("CLIENT_UPDATED"),
        CLIENT_DELETED("CLIENT_DELETED"),
        USER_JOINED("USER_JOINED");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private EventType eventType = EventType.TIME_ENTRY_CREATED;
    private String workspaceId = "ws-test";
    private String workspaceName = "Test Workspace";
    private String userId = "user-123";
    private String userName = "Test User";
    private String userEmail = "test@example.com";
    private ObjectNode timeEntry = null;
    private ObjectNode project = null;
    private ObjectNode task = null;
    private ObjectNode client = null;
    private ObjectNode user = null;
    private String timeEntryId = null;
    private String projectId = null;
    private String taskId = null;
    private String clientId = null;

    /**
     * Create a new WebhookEventBuilder
     */
    public static WebhookEventBuilder create() {
        return new WebhookEventBuilder();
    }

    /**
     * Set the event type
     */
    public WebhookEventBuilder eventType(EventType type) {
        this.eventType = type;
        return this;
    }

    /**
     * Set the event type by string value
     */
    public WebhookEventBuilder eventType(String type) {
        this.eventType = EventType.valueOf(type);
        return this;
    }

    /**
     * Set workspace ID
     */
    public WebhookEventBuilder workspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    /**
     * Set workspace name
     */
    public WebhookEventBuilder workspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    /**
     * Set user ID
     */
    public WebhookEventBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Set user name
     */
    public WebhookEventBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Set user email
     */
    public WebhookEventBuilder userEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    /**
     * Set the time entry object
     */
    public WebhookEventBuilder withTimeEntry(ObjectNode timeEntry) {
        this.timeEntry = timeEntry;
        return this;
    }

    /**
     * Set the project object
     */
    public WebhookEventBuilder withProject(ObjectNode project) {
        this.project = project;
        return this;
    }

    /**
     * Set the task object
     */
    public WebhookEventBuilder withTask(ObjectNode task) {
        this.task = task;
        return this;
    }

    /**
     * Set the client object
     */
    public WebhookEventBuilder withClient(ObjectNode client) {
        this.client = client;
        return this;
    }

    /**
     * Set the user object
     */
    public WebhookEventBuilder withUser(ObjectNode user) {
        this.user = user;
        return this;
    }

    /**
     * Set time entry ID (for deletion events)
     */
    public WebhookEventBuilder timeEntryId(String timeEntryId) {
        this.timeEntryId = timeEntryId;
        return this;
    }

    /**
     * Set project ID (for deletion events)
     */
    public WebhookEventBuilder projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    /**
     * Set task ID (for deletion events)
     */
    public WebhookEventBuilder taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    /**
     * Set client ID (for deletion events)
     */
    public WebhookEventBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * Build the webhook event payload
     */
    public ObjectNode build() {
        ObjectNode event = MAPPER.createObjectNode();

        // Add standard fields
        event.put("event", eventType.getValue());
        event.put("workspaceId", workspaceId);
        event.put("workspaceName", workspaceName);

        // Add user context for non-deletion events
        if (!eventType.getValue().endsWith("_DELETED")) {
            event.put("userId", userId);
            event.put("userName", userName);
            event.put("userEmail", userEmail);
        }

        // Add event-specific payloads
        if (timeEntry != null) {
            event.set("timeEntry", timeEntry);
        }
        if (timeEntryId != null) {
            event.put("timeEntryId", timeEntryId);
        }

        if (project != null) {
            event.set("project", project);
        }
        if (projectId != null) {
            event.put("projectId", projectId);
        }

        if (task != null) {
            event.set("task", task);
        }
        if (taskId != null) {
            event.put("taskId", taskId);
        }

        if (client != null) {
            event.set("client", client);
        }
        if (clientId != null) {
            event.put("clientId", clientId);
        }

        if (user != null) {
            event.set("user", user);
        }

        return event;
    }

    /**
     * Build the webhook event as JSON string
     */
    public String buildAsString() {
        return build().toString();
    }
}
