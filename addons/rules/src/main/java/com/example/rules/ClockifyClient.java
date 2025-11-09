package com.example.rules;

import com.clockify.addon.sdk.http.ClockifyHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Thin wrapper around the SDK ClockifyHttpClient for the Rules add-on use cases.
 */
public class ClockifyClient {
    private final ClockifyHttpClient http;
    private final String token;
    private final ObjectMapper om = new ObjectMapper();

    public ClockifyClient(String baseUrl, String addonToken) {
        this.http = new ClockifyHttpClient(baseUrl);
        this.token = addonToken;
    }

    /**
     * Generic OpenAPI call helper used by the IFTTT engine.
     * Method is one of GET, POST, PUT, PATCH, DELETE. Path must start with "/" (e.g., "/v1/workspaces/{id}/…").
     * Body is JSON string for methods that send a payload; ignored for GET/DELETE.
     */
    public void openapiCall(String method, String path, String jsonBody) throws Exception {
        String m = method != null ? method.trim().toUpperCase(Locale.ROOT) : "";
        switch (m) {
            case "GET" -> http.get(path, token, Map.of());
            case "POST" -> http.postJson(path, token, jsonBody != null ? jsonBody : "{}", Map.of());
            case "PUT" -> http.putJson(path, token, jsonBody != null ? jsonBody : "{}", Map.of());
            case "PATCH" -> http.putJson(path, token, jsonBody != null ? jsonBody : "{}", Map.of()); // fallback if PATCH not present
            case "DELETE" -> http.delete(path, token, Map.of());
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    public JsonNode getTags(String workspaceId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/tags", token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode createTag(String workspaceId, String tagName) throws Exception {
        ObjectNode body = om.createObjectNode().put("name", tagName);
        HttpResponse<String> resp = http.postJson("/workspaces/" + workspaceId + "/tags", token, body.toString(), Map.of());
        ensure2xx(resp, 201);
        return om.readTree(resp.body());
    }

    public JsonNode getProjects(String workspaceId, boolean archived) throws Exception {
        String qs = "?archived=" + archived + "&page-size=500";
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/projects" + qs, token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode getClients(String workspaceId, boolean archived) throws Exception {
        String qs = "?archived=" + archived + "&page-size=500";
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/clients" + qs, token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode getUsers(String workspaceId) throws Exception {
        String qs = "?page-size=500";
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/users" + qs, token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode getTasks(String workspaceId, String projectId) throws Exception {
        String qs = "?page-size=500";
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks" + qs, token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
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
}
