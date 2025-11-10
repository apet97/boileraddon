package com.clockify.addon.sdk.testing.builders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating test TimeEntry objects.
 * Provides sensible defaults for testing webhook payloads and API responses.
 *
 * Usage:
 * <pre>
 * ObjectNode entry = TimeEntryBuilder.create()
 *     .withWorkspaceId("ws-123")
 *     .withDescription("Test task")
 *     .withDuration(3600)
 *     .billable()
 *     .build();
 * </pre>
 */
public class TimeEntryBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String id = "entry-" + System.currentTimeMillis();
    private String workspaceId = "ws-test";
    private String userId = "user-123";
    private String description = "Test time entry";
    private String projectId = "proj-456";
    private String projectName = "Test Project";
    private String taskId = "task-789";
    private String taskName = "Test Task";
    private long startMillis = System.currentTimeMillis();
    private long endMillis = startMillis + 3600000; // 1 hour by default
    private long durationSeconds = 3600;
    private List<String> tags = new ArrayList<>();
    private boolean billable = true;
    private Double hourlyRate = 75.0;
    private String createdAt = Instant.now().toString();
    private String updatedAt = Instant.now().toString();

    /**
     * Create a new TimeEntryBuilder with defaults
     */
    public static TimeEntryBuilder create() {
        return new TimeEntryBuilder();
    }

    /**
     * Set the entry ID
     */
    public TimeEntryBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Set the workspace ID
     */
    public TimeEntryBuilder withWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    /**
     * Set the user ID
     */
    public TimeEntryBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Set the entry description
     */
    public TimeEntryBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the project ID
     */
    public TimeEntryBuilder withProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    /**
     * Set the project name
     */
    public TimeEntryBuilder withProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    /**
     * Set the task ID
     */
    public TimeEntryBuilder withTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    /**
     * Set the task name
     */
    public TimeEntryBuilder withTaskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    /**
     * Set start time (milliseconds since epoch)
     */
    public TimeEntryBuilder withStartMillis(long startMillis) {
        this.startMillis = startMillis;
        return this;
    }

    /**
     * Set end time (milliseconds since epoch)
     */
    public TimeEntryBuilder withEndMillis(long endMillis) {
        this.endMillis = endMillis;
        this.durationSeconds = (endMillis - startMillis) / 1000;
        return this;
    }

    /**
     * Set duration in seconds
     */
    public TimeEntryBuilder withDuration(long durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.endMillis = startMillis + (durationSeconds * 1000);
        return this;
    }

    /**
     * Add a tag to the entry
     */
    public TimeEntryBuilder withTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    /**
     * Mark entry as billable
     */
    public TimeEntryBuilder billable() {
        this.billable = true;
        return this;
    }

    /**
     * Mark entry as not billable
     */
    public TimeEntryBuilder notBillable() {
        this.billable = false;
        return this;
    }

    /**
     * Set hourly rate
     */
    public TimeEntryBuilder withHourlyRate(Double rate) {
        this.hourlyRate = rate;
        return this;
    }

    /**
     * Build the TimeEntry as a JSON object
     */
    public ObjectNode build() {
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put("id", id);
        entry.put("description", description);
        entry.put("start", Instant.ofEpochMilli(startMillis).toString());
        entry.put("end", Instant.ofEpochMilli(endMillis).toString());
        entry.put("duration", durationSeconds);
        entry.put("projectId", projectId);
        entry.put("projectName", projectName);
        entry.put("taskId", taskId);
        entry.put("taskName", taskName);
        entry.put("billable", billable);
        if (hourlyRate != null) {
            entry.put("hourlyRate", hourlyRate);
        }
        entry.put("createdAt", createdAt);
        entry.put("updatedAt", updatedAt);

        // Add tags array
        if (!tags.isEmpty()) {
            var tagsArray = entry.putArray("tags");
            for (String tag : tags) {
                tagsArray.add(tag);
            }
        }

        return entry;
    }

    /**
     * Build the TimeEntry as JSON string
     */
    public String buildAsString() {
        return build().toString();
    }
}
