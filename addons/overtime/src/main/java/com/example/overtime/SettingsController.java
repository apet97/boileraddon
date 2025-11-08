package com.example.overtime;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

public class SettingsController {
    private final SettingsStore store;
    private final ObjectMapper om = new ObjectMapper();

    public SettingsController(SettingsStore store) { this.store = store; }

    public HttpResponse handleHtml(HttpServletRequest req) throws Exception {
        String ws = req.getParameter("workspaceId");
        SettingsStore.Settings s = store.get(ws != null ? ws : "");
        String html = "<html><body><h1>Overtime Settings</h1>" +
                "<p>Daily hours: " + s.dailyHours + "</p>" +
                "<p>Weekly hours: " + s.weeklyHours + "</p>" +
                "<p>Tag name: " + s.tagName + "</p>" +
                "<p>Use /api/settings (GET/POST JSON) to update.</p>" +
                "</body></html>";
        return HttpResponse.ok(html, "text/html");
    }

    public HttpResponse handleApi(HttpServletRequest req) throws Exception {
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            String ws = req.getParameter("workspaceId");
            SettingsStore.Settings s = store.get(ws != null ? ws : "");
            ObjectNode n = om.createObjectNode();
            n.put("dailyHours", s.dailyHours);
            n.put("weeklyHours", s.weeklyHours);
            n.put("tagName", s.tagName);
            return HttpResponse.ok(n.toString(), "application/json");
        }
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String ws = req.getParameter("workspaceId");
            if (ws == null || ws.isBlank()) return HttpResponse.error(400, "{\"error\":\"workspaceId required\"}", "application/json");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = req.getReader()) { String line; while ((line = r.readLine()) != null) sb.append(line); }
            ObjectNode body = (ObjectNode) om.readTree(sb.toString());
            SettingsStore.Settings s = store.get(ws);
            if (body.has("dailyHours")) s.dailyHours = body.get("dailyHours").asDouble(s.dailyHours);
            if (body.has("weeklyHours")) s.weeklyHours = body.get("weeklyHours").asDouble(s.weeklyHours);
            if (body.has("tagName")) s.tagName = body.get("tagName").asText(s.tagName);
            store.put(ws, s);
            return HttpResponse.ok("{\"saved\":true}", "application/json");
        }
        return HttpResponse.error(405, "{\"error\":\"Method not allowed\"}", "application/json");
    }
}

