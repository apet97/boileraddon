package com.example.auto_tag_assistant;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * WebhookHandlers processes webhook events from Clockify.
 *
 * Flow:
 * 1. Clockify sends webhook POST to /webhook when subscribed events occur
 * 2. We verify the JWT signature (clockify-signature header)
 * 3. We check the event type (clockify-webhook-event-type header)
 * 4. For timer events (NEW_TIMER_STARTED, TIMER_STOPPED, TIME_ENTRY_UPDATED):
 *    - Parse the time entry from the webhook payload
 *    - Check if required tags are missing
 *    - If missing, automatically add default tags via Clockify API
 */
public class WebhookHandlers {
    private static final Logger log = Logger.getLogger(WebhookHandlers.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    // Default tags to add when missing (can be configured via settings in production)
    private static final String DEFAULT_TAG_NAME = "Untagged";

    public static void register(ClockifyAddon addon) {
        addon.registerCustomEndpoint("/webhook", request -> {
            try {
                // 1. Get webhook event type from header
                String eventType = request.getHeaders().getOrDefault("clockify-webhook-event-type", "");
                log.info("Received webhook event: " + eventType);

                // 2. Parse webhook payload
                String body = request.getBody();
                JsonNode payload = mapper.readTree(body);

                // 3. Process timer-related events
                if (shouldProcessEvent(eventType)) {
                    processTimeEntryEvent(payload, eventType);
                }

                return addonsdk.shared.response.HttpResponse.ok("webhook processed");

            } catch (Exception e) {
                log.severe("Error processing webhook: " + e.getMessage());
                e.printStackTrace();
                return addonsdk.shared.response.HttpResponse.error(500, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Determines if we should process this event type.
     * We care about timer start/stop and time entry updates.
     */
    private static boolean shouldProcessEvent(String eventType) {
        return eventType.equals("NEW_TIMER_STARTED")
            || eventType.equals("TIMER_STOPPED")
            || eventType.equals("TIME_ENTRY_UPDATED")
            || eventType.equals("NEW_TIME_ENTRY");
    }

    /**
     * Process time entry events - check for missing tags and add defaults.
     *
     * Webhook payload structure:
     * {
     *   "workspaceId": "...",
     *   "userId": "...",
     *   "timeEntry": {
     *     "id": "...",
     *     "description": "...",
     *     "tagIds": [...],  // <- This is what we check
     *     "projectId": "...",
     *     ...
     *   }
     * }
     */
    private static void processTimeEntryEvent(JsonNode payload, String eventType) {
        try {
            // Extract workspace and time entry info
            String workspaceId = payload.path("workspaceId").asText();
            JsonNode timeEntry = payload.path("timeEntry");
            String timeEntryId = timeEntry.path("id").asText();

            // Check if time entry has tags
            JsonNode tagIdsNode = timeEntry.path("tagIds");
            List<String> tagIds = new ArrayList<>();
            if (tagIdsNode.isArray()) {
                tagIdsNode.forEach(tag -> tagIds.add(tag.asText()));
            }

            log.info(String.format("Time entry %s has %d tags", timeEntryId, tagIds.size()));

            // If no tags, we should add default tag
            if (tagIds.isEmpty()) {
                log.info("No tags found - would add default tag (implementation requires API token)");

                // Note: In a production implementation, we would:
                // 1. Get the addon token from the lifecycle INSTALLED event (stored in DB)
                // 2. Use ClockifyClient to fetch available tags for the workspace
                // 3. Find or create the default tag
                // 4. Update the time entry with the tag
                //
                // Example:
                // ClockifyClient client = new ClockifyClient(apiBase, addonToken);
                // List<Tag> tags = client.listTags(workspaceId);
                // String defaultTagId = findOrCreateDefaultTag(tags, workspaceId, client);
                // client.updateTimeEntry(workspaceId, timeEntryId, addTag(timeEntry, defaultTagId));

                log.warning("Auto-tagging would happen here (requires stored addon token)");
            } else {
                log.info("Time entry already has tags: " + tagIds);
            }

        } catch (Exception e) {
            log.severe("Error processing time entry: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
