package com.clockify.addon.rules;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.DefaultManifestController;

/**
 * Serves the runtime manifest.
 *
 * Update RulesApp to configure your add-on name, description, and required scopes.
 */
public class ManifestController extends DefaultManifestController {
    public ManifestController(ClockifyManifest manifest) {
        super(manifest);
    }
}
