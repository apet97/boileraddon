package com.example.addon;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;

/**
 * Handles add-on lifecycle events: INSTALLED and DELETED.
 *
 * IMPORTANT: Store the auth token from INSTALLED event - it's needed for all Clockify API calls.
 */
public class LifecycleHandlers {
    public static void register(ClockifyAddon addon) {
        // Handle INSTALLED event
        addon.onLifecycleInstalled(request -> {
            String workspaceId = request.getResourceId();
            System.out.println("Add-on installed in workspace: " + workspaceId);

            // TODO: Store auth token from request.getPayload().get("authToken")
            // for making Clockify API calls for this workspace

            return addonsdk.shared.response.HttpResponse.ok("Installed");
        });

        // Handle DELETED event
        addon.onLifecycleDeleted(request -> {
            String workspaceId = request.getResourceId();
            System.out.println("Add-on deleted from workspace: " + workspaceId);

            // TODO: Clean up any stored data for this workspace

            return addonsdk.shared.response.HttpResponse.ok("Deleted");
        });
    }
}
