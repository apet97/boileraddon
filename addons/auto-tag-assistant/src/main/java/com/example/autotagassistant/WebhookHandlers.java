package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyAddon;
import com.example.autotagassistant.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 * 5. Use stored auth token to call Clockify API
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
                    String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                    String workspaceDisplayId = workspaceId != null ? workspaceId : "unknown";
                    String eventType = payload.has("event") ? payload.get("event").asText() : event;

                    System.out.println("\n" + "=".repeat(80));
                    System.out.println("WEBHOOK EVENT: " + eventType);
                    System.out.println("=".repeat(80));
                    System.out.println("Workspace ID: " + workspaceDisplayId);
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

                    List<String> tagIds = extractTagIds(timeEntry);
                    List<String> tagNames = extractTagNames(timeEntry);
                    List<String> formattedTags = extractFormattedTags(timeEntry, tagIds, tagNames);

                    boolean hasTags = !tagIds.isEmpty()
                        || !tagNames.isEmpty()
                        || !formattedTags.isEmpty();

                    System.out.println("\n📋 Auto-Tag Assistant Analysis:");
                    System.out.println("  Time Entry ID: " + timeEntryId);
                    System.out.println("  Description: " + (description.isEmpty() ? "(empty)" : description));
                    System.out.println("  Has Tags: " + (hasTags ? "Yes ✓" : "No ✗"));
                    if (hasTags) {
                        System.out.println("  Tags: " + formatTagSummary(formattedTags, tagNames, tagIds));
                        if (!tagIds.isEmpty()) {
                            System.out.println("    IDs: " + String.join(", ", tagIds));
                        }
                        if (!tagNames.isEmpty()) {
                            System.out.println("    Names: " + String.join(", ", tagNames));
                        }
                    } else {
                        System.out.println("  Tags: (none)");
                    }

                    HttpResponse response;

                    if (!hasTags) {
                        System.out.println("\n⚠️  MISSING TAGS DETECTED!");

                        TagSuggestionResult suggestionResult = suggestTagsForTimeEntry(workspaceId, timeEntryId, description, timeEntry);
                        List<String> candidateTagNames = suggestionResult.getTagNames();

                        if (candidateTagNames.isEmpty()) {
                            System.out.println("  ❌ No tag suggestions available for this time entry.");
                            response = skipResponse("No tag suggestions available for this time entry.");
                        } else {
                            Optional<TokenStore.WorkspaceToken> workspaceToken = TokenStore.get(workspaceId);
                            if (workspaceToken.isEmpty()) {
                                String message = "Missing stored auth token/API base URL for workspace " + workspaceId;
                                System.err.println("❌ " + message);
                                response = errorResponse(500, message);
                            } else {
                                TokenStore.WorkspaceToken token = workspaceToken.get();
                                ClockifyApiClient apiClient = new ClockifyApiClient(token.apiBaseUrl(), token.authToken());

                                try {
                                    TagUpdateResult updateResult = applySuggestedTags(apiClient, workspaceId, timeEntryId, candidateTagNames);

                                    if (updateResult.getTagIdsByName().isEmpty()) {
                                        System.out.println("  ℹ️  Suggestions did not resolve to any tags.");
                                        response = skipResponse("Suggestions did not resolve to any tags.");
                                    } else {
                                        logSuccessfulUpdate(timeEntryId, updateResult);
                                        response = successResponse("Applied tags to time entry " + timeEntryId, updateResult);
                                    }
                                } catch (Exception apiError) {
                                    String message = "Failed to update time entry tags: " + apiError.getMessage();
                                    System.err.println("❌ " + message);
                                    apiError.printStackTrace();
                                    response = errorResponse(500, message);
                                }
                            }
                        }
                    } else {
                        System.out.println("  ✓ Time entry already has tags, no action needed");
                        response = skipResponse("Time entry already had tags; no changes applied.");
                    }

                    System.out.println("=".repeat(80) + "\n");

                    return response;

                } catch (Exception e) {
                    System.err.println("Error handling webhook: " + e.getMessage());
                    e.printStackTrace();
                    return errorResponse(500, "Failed to process webhook: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Suggest tag names based on the time entry description and other details.
     */
    private static TagSuggestionResult suggestTagsForTimeEntry(String workspaceId, String timeEntryId, String description, JsonNode timeEntry) {
        System.out.println("  🤖 Auto-tagging analysis for workspace " + workspaceId + ":");

        String normalizedDescription = description == null ? "" : description.toLowerCase(Locale.ROOT);
        List<TagSuggestion> suggestions = new ArrayList<>();
        Set<String> seenSuggestionNames = new LinkedHashSet<>();

        if (!normalizedDescription.isEmpty()) {
            if (normalizedDescription.contains("meeting")) {
                addSuggestion(suggestions, seenSuggestionNames, "meeting", "Description contains 'meeting'.");
            }
            if (normalizedDescription.contains("bug") || normalizedDescription.contains("fix")) {
                addSuggestion(suggestions, seenSuggestionNames, "bugfix", "Description references a bug or fix.");
            }
            if (normalizedDescription.contains("review")) {
                addSuggestion(suggestions, seenSuggestionNames, "code-review", "Description references a review.");
            }
            if (normalizedDescription.contains("client")) {
                addSuggestion(suggestions, seenSuggestionNames, "client-work", "Description references a client.");
            }
        }

        if (timeEntry != null && timeEntry.has("projectName")) {
            String projectName = timeEntry.get("projectName").asText("").trim();
            if (!projectName.isEmpty()) {
                addSlugSuggestion(suggestions, seenSuggestionNames, projectName, "Derived from project name.");
            }
        }

        if (timeEntry != null) {
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("project").path("name")),
                "Derived from project.name.");
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("project").path("clientName")),
                "Derived from project.clientName.");
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("project").path("client").path("name")),
                "Derived from project.client.name.");
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("client").path("name")),
                "Derived from client.name.");
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("task").path("name")),
                "Derived from task.name.");
            addSlugSuggestion(suggestions, seenSuggestionNames, extractText(timeEntry.path("project").path("task").path("name")),
                "Derived from project.task.name.");
        }

        TagSuggestionResult result = new TagSuggestionResult(suggestions);

        if (result.getSuggestions().isEmpty()) {
            System.out.println("  💤 No keyword-based tag suggestions found.");
        } else {
            System.out.println("  🏷️  Suggested Tags:");
            for (TagSuggestion suggestion : result.getSuggestions()) {
                System.out.println("     - '" + suggestion.getName() + "' (" + suggestion.getReason() + ")");
            }
        }

        return result;
    }

    private static void addSuggestion(List<TagSuggestion> suggestions, Set<String> seenSuggestionNames, String name, String reason) {
        String normalized = normalizeTagName(name);
        if (normalized == null || !seenSuggestionNames.add(normalized)) {
            return;
        }
        suggestions.add(new TagSuggestion(name, reason));
    }

    private static void addSlugSuggestion(List<TagSuggestion> suggestions, Set<String> seenSuggestionNames, String rawValue, String reason) {
        String slug = slugify(rawValue);
        if (slug != null) {
            addSuggestion(suggestions, seenSuggestionNames, slug, reason);
        }
    }

    private static String extractText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText(null);
        return text != null ? text.trim() : null;
    }

    private static String slugify(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String slug = trimmed.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+", "")
            .replaceAll("-+$", "");

        return slug.isEmpty() ? null : slug;
    }

    private static TagUpdateResult applySuggestedTags(ClockifyApiClient apiClient, String workspaceId, String timeEntryId, List<String> candidateTagNames) throws Exception {
        JsonNode existingTagsNode = apiClient.getTags(workspaceId);
        Map<String, String> tagsByName = mapTagsByNormalizedName(existingTagsNode);
        Set<String> seenNames = new LinkedHashSet<>();
        LinkedHashMap<String, String> resolvedTags = new LinkedHashMap<>();
        List<String> createdTags = new ArrayList<>();

        for (String candidate : candidateTagNames) {
            String normalized = normalizeTagName(candidate);
            if (normalized == null || !seenNames.add(normalized)) {
                continue;
            }

            String tagId = tagsByName.get(normalized);
            if (tagId == null) {
                JsonNode createdTag = apiClient.createTag(workspaceId, candidate);
                if (createdTag == null || !createdTag.has("id")) {
                    throw new IllegalStateException("Clockify API did not return a tag ID for '" + candidate + "'.");
                }
                tagId = createdTag.get("id").asText();
                tagsByName.put(normalized, tagId);
                createdTags.add(candidate);
                System.out.println("  ➕ Created tag '" + candidate + "' with ID " + tagId);
            }

            if (tagId != null && !tagId.isBlank()) {
                resolvedTags.put(candidate, tagId);
            }
        }

        if (resolvedTags.isEmpty()) {
            return new TagUpdateResult(resolvedTags, createdTags, null);
        }

        String[] tagIds = resolvedTags.values().toArray(new String[0]);
        JsonNode updatedEntry = apiClient.updateTimeEntryTags(workspaceId, timeEntryId, tagIds);

        return new TagUpdateResult(resolvedTags, createdTags, updatedEntry);
    }

    private static Map<String, String> mapTagsByNormalizedName(JsonNode tagsNode) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                if (tag != null && tag.has("name") && tag.has("id")) {
                    String normalized = normalizeTagName(tag.get("name").asText());
                    String id = tag.get("id").asText();
                    if (normalized != null && id != null && !normalized.isEmpty() && !id.isEmpty() && !tags.containsKey(normalized)) {
                        tags.put(normalized, id);
                    }
                }
            }
        }
        return tags;
    }

    private static String normalizeTagName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static void logSuccessfulUpdate(String timeEntryId, TagUpdateResult updateResult) {
        Map<String, String> appliedMap = updateResult.getTagIdsByName();
        List<String> appliedNames = new ArrayList<>(appliedMap.keySet());
        List<String> appliedIds = new ArrayList<>(appliedMap.values());

        System.out.println("  ✅ Applied tags to time entry " + timeEntryId + ": " + String.join(", ", appliedNames));
        System.out.println("  ✅ Tag IDs: " + String.join(", ", appliedIds));

        if (!updateResult.getCreatedTagNames().isEmpty()) {
            System.out.println("  🆕 Created tags during update: " + String.join(", ", updateResult.getCreatedTagNames()));
        }
    }

    private static HttpResponse successResponse(String message, TagUpdateResult result) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("status", "success");
        body.put("message", message);

        ArrayNode appliedArray = body.putArray("appliedTags");
        for (Map.Entry<String, String> entry : result.getTagIdsByName().entrySet()) {
            ObjectNode tagNode = appliedArray.addObject();
            tagNode.put("name", entry.getKey());
            tagNode.put("id", entry.getValue());
        }

        if (!result.getCreatedTagNames().isEmpty()) {
            ArrayNode createdArray = body.putArray("createdTags");
            for (String name : result.getCreatedTagNames()) {
                createdArray.add(name);
            }
        }

        if (result.getUpdatedEntry() != null) {
            body.set("timeEntry", result.getUpdatedEntry());
        }

        return HttpResponse.ok(body.toString(), "application/json");
    }

    private static HttpResponse skipResponse(String message) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("status", "skipped");
        body.put("message", message);
        return HttpResponse.ok(body.toString(), "application/json");
    }

    private static HttpResponse errorResponse(int statusCode, String message) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("status", "error");
        body.put("message", message);
        return HttpResponse.error(statusCode, body.toString(), "application/json");
    }

    private static List<String> extractTagIds(JsonNode timeEntry) {
        List<String> tagIds = new ArrayList<>();

        if (timeEntry.has("tagIds") && timeEntry.get("tagIds").isArray()) {
            for (JsonNode tagIdNode : timeEntry.get("tagIds")) {
                String id = tagIdNode.asText(null);
                if (id != null && !id.isBlank() && !tagIds.contains(id)) {
                    tagIds.add(id);
                }
            }
        }

        if (timeEntry.has("tags") && timeEntry.get("tags").isArray()) {
            for (JsonNode tagNode : timeEntry.get("tags")) {
                String id = null;
                if (tagNode.has("id")) {
                    id = tagNode.get("id").asText();
                } else if (tagNode.isTextual()) {
                    // Some payloads may represent tags as strings (names or IDs)
                    id = tagNode.asText();
                }

                if (id != null && !id.isBlank() && !tagIds.contains(id)) {
                    tagIds.add(id);
                }
            }
        }

        return tagIds;
    }

    private static List<String> extractTagNames(JsonNode timeEntry) {
        List<String> tagNames = new ArrayList<>();

        if (timeEntry.has("tagNames") && timeEntry.get("tagNames").isArray()) {
            for (JsonNode tagNameNode : timeEntry.get("tagNames")) {
                String name = tagNameNode.asText(null);
                if (name != null && !name.isBlank() && !tagNames.contains(name)) {
                    tagNames.add(name);
                }
            }
        }

        if (timeEntry.has("tags") && timeEntry.get("tags").isArray()) {
            for (JsonNode tagNode : timeEntry.get("tags")) {
                String name = null;
                if (tagNode.has("name")) {
                    name = tagNode.get("name").asText();
                } else if (tagNode.isTextual()) {
                    // If payload uses simple string tags, treat them as names
                    name = tagNode.asText();
                }

                if (name != null && !name.isBlank() && !tagNames.contains(name)) {
                    tagNames.add(name);
                }
            }
        }

        return tagNames;
    }

    private static List<String> extractFormattedTags(JsonNode timeEntry, List<String> tagIds, List<String> tagNames) {
        List<String> formattedTags = new ArrayList<>();

        if (timeEntry.has("tags") && timeEntry.get("tags").isArray()) {
            for (JsonNode tagNode : timeEntry.get("tags")) {
                String id = tagNode.has("id") ? tagNode.get("id").asText(null) : null;
                String name = tagNode.has("name") ? tagNode.get("name").asText(null) : null;

                if (tagNode.isTextual()) {
                    String value = tagNode.asText();
                    if (!value.isBlank() && !formattedTags.contains(value)) {
                        formattedTags.add(value);
                    }
                    continue;
                }

                if (name != null && !name.isBlank() && id != null && !id.isBlank()) {
                    String combined = name + " (" + id + ")";
                    if (!formattedTags.contains(combined)) {
                        formattedTags.add(combined);
                    }
                } else if (name != null && !name.isBlank()) {
                    if (!formattedTags.contains(name)) {
                        formattedTags.add(name);
                    }
                } else if (id != null && !id.isBlank()) {
                    if (!formattedTags.contains(id)) {
                        formattedTags.add(id);
                    }
                }
            }
        }

        if (formattedTags.isEmpty()) {
            if (!tagNames.isEmpty()) {
                formattedTags.addAll(tagNames);
            } else if (!tagIds.isEmpty()) {
                formattedTags.addAll(tagIds);
            }
        }

        return formattedTags;
    }

    private static String formatTagSummary(List<String> formattedTags, List<String> tagNames, List<String> tagIds) {
        if (!formattedTags.isEmpty()) {
            return String.join(", ", formattedTags);
        }
        if (!tagNames.isEmpty()) {
            return String.join(", ", tagNames);
        }
        if (!tagIds.isEmpty()) {
            return String.join(", ", tagIds);
        }
        return "(none)";
    }

    private static JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode) {
            return (JsonNode) cachedJson;
        }

        Object cachedBody = request.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String) {
            return objectMapper.readTree((String) cachedBody);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return objectMapper.readTree(sb.toString());
    }

    private static final class TagSuggestionResult {
        private final List<TagSuggestion> suggestions;
        private final List<String> tagNames;

        private TagSuggestionResult(List<TagSuggestion> suggestions) {
            if (suggestions == null) {
                suggestions = Collections.emptyList();
            }
            this.suggestions = Collections.unmodifiableList(new ArrayList<>(suggestions));

            Set<String> uniqueNames = new LinkedHashSet<>();
            for (TagSuggestion suggestion : this.suggestions) {
                if (suggestion != null) {
                    String name = suggestion.getName();
                    if (name != null && !name.isBlank()) {
                        uniqueNames.add(name);
                    }
                }
            }
            this.tagNames = Collections.unmodifiableList(new ArrayList<>(uniqueNames));
        }

        public List<TagSuggestion> getSuggestions() {
            return suggestions;
        }

        public List<String> getTagNames() {
            return tagNames;
        }
    }

    private static final class TagSuggestion {
        private final String name;
        private final String reason;

        private TagSuggestion(String name, String reason) {
            this.name = name;
            this.reason = reason != null ? reason : "";
        }

        public String getName() {
            return name;
        }

        public String getReason() {
            return reason;
        }
    }

    private static final class TagUpdateResult {
        private final LinkedHashMap<String, String> tagIdsByName;
        private final List<String> createdTagNames;
        private final JsonNode updatedEntry;

        private TagUpdateResult(LinkedHashMap<String, String> tagIdsByName, List<String> createdTagNames, JsonNode updatedEntry) {
            this.tagIdsByName = new LinkedHashMap<>(tagIdsByName);
            this.createdTagNames = Collections.unmodifiableList(new ArrayList<>(createdTagNames));
            this.updatedEntry = updatedEntry;
        }

        public Map<String, String> getTagIdsByName() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(tagIdsByName));
        }

        public List<String> getCreatedTagNames() {
            return createdTagNames;
        }

        public JsonNode getUpdatedEntry() {
            return updatedEntry;
        }
    }
}
