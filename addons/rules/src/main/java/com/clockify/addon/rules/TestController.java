package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;

public class TestController implements RequestHandler {
    private static final ObjectMapper om = new ObjectMapper();
    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = request.getReader()) { String line; while ((line = br.readLine()) != null) sb.append(line); }
        ObjectNode result = om.createObjectNode();
        result.put("status","rules-ok");
        if (!sb.isEmpty()) result.set("echo", om.readTree(sb.toString()));
        return HttpResponse.ok(result.toString(), "application/json");
    }
}
