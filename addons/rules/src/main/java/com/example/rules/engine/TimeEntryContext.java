package com.example.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Context object containing time entry data for rule evaluation.
 */
public class TimeEntryContext {

    private final JsonNode timeEntry;

    public TimeEntryContext(JsonNode timeEntry) {
        this.timeEntry = timeEntry;
    }

    public String getDescription() {
        JsonNode descNode = timeEntry.path("description");
        return descNode.isTextual() ? descNode.asText() : "";
    }

    public String getProjectId() {
        JsonNode projectNode = timeEntry.path("projectId");
        return projectNode.isTextual() ? projectNode.asText() : null;
    }

    public List<String> getTagIds() {
        List<String> tagIds = new ArrayList<>();
        JsonNode tagsNode = timeEntry.path("tagIds");
        if (tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                if (tagNode.isTextual()) {
                    tagIds.add(tagNode.asText());
                }
            }
        }
        return tagIds;
    }

    public boolean isBillable() {
        JsonNode billableNode = timeEntry.path("billable");
        return billableNode.isBoolean() && billableNode.asBoolean();
    }

    public JsonNode getTimeEntry() {
        return timeEntry;
    }
}
