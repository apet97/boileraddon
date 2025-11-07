package com.example.addon;

import java.net.http.*;
import java.net.URI;

public class ClockifyClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiBase; // e.g., https://euc1.clockify.me/api/v1
    private final String addonToken; // X-Addon-Token or user token

    public ClockifyClient(String apiBase, String addonToken) {
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length()-1) : apiBase;
        this.addonToken = addonToken;
    }

    public HttpResponse<String> getWorkspace(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> listProjects(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/projects"))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> listUsers(String workspaceId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/workspaces/" + workspaceId + "/users"))
                .header("X-Addon-Token", addonToken)
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
