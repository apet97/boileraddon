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
        String html = "<!doctype html><html><head><meta charset='utf-8'><title>Overtime Settings</title>"+
                "<style>body{font-family:sans-serif;margin:2rem;}input{padding:.4rem;margin:.2rem 0;width:12rem}button{padding:.5rem 1rem}code{background:#eee;padding:.1rem .3rem}</style>"+
                "</head><body>"+
                "<h1>Overtime Settings</h1>"+
                "<form onsubmit=\"save();return false;\">"+
                "<label>Workspace ID<br><input id='ws' value='"+(ws!=null?ws:"")+"' placeholder='workspaceId'></label><br>"+
                "<label>Daily hours<br><input id='daily' type='number' step='0.25' value='"+s.dailyHours+"'></label><br>"+
                "<label>Weekly hours<br><input id='weekly' type='number' step='0.25' value='"+s.weeklyHours+"'></label><br>"+
                "<label>Tag name<br><input id='tag' value='"+s.tagName+"'></label><br>"+
                "<button type='submit'>Save</button>"+
                "</form>"+
                "<p>API endpoint: <code>/api/settings?workspaceId=&lt;id&gt;</code></p>"+
                "<script>function save(){var w=document.getElementById('ws').value;var d=parseFloat(document.getElementById('daily').value);var wk=parseFloat(document.getElementById('weekly').value);var t=document.getElementById('tag').value;fetch('api/settings?workspaceId='+encodeURIComponent(w),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({dailyHours:d,weeklyHours:wk,tagName:t})}).then(r=>r.json()).then(_=>alert('Saved'));}</script>"+
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
