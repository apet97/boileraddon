package com.example.auto_tag_assistant;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.logging.Logger;

/**
 * LifecycleHandlers processes lifecycle events from Clockify.
 *
 * Lifecycle Flow:
 * 1. INSTALLED - When addon is installed on a workspace
 *    - Receive workspace ID and addon installation details
 *    - Receive addon token for making API calls
 *    - Receive webhook auth tokens for verifying webhook requests
 *    - Store this information (in production, use database)
 *
 * 2. DELETED - When addon is uninstalled from a workspace
 *    - Clean up any stored data for this workspace
 *    - Stop processing webhooks for this workspace
 *
 * 3. SETTINGS_UPDATED (optional) - When user updates addon settings
 *    - Receive new settings configuration
 *    - Update internal state
 */
public class LifecycleHandlers {
    private static final Logger log = Logger.getLogger(LifecycleHandlers.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        addon.registerCustomEndpoint("/lifecycle", request -> {
            try {
                // Parse the lifecycle event payload
                String body = request.getBody();
                JsonNode payload = mapper.readTree(body);

                String eventType = payload.path("type").asText();
                log.info("Received lifecycle event: " + eventType);

                switch (eventType) {
                    case "INSTALLED":
                        handleInstalled(payload);
                        break;
                    case "DELETED":
                        handleDeleted(payload);
                        break;
                    case "SETTINGS_UPDATED":
                        handleSettingsUpdated(payload);
                        break;
                    default:
                        log.warning("Unknown lifecycle event type: " + eventType);
                }

                return addonsdk.shared.response.HttpResponse.ok("lifecycle processed");

            } catch (Exception e) {
                log.severe("Error processing lifecycle event: " + e.getMessage());
                e.printStackTrace();
                return addonsdk.shared.response.HttpResponse.error(500, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Handle INSTALLED event.
     *
     * Payload structure:
     * {
     *   "type": "INSTALLED",
     *   "workspaceId": "...",
     *   "addonId": "...",
     *   "addon": {
     *     "key": "auto-tag-assistant",
     *     "name": "Auto-Tag Assistant",
     *     "apiUrl": "https://euc1.clockify.me/api/v1",
     *     "addonToken": "eyJ..." // <- Important! Use this for API calls
     *   },
     *   "webhooks": [{
     *     "authToken": "eyJ...", // <- Important! Use to verify webhook signatures
     *     "path": "/webhook",
     *     "webhookType": "ADDON"
     *   }]
     * }
     *
     * In production, store:
     * - workspaceId
     * - addonToken (for API calls)
     * - webhook authToken (for signature verification)
     */
    private static void handleInstalled(JsonNode payload) {
        String workspaceId = payload.path("workspaceId").asText();
        String addonToken = payload.path("addon").path("addonToken").asText();
        String apiUrl = payload.path("addon").path("apiUrl").asText();

        log.info("Addon installed on workspace: " + workspaceId);
        log.info("API URL: " + apiUrl);
        log.info("Addon token received (length=" + addonToken.length() + ")");

        // In production, store these in a database:
        // db.save(new Installation(workspaceId, addonToken, apiUrl));

        // Extract webhook tokens
        JsonNode webhooks = payload.path("webhooks");
        if (webhooks.isArray()) {
            webhooks.forEach(webhook -> {
                String webhookToken = webhook.path("authToken").asText();
                String webhookPath = webhook.path("path").asText();
                log.info("Webhook registered: " + webhookPath + " (token length=" + webhookToken.length() + ")");
                // In production: db.saveWebhookToken(workspaceId, webhookPath, webhookToken);
            });
        }

        log.warning("IMPORTANT: In production, store addon token and webhook tokens in database!");
    }

    /**
     * Handle DELETED event.
     * Clean up any stored data for this workspace.
     */
    private static void handleDeleted(JsonNode payload) {
        String workspaceId = payload.path("workspaceId").asText();
        log.info("Addon deleted from workspace: " + workspaceId);

        // In production: db.deleteInstallation(workspaceId);
    }

    /**
     * Handle SETTINGS_UPDATED event (if addon has settings component).
     * Update internal configuration when user changes settings.
     */
    private static void handleSettingsUpdated(JsonNode payload) {
        String workspaceId = payload.path("workspaceId").asText();
        JsonNode settings = payload.path("settings");

        log.info("Settings updated for workspace: " + workspaceId);
        log.info("New settings: " + settings.toString());

        // In production: db.updateSettings(workspaceId, settings);
    }
}
