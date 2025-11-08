package com.example.templateaddon;

import com.example.templateaddon.sdk.ClockifyAddon;
import com.example.templateaddon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

/**
 * Handles add-on lifecycle events.
 *
 * TODO: Replace the log statements with your persistence logic (store workspace tokens, clean up data, etc.).
 */
public final class LifecycleHandlers {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private LifecycleHandlers() {
    }

    public static void register(ClockifyAddon addon) {
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: INSTALLED");
                System.out.println("Payload:\n" + payload.toPrettyString());
                System.out.println("=".repeat(80));
                System.out.println();

                // TODO: Save payload.get("authToken").asText() keyed by workspaceId.

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "installed")
                        .put("message", "Add-on installed successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");
            } catch (Exception e) {
                return installationError(e);
            }
        });

        addon.registerLifecycleHandler("DELETED", "/lifecycle/deleted", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: DELETED");
                System.out.println("Payload:\n" + payload.toPrettyString());
                System.out.println("=".repeat(80));
                System.out.println();

                // TODO: Remove any data stored for payload.get("workspaceId")

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "uninstalled")
                        .put("message", "Add-on uninstalled successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");
            } catch (Exception e) {
                return uninstallationError(e);
            }
        });
    }

    private static HttpResponse installationError(Exception e) {
        System.err.println("Error handling INSTALLED event: " + e.getMessage());
        e.printStackTrace();
        String errorBody = objectMapper.createObjectNode()
                .put("message", "Failed to process installation")
                .put("details", e.getMessage())
                .toString();
        return HttpResponse.error(500, errorBody, "application/json");
    }

    private static HttpResponse uninstallationError(Exception e) {
        System.err.println("Error handling DELETED event: " + e.getMessage());
        e.printStackTrace();
        String errorBody = objectMapper.createObjectNode()
                .put("message", "Failed to process uninstallation")
                .put("details", e.getMessage())
                .toString();
        return HttpResponse.error(500, errorBody, "application/json");
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
