package com.example.autotagassistant;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.DefaultManifestController;

/**
 * Serves the runtime manifest so Clockify can discover the add-on.
 */
public class ManifestController extends DefaultManifestController {
    public ManifestController(ClockifyManifest manifest) {
        super(manifest);
    }
}
