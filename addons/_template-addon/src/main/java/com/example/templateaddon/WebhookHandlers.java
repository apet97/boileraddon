package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

public class WebhookHandlers {
    private static final ObjectMapper om = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        String[] events = {"TIME_ENTRY_CREATED","TIME_ENTRY_UPDATED","NEW_TIMER_STARTED","TIMER_STOPPED"};
        for (String e: events) {
            addon.registerWebhookHandler(e, WebhookHandlers::handle);
        }
    }

    private static HttpResponse handle(HttpServletRequest req) throws Exception {
        JsonNode payload = parse(req);
        String ws = payload.has("workspaceId")?payload.get("workspaceId").asText(null):null;
        var sig = WebhookSignatureValidator.verify(req, ws);
        if (!sig.isValid()) return sig.response();
        // Implement your business logic here
        // Example: Process time entry data from payload
        String eventType = payload.has("event") ? payload.get("event").asText() : "unknown";
        String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText() : "unknown";

        // Add your custom logic based on event type
        return HttpResponse.ok("{\"processed\":true,\"event\":\"" + eventType + "\"}", "application/json");
    }

    private static JsonNode parse(HttpServletRequest r) throws Exception {
        Object c = r.getAttribute("clockify.jsonBody"); if (c instanceof JsonNode) return (JsonNode)c;
        StringBuilder sb=new StringBuilder(); try(BufferedReader br=r.getReader()){String line;while((line=br.readLine())!=null)sb.append(line);}return om.readTree(sb.toString());
    }
}

