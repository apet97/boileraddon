package com.example.addon;

import java.net.http.*;
import java.net.URI;

/**
 * Simple Clockify API client.
 *
 * Use the addon token received in the INSTALLED lifecycle event.
 * Store it per workspace and use it for all API calls to that workspace.
 *
 * See: dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json for full API reference.
 */
public class ClockifyClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiBase; // e.g., https://api.clockify.me/api/v1
    private final String addonToken;

    public ClockifyClient(String apiBase, String addonToken) {
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length()-1) : apiBase;
        this.addonToken = addonToken;
    }

    public HttpResponse<String> getWorkspace(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId))
                .header("Authorization", "Bearer " + addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> listProjects(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/projects"))
                .header("Authorization", "Bearer " + addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> listUsers(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/users"))
                .header("Authorization", "Bearer " + addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
