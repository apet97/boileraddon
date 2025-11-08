package com.example.overtime;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WebhookHandlers {
    private static final Logger log = LoggerFactory.getLogger(WebhookHandlers.class);
    private static final ObjectMapper om = new ObjectMapper();
    private static SettingsStore settings;

    public static void register(ClockifyAddon addon, SettingsStore store) {
        settings = store;
        String[] events = {"TIMER_STOPPED", "TIME_ENTRY_UPDATED"};
        for (String e : events) {
            addon.registerWebhookHandler(e, WebhookHandlers::handle);
        }
    }

    private static HttpResponse handle(HttpServletRequest req) throws Exception {
        JsonNode body = parse(req);
        String ws = text(body, "workspaceId");
        String event = text(body, "event");
        if (ws == null) return HttpResponse.error(400, "{\"error\":\"workspaceId missing\"}", "application/json");

        var sig = WebhookSignatureValidator.verify(req, ws);
        if (!sig.isValid()) return sig.response();

        JsonNode te = body.has("timeEntry") ? body.get("timeEntry") : body;
        String entryId = text(te, "id");
        double dailyHours = settings.get(ws).dailyHours;
        double weeklyHours = settings.get(ws).weeklyHours;
        String tagName = Optional.ofNullable(settings.get(ws).tagName).orElse("Overtime");

        // Fallback MVP heuristic: use this entry's duration if totals are unavailable
        long entryMinutes = extractDurationMinutes(te);
        boolean overtime = false;
        if (entryMinutes >= Math.round(dailyHours * 60)) {
            overtime = true;
        }

        if (!overtime) {
            // Try aggregate via API (simple read + compare)
            var tok = com.clockify.addon.sdk.security.TokenStore.get(ws);
            if (tok.isPresent() && entryId != null) {
                String base = tok.get().apiBaseUrl();
                String tkn = tok.get().token();
                OvertimeClient api = new OvertimeClient(base, tkn);
                try {
                    // In a complete version, fetch user's daily/weekly totals.
                    // Here we just re-read the entry for resilience and proceed with tag application if needed.
                    ObjectNode entry = api.getTimeEntry(ws, entryId);
                    entryMinutes = Math.max(entryMinutes, extractDurationMinutes(entry));
                    overtime = entryMinutes >= Math.round(dailyHours * 60);
                    if (overtime) {
                        ensureTagApplied(api, ws, entryId, tagName);
                        return ok(event, "overtime_tag_applied");
                    }
                } catch (Exception ex) {
                    log.warn("Overtime check failed for workspace {} entry {}: {}", ws, entryId, ex.toString());
                }
            }
        } else {
            var tok = com.clockify.addon.sdk.security.TokenStore.get(ws);
            if (tok.isPresent() && entryId != null) {
                OvertimeClient api = new OvertimeClient(tok.get().apiBaseUrl(), tok.get().token());
                ensureTagApplied(api, ws, entryId, tagName);
                return ok(event, "overtime_tag_applied");
            }
        }

        return ok(event, "no_overtime");
    }

    private static void ensureTagApplied(OvertimeClient api, String ws, String entryId, String tagName) throws Exception {
        JsonNode tags = api.getTags(ws);
        Map<String,String> byNorm = new LinkedHashMap<>();
        if (tags.isArray()) {
            for (JsonNode t : tags) {
                if (t.has("name") && t.has("id")) {
                    byNorm.put(OvertimeClient.normalizeTagName(t.get("name").asText()), t.get("id").asText());
                }
            }
        }
        String norm = OvertimeClient.normalizeTagName(tagName);
        String tagId = byNorm.get(norm);
        if (tagId == null) {
            ObjectNode created = api.createTag(ws, tagName);
            tagId = created.has("id") ? created.get("id").asText() : null;
        }
        if (tagId == null) return; // best effort

        ObjectNode entry = api.getTimeEntry(ws, entryId);
        ArrayNode arr = OvertimeClient.ensureTagIds(entry, om);
        Set<String> set = new LinkedHashSet<>();
        for (JsonNode n : arr) if (n.isTextual()) set.add(n.asText());
        if (!set.contains(tagId)) {
            arr.removeAll();
            set.add(tagId);
            set.forEach(arr::add);
            ObjectNode patch = om.createObjectNode();
            patch.set("tagIds", arr);
            api.updateTimeEntry(ws, entryId, patch);
        }
    }

    private static long extractDurationMinutes(JsonNode te) {
        if (te == null || !te.has("timeInterval")) return 0;
        JsonNode ti = te.get("timeInterval");
        String start = ti.has("start") && !ti.get("start").isNull() ? ti.get("start").asText() : null;
        String end = ti.has("end") && !ti.get("end").isNull() ? ti.get("end").asText() : null;
        if (start == null || end == null) return 0;
        try {
            OffsetDateTime s = OffsetDateTime.parse(start, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            OffsetDateTime e = OffsetDateTime.parse(end, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Duration.between(s, e).toMinutes();
        } catch (Exception ex) {
            return 0;
        }
    }

    private static HttpResponse ok(String event, String status) throws Exception {
        ObjectNode n = om.createObjectNode();
        n.put("event", event);
        n.put("status", status);
        return HttpResponse.ok(n.toString(), "application/json");
    }

    private static JsonNode parse(HttpServletRequest request) throws Exception {
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode) return (JsonNode) cachedJson;
        Object cachedBody = request.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String) return om.readTree((String) cachedBody);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) { String line; while ((line = reader.readLine()) != null) sb.append(line); }
        return om.readTree(sb.toString());
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.has(f) && !n.get(f).isNull() ? n.get(f).asText(null) : null;
    }
}

