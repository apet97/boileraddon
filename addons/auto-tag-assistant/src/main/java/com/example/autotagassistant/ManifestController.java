package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyManifest;
import com.example.autotagassistant.sdk.HttpResponse;
import com.example.autotagassistant.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Optional;

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
        String detectedBaseUrl = detectBaseUrl(request);
        if (detectedBaseUrl != null && !detectedBaseUrl.isBlank() && !detectedBaseUrl.equals(manifest.getBaseUrl())) {
            manifest.setBaseUrl(detectedBaseUrl);
        }

        // Serialize the manifest - the ClockifyManifest class only includes valid runtime fields
        String json = mapper.writeValueAsString(manifest);
        return HttpResponse.ok(json, "application/json");
    }

    private String detectBaseUrl(HttpServletRequest request) {
        Optional<String> forwardedProto = forwardedHeaderValue(request, "proto");
        Optional<String> forwardedHost = forwardedHeaderValue(request, "host");
        Optional<String> forwardedPort = forwardedHeaderValue(request, "port");

        boolean hostFromForwardedHeader = forwardedHost.isPresent();

        String scheme = forwardedProto
                .or(() -> firstHeaderValue(request, "X-Forwarded-Proto"))
                .orElseGet(() -> Optional.ofNullable(request.getScheme()).orElse("http"));

        String host = forwardedHost
                .or(() -> firstHeaderValue(request, "X-Forwarded-Host"))
                .orElse(request.getServerName());

        String port = forwardedPort.orElse(null);
        if (port == null) {
            port = firstHeaderValue(request, "X-Forwarded-Port").orElse(null);
        }
        if (port == null && !hostFromForwardedHeader) {
            port = derivePort(host, scheme, request.getServerPort());
        }

        if (host != null && !host.isBlank()) {
            host = host.trim();
        }

        if (host == null || host.isBlank()) {
            return null;
        }

        if (port != null && !port.isBlank() && !host.contains(":")) {
            host = host + ":" + port;
        }

        String contextPath = Optional.ofNullable(request.getContextPath()).orElse("");
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        String baseUrl = scheme + "://" + host + contextPath;
        return baseUrl.replaceAll("/+$", "");
    }

    private Optional<String> firstHeaderValue(HttpServletRequest request, String headerName) {
        String raw = request.getHeader(headerName);
        if (raw == null) {
            return Optional.empty();
        }
        int commaIndex = raw.indexOf(',');
        String value = commaIndex >= 0 ? raw.substring(0, commaIndex) : raw;
        value = value == null ? null : value.trim();
        return (value == null || value.isEmpty()) ? Optional.empty() : Optional.of(value);
    }

    private Optional<String> forwardedHeaderValue(HttpServletRequest request, String parameterName) {
        String raw = request.getHeader("Forwarded");
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        int commaIndex = raw.indexOf(',');
        String firstSegment = commaIndex >= 0 ? raw.substring(0, commaIndex) : raw;
        String[] pairs = firstSegment.split(";");
        String targetKey = parameterName == null ? "" : parameterName.trim().toLowerCase(Locale.ROOT);
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }
            String key = pair.substring(0, equalsIndex).trim().toLowerCase(Locale.ROOT);
            if (!key.equals(targetKey)) {
                continue;
            }
            String value = pair.substring(equalsIndex + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        }
        return Optional.empty();
    }

    private String derivePort(String host, String scheme, int serverPort) {
        if (host != null && host.contains(":")) {
            return null;
        }

        if (serverPort <= 0) {
            return null;
        }

        if (isStandardPort(serverPort, scheme)) {
            return null;
        }

        return Integer.toString(serverPort);
    }

    private boolean isStandardPort(int port, String scheme) {
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        return (port == 80 && "http".equals(normalizedScheme)) || (port == 443 && "https".equals(normalizedScheme));
    }
}
