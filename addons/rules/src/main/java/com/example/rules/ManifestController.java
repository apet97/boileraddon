package com.example.rules;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.controllers.DefaultManifestController;

/**
 * Serves the runtime manifest for the Rules add-on.
 * Extends DefaultManifestController to inherit base URL detection from X-Forwarded-* headers.
 */
public class ManifestController extends DefaultManifestController {

    public ManifestController(ClockifyManifest manifest) {
        super(manifest);
    }
}
