package com.example.rules;

import com.clockify.addon.sdk.http.ClockifyHttpClient;
import com.example.rules.engine.OpenApiCallConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around the SDK ClockifyHttpClient for the Rules add-on use cases.
 */
public class ClockifyClient {
    private static final int PAGE_SIZE = 500;

    private final ClockifyHttpClient http;
    private final String token;
    private final ObjectMapper om = new ObjectMapper();

    public ClockifyClient(String baseUrl, String addonToken) {
        this(new ClockifyHttpClient(baseUrl), addonToken);
    }

    ClockifyClient(ClockifyHttpClient httpClient, String addonToken) {
        this.http = httpClient;
        this.token = addonToken;
    }

    public HttpResponse<String> openapiCall(OpenApiCallConfig.HttpMethod method, String path, String jsonBody) throws Exception {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        String normalizedBody = (jsonBody == null || jsonBody.isBlank()) ? "{}" : jsonBody;
        return switch (method) {
            case GET -> http.get(path, token, Map.of());
            case POST -> http.postJsonWithIdempotency(path, token, normalizedBody, Map.of());
            case PUT -> http.putJson(path, token, normalizedBody, Map.of());
            case PATCH -> http.patchJson(path, token, normalizedBody, Map.of());
            case DELETE -> http.delete(path, token, Map.of());
        };
    }

    public JsonNode getTags(String workspaceId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/tags", token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode createTag(String workspaceId, String tagName) throws Exception {
        ObjectNode body = om.createObjectNode().put("name", tagName);
        HttpResponse<String> resp = http.postJsonWithIdempotency("/workspaces/" + workspaceId + "/tags", token, body.toString(), Map.of());
        ensure2xx(resp, 201);
        return om.readTree(resp.body());
    }

    public JsonNode getProjects(String workspaceId, boolean archived) throws Exception {
        return fetchPaginatedArray(
                "/workspaces/" + workspaceId + "/projects",
                Map.of("archived", String.valueOf(archived))
        );
    }

    public JsonNode getClients(String workspaceId, boolean archived) throws Exception {
        return fetchPaginatedArray(
                "/workspaces/" + workspaceId + "/clients",
                Map.of("archived", String.valueOf(archived))
        );
    }

    public JsonNode getUsers(String workspaceId) throws Exception {
        return fetchPaginatedArray("/workspaces/" + workspaceId + "/users");
    }

    public JsonNode getTasks(String workspaceId, String projectId) throws Exception {
        return fetchPaginatedArray("/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks");
    }

    public ObjectNode getTimeEntry(String workspaceId, String timeEntryId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId, token, Map.of());
        ensure2xx(resp, 200);
        JsonNode n = om.readTree(resp.body());
        if (!(n instanceof ObjectNode)) throw new IllegalStateException("Time entry is not an object");
        return (ObjectNode) n;
    }

    public ObjectNode updateTimeEntry(String workspaceId, String timeEntryId, ObjectNode patch) throws Exception {
        // The update endpoint expects certain top-level fields (start/end). Build from GET payload.
        ObjectNode existing = getTimeEntry(workspaceId, timeEntryId);
        ObjectNode req = existing.deepCopy();

        // Move timeInterval.start/end to root if not present (compat with v1 behavior)
        if (!req.has("start") && existing.has("timeInterval") && existing.get("timeInterval").has("start")) {
            req.set("start", existing.get("timeInterval").get("start"));
        }
        if (!req.has("end") && existing.has("timeInterval") && existing.get("timeInterval").has("end")) {
            req.set("end", existing.get("timeInterval").get("end"));
        }

        // Apply provided patch keys onto req
        patch.fields().forEachRemaining(e -> req.set(e.getKey(), e.getValue()));

        HttpResponse<String> resp = http.putJson("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId,
                token, req.toString(), Map.of());
        ensure2xx(resp, 200);
        JsonNode out = om.readTree(resp.body());
        if (!(out instanceof ObjectNode)) throw new IllegalStateException("Update response is not an object");
        return (ObjectNode) out;
    }

    public static Map<String, String> mapTagsByNormalizedName(JsonNode tagsArray) {
        Map<String, String> map = new LinkedHashMap<>();
        if (tagsArray != null && tagsArray.isArray()) {
            for (JsonNode t : tagsArray) {
                if (t != null && t.has("name") && t.has("id")) {
                    String norm = normalizeTagName(t.get("name").asText());
                    String id = t.get("id").asText();
                    if (norm != null && !map.containsKey(norm)) map.put(norm, id);
                }
            }
        }
        return map;
    }

    public static String normalizeTagName(String name) {
        if (name == null) return null;
        String t = name.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }

    public static ArrayNode ensureTagIdsArray(ObjectNode timeEntry, ObjectMapper om) {
        if (timeEntry.has("tagIds") && timeEntry.get("tagIds").isArray()) {
            return (ArrayNode) timeEntry.get("tagIds");
        }
        ArrayNode arr = om.createArrayNode();
        timeEntry.set("tagIds", arr);
        return arr;
    }

    private static void ensure2xx(HttpResponse<String> resp, int expected) {
        int code = resp.statusCode();
        if (code != expected && (code / 100) != 2) {
            throw new RuntimeException("Clockify API error: status=" + code + " body=" + resp.body());
        }
    }

    private ArrayNode fetchPaginatedArray(String basePath) throws Exception {
        return fetchPaginatedArray(basePath, Map.of());
    }

    private ArrayNode fetchPaginatedArray(String basePath, Map<String, String> queryParams) throws Exception {
        ArrayNode aggregated = om.createArrayNode();
        int page = 1;
        while (true) {
            String pathWithQuery = buildPaginatedPath(basePath, queryParams, page);
            HttpResponse<String> resp = http.get(pathWithQuery, token, Map.of());
            ensure2xx(resp, 200);
            JsonNode node = om.readTree(resp.body());
            if (node == null || node.isNull()) {
                break;
            }
            if (!node.isArray()) {
                throw new IllegalStateException("Clockify API response for " + basePath + " is not an array");
            }
            ArrayNode pageItems = (ArrayNode) node;
            if (pageItems.isEmpty()) {
                break;
            }
            aggregated.addAll(pageItems);

            Optional<String> nextPage = resp.headers().firstValue("X-Next-Page");
            if (nextPage.isPresent() && !nextPage.get().isBlank()) {
                try {
                    int parsed = Integer.parseInt(nextPage.get());
                    if (parsed <= page) {
                        break;
                    }
                    page = parsed;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Fallback to size-based pagination below
                }
            }

            if (pageItems.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }
        return aggregated;
    }

    private String buildPaginatedPath(String basePath, Map<String, String> queryParams, int page) {
        StringBuilder sb = new StringBuilder(basePath);
        if (!basePath.contains("?")) {
            sb.append('?');
        } else if (!basePath.endsWith("?") && !basePath.endsWith("&")) {
            sb.append('&');
        }
        sb.append("page-size=").append(PAGE_SIZE);
        sb.append("&page=").append(page);
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (e.getValue() == null) continue;
                sb.append('&')
                        .append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
