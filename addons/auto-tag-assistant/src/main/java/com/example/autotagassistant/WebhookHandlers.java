package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyAddon;
import com.example.autotagassistant.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

/**
 * Handles Clockify webhook events for time entries.
 *
 * This addon listens to:
 * - NEW_TIMER_STARTED: User starts a new timer
 * - TIMER_STOPPED: User stops a running timer
 * - NEW_TIME_ENTRY: A new time entry is created
 * - TIME_ENTRY_UPDATED: An existing time entry is modified
 *
 * Auto-tagging logic:
 * 1. Receive webhook event with time entry data
 * 2. Check if time entry has tags
 * 3. If missing tags, analyze project/task/description
 * 4. Suggest or auto-apply appropriate tags
 * 5. Use stored addon token to call Clockify API
 */
public class WebhookHandlers {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        // Register handlers for all time entry events
        String[] events = {
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "NEW_TIME_ENTRY",
            "TIME_ENTRY_UPDATED"
        };

        for (String event : events) {
            addon.registerWebhookHandler(event, request -> {
                try {
                    JsonNode payload = parseRequestBody(request);
                    String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText() : "unknown";
                    String eventType = payload.has("event") ? payload.get("event").asText() : event;

                    System.out.println("\n" + "=".repeat(80));
                    System.out.println("WEBHOOK EVENT: " + eventType);
                    System.out.println("=".repeat(80));
                    System.out.println("Workspace ID: " + workspaceId);
                    System.out.println("Event Type: " + eventType);
                    System.out.println("Payload:");
                    System.out.println(payload.toPrettyString());
                    System.out.println("=".repeat(80));

                    // Extract time entry information from payload
                    JsonNode timeEntry = payload.has("timeEntry")
                        ? payload.get("timeEntry")
                        : payload;

                    String timeEntryId = timeEntry.has("id")
                        ? timeEntry.get("id").asText()
                        : "unknown";

                    String description = timeEntry.has("description")
                        ? timeEntry.get("description").asText()
                        : "";

                    // Check if time entry has tags
                    boolean hasTags = timeEntry.has("tagIds")
                        && timeEntry.get("tagIds").isArray()
                        && timeEntry.get("tagIds").size() > 0;

                    System.out.println("\n📋 Auto-Tag Assistant Analysis:");
                    System.out.println("  Time Entry ID: " + timeEntryId);
                    System.out.println("  Description: " + (description.isEmpty() ? "(empty)" : description));
                    System.out.println("  Has Tags: " + (hasTags ? "Yes ✓" : "No ✗"));

                    if (!hasTags) {
                        System.out.println("\n⚠️  MISSING TAGS DETECTED!");
                        System.out.println("  🤖 Auto-tagging logic would run here:");
                        System.out.println("     1. Analyze description: \"" + description + "\"");
                        System.out.println("     2. Check project/task context");
                        System.out.println("     3. Query historical tagging patterns");
                        System.out.println("     4. Suggest tags: [meeting, client-work, development]");
                        System.out.println("     5. Apply tags via Clockify API");
                        System.out.println();
                        System.out.println("  📝 To implement:");
                        System.out.println("     - Edit WebhookHandlers.java");
                        System.out.println("     - Use ClockifyApiClient.java to call:");
                        System.out.println("       PUT /workspaces/{workspaceId}/time-entries/{timeEntryId}");
                        System.out.println("     - Include Authorization header with addon token");
                        System.out.println();

                        // Simulate tag suggestion logic
                        suggestTagsForTimeEntry(workspaceId, timeEntryId, description);
                    } else {
                        System.out.println("  ✓ Time entry already has tags, no action needed");
                    }

                    System.out.println("=".repeat(80) + "\n");

                    return HttpResponse.ok("Webhook processed");

                } catch (Exception e) {
                    System.err.println("Error handling webhook: " + e.getMessage());
                    e.printStackTrace();
                    return HttpResponse.error(500, "Failed to process webhook: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Simulates tag suggestion logic based on time entry description.
     * In a real implementation, this would:
     * 1. Use NLP or keyword matching to analyze description
     * 2. Query workspace tags via Clockify API
     * 3. Find best matching tags
     * 4. Apply tags via API using stored addon token
     */
    private static void suggestTagsForTimeEntry(String workspaceId, String timeEntryId, String description) {
        System.out.println("  🏷️  Suggested Tags (based on description analysis):");

        // Simple keyword-based suggestions (replace with real logic)
        if (description.toLowerCase().contains("meeting")) {
            System.out.println("     - 'meeting' (keyword match)");
        }
        if (description.toLowerCase().contains("bug") || description.toLowerCase().contains("fix")) {
            System.out.println("     - 'bugfix' (keyword match)");
        }
        if (description.toLowerCase().contains("review")) {
            System.out.println("     - 'code-review' (keyword match)");
        }
        if (description.toLowerCase().contains("client")) {
            System.out.println("     - 'client-work' (keyword match)");
        }

        System.out.println();
        System.out.println("  💡 Implementation Plan:");
        System.out.println("     1. Retrieve addon token for workspace: " + workspaceId);
        System.out.println("     2. GET /workspaces/{workspaceId}/tags to get available tags");
        System.out.println("     3. Match suggested tags to actual tag IDs");
        System.out.println("     4. PUT /workspaces/{workspaceId}/time-entries/{timeEntryId}");
        System.out.println("        with body: { \"tagIds\": [\"tag-id-1\", \"tag-id-2\"] }");
        System.out.println("     5. Handle API errors gracefully");
    }

    private static JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return objectMapper.readTree(sb.toString());
    }
}
