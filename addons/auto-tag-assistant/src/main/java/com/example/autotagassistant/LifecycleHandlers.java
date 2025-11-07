package com.example.autotagassistant;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.google.gson.JsonObject;

/**
 * Handles add-on lifecycle events.
 *
 * INSTALLED event:
 * - Sent when workspace admin installs the add-on
 * - Payload includes workspace-specific addon token
 * - CRITICAL: Store this token securely - it's needed for all Clockify API calls
 *
 * DELETED event:
 * - Sent when workspace admin uninstalls the add-on
 * - Clean up any stored data for this workspace
 */
public class LifecycleHandlers {

    public static void register(ClockifyAddon addon) {
        // Handle INSTALLED event
        addon.onLifecycleInstalled(request -> {
            try {
                String workspaceId = request.getResourceId();
                JsonObject payload = request.getPayload();

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: INSTALLED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceId);
                System.out.println("User ID: " + request.getUserId());
                System.out.println("Payload: " + payload);
                System.out.println("=".repeat(80));

                // IMPORTANT: In a real implementation, you MUST:
                // 1. Extract the addon token from the payload
                // 2. Store it securely (database, vault) keyed by workspaceId
                // 3. Use this token for all subsequent Clockify API calls for this workspace
                //
                // Example:
                // String addonToken = payload.get("addonToken").getAsString();
                // tokenStore.save(workspaceId, addonToken);

                System.out.println("⚠️  TODO: Store addon token for workspace " + workspaceId);
                System.out.println("    Add token storage in LifecycleHandlers.java:register()");
                System.out.println();

                return com.cake.clockify.addonsdk.shared.response.HttpResponse.ok("Add-on installed successfully");

            } catch (Exception e) {
                System.err.println("Error handling INSTALLED event: " + e.getMessage());
                e.printStackTrace();
                return com.cake.clockify.addonsdk.shared.response.HttpResponse.internalServerError(
                    "Failed to process installation: " + e.getMessage()
                );
            }
        });

        // Handle DELETED event
        addon.onLifecycleDeleted(request -> {
            try {
                String workspaceId = request.getResourceId();

                System.out.println("\n" + "=".repeat(80));
                System.out.println("LIFECYCLE EVENT: DELETED");
                System.out.println("=".repeat(80));
                System.out.println("Workspace ID: " + workspaceId);
                System.out.println("=".repeat(80));

                // IMPORTANT: In a real implementation:
                // 1. Remove stored addon token for this workspace
                // 2. Clean up any workspace-specific data
                // 3. Cancel any scheduled jobs for this workspace
                //
                // Example:
                // tokenStore.delete(workspaceId);
                // userSettingsStore.deleteByWorkspace(workspaceId);

                System.out.println("⚠️  TODO: Clean up data for workspace " + workspaceId);
                System.out.println("    Add cleanup logic in LifecycleHandlers.java:register()");
                System.out.println();

                return com.cake.clockify.addonsdk.shared.response.HttpResponse.ok("Add-on uninstalled successfully");

            } catch (Exception e) {
                System.err.println("Error handling DELETED event: " + e.getMessage());
                e.printStackTrace();
                return com.cake.clockify.addonsdk.shared.response.HttpResponse.internalServerError(
                    "Failed to process uninstallation: " + e.getMessage()
                );
            }
        });
    }
}
