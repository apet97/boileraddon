package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
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

    private final RulesStore rulesStore;

    public RulesController(RulesStore rulesStore) {
        this.rulesStore = rulesStore;
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

                Rule saved = rulesStore.save(workspaceId, rule);
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
     * DELETE /api/rules/{id} - Delete a rule
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

    private String extractRuleId(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            return null;
        }

        // Path format: /api/rules/{ruleId}
        String[] segments = path.split("/");
        if (segments.length >= 4 && "api".equals(segments[1]) && "rules".equals(segments[2])) {
            return segments[3];
        }

        return null;
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
