package com.example.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves placeholders in strings using webhook payload data.
 * Placeholders follow the format {{field.path}} where field.path is a dotted JSON path.
 *
 * Examples:
 * - {{id}} resolves to payload.id
 * - {{project.name}} resolves to payload.project.name
 * - {{user.email}} resolves to payload.user.email
 */
public class PlaceholderResolver {

    private static final Logger logger = LoggerFactory.getLogger(PlaceholderResolver.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Resolve all placeholders in a string against the given payload.
     */
    public static String resolve(String template, JsonNode payload) {
        if (template == null || template.isBlank() || payload == null) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1); // e.g., "project.name"
            String value = extractValue(payload, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extract a value from the payload using a dotted path.
     * Examples: "id", "project.name", "user.email", "workspaceId"
     */
    private static String extractValue(JsonNode payload, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        JsonNode node = payload;
        String[] parts = path.trim().split("\\.");

        for (String part : parts) {
            if (node == null || node.isMissingNode()) {
                logger.debug("Path segment '{}' not found in payload", part);
                return null;
            }
            node = node.path(part);
        }

        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        // Return as text
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        } else if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return String.valueOf(node.asLong());
            } else {
                return String.valueOf(node.asDouble());
            }
        } else if (node.isArray() || node.isObject()) {
            // Return JSON string for complex types
            return node.toString();
        }

        return null;
    }

    /**
     * Resolve placeholders in a JSON object (recursively for all string values).
     */
    public static JsonNode resolveInJson(JsonNode template, JsonNode payload) {
        if (template == null || template.isMissingNode()) {
            return template;
        }

        if (template.isTextual()) {
            String resolved = resolve(template.asText(), payload);
            return com.fasterxml.jackson.databind.node.TextNode.valueOf(resolved);
        } else if (template.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode result =
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            template.fields().forEachRemaining(entry -> {
                result.set(entry.getKey(), resolveInJson(entry.getValue(), payload));
            });
            return result;
        } else if (template.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode result =
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            template.forEach(item -> result.add(resolveInJson(item, payload)));
            return result;
        }

        return template;
    }
}
