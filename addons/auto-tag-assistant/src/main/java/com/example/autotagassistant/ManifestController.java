package com.example.autotagassistant;

import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
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
        Optional<String> hostHeader = firstHeaderValue(request, "Host");

        boolean hostFromForwardedHeader = false;
        boolean hostFromDirectHostHeader = false;

        String scheme = forwardedProto
                .or(() -> firstHeaderValue(request, "X-Forwarded-Proto"))
                .orElseGet(() -> Optional.ofNullable(request.getScheme()).orElse("http"));

        String host = null;
        if (forwardedHost.isPresent()) {
            host = forwardedHost.get();
            hostFromForwardedHeader = true;
        } else {
            Optional<String> xForwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
            if (xForwardedHost.isPresent()) {
                host = xForwardedHost.get();
                hostFromForwardedHeader = true;
            } else if (hostHeader.isPresent()) {
                host = hostHeader.get();
                hostFromDirectHostHeader = true;
            } else {
                host = request.getServerName();
            }
        }

        String port = forwardedPort.orElse(null);
        if (port == null) {
            port = firstHeaderValue(request, "X-Forwarded-Port").orElse(null);
        }
        if (port == null && !hostFromForwardedHeader) {
            boolean hostAppearsLocal = hostFromDirectHostHeader
                    ? isLocalAddress(extractHostname(hostHeader.orElse(host)))
                    : isLocalAddress(extractHostname(host));
            if (hostAppearsLocal) {
                port = derivePort(scheme, request.getServerPort());
            }
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

    private String derivePort(String scheme, int serverPort) {
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

    private String extractHostname(String host) {
        if (host == null) {
            return null;
        }
        String trimmed = host.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int closing = trimmed.indexOf(']');
            if (closing >= 0) {
                return trimmed.substring(0, closing + 1);
            }
        }
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex >= 0) {
            return trimmed.substring(0, colonIndex);
        }
        return trimmed;
    }

    private boolean isLocalAddress(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "0.0.0.0".equals(normalized)
                || normalized.equals("::1")
                || normalized.startsWith("127.");
    }
}
