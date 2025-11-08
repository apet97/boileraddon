package com.clockify.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Normalizes Clockify add-on base URLs by respecting forwarded headers.
 */
class BaseUrlDetector {
    Optional<String> detectBaseUrl(HttpServletRequest request) {
        Optional<String> forwardedProto = forwardedOrHeader(request, "proto", "X-Forwarded-Proto");
        Optional<String> forwardedHost = forwardedOrHeader(request, "host", "X-Forwarded-Host");
        Optional<String> forwardedPort = forwardedOrHeader(request, "port", "X-Forwarded-Port");
        Optional<String> hostHeader = firstHeaderValue(request, "Host");

        String scheme = forwardedProto.filter(value -> !value.isBlank()).orElseGet(() -> {
            String requestScheme = request.getScheme();
            return (requestScheme == null || requestScheme.isBlank()) ? "http" : requestScheme;
        });
        String host = forwardedHost.filter(value -> !value.isBlank())
                .or(() -> hostHeader)
                .orElse(request.getServerName());

        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        boolean hasForwardingHeaders = forwardedProto.isPresent() || forwardedHost.isPresent() || forwardedPort.isPresent();
        String port = forwardedPort.filter(value -> !value.isBlank()).orElse(null);
        if (port == null) {
            port = inferPort(host, scheme, request, hasForwardingHeaders);
        }

        if (port != null && !port.isBlank() && !hostHasExplicitPort(host)) {
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

    private Optional<String> forwardedOrHeader(HttpServletRequest request, String forwardedKey, String headerName) {
        Optional<String> forwarded = forwardedHeaderValue(request, forwardedKey);
        if (forwarded.isPresent()) {
            return forwarded;
        }
        Optional<String> header = firstHeaderValue(request, headerName);
        if (header.isPresent()) {
            return header;
        }
        return Optional.empty();
    }

    private String inferPort(String host, String scheme, HttpServletRequest request, boolean hasForwardingHeaders) {
        if (hostHasExplicitPort(host)) {
            return null;
        }

        if (hasForwardingHeaders) {
            return null;
        }

        int serverPort = request.getServerPort();
        if (!shouldIncludeServerPort(scheme, serverPort)) {
            return null;
        }
        return Integer.toString(serverPort);
    }

    private boolean shouldIncludeServerPort(String scheme, int serverPort) {
        if (serverPort <= 0) {
            return false;
        }

        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase();
        if (("http".equals(normalizedScheme) && serverPort == 80)
                || ("https".equals(normalizedScheme) && serverPort == 443)) {
            return false;
        }
        return true;
    }

    private boolean hostHasExplicitPort(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        if (host.startsWith("[")) {
            int endBracket = host.indexOf(']');
            if (endBracket < 0) {
                return false;
            }
            return host.indexOf(':', endBracket) > endBracket;
        }

        return host.contains(":");
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
