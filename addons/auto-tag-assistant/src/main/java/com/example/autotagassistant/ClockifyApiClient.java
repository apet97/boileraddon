package com.example.autotagassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Helper client for making Clockify API calls.
 *
 * IMPORTANT:
 * - Use the addon token received in the INSTALLED lifecycle event
 * - Token is workspace-specific - store it keyed by workspaceId
 * - Include token in Authorization header: "Bearer {addonToken}"
 * - Respect rate limits: 50 requests/second per addon per workspace
 * - Base URL comes from token claims (different for prod/staging/dev)
 *
 * Common endpoints for Auto-Tag Assistant:
 * - GET  /workspaces/{workspaceId}/tags - List all workspace tags
 * - GET  /workspaces/{workspaceId}/time-entries/{timeEntryId} - Get time entry
 * - PUT  /workspaces/{workspaceId}/time-entries/{timeEntryId} - Update time entry (add tags)
 * - POST /workspaces/{workspaceId}/tags - Create new tag
 *
 * Reference: dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json
 */
public class ClockifyApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String addonToken;

    /**
     * @param baseUrl The Clockify API base URL (from token claims, e.g., https://api.clockify.me/api/v1)
     * @param addonToken The workspace-specific addon token from INSTALLED event
     */
    public ClockifyApiClient(String baseUrl, String addonToken) {
        this.baseUrl = baseUrl;
        this.addonToken = addonToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get all tags for a workspace.
     *
     * @param workspaceId The workspace ID
     * @return JSON array of tags
     */
    public JsonNode getTags(String workspaceId) throws Exception {
        String url = String.format("%s/workspaces/%s/tags", baseUrl, workspaceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + addonToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get tags: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Get a specific time entry.
     *
     * @param workspaceId The workspace ID
     * @param timeEntryId The time entry ID
     * @return Time entry JSON
     */
    public JsonNode getTimeEntry(String workspaceId, String timeEntryId) throws Exception {
        String url = String.format("%s/workspaces/%s/time-entries/%s", baseUrl, workspaceId, timeEntryId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + addonToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get time entry: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Update a time entry's tags.
     *
     * @param workspaceId The workspace ID
     * @param timeEntryId The time entry ID
     * @param tagIds Array of tag IDs to set
     * @return Updated time entry JSON
     */
    public JsonNode updateTimeEntryTags(String workspaceId, String timeEntryId, String[] tagIds) throws Exception {
        String url = String.format("%s/workspaces/%s/time-entries/%s", baseUrl, workspaceId, timeEntryId);

        JsonNode existingEntry = getTimeEntry(workspaceId, timeEntryId);
        if (!(existingEntry instanceof com.fasterxml.jackson.databind.node.ObjectNode)) {
            throw new RuntimeException("Time entry payload must be an object to update tagIds");
        }

        com.fasterxml.jackson.databind.node.ObjectNode existingObject = (com.fasterxml.jackson.databind.node.ObjectNode) existingEntry;
        com.fasterxml.jackson.databind.node.ObjectNode requestNode = existingObject.deepCopy();

        if (!requestNode.has("start") && existingObject.has("timeInterval")) {
            JsonNode timeInterval = existingObject.get("timeInterval");
            if (timeInterval != null && timeInterval.has("start")) {
                requestNode.set("start", timeInterval.get("start"));
            }
            if (timeInterval != null && timeInterval.has("end")) {
                requestNode.set("end", timeInterval.get("end"));
            }
        }

        requestNode.set("tagIds", objectMapper.valueToTree(tagIds));

        String requestBody = objectMapper.writeValueAsString(requestNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + addonToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to update time entry: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Create a new tag in the workspace.
     *
     * @param workspaceId The workspace ID
     * @param tagName The tag name
     * @return Created tag JSON
     */
    public JsonNode createTag(String workspaceId, String tagName) throws Exception {
        String url = String.format("%s/workspaces/%s/tags", baseUrl, workspaceId);

        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .put("name", tagName)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + addonToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create tag: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }
}
