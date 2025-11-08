package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

public class LifecycleHandlers {
    private static final ObjectMapper om = new ObjectMapper();

    public static void register(ClockifyAddon addon) {
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", LifecycleHandlers::installed);
        addon.registerLifecycleHandler("DELETED", "/lifecycle/deleted", LifecycleHandlers::deleted);
    }

    private static HttpResponse installed(HttpServletRequest req) throws Exception {
        JsonNode b = parse(req);
        String ws = text(b, "workspaceId");
        String token = text(b, "installationToken");
        String apiUrl = text(b, "apiUrl");
        if (ws != null && token != null) com.clockify.addon.sdk.security.TokenStore.save(ws, token, apiUrl);
        return HttpResponse.ok("{\"status\":\"installed\"}", "application/json");
    }

    private static HttpResponse deleted(HttpServletRequest req) throws Exception {
        JsonNode b = parse(req);
        String ws = text(b, "workspaceId");
        if (ws != null) com.clockify.addon.sdk.security.TokenStore.delete(ws);
        return HttpResponse.ok("{\"status\":\"uninstalled\"}", "application/json");
    }

    private static JsonNode parse(HttpServletRequest r) throws Exception {
        Object c = r.getAttribute("clockify.jsonBody"); if (c instanceof JsonNode) return (JsonNode)c;
        StringBuilder sb=new StringBuilder(); try(BufferedReader br=r.getReader()){String line;while((line=br.readLine())!=null)sb.append(line);}return om.readTree(sb.toString());
    }
    private static String text(JsonNode n,String f){return n!=null&&n.has(f)&&!n.get(f).isNull()?n.get(f).asText(null):null;}
}

