package com.example.auto_tag_assistant;

import addonsdk.shared.RequestHandler;
import addonsdk.shared.response.HttpResponse;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ManifestController implements RequestHandler<jakarta.servlet.http.HttpServletRequest> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ClockifyManifest manifest;

    public ManifestController(ClockifyManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public HttpResponse handle(jakarta.servlet.http.HttpServletRequest request) {
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
            return HttpResponse.json(json);
        } catch (Exception e) {
            return HttpResponse.internalServerError(e.getMessage());
        }
    }
}
