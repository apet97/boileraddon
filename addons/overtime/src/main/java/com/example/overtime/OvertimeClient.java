package com.example.overtime;

import com.clockify.addon.sdk.http.ClockifyHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class OvertimeClient {
    private final ClockifyHttpClient http;
    private final String token;
    private final ObjectMapper om = new ObjectMapper();

    public OvertimeClient(String baseUrl, String token) {
        this.http = new ClockifyHttpClient(baseUrl, Duration.ofSeconds(10), 3);
        this.token = token;
    }

    public ObjectNode getTimeEntry(String ws, String entryId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + ws + "/time-entries/" + entryId, token, Map.of());
        JsonNode n = om.readTree(resp.body());
        if (!(n instanceof ObjectNode)) throw new IllegalStateException("time entry is not object");
        return (ObjectNode) n;
    }

    public JsonNode getTags(String ws) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + ws + "/tags", token, Map.of());
        return om.readTree(resp.body());
    }

    public ObjectNode createTag(String ws, String name) throws Exception {
        ObjectNode body = om.createObjectNode().put("name", name);
        HttpResponse<String> resp = http.postJson("/workspaces/" + ws + "/tags", token, body.toString(), Map.of());
        return (ObjectNode) om.readTree(resp.body());
    }

    public ObjectNode updateTimeEntry(String ws, String entryId, ObjectNode patch) throws Exception {
        ObjectNode existing = getTimeEntry(ws, entryId);
        ObjectNode req = existing.deepCopy();
        if (!req.has("start") && existing.has("timeInterval") && existing.get("timeInterval").has("start")) {
            req.set("start", existing.get("timeInterval").get("start"));
        }
        if (!req.has("end") && existing.has("timeInterval") && existing.get("timeInterval").has("end")) {
            req.set("end", existing.get("timeInterval").get("end"));
        }
        patch.fieldNames().forEachRemaining(fn -> req.set(fn, patch.get(fn)));
        HttpResponse<String> resp = http.putJson("/workspaces/" + ws + "/time-entries/" + entryId, token, req.toString(), Map.of());
        return (ObjectNode) om.readTree(resp.body());
    }

    public static String normalizeTagName(String name) {
        return name == null ? null : name.trim().toLowerCase();
    }

    public static ArrayNode ensureTagIds(ObjectNode entry, ObjectMapper om) {
        if (entry.has("tagIds") && entry.get("tagIds").isArray()) return (ArrayNode) entry.get("tagIds");
        ArrayNode arr = om.createArrayNode();
        entry.set("tagIds", arr);
        return arr;
    }
}

