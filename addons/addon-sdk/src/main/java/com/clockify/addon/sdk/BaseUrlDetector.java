package com.clockify.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Normalizes Clockify add-on base URLs by respecting forwarded headers.
 */
class BaseUrlDetector {
    Optional<String> detectBaseUrl(HttpServletRequest request) {
        Optional<String> forwardedProto = forwardedHeaderValue(request, "proto")
            .or(() -> firstHeaderValue(request, "X-Forwarded-Proto"));
        Optional<String> forwardedHost = forwardedHeaderValue(request, "host")
            .or(() -> firstHeaderValue(request, "X-Forwarded-Host"));
        Optional<String> forwardedPort = forwardedHeaderValue(request, "port")
            .or(() -> firstHeaderValue(request, "X-Forwarded-Port"));
        Optional<String> hostHeader = firstHeaderValue(request, "Host");

        Optional<String> forwardedProtoValue = forwardedProto.filter(BaseUrlDetector::isNotBlank);
        Optional<String> forwardedHostValue = forwardedHost.filter(BaseUrlDetector::isNotBlank);
        Optional<String> forwardedPortValue = forwardedPort.filter(BaseUrlDetector::isNotBlank);

        String scheme = forwardedProtoValue
            .orElseGet(() -> Optional.ofNullable(request.getScheme()).filter(BaseUrlDetector::isNotBlank).orElse("http"));
        String host = forwardedHostValue
            .or(() -> hostHeader)
            .orElse(request.getServerName());
        String port = forwardedPortValue.orElse(null);

        boolean hasForwardingInfo = forwardedProtoValue.isPresent()
            || forwardedHostValue.isPresent()
            || forwardedPortValue.isPresent();

        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        if ((port == null || port.isBlank()) && !hostContainsPort(host) && !hasForwardingInfo) {
            int serverPort = request.getServerPort();
            if (shouldAppendServerPort(scheme, serverPort)) {
                port = Integer.toString(serverPort);
            }
        }

        if (port != null && !port.isBlank() && !hostContainsPort(host)) {
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

    private static boolean hostContainsPort(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        if (host.startsWith("[")) {
            int endBracket = host.indexOf(']');
            return endBracket > 0 && endBracket + 1 < host.length() && host.charAt(endBracket + 1) == ':';
        }
        int firstColon = host.indexOf(':');
        if (firstColon < 0) {
            return false;
        }
        return host.indexOf(':', firstColon + 1) < 0;
    }

    private static boolean shouldAppendServerPort(String scheme, int serverPort) {
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

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
