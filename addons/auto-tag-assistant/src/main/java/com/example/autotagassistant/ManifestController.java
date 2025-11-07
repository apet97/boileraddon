package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyManifest;
import com.example.autotagassistant.sdk.HttpResponse;
import com.example.autotagassistant.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        String effectiveBaseUrl = (detectedBaseUrl == null || detectedBaseUrl.isBlank())
                ? manifest.getBaseUrl()
                : detectedBaseUrl;

        // Serialize a snapshot of the manifest without mutating the shared instance
        ObjectNode manifestNode = mapper.valueToTree(manifest);
        if (effectiveBaseUrl != null && !effectiveBaseUrl.isBlank()) {
            manifestNode.put("baseUrl", effectiveBaseUrl);
        }

        String json = mapper.writeValueAsString(manifestNode);
        return HttpResponse.ok(json, "application/json");
    }

    private String detectBaseUrl(HttpServletRequest request) {
        Forwarded forwarded = parseForwardedHeader(request);

        String scheme = Optional.ofNullable(forwarded.proto)
                .or(() -> firstHeaderValue(request, "X-Forwarded-Proto"))
                .orElseGet(() -> Optional.ofNullable(request.getScheme()).orElse("http"));

        boolean usedForwardedHost = forwarded.host != null && !forwarded.host.isBlank();
        Optional<String> xForwardedHost = usedForwardedHost ? Optional.empty() : firstHeaderValue(request, "X-Forwarded-Host");

        String hostHeader = usedForwardedHost
                ? forwarded.host
                : xForwardedHost.orElseGet(request::getServerName);

        Optional<String> xForwardedPort = firstHeaderValue(request, "X-Forwarded-Port");

        String port;
        if (forwarded.port != null && !forwarded.port.isBlank()) {
            port = forwarded.port;
        } else if (xForwardedPort.isPresent()) {
            port = xForwardedPort.get();
        } else if (usedForwardedHost || xForwardedHost.isPresent()) {
            port = null;
        } else {
            port = derivePort(hostHeader, scheme, request.getServerPort());
        }

        String host = hostHeader;
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

    private Forwarded parseForwardedHeader(HttpServletRequest request) {
        String header = request.getHeader("Forwarded");
        if (header == null || header.isBlank()) {
            return new Forwarded();
        }

        String firstValue = header.split(",", 2)[0];
        String[] directives = firstValue.split(";");
        Forwarded forwarded = new Forwarded();
        for (String directive : directives) {
            String[] parts = directive.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = trimQuotes(parts[1].trim());
            switch (key) {
                case "proto" -> forwarded.proto = value;
                case "host" -> {
                    String[] hostParts = splitHostAndPort(value);
                    forwarded.host = hostParts[0];
                    forwarded.port = hostParts[1];
                }
            }
        }
        return forwarded;
    }

    private String[] splitHostAndPort(String hostValue) {
        if (hostValue == null || hostValue.isBlank()) {
            return new String[]{null, null};
        }

        String trimmed = hostValue.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            // IPv6 literal without port
            return new String[]{trimmed, null};
        }

        int colonIndex = trimmed.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < trimmed.length() - 1 && trimmed.indexOf(':') == colonIndex) {
            String host = trimmed.substring(0, colonIndex);
            String port = trimmed.substring(colonIndex + 1);
            if (port.chars().allMatch(Character::isDigit)) {
                return new String[]{host, port};
            }
        }

        return new String[]{trimmed, null};
    }

    private String trimQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static class Forwarded {
        String proto;
        String host;
        String port;
    }
}
