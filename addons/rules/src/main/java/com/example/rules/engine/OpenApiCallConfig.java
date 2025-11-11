package com.example.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

/**
 * Typed representation of an {@code openapi_call} action. Validates method/path upfront.
 */
public final class OpenApiCallConfig {
    private final HttpMethod method;
    private final String pathTemplate;
    private final JsonNode bodyTemplate;

    private OpenApiCallConfig(HttpMethod method, String pathTemplate, JsonNode bodyTemplate) {
        this.method = method;
        this.pathTemplate = pathTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public static OpenApiCallConfig from(Action action, ObjectMapper objectMapper) {
        if (action == null || !"openapi_call".equals(action.getType())) {
            throw new IllegalArgumentException("Only openapi_call actions can be converted");
        }
        Map<String, String> args = action.getArgs();
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("openapi_call action has no args");
        }
        HttpMethod method = HttpMethod.from(args.get("method"));
        if (method == null) {
            throw new IllegalArgumentException("openapi_call.method is required (GET/POST/PUT/PATCH/DELETE)");
        }
        String path = args.get("path");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("openapi_call.path is required");
        }
        JsonNode bodyTemplate = null;
        String body = args.get("body");
        if (body != null && !body.isBlank()) {
            try {
                bodyTemplate = objectMapper.readTree(body);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON body template", e);
            }
        }
        return new OpenApiCallConfig(method, path.trim(), bodyTemplate);
    }

    public ResolvedCall resolve(JsonNode payload) {
        String resolvedPath = PlaceholderResolver.resolveForPath(pathTemplate, payload);
        String resolvedBody = null;
        if (bodyTemplate != null) {
            JsonNode resolved = PlaceholderResolver.resolveInJson(bodyTemplate, payload);
            resolvedBody = resolved.toString();
        }
        return new ResolvedCall(method, resolvedPath, resolvedBody);
    }

    public enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE;

        public static HttpMethod from(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return HttpMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    public record ResolvedCall(HttpMethod method, String path, String body) {}
}
