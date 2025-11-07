package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyAddon;
import com.example.autotagassistant.sdk.HttpResponse;
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

    public static void register(ClockifyAddon addon) {
        // Handle INSTALLED event
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText() : "unknown";
                String userId = payload.has("userId") ? payload.get("userId").asText() : "unknown";
                String authToken = payload.has("authToken") ? payload.get("authToken").asText() : null;

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: INSTALLED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceId);
                System.out.println("User ID: " + userId);
                System.out.println("Payload: " + payload.toPrettyString());
                System.out.println("Auth token provided: " + (authToken != null && !authToken.isEmpty()));
                System.out.println("=".repeat(80));

                // IMPORTANT: In a real implementation, you MUST:
                // 1. Extract the auth token from the payload
                // 2. Store it securely (database, vault) keyed by workspaceId
                // 3. Use this token for all subsequent Clockify API calls for this workspace
                //
                // Example:
                // String authToken = payload.get("authToken").asText();
                // tokenStore.save(workspaceId, authToken);

                if (authToken == null || authToken.isEmpty()) {
                    System.out.println("⚠️  TODO: Missing auth token in payload; verify installation payload structure.");
                } else {
                    System.out.println("⚠️  TODO: Store auth token for workspace " + workspaceId);
                    System.out.println("    Add token storage in LifecycleHandlers.java:register()");
                }
                System.out.println();

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "installed")
                        .put("message", "Add-on installed successfully")
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
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText() : "unknown";

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: DELETED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceId);
                System.out.println("=".repeat(80));

                // IMPORTANT: In a real implementation:
                // 1. Remove stored auth token for this workspace
                // 2. Clean up any workspace-specific data
                // 3. Cancel any scheduled jobs for this workspace
                //
                // Example:
                // tokenStore.delete(workspaceId);
                // userSettingsStore.deleteByWorkspace(workspaceId);

                System.out.println("⚠️  TODO: Clean up data for workspace " + workspaceId);
                System.out.println("    Add cleanup logic in LifecycleHandlers.java:register()");
                System.out.println();

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "uninstalled")
                        .put("message", "Add-on uninstalled successfully")
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
