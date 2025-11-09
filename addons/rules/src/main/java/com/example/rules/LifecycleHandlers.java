package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.store.RulesStoreSPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

/**
 * Handles add-on lifecycle events.
 *
 * INSTALLED event:
 * - Sent when workspace admin installs the add-on
 * - Payload includes workspace-specific auth token
 * - CRITICAL: Store this token securely - it's needed for all Clockify API calls
 *
 * DELETED event:
 * - Sent when workspace admin uninstalls the add-on
 * - Clean up any stored data for this workspace
 */
public class LifecycleHandlers {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static RulesStoreSPI rulesStore;

    public static void register(ClockifyAddon addon, RulesStoreSPI store) {
        rulesStore = store;

        // Handle INSTALLED event
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                String workspaceDisplayId = workspaceId != null ? workspaceId : "unknown";
                String userId = payload.has("userId") ? payload.get("userId").asText("unknown") : "unknown";
                String authToken = payload.has("authToken") ? payload.get("authToken").asText(null) : null;
                String apiUrl = payload.has("apiUrl") ? payload.get("apiUrl").asText(null) : null;

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: INSTALLED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceDisplayId);
                System.out.println("User ID: " + userId);
                System.out.println("Auth token provided: " + (authToken != null && !authToken.isEmpty()));
                System.out.println("API base URL provided: " + (apiUrl != null && !apiUrl.isEmpty()));
                System.out.println("=".repeat(80));

                if (authToken == null || authToken.isEmpty()) {
                    System.out.println("⚠️  Warning: Missing auth token in INSTALLED payload. This may indicate an incomplete installation or payload structure issue.");
                    System.out.println("   Expected payload fields: workspaceId, authToken, apiUrl");
                } else if (workspaceId == null || workspaceId.isEmpty()) {
                    System.out.println("⚠️  Unable to store auth token because workspaceId is missing.");
                } else {
                    com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, apiUrl);
                    System.out.println("✅ Stored auth token for workspace " + workspaceId);
                    // Preload workspace cache asynchronously for ID<->name mapping
                    try {
                        var wk = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).orElse(null);
                        if (wk != null) {
                            com.example.rules.cache.WorkspaceCache.refreshAsync(workspaceId, wk.apiBaseUrl(), wk.token());
                        }
                    } catch (Exception ignored) {}
                }
                System.out.println();

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "installed")
                        .put("message", "Rules add-on installed successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                System.err.println("Error handling INSTALLED event: " + e.getMessage());
                e.printStackTrace();
                String errorBody = objectMapper.createObjectNode()
                        .put("message", "Failed to process installation")
                        .put("details", e.getMessage())
                        .toString();
                return HttpResponse.error(500, errorBody, "application/json");
            }
        });

        // Handle DELETED event
        addon.registerLifecycleHandler("DELETED", "/lifecycle/deleted", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                String workspaceDisplayId = workspaceId != null ? workspaceId : "unknown";

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: DELETED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceDisplayId);
                System.out.println("=".repeat(80));

                if (workspaceId == null || workspaceId.isEmpty()) {
                    System.out.println("⚠️  Unable to remove auth token because workspaceId is missing.");
                } else {
                    boolean removed = com.clockify.addon.sdk.security.TokenStore.delete(workspaceId);
                    if (removed) {
                        System.out.println("✅ Removed stored auth token for workspace " + workspaceId);
                    } else {
                        System.out.println("ℹ️  No stored auth token found for workspace " + workspaceId);
                    }

                    // Clean up rules for this workspace
                    int deletedRules = rulesStore.deleteAll(workspaceId);
                    System.out.println("ℹ️  Deleted " + deletedRules + " rules for workspace " + workspaceId);
                }
                System.out.println();

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "uninstalled")
                        .put("message", "Rules add-on uninstalled successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                System.err.println("Error handling DELETED event: " + e.getMessage());
                e.printStackTrace();
                String errorBody = objectMapper.createObjectNode()
                        .put("message", "Failed to process uninstallation")
                        .put("details", e.getMessage())
                        .toString();
                return HttpResponse.error(500, errorBody, "application/json");
            }
        });
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
}
