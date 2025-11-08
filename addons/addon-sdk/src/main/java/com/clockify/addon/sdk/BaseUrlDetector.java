package com.clockify.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Normalizes Clockify add-on base URLs by respecting forwarded headers.
 */
class BaseUrlDetector {
    Optional<String> detectBaseUrl(HttpServletRequest request) {
        Optional<String> forwardedProto = headerOrForwarded(request, "proto", "X-Forwarded-Proto", request.getScheme());
        Optional<String> forwardedHost = headerOrForwarded(request, "host", "X-Forwarded-Host", request.getServerName());
        Optional<String> forwardedPort = headerOrForwarded(request, "port", "X-Forwarded-Port", Integer.toString(request.getServerPort()));
        Optional<String> hostHeader = firstHeaderValue(request, "Host");

        String scheme = forwardedProto.orElse("http");
        String host = forwardedHost.or(() -> hostHeader).orElse(request.getServerName());
        String port = forwardedPort.orElse(null);

        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        if (port != null && !port.isBlank() && !host.contains(":")) {
            host = host + ":" + port;
        }

        String contextPath = Optional.ofNullable(request.getContextPath()).orElse("");
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        String normalized = (scheme + "://" + host + contextPath).replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    private Optional<String> headerOrForwarded(HttpServletRequest request, String forwardedKey, String headerName, String fallback) {
        Optional<String> forwarded = forwardedHeaderValue(request, forwardedKey);
        if (forwarded.isPresent()) {
            return forwarded;
        }
        Optional<String> header = firstHeaderValue(request, headerName);
        if (header.isPresent()) {
            return header;
        }
        return Optional.ofNullable(fallback);
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
        String targetKey = parameterName == null ? "" : parameterName.trim().toLowerCase();
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }
            String key = pair.substring(0, equalsIndex).trim().toLowerCase();
            if (!key.equals(targetKey)) {
                continue;
            }
            String value = pair.substring(equalsIndex + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            if (value.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(value);
        }
        return Optional.empty();
    }
}
