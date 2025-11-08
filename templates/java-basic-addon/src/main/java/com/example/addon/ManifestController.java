package com.example.addon;

import com.example.addon.sdk.ClockifyManifest;
import com.example.addon.sdk.HttpResponse;
import com.example.addon.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves the add-on manifest at /manifest.json.
 *
 * CRITICAL: This endpoint MUST NOT return "$schema" field.
 * Clockify's /addons endpoint rejects unknown fields like "$schema".
 *
 * The ClockifyManifest class only includes valid runtime fields.
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
        String json = mapper.writeValueAsString(manifest);
        return HttpResponse.ok(json, "application/json");
    }
}
