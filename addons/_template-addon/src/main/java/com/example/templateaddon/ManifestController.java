package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.DefaultManifestController;

/**
 * Serves the runtime manifest.
 *
 * TODO: Update TemplateAddonApp so the manifest reflects your add-on name, description, and scopes.
 */
public class ManifestController extends DefaultManifestController {
    public ManifestController(ClockifyManifest manifest) {
        super(manifest);
    }
}
