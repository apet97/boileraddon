package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

public class RulesController implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RulesController.class);
    private static final ObjectMapper om = new ObjectMapper();
    private final boolean allowWorkspaceParam;

    public RulesController(String environment) {
        this.allowWorkspaceParam = environment != null && environment.equalsIgnoreCase("dev");
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        String workspaceId = resolveWorkspaceId(request);
        if (workspaceId == null || workspaceId.isBlank()) {
            return HttpResponse.error(401, "{\"error\":\"workspace context required\"}", "application/json");
        }

        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            ArrayNode array = om.createArrayNode();
            RuleStore.getRules(workspaceId).forEach(rule -> array.add(rule.toJson(om)));
            ObjectNode result = om.createObjectNode();
            result.put("workspaceId", workspaceId);
            result.set("rules", array);
            return HttpResponse.ok(result.toString(), "application/json");
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            String id = request.getParameter("id");
            if (id == null || id.isBlank()) {
                return HttpResponse.error(400, "{\"error\":\"id is required\"}", "application/json");
            }
            boolean removed = RuleStore.deleteRule(workspaceId, id);
            if (!removed) {
                return HttpResponse.error(404, "{\"error\":\"rule not found\"}", "application/json");
            }
            ObjectNode result = om.createObjectNode();
            result.put("workspaceId", workspaceId);
            result.put("deletedId", id);
            result.put("ruleCount", RuleStore.ruleCount(workspaceId));
            return HttpResponse.ok(result.toString(), "application/json");
        }

        if (!"POST".equalsIgnoreCase(method)) {
            return HttpResponse.error(405, "{\"error\":\"method not allowed\"}", "application/json");
        }

        JsonNode body = parse(request);
        String match = text(body, "matchText");
        String actionTag = text(body, "tag");
        if (match == null || match.isBlank() || actionTag == null || actionTag.isBlank()) {
            return HttpResponse.error(400, "{\"error\":\"matchText and tag are required\"}", "application/json");
        }

        RuleStore.RuleDefinition def = RuleStore.addRule(workspaceId, match, actionTag);
        logger.info("Rule registered for workspace {}: '{}' -> tag '{}' (id={})", workspaceId, match, actionTag, def.id());

        ObjectNode result = om.createObjectNode();
        result.put("workspaceId", workspaceId);
        result.set("rule", def.toJson(om));
        result.put("ruleCount", RuleStore.ruleCount(workspaceId));
        return HttpResponse.ok(result.toString(), "application/json");
    }

    private String resolveWorkspaceId(HttpServletRequest request) {
        Object workspace = request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
        if (workspace instanceof String w && !w.isBlank()) {
            return w;
        }
        if (allowWorkspaceParam) {
            String fromParam = request.getParameter("workspaceId");
            return fromParam != null && !fromParam.isBlank() ? fromParam : null;
        }
        return null;
    }

    private static JsonNode parse(HttpServletRequest r) throws Exception {
        Object c = r.getAttribute("clockify.jsonBody");
        if (c instanceof JsonNode) {
            return (JsonNode) c;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = r.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return om.readTree(sb.toString());
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.has(f) && !n.get(f).isNull() ? n.get(f).asText(null) : null;
    }
}
