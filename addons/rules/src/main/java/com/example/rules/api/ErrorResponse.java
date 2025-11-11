package com.example.rules.api;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.example.rules.config.RuntimeFlags;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * Standardized error response builder using RFC 7807 (problem+json) with correlation IDs.
 */
public final class ErrorResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MEDIA_JSON = "application/problem+json";
    private static final boolean INCLUDE_DETAILS = RuntimeFlags.isDevEnvironment();

    private ErrorResponse() {
    }

    public static HttpResponse of(int status, String code, String message, HttpServletRequest request, boolean retryable) {
        return of(status, code, message, request, retryable, null, null);
    }

    public static HttpResponse of(int status, String code, String message, HttpServletRequest request,
                                  boolean retryable, String details) {
        return of(status, code, message, request, retryable, details, null);
    }

    public static HttpResponse of(int status, String code, String message, HttpServletRequest request,
                                  boolean retryable, String details, JsonNode errors) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("type", typeFor(code));
            node.put("title", titleFor(code));
            node.put("status", status);
            node.put("detail", message);
            node.put("instance", instance(request));
            node.put("code", code);
            node.put("retryable", retryable);
            node.put("requestId", requestId(request));
            if (INCLUDE_DETAILS && details != null && !details.isBlank()) {
                node.put("details", details);
            }
            if (errors != null && !errors.isEmpty()) {
                node.set("errors", errors);
            }
            return HttpResponse.error(status, node.toString(), MEDIA_JSON);
        } catch (Exception e) {
            return HttpResponse.error(status, String.format("{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}",
                    typeFor(code), titleFor(code), status, message), MEDIA_JSON);
        }
    }

    private static String typeFor(String code) {
        if (code == null || code.isBlank()) {
            return "about:blank";
        }
        return "https://developer.clockify.me/addons/errors/" + code.toLowerCase(Locale.ROOT);
    }

    private static String titleFor(String code) {
        if (code == null || code.isBlank()) {
            return "Error";
        }
        // Convert "PROJECTS.INSUFFICIENT_PERMISSIONS" to "Insufficient Permissions"
        String[] parts = code.split("\\.");
        String lastPart = parts[parts.length - 1];
        String[] words = lastPart.split("_");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                title.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1).toLowerCase())
                     .append(" ");
            }
        }
        return title.toString().trim();
    }

    private static String requestId(HttpServletRequest request) {
        Object attr = request == null ? null : request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }

    private static String instance(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String path = request.getRequestURI();
        return path == null ? "" : path;
    }

    public static ArrayNode validationErrors(String... messages) {
        ArrayNode array = MAPPER.createArrayNode();
        if (messages != null) {
            for (String msg : messages) {
                if (msg != null && !msg.isBlank()) {
                    array.add(MAPPER.createObjectNode().put("message", msg));
                }
            }
        }
        return array;
    }
}
