package com.example.auto_tag_assistant;

import java.net.http.*;
import java.net.URI;

/**
 * ClockifyClient - Helper class for making Clockify API calls.
 *
 * This client uses the Clockify REST API to:
 * - List and create tags
 * - Get and update time entries
 * - Fetch workspace information
 *
 * Authentication:
 * - Uses X-Addon-Token header (provided during lifecycle INSTALLED event)
 * - Token is valid for the specific workspace where addon is installed
 *
 * Rate Limits:
 * - 50 requests per second per workspace per addon
 * - Should implement retry logic with exponential backoff for production
 */
public class ClockifyClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiBase; // e.g., https://euc1.clockify.me/api/v1
    private final String addonToken; // X-Addon-Token or user token

    public ClockifyClient(String apiBase, String addonToken) {
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length()-1) : apiBase;
        this.addonToken = addonToken;
    }

    /**
     * Get workspace details.
     * GET /workspaces/{workspaceId}
     */
    public HttpResponse<String> getWorkspace(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * List all tags in a workspace.
     * GET /workspaces/{workspaceId}/tags
     *
     * Response contains array of tags with:
     * - id: tag identifier
     * - name: tag name
     * - workspaceId: workspace identifier
     */
    public HttpResponse<String> listTags(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/tags"))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Create a new tag in a workspace.
     * POST /workspaces/{workspaceId}/tags
     *
     * Request body: {"name": "Tag Name"}
     * Returns created tag with id
     */
    public HttpResponse<String> createTag(String workspaceId, String tagName) throws Exception {
        String body = String.format("{\"name\":\"%s\"}", tagName);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/tags"))
                .header("X-Addon-Token", addonToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Get a specific time entry.
     * GET /workspaces/{workspaceId}/time-entries/{id}
     */
    public HttpResponse<String> getTimeEntry(String workspaceId, String timeEntryId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/time-entries/" + timeEntryId))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Update a time entry (e.g., to add tags).
     * PUT /workspaces/{workspaceId}/time-entries/{id}
     *
     * Body should contain the complete time entry with modifications.
     * To add tags, include all existing fields plus updated tagIds array.
     */
    public HttpResponse<String> updateTimeEntry(String workspaceId, String timeEntryId, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/time-entries/" + timeEntryId))
                .header("X-Addon-Token", addonToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * List projects in workspace.
     * GET /workspaces/{workspaceId}/projects
     */
    public HttpResponse<String> listProjects(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/projects"))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * List users in workspace.
     * GET /workspaces/{workspaceId}/users
     */
    public HttpResponse<String> listUsers(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/users"))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
