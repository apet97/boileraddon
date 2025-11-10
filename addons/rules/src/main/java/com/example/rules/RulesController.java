package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.example.rules.engine.Rule;
import com.example.rules.engine.RuleValidator;
import com.example.rules.store.RulesStoreSPI;
import com.example.rules.cache.RuleCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.List;

/**
 * CRUD controller for rules management.
 * Provides endpoints for creating, reading, updating, and deleting rules.
 */
public class RulesController {

    private static final Logger logger = LoggerFactory.getLogger(RulesController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RulesStoreSPI rulesStore;

    public RulesController(RulesStoreSPI rulesStore) {
        this.rulesStore = rulesStore;
        // Initialize rule cache
        RuleCache.initialize(rulesStore);
    }

    /**
     * GET /api/rules - List all rules for a workspace
     */
    public RequestHandler listRules() {
        return request -> {
            try {
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return HttpResponse.error(400, "{\"error\":\"workspaceId is required\"}", "application/json");
                }

                List<Rule> rules = rulesStore.getAll(workspaceId);
                String json = objectMapper.writeValueAsString(rules);
                return HttpResponse.ok(json, "application/json");

            } catch (Exception e) {
                logger.error("Error listing rules", e);
                return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        };
    }

    /**
     * POST /api/rules - Create or update a rule
     */
    public RequestHandler saveRule() {
        return request -> {
            try {
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return HttpResponse.error(400, "{\"error\":\"workspaceId is required\"}", "application/json");
                }

                JsonNode body = parseRequestBody(request);
                Rule rule = objectMapper.treeToValue(body, Rule.class);

                // Validate the rule before saving
                try {
                    RuleValidator.validate(rule);
                } catch (RuleValidator.RuleValidationException e) {
                    logger.warn("Rule validation failed: {}", e.getMessage());
                    return HttpResponse.error(400, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
                }

                Rule saved = rulesStore.save(workspaceId, rule);
                // Invalidate cache for this workspace
                RuleCache.invalidate(workspaceId);
                String json = objectMapper.writeValueAsString(saved);
                return HttpResponse.ok(json, "application/json");

            } catch (IllegalArgumentException e) {
                logger.warn("Invalid rule: {}", e.getMessage());
                return HttpResponse.error(400, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            } catch (Exception e) {
                logger.error("Error saving rule", e);
                return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        };
    }

    /**
     * DELETE /api/rules - Delete a rule by id provided via `?id=` or JSON body {"id":"..."}.
     * (SDK routes by exact path; using query/body ensures this works at /api/rules.)
     */
    public RequestHandler deleteRule() {
        return request -> {
            try {
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return HttpResponse.error(400, "{\"error\":\"workspaceId is required\"}", "application/json");
                }

                String ruleId = extractRuleId(request);
                if (ruleId == null) {
                    return HttpResponse.error(400, "{\"error\":\"ruleId is required\"}", "application/json");
                }

                boolean deleted = rulesStore.delete(workspaceId, ruleId);
                // Invalidate cache for this workspace
                RuleCache.invalidate(workspaceId);
                String json = objectMapper.createObjectNode()
                        .put("deleted", deleted)
                        .put("ruleId", ruleId)
                        .toString();

                return HttpResponse.ok(json, "application/json");

            } catch (Exception e) {
                logger.error("Error deleting rule", e);
                return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        };
    }

    private String getWorkspaceId(HttpServletRequest request) {
        // Try to get from query parameter
        String workspaceId = request.getParameter("workspaceId");
        if (workspaceId != null && !workspaceId.trim().isEmpty()) {
            return workspaceId.trim();
        }

        // For demo purposes, allow passing via header
        String header = request.getHeader("X-Workspace-Id");
        if (header != null && !header.trim().isEmpty()) {
            return header.trim();
        }

        return null;
    }

    private String extractRuleId(HttpServletRequest request) throws Exception {
        // Prefer query parameter first
        String q = request.getParameter("id");
        if (q != null && !q.trim().isEmpty()) {
            return q.trim();
        }
        // Try JSON body if available
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode json && json.hasNonNull("id")) {
            String id = json.get("id").asText("");
            if (!id.isBlank()) return id;
        }
        // Fallback for unit tests invoking controller directly with path suffix
        String path = request.getPathInfo();
        if (path != null) {
            String[] segments = path.split("/");
            if (segments.length >= 4 && "api".equals(segments[1]) && "rules".equals(segments[2])) {
                return segments[3];
            }
        }
        return null;
    }

    /**
     * POST /api/test — Evaluate rules against a provided timeEntry (no side effects).
     * Body: { "workspaceId": "...", "timeEntry": { ... } }
     */
    public RequestHandler testRules() {
        return request -> {
            try {
                String workspaceId = getWorkspaceId(request);
                JsonNode body = parseRequestBody(request);
                if ((workspaceId == null || workspaceId.isBlank()) && body != null && body.hasNonNull("workspaceId")) {
                    workspaceId = body.get("workspaceId").asText();
                }
                if (workspaceId == null || workspaceId.isBlank()) {
                    return HttpResponse.error(400, "{\"error\":\"workspaceId is required\"}", "application/json");
                }

                JsonNode timeEntry = (body != null && body.has("timeEntry")) ? body.get("timeEntry") : body;
                if (timeEntry == null || timeEntry.isNull()) {
                    return HttpResponse.error(400, "{\"error\":\"timeEntry is required\"}", "application/json");
                }

                var evaluator = new com.example.rules.engine.Evaluator();
                var context = new com.example.rules.engine.TimeEntryContext(timeEntry);
                var matched = new java.util.ArrayList<com.example.rules.engine.Action>();
                // Use cached enabled rules for better performance
                for (var r : RuleCache.getEnabledRules(workspaceId)) {
                    if (evaluator.evaluate(r, context) && r.getActions() != null) {
                        matched.addAll(r.getActions());
                    }
                }

                var node = objectMapper.createObjectNode();
                node.put("workspaceId", workspaceId);
                node.put("actionsCount", matched.size());
                var arr = objectMapper.createArrayNode();
                for (var a : matched) {
                    var an = objectMapper.createObjectNode();
                    an.put("type", a.getType());
                    if (a.getArgs() != null) {
                        var args = objectMapper.createObjectNode();
                        a.getArgs().forEach(args::put);
                        an.set("args", args);
                    }
                    arr.add(an);
                }
                node.set("actions", arr);
                return HttpResponse.ok(node.toString(), "application/json");

            } catch (Exception e) {
                logger.error("Error testing rules", e);
                return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        };
    }

    private JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode) {
            return (JsonNode) cachedJson;
        }

        Object cachedBody = request.getAttribute("clockify.rawBody");
        if (cachedBody instanceof String) {
            return objectMapper.readTree((String) cachedBody);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return objectMapper.readTree(sb.toString());
    }
}
