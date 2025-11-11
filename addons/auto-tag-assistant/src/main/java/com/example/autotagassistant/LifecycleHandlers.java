package com.example.autotagassistant;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(LifecycleHandlers.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        // Handle INSTALLED event
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                String workspaceDisplayId = workspaceId != null ? workspaceId : "unknown";
                String userId = payload.has("userId") ? payload.get("userId").asText("unknown") : "unknown";
                String authToken = payload.has("authToken") ? payload.get("authToken").asText(null) : null;
                String apiUrl = payload.has("apiUrl") ? payload.get("apiUrl").asText(null) : null;

                logger.info("LIFECYCLE EVENT: INSTALLED");
                logger.info("Workspace ID: {}", workspaceDisplayId);
                logger.info("User ID: {}", userId);
                logger.info("Auth token provided: {}", authToken != null && !authToken.isEmpty());
                logger.info("API base URL provided: {}", apiUrl != null && !apiUrl.isEmpty());
                logger.debug("Installation payload: {}", payload.toPrettyString());

                // IMPORTANT: In a real implementation, you MUST:
                // 1. Extract the auth token from the payload
                // 2. Store it securely (database, vault) keyed by workspaceId
                // 3. Use this token for all subsequent Clockify API calls for this workspace
                //
                // Example:
                // String authToken = payload.get("authToken").asText();
                // tokenStore.save(workspaceId, authToken);

                if (authToken == null || authToken.isEmpty()) {
                    logger.warn("Missing auth token in payload; verify installation payload structure");
                } else if (workspaceId == null || workspaceId.isEmpty()) {
                    logger.warn("Unable to store auth token because workspaceId is missing");
                } else {
                    com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, apiUrl);
                    logger.info("Stored auth token for workspace {}", workspaceId);
                }

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "installed")
                        .put("message", "Add-on installed successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                logger.error("Error handling INSTALLED event", e);
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

                logger.info("LIFECYCLE EVENT: DELETED");
                logger.info("Workspace ID: {}", workspaceDisplayId);

                // IMPORTANT: In a real implementation:
                // 1. Remove stored auth token for this workspace
                // 2. Clean up any workspace-specific data
                // 3. Cancel any scheduled jobs for this workspace
                //
                // Example:
                // tokenStore.delete(workspaceId);
                // userSettingsStore.deleteByWorkspace(workspaceId);

                if (workspaceId == null || workspaceId.isEmpty()) {
                    logger.warn("Unable to remove auth token because workspaceId is missing");
                } else {
                    boolean removed = com.clockify.addon.sdk.security.TokenStore.delete(workspaceId);
                    if (removed) {
                        logger.info("Removed stored auth token for workspace {}", workspaceId);
                    } else {
                        logger.info("No stored auth token found for workspace {}", workspaceId);
                    }
                }

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "uninstalled")
                        .put("message", "Add-on uninstalled successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                logger.error("Error handling DELETED event", e);
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
