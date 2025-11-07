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
        ForwardedDetails forwarded = parseForwardedHeader(request);

        String scheme = forwarded.proto != null ? forwarded.proto
                : firstHeaderValue(request, "X-Forwarded-Proto")
                        .orElseGet(() -> Optional.ofNullable(request.getScheme()).orElse("http"));

        String host = forwarded.host != null ? forwarded.host
                : firstHeaderValue(request, "X-Forwarded-Host")
                        .orElse(request.getServerName());

        String port;
        if (forwarded.port != null) {
            port = forwarded.port;
        } else if (forwarded.host != null) {
            port = null; // trust forwarded host as authoritative
        } else {
            port = firstHeaderValue(request, "X-Forwarded-Port")
                    .orElse(derivePort(host, scheme, request.getServerPort()));
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

    private ForwardedDetails parseForwardedHeader(HttpServletRequest request) {
        Optional<String> rawForwarded = firstHeaderValue(request, "Forwarded");
        if (rawForwarded.isEmpty()) {
            return ForwardedDetails.EMPTY;
        }

        String host = null;
        String proto = null;
        String port = null;

        String[] directives = rawForwarded.get().split(";");
        for (String directive : directives) {
            String[] keyValue = directive.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }

            String key = keyValue[0].trim().toLowerCase(Locale.ROOT);
            String value = stripQuotes(keyValue[1].trim());

            switch (key) {
                case "host" -> {
                    host = value;
                    int colonIndex = value.lastIndexOf(':');
                    if (colonIndex > value.lastIndexOf(']')) { // handle IPv6 addresses
                        port = value.substring(colonIndex + 1);
                    }
                }
                case "proto" -> proto = value;
                case "for" -> {
                    // RFC 7239 allows port as part of "for" directive (e.g., for=192.0.2.60:443)
                    int colonIndex = value.lastIndexOf(':');
                    if (colonIndex > value.lastIndexOf(']')) {
                        port = value.substring(colonIndex + 1);
                    }
                }
                default -> {
                }
            }
        }

        return new ForwardedDetails(host, proto, port);
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static final class ForwardedDetails {
        static final ForwardedDetails EMPTY = new ForwardedDetails(null, null, null);

        final String host;
        final String proto;
        final String port;

        ForwardedDetails(String host, String proto, String port) {
            this.host = host;
            this.proto = proto;
            this.port = port;
        }
    }
}
