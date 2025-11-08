package com.clockify.addon.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Default implementation for serving a Clockify manifest with forwarded-header awareness.
 */
public class DefaultManifestController implements RequestHandler {
    private final ClockifyManifest manifest;
    private final ObjectMapper mapper;
    private final BaseUrlDetector baseUrlDetector;

    public DefaultManifestController(ClockifyManifest manifest) {
        this(manifest, new ObjectMapper(), new BaseUrlDetector());
    }

    DefaultManifestController(ClockifyManifest manifest, ObjectMapper mapper, BaseUrlDetector baseUrlDetector) {
        this.manifest = manifest;
        this.mapper = mapper;
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.baseUrlDetector = baseUrlDetector;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        Optional<String> detectedBaseUrl = baseUrlDetector.detectBaseUrl(request);
        detectedBaseUrl
            .filter(base -> !base.isBlank() && !base.equals(manifest.getBaseUrl()))
            .ifPresent(manifest::setBaseUrl);

        String json = mapper.writeValueAsString(manifest);
        return HttpResponse.ok(json, "application/json");
    }
}
