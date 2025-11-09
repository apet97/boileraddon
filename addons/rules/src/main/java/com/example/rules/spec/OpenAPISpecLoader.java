package com.example.rules.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * Loads and caches the Clockify OpenAPI specification.
 * Provides simplified endpoint metadata for dynamic form generation in the IFTTT builder.
 */
public class OpenAPISpecLoader {

    private static final Logger logger = LoggerFactory.getLogger(OpenAPISpecLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static JsonNode cachedSpec = null;
    private static List<EndpointInfo> cachedEndpoints = null;

    /**
     * Load the OpenAPI spec from the filesystem or classpath.
     * Tries openapi (1).json at repo root first, then downloads/openapi (1).json,
     * then classpath (clockify-openapi.json), then falls back to
     * dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json.
     */
    public static synchronized JsonNode loadSpec() {
        if (cachedSpec != null) {
            return cachedSpec;
        }

        // Try root directory first (preferred location)
        try {
            java.nio.file.Path rootPath = java.nio.file.Paths.get("openapi (1).json");
            if (java.nio.file.Files.exists(rootPath)) {
                cachedSpec = mapper.readTree(rootPath.toFile());
                logger.info("Loaded OpenAPI spec from root: {}", rootPath);
                return cachedSpec;
            }
        } catch (Exception e) {
            logger.warn("Failed to load OpenAPI spec from root", e);
        }

        // Try downloads directory
        try {
            java.nio.file.Path downloadsPath = java.nio.file.Paths.get("downloads", "openapi (1).json");
            if (java.nio.file.Files.exists(downloadsPath)) {
                cachedSpec = mapper.readTree(downloadsPath.toFile());
                logger.info("Loaded OpenAPI spec from downloads: {}", downloadsPath);
                return cachedSpec;
            }
        } catch (Exception e) {
            logger.warn("Failed to load OpenAPI spec from downloads", e);
        }

        // Try classpath resource next
        try (InputStream is = OpenAPISpecLoader.class.getClassLoader()
                .getResourceAsStream("clockify-openapi.json")) {
            if (is != null) {
                cachedSpec = mapper.readTree(is);
                logger.info("Loaded OpenAPI spec from classpath: clockify-openapi.json");
                return cachedSpec;
            }
        } catch (Exception e) {
            logger.warn("Failed to load OpenAPI spec from classpath", e);
        }

        // Fallback: try reading from dev-docs snapshot
        try {
            java.nio.file.Path fallbackPath = java.nio.file.Paths.get(
                    "dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json");
            if (java.nio.file.Files.exists(fallbackPath)) {
                cachedSpec = mapper.readTree(fallbackPath.toFile());
                logger.info("Loaded OpenAPI spec from dev-docs snapshot: {}", fallbackPath);
                return cachedSpec;
            }
        } catch (Exception e) {
            logger.error("Failed to load OpenAPI spec from dev-docs snapshot", e);
        }

        logger.error("OpenAPI spec not found in any expected location");
        return mapper.createObjectNode();
    }

    /**
     * Parse the OpenAPI spec and return a list of endpoint metadata.
     * Grouped by tag, includes method, path, summary, and simplified parameter/body schemas.
     */
    public static synchronized List<EndpointInfo> getEndpoints() {
        if (cachedEndpoints != null) {
            return cachedEndpoints;
        }

        JsonNode spec = loadSpec();
        List<EndpointInfo> endpoints = new ArrayList<>();

        JsonNode paths = spec.path("paths");
        if (!paths.isObject()) {
            cachedEndpoints = endpoints;
            return endpoints;
        }

        paths.fields().forEachRemaining(pathEntry -> {
            String path = pathEntry.getKey();
            JsonNode methods = pathEntry.getValue();

            methods.fields().forEachRemaining(methodEntry -> {
                String method = methodEntry.getKey().toUpperCase();
                if (!Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE").contains(method)) {
                    return; // Skip non-HTTP methods
                }

                JsonNode operation = methodEntry.getValue();
                EndpointInfo info = new EndpointInfo();
                info.method = method;
                info.path = path;
                info.operationId = operation.path("operationId").asText("");
                info.summary = operation.path("summary").asText("");
                info.description = operation.path("description").asText("");

                // Extract tags
                JsonNode tagsNode = operation.path("tags");
                if (tagsNode.isArray()) {
                    tagsNode.forEach(tag -> info.tags.add(tag.asText()));
                }
                if (info.tags.isEmpty()) {
                    info.tags.add("Other");
                }

                // Extract parameters (path, query)
                JsonNode params = operation.path("parameters");
                if (params.isArray()) {
                    params.forEach(param -> {
                        ParameterInfo pi = new ParameterInfo();
                        pi.name = param.path("name").asText("");
                        pi.in = param.path("in").asText("");
                        pi.required = param.path("required").asBoolean(false);
                        pi.description = param.path("description").asText("");
                        pi.type = extractType(param.path("schema"));
                        pi.enumValues = extractEnum(param.path("schema"));
                        info.parameters.add(pi);
                    });
                }

                // Extract request body schema (simplified)
                JsonNode requestBody = operation.path("requestBody");
                if (!requestBody.isMissingNode()) {
                    JsonNode content = requestBody.path("content");
                    JsonNode jsonContent = content.path("application/json");
                    if (!jsonContent.isMissingNode()) {
                        JsonNode schema = jsonContent.path("schema");
                        info.requestBodySchema = simplifySchema(schema, spec);
                        info.hasRequestBody = true;
                        info.requestBodyRequired = requestBody.path("required").asBoolean(false);
                    }
                }

                endpoints.add(info);
            });
        });

        cachedEndpoints = endpoints;
        logger.info("Parsed {} endpoints from OpenAPI spec", endpoints.size());
        return endpoints;
    }

    /**
     * Get endpoints grouped by tag for easier navigation.
     */
    public static Map<String, List<EndpointInfo>> getEndpointsByTag() {
        List<EndpointInfo> endpoints = getEndpoints();
        Map<String, List<EndpointInfo>> grouped = new LinkedHashMap<>();

        for (EndpointInfo ep : endpoints) {
            for (String tag : ep.tags) {
                grouped.computeIfAbsent(tag, k -> new ArrayList<>()).add(ep);
            }
        }

        return grouped;
    }

    /**
     * Convert endpoints to JSON for API response.
     */
    public static JsonNode endpointsToJson() {
        Map<String, List<EndpointInfo>> grouped = getEndpointsByTag();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode tagsArray = mapper.createArrayNode();

        grouped.forEach((tag, endpoints) -> {
            ObjectNode tagNode = mapper.createObjectNode();
            tagNode.put("tag", tag);
            ArrayNode endpointsArray = mapper.createArrayNode();

            for (EndpointInfo ep : endpoints) {
                ObjectNode epNode = mapper.createObjectNode();
                epNode.put("method", ep.method);
                epNode.put("path", ep.path);
                epNode.put("operationId", ep.operationId);
                epNode.put("summary", ep.summary);
                epNode.put("description", ep.description);

                ArrayNode paramsArray = mapper.createArrayNode();
                for (ParameterInfo pi : ep.parameters) {
                    ObjectNode pNode = mapper.createObjectNode();
                    pNode.put("name", pi.name);
                    pNode.put("in", pi.in);
                    pNode.put("required", pi.required);
                    pNode.put("type", pi.type);
                    pNode.put("description", pi.description);
                    if (pi.enumValues != null && !pi.enumValues.isEmpty()) {
                        ArrayNode enumArray = mapper.createArrayNode();
                        pi.enumValues.forEach(enumArray::add);
                        pNode.set("enum", enumArray);
                    }
                    paramsArray.add(pNode);
                }
                epNode.set("parameters", paramsArray);

                if (ep.hasRequestBody) {
                    epNode.put("hasRequestBody", true);
                    epNode.put("requestBodyRequired", ep.requestBodyRequired);
                    epNode.set("requestBodySchema", ep.requestBodySchema);
                }

                endpointsArray.add(epNode);
            }

            tagNode.set("endpoints", endpointsArray);
            tagsArray.add(tagNode);
        });

        root.set("tags", tagsArray);
        root.put("count", cachedEndpoints != null ? cachedEndpoints.size() : 0);
        return root;
    }

    private static String extractType(JsonNode schema) {
        if (schema.has("type")) {
            return schema.get("type").asText("string");
        }
        return "string";
    }

    private static List<String> extractEnum(JsonNode schema) {
        List<String> enumValues = new ArrayList<>();
        if (schema.has("enum") && schema.get("enum").isArray()) {
            schema.get("enum").forEach(v -> enumValues.add(v.asText()));
        }
        return enumValues;
    }

    /**
     * Simplify a JSON schema for display purposes (avoid deep nesting).
     * Returns a simplified representation of properties with types and required fields.
     */
    private static JsonNode simplifySchema(JsonNode schema, JsonNode spec) {
        if (schema.has("$ref")) {
            // Resolve reference
            String ref = schema.get("$ref").asText();
            schema = resolveRef(ref, spec);
        }

        ObjectNode simplified = mapper.createObjectNode();
        simplified.put("type", schema.path("type").asText("object"));

        JsonNode properties = schema.path("properties");
        if (properties.isObject()) {
            ArrayNode fieldsArray = mapper.createArrayNode();
            final JsonNode schemaFinal = schema; // for lambda capture
            properties.fields().forEachRemaining(field -> {
                ObjectNode fieldNode = mapper.createObjectNode();
                fieldNode.put("name", field.getKey());
                JsonNode fieldSchema = field.getValue();

                if (fieldSchema.has("$ref")) {
                    fieldSchema = resolveRef(fieldSchema.get("$ref").asText(), spec);
                }

                fieldNode.put("type", fieldSchema.path("type").asText("string"));
                fieldNode.put("description", fieldSchema.path("description").asText(""));

                // Check if required
                JsonNode required = schemaFinal.path("required");
                boolean isRequired = false;
                if (required.isArray()) {
                    for (JsonNode r : required) {
                        if (r.asText().equals(field.getKey())) {
                            isRequired = true;
                            break;
                        }
                    }
                }
                fieldNode.put("required", isRequired);

                // Add enum if present
                if (fieldSchema.has("enum")) {
                    ArrayNode enumArray = mapper.createArrayNode();
                    fieldSchema.get("enum").forEach(e -> enumArray.add(e.asText()));
                    fieldNode.set("enum", enumArray);
                }

                fieldsArray.add(fieldNode);
            });
            simplified.set("fields", fieldsArray);
        }

        return simplified;
    }

    private static JsonNode resolveRef(String ref, JsonNode spec) {
        // Simple $ref resolver for #/components/schemas/SomeName
        if (ref.startsWith("#/")) {
            String[] parts = ref.substring(2).split("/");
            JsonNode node = spec;
            for (String part : parts) {
                node = node.path(part);
                if (node.isMissingNode()) {
                    return mapper.createObjectNode();
                }
            }
            return node;
        }
        return mapper.createObjectNode();
    }

    public static class EndpointInfo {
        public String method;
        public String path;
        public String operationId;
        public String summary;
        public String description;
        public List<String> tags = new ArrayList<>();
        public List<ParameterInfo> parameters = new ArrayList<>();
        public boolean hasRequestBody = false;
        public boolean requestBodyRequired = false;
        public JsonNode requestBodySchema;
    }

    public static class ParameterInfo {
        public String name;
        public String in; // path, query, header, cookie
        public boolean required;
        public String type;
        public String description;
        public List<String> enumValues = new ArrayList<>();
    }
}
