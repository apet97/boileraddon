package com.clockify.addon.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Default implementation for serving a Clockify manifest with forwarded-header awareness.
 * <p>
 * When a forwarded base URL is detected, the manifest JSON is cloned with a per-request
 * {@code baseUrl} override so the underlying {@link ClockifyManifest} configuration remains
 * unchanged for subsequent requests.
 * </p>
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
        Optional<String> detectedBaseUrl = baseUrlDetector.detectBaseUrl(request)
                .filter(base -> !base.isBlank() && !base.equals(manifest.getBaseUrl()));

        String json;
        if (detectedBaseUrl.isPresent()) {
            ObjectNode manifestNode = mapper.convertValue(manifest, ObjectNode.class);
            manifestNode.put("baseUrl", detectedBaseUrl.get());
            json = mapper.writeValueAsString(manifestNode);
        } else {
            json = mapper.writeValueAsString(manifest);
        }

        return HttpResponse.ok(json, "application/json");
    }
}
