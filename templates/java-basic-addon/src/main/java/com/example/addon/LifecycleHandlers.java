package com.example.addon;

import com.example.addon.sdk.ClockifyAddon;
import com.example.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

/**
 * Handles add-on lifecycle events: INSTALLED and DELETED.
 *
 * IMPORTANT: Store the auth token from INSTALLED event - it's needed for all Clockify API calls.
 */
public class LifecycleHandlers {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        // Handle INSTALLED event
        addon.registerLifecycleHandler("INSTALLED", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload != null && payload.has("workspaceId")
                        ? payload.get("workspaceId").asText()
                        : "unknown";

                System.out.println("Add-on installed in workspace: " + workspaceId);

                // TODO: Store auth token from payload.get("authToken") for
                // making Clockify API calls for this workspace.

                return HttpResponse.ok("Installed");
            } catch (Exception e) {
                System.err.println("Failed to process INSTALLED payload: " + e.getMessage());
                return HttpResponse.error(500, "Failed to process installation event");
            }
        });

        // Handle DELETED event
        addon.registerLifecycleHandler("DELETED", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload != null && payload.has("workspaceId")
                        ? payload.get("workspaceId").asText()
                        : "unknown";

                System.out.println("Add-on deleted from workspace: " + workspaceId);

                // TODO: Clean up any stored data for this workspace.

                return HttpResponse.ok("Deleted");
            } catch (Exception e) {
                System.err.println("Failed to process DELETED payload: " + e.getMessage());
                return HttpResponse.error(500, "Failed to process deletion event");
            }
        });
    }

    private static JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        Object cachedBody = request.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String bodyString) {
            if (bodyString.isBlank()) {
                return null;
            }
            JsonNode json = objectMapper.readTree(bodyString);
            request.setAttribute("clockify.jsonBody", json);
            return json;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        if (body.length() == 0) {
            return null;
        }

        JsonNode json = objectMapper.readTree(body.toString());
        request.setAttribute("clockify.jsonBody", json);
        request.setAttribute("clockify.rawBody", body.toString());
        return json;
    }
}
