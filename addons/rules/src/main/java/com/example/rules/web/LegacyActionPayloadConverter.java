package com.example.rules.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility to translate legacy IFTTT payloads ({@code endpoint/params/body}) into
 * the modern {@code args} map expected by {@code openapi_call} actions.
 */
public final class LegacyActionPayloadConverter {

    private LegacyActionPayloadConverter() {
    }

    /**
     * Normalize legacy openapi_call actions so they contain an {@code args} object.
     *
     * @param mapper object mapper for serialization
     * @param root   incoming JSON payload
     * @return normalized node (same instance when no changes were required)
     */
    public static JsonNode normalize(ObjectMapper mapper, JsonNode root) {
        if (!(root instanceof ObjectNode objectNode)) {
            return root;
        }

        JsonNode actionsNode = objectNode.get("actions");
        if (!(actionsNode instanceof ArrayNode actionsArray)) {
            return root;
        }

        for (JsonNode node : actionsArray) {
            if (!(node instanceof ObjectNode actionNode)) {
                continue;
            }
            if (actionNode.has("args")) {
                continue;
            }
            if (!"openapi_call".equals(actionNode.path("type").asText())) {
                continue;
            }

            JsonNode endpointNode = actionNode.get("endpoint");
            if (!(endpointNode instanceof ObjectNode endpointObject)) {
                continue;
            }

            String method = endpointObject.path("method").asText(null);
            String pathTemplate = endpointObject.path("path").asText(null);
            if (method == null || pathTemplate == null || pathTemplate.isBlank()) {
                continue;
            }

            String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
            String normalizedPath = buildPath(pathTemplate, actionNode.get("params"));
            String body = extractBody(mapper, actionNode.get("body"));

            ObjectNode args = mapper.createObjectNode();
            args.put("method", normalizedMethod);
            args.put("path", normalizedPath);
            if (body != null && !body.isBlank()) {
                args.put("body", body);
            }

            actionNode.set("args", args);
            actionNode.remove(List.of("endpoint", "params", "body"));
        }

        return root;
    }

    private static String buildPath(String basePath, JsonNode paramsNode) {
        if (basePath == null) {
            return null;
        }

        String path = basePath;
        List<String> queryParts = new ArrayList<>();

        if (paramsNode instanceof ObjectNode paramsObject) {
            Iterator<Map.Entry<String, JsonNode>> fields = paramsObject.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode paramNode = entry.getValue();
                if (!(paramNode instanceof ObjectNode paramObject)) {
                    continue;
                }

                String location = paramObject.path("in").asText(null);
                String value = paramObject.path("value").asText("");

                if ("path".equals(location)) {
                    if (!value.isBlank()) {
                        path = path.replace("{" + name + "}", value);
                    }
                } else if ("query".equals(location)) {
                    queryParts.add(name + "=" + value);
                }
            }
        }

        if (!queryParts.isEmpty()) {
            String delimiter = path.contains("?") ? "&" : "?";
            path = path + delimiter + String.join("&", queryParts);
        }

        return path;
    }

    private static String extractBody(ObjectMapper mapper, JsonNode bodyNode) {
        if (bodyNode == null || bodyNode.isNull()) {
            return null;
        }

        if (!bodyNode.isObject()) {
            return bodyNode.asText();
        }

        ObjectNode cleaned = mapper.createObjectNode();
        bodyNode.fields().forEachRemaining(entry -> {
            JsonNode valueNode = entry.getValue();
            if (valueNode == null || valueNode.isNull()) {
                return;
            }
            if (valueNode.isTextual() && valueNode.asText().trim().isEmpty()) {
                return;
            }
            cleaned.set(entry.getKey(), valueNode);
        });

        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned.toString();
    }
}
