package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.example.rules.api.ErrorResponse;
import com.example.rules.cache.RuleCache;
import com.example.rules.DynamicWebhookHandlers;
import com.example.rules.engine.Rule;
import com.example.rules.engine.RuleValidator;
import com.example.rules.store.RulesStoreSPI;
import com.example.rules.web.LegacyActionPayloadConverter;
import com.example.rules.web.RequestContext;
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
    private static final String MEDIA_JSON = "application/json";

    private final RulesStoreSPI rulesStore;
    private final ClockifyAddon addon;

    public RulesController(RulesStoreSPI rulesStore, ClockifyAddon addon) {
        this.rulesStore = rulesStore;
        this.addon = addon;
        // Initialize rule cache
        RuleCache.initialize(rulesStore);
    }

    /**
     * GET /api/rules - List all rules for a workspace
     */
    public RequestHandler listRules() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                List<Rule> rules = rulesStore.getAll(workspaceId);
                String json = objectMapper.writeValueAsString(rules);
                return HttpResponse.ok(json, MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "RULES.LIST_FAILED", "Failed to list rules", e, true);
            }
        };
    }

    /**
     * POST /api/rules - Create or update a rule
     */
    public RequestHandler saveRule() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                JsonNode body = parseRequestBody(request);
                body = LegacyActionPayloadConverter.normalize(objectMapper, body);
                Rule rule = objectMapper.treeToValue(body, Rule.class);

                // Validate the rule before saving
                try {
                    RuleValidator.validate(rule);
                } catch (RuleValidator.RuleValidationException e) {
                    logger.warn("Rule validation failed: {}", e.getMessage());
                    return ErrorResponse.of(400, "RULES.VALIDATION_FAILED",
                            "Rule validation failed", request, false, e.getMessage(),
                            ErrorResponse.validationErrors(e.getMessage()));
                }

                Rule saved = rulesStore.save(workspaceId, rule);
                // Invalidate cache for this workspace
                RuleCache.invalidate(workspaceId);
                DynamicWebhookHandlers.registerTriggerEvent(addon, saved);
                String json = objectMapper.writeValueAsString(saved);
                return HttpResponse.ok(json, MEDIA_JSON);

            } catch (IllegalArgumentException e) {
                logger.warn("Invalid rule: {}", e.getMessage());
                return ErrorResponse.of(400, "RULES.INVALID_RULE",
                        "Invalid rule payload", request, false, e.getMessage(),
                        ErrorResponse.validationErrors(e.getMessage()));
            } catch (Exception e) {
                return internalError(request, "RULES.SAVE_FAILED", "Failed to save rule", e, true);
            }
        };
    }

    /**
     * DELETE /api/rules - Delete a rule by id provided via `?id=` or JSON body {"id":"..."}.
     * (SDK routes by exact path; using query/body ensures this works at /api/rules.)
     */
    public RequestHandler deleteRule() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String ruleId = extractRuleId(request);
                if (ruleId == null) {
                    return ErrorResponse.of(400, "RULES.RULE_ID_REQUIRED", "ruleId is required", request, false);
                }

                boolean deleted = rulesStore.delete(workspaceId, ruleId);
                // Invalidate cache for this workspace
                RuleCache.invalidate(workspaceId);
                String json = objectMapper.createObjectNode()
                        .put("deleted", deleted)
                        .put("ruleId", ruleId)
                        .toString();

                return HttpResponse.ok(json, MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "RULES.DELETE_FAILED", "Failed to delete rule", e, true);
            }
        };
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
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                JsonNode body = parseRequestBody(request);
                if ((workspaceId == null || workspaceId.isBlank())
                        && RequestContext.workspaceFallbackAllowed()
                        && body != null && body.hasNonNull("workspaceId")) {
                    workspaceId = body.get("workspaceId").asText();
                }
                if (workspaceId == null || workspaceId.isBlank()) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                JsonNode timeEntry = (body != null && body.has("timeEntry")) ? body.get("timeEntry") : body;
                if (timeEntry == null || timeEntry.isNull()) {
                    return ErrorResponse.of(400, "RULES.TIME_ENTRY_REQUIRED", "timeEntry is required", request, false);
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
                return HttpResponse.ok(node.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "RULES.TEST_FAILED", "Failed to evaluate rules", e, true);
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

    private HttpResponse workspaceRequired(HttpServletRequest request) {
        String hint = RequestContext.workspaceFallbackAllowed()
                ? "workspaceId is required"
                : "workspaceId is required (Authorization bearer token missing or expired)";
        return ErrorResponse.of(400, "RULES.WORKSPACE_REQUIRED", hint, request, false);
    }

    private HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e, boolean retryable) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, retryable, e.getMessage());
    }
}
