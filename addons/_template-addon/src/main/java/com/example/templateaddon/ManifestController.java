package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.DefaultManifestController;

/**
 * Serves the runtime manifest.
 *
 * Update TemplateAddonApp to configure your add-on name, description, and required scopes.
 */
public class ManifestController extends DefaultManifestController {
    public ManifestController(ClockifyManifest manifest) {
        super(manifest);
    }
}
