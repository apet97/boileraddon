package com.example.autotagassistant;

import com.cake.clockify.addonsdk.shared.RequestHandler;
import com.cake.clockify.addonsdk.shared.response.HttpResponse;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.servlet.http.HttpServletRequest;

/**
 * Serves the add-on manifest.
 *
 * CRITICAL: The manifest returned here MUST NOT contain "$schema" field.
 * Clockify's /addons endpoint rejects unknown fields like "$schema".
 *
 * The manifest.json file in the project root can reference "$schema" for IDE validation,
 * but this endpoint must serve only the runtime manifest fields that Clockify accepts.
 */
public class ManifestController implements RequestHandler {
    private final ClockifyManifest manifest;
    private final ObjectMapper mapper;

    public ManifestController(ClockifyManifest manifest) {
        this.manifest = manifest;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        // Serialize the manifest - the ClockifyManifest class only includes valid runtime fields
        String json = mapper.writeValueAsString(manifest);
        return HttpResponse.ok(json, "application/json");
    }
}
