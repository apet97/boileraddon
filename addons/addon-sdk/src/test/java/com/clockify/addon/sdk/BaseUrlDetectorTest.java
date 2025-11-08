package com.clockify.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseUrlDetectorTest {
    private final BaseUrlDetector detector = new BaseUrlDetector();

    @Test
    void detectsForwardedProtoHostAndPort() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=https; host=example.com; port=8443");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/addon");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://example.com:8443/addon", detected.get());
    }

    @Test
    void ignoresAdditionalForwardedSegments() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=https; host=primary.example; port=443, proto=http; host=secondary.example; port=80");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://primary.example:443", detected.get());
    }

    @Test
    void fallsBackToXForwardedHeadersWhenForwardedMissing() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", "https");
        headers.put("X-Forwarded-Host", "proxy.example.com");
        headers.put("X-Forwarded-Port", "443");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/nested/context");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://proxy.example.com:443/nested/context", detected.get());
    }

    @Test
    void trimsQuotedForwardedValues() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=\"https\"; host=\"quoted.example.com\"; port=\"443\"");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://quoted.example.com:443", detected.get());
    }

    @Test
    void omitsServerPortWhenForwardedPortMissing() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=https; host=example.com");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/addon");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://example.com/addon", detected.get());
    }

    @Test
    void doesNotAppendInternalPortWhenXForwardedHostOmitsPort() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", "https");
        headers.put("X-Forwarded-Host", "external.example.com");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/addon");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("https://external.example.com/addon", detected.get());
    }

    @Test
    void fallsBackToServerPortWhenNoForwardingPresent() {
        Map<String, String> headers = new HashMap<>();
        HttpServletRequest request = request(headers, "http", "localhost", 8080, "/addon");

        Optional<String> detected = detector.detectBaseUrl(request);

        assertTrue(detected.isPresent());
        assertEquals("http://localhost:8080/addon", detected.get());
    }

    @Test
    void supportsIpv6HostWithExplicitPortInForwarded() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=https; host=\"[2001:db8::1]:8443\"");
        HttpServletRequest request = request(headers, "http", "ignored", 8080, "/ctx");

        Optional<String> detected = detector.detectBaseUrl(request);
        assertTrue(detected.isPresent());
        assertEquals("https://[2001:db8::1]:8443/ctx", detected.get());
    }

    @Test
    void honorsHostHeaderWithPortWhenNoForwarding() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "example.com:8080");
        HttpServletRequest request = request(headers, "http", "internal", 80, "/app");

        Optional<String> detected = detector.detectBaseUrl(request);
        assertTrue(detected.isPresent());
        assertEquals("http://example.com:8080/app", detected.get());
    }

    @Test
    void forwardedKeysCaseInsensitive() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "Proto=https; Host=MiXeD.example; Port=443");
        HttpServletRequest request = request(headers, "http", "internal", 8080, "/addon");

        Optional<String> detected = detector.detectBaseUrl(request);
        assertTrue(detected.isPresent());
        assertEquals("https://MiXeD.example:443/addon", detected.get());
    }

    @Test
    void ipv6HostWithoutPortAppendsForwardedPort() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Forwarded", "proto=https; host=\"[2001:db8::2]\"; port=443");
        HttpServletRequest request = request(headers, "http", "ignored", 8081, "");

        Optional<String> detected = detector.detectBaseUrl(request);
        assertTrue(detected.isPresent());
        assertEquals("https://[2001:db8::2]:443", detected.get());
    }

    private HttpServletRequest request(Map<String, String> headers, String scheme, String serverName, int port, String contextPath) {
        Map<String, String> normalizedHeaders = new HashMap<>();
        headers.forEach((key, value) -> normalizedHeaders.put(key, value));
        return (HttpServletRequest) Proxy.newProxyInstance(
            HttpServletRequest.class.getClassLoader(),
            new Class[]{HttpServletRequest.class},
            (proxy, method, args) -> {
                String name = method.getName();
                switch (name) {
                    case "getHeader":
                        String headerName = (String) args[0];
                        return normalizedHeaders.get(headerName);
                    case "getScheme":
                        return scheme;
                    case "getServerName":
                        return serverName;
                    case "getServerPort":
                        return port;
                    case "getContextPath":
                        return contextPath;
                    case "getLocales":
                        return enumeration();
                    case "getLocale":
                        return Locale.getDefault();
                    case "getAttributeNames":
                    case "getParameterNames":
                        return enumeration();
                    default:
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(boolean.class)) {
                            return false;
                        }
                        if (returnType.equals(int.class)) {
                            return 0;
                        }
                        if (returnType.equals(long.class)) {
                            return 0L;
                        }
                        if (returnType.equals(double.class)) {
                            return 0d;
                        }
                        if (returnType.equals(float.class)) {
                            return 0f;
                        }
                        return null;
                }
            }
        );
    }

    private Enumeration<String> enumeration() {
        return Collections.emptyEnumeration();
    }
}
