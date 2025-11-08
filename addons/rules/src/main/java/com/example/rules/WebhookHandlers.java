package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.engine.Action;
import com.example.rules.engine.Evaluator;
import com.example.rules.engine.Rule;
import com.example.rules.engine.TimeEntryContext;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Clockify webhook events for time entries.
 * Evaluates rules and applies actions when time entries are created or updated.
 */
public class WebhookHandlers {

    private static final Logger logger = LoggerFactory.getLogger(WebhookHandlers.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static com.example.rules.store.RulesStoreSPI rulesStore;
    private static Evaluator evaluator;

    @FunctionalInterface
    public interface ClientFactory { ClockifyClient create(String baseUrl, String token); }

    private static ClientFactory clientFactory = ClockifyClient::new;
    public static void setClientFactory(ClientFactory factory) {
        clientFactory = (factory != null) ? factory : ClockifyClient::new;
    }

    public static void register(ClockifyAddon addon, com.example.rules.store.RulesStoreSPI store) {
        rulesStore = store;
        evaluator = new Evaluator();

        // Register handlers for time entry events
        String[] events = {
            "NEW_TIME_ENTRY",
            "TIME_ENTRY_UPDATED"
        };

        for (String event : events) {
            addon.registerWebhookHandler(event, request -> {
                try {
                    JsonNode payload = parseRequestBody(request);
                    String workspaceId = extractWorkspaceId(payload);
                    String eventType = payload.has("event") ? payload.get("event").asText() : event;

                    // Verify webhook signature (allow opt-out in dev)
                    boolean skipSig = "true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_SKIP_SIGNATURE_VERIFY", "false"));
                    if (!skipSig) {
                        WebhookSignatureValidator.VerificationResult verificationResult =
                                WebhookSignatureValidator.verify(request, workspaceId);
                        if (!verificationResult.isValid()) {
                            logger.warn("Webhook signature verification failed for workspace {}", workspaceId);
                            return verificationResult.response();
                        }
                    } else {
                        logger.info("ADDON_SKIP_SIGNATURE_VERIFY=true — skipping webhook signature verification (DEV ONLY)");
                    }

                    logger.info("Webhook event received: {} for workspace {}", eventType, workspaceId);

                    // Extract time entry from payload
                    JsonNode timeEntry = extractTimeEntry(payload);
                    if (timeEntry == null || timeEntry.isMissingNode()) {
                        logger.warn("Time entry not found in webhook payload");
                        return HttpResponse.error(400, "{\"error\":\"Time entry not found in payload\"}",
                                "application/json");
                    }

                    // Load enabled rules for workspace
                    List<Rule> rules = rulesStore.getEnabled(workspaceId);
                    if (rules.isEmpty()) {
                        logger.debug("No enabled rules found for workspace {}", workspaceId);
                        return createResponse(eventType, "no_rules", new ArrayList<>());
                    }

                    // Evaluate rules and collect actions
                    TimeEntryContext context = new TimeEntryContext(timeEntry);
                    List<Action> actionsToApply = new ArrayList<>();

                    for (Rule rule : rules) {
                        boolean matches = evaluator.evaluate(rule, context);
                        if (matches) {
                            logger.info("Rule '{}' matched for time entry", rule.getName());
                            actionsToApply.addAll(rule.getActions());
                        }
                    }

                    if (actionsToApply.isEmpty()) {
                        logger.debug("No rules matched for time entry");
                        return createResponse(eventType, "no_match", new ArrayList<>());
                    }

                    // If not enabled to mutate, log and exit (backward-compatible behavior for tests)
                    boolean applyEnv = "true".equalsIgnoreCase(System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false"));
                    boolean applyProp = "true".equalsIgnoreCase(System.getProperty("RULES_APPLY_CHANGES", "false"));
                    if (!(applyEnv || applyProp)) {
                        logger.info("RULES_APPLY_CHANGES=false — logging actions only");
                        logActions(actionsToApply);
                        return createResponse(eventType, "actions_logged", actionsToApply);
                    }

                    // Apply actions idempotently using SDK HTTP client
                    var wkOpt = com.clockify.addon.sdk.security.TokenStore.get(workspaceId);
                    if (wkOpt.isEmpty()) {
                        logger.warn("Missing installation token for workspace {} — skipping mutations", workspaceId);
                        return createResponse(eventType, "missing_token", new ArrayList<>());
                    }

                    var wk = wkOpt.get();
                    ClockifyClient api = clientFactory.create(wk.apiBaseUrl(), wk.token());

                    // Read existing entry and tags once, then apply changes in-memory and PUT once if needed
                    com.fasterxml.jackson.databind.node.ObjectNode entry = api.getTimeEntry(workspaceId, timeEntry.path("id").asText());
                    com.fasterxml.jackson.databind.JsonNode tagsArray = api.getTags(workspaceId);
                    java.util.Map<String, String> tagsByNorm = ClockifyClient.mapTagsByNormalizedName(tagsArray);
                    // Workspace cache for name→id mappings (projects, tasks, clients)
                    var snap = com.example.rules.cache.WorkspaceCache.get(workspaceId);

                    boolean changed = false;
                    com.fasterxml.jackson.databind.node.ObjectNode patch = objectMapper.createObjectNode();
                    // seed patch with current tagIds for unified updates when needed
                    var patchTagIds = objectMapper.createArrayNode();
                    boolean patchHasTags = false;
                    String pendingProjectId = null; // track if we set project later; affects task resolution

                    for (Action action : actionsToApply) {
                        String type = action.getType();
                        java.util.Map<String, String> args = action.getArgs();
                        if (type == null) continue;

                        switch (type) {
                            case "add_tag": {
                                String tagName = args != null ? args.getOrDefault("tag", args.get("name")) : null;
                                if (tagName == null || tagName.isBlank()) break;
                                String norm = ClockifyClient.normalizeTagName(tagName);
                                String tagId = tagsByNorm.get(norm);
                                if (tagId == null) {
                                    // create tag then map it
                                    var created = api.createTag(workspaceId, tagName);
                                    tagId = created.has("id") ? created.get("id").asText() : null;
                                    if (tagId != null) tagsByNorm.put(norm, tagId);
                                }
                                if (tagId != null) {
                                    // ensure tagIds array; gather from current entry or patch-in-progress
                                    java.util.Set<String> current = new java.util.LinkedHashSet<>();
                                    if (entry.has("tagIds") && entry.get("tagIds").isArray()) {
                                        entry.get("tagIds").forEach(n -> { if (n.isTextual()) current.add(n.asText()); });
                                    }
                                    if (!patchHasTags && patch.has("tagIds")) {
                                        patch.get("tagIds").forEach(n -> { if (n.isTextual()) current.add(n.asText()); });
                                    }
                                    if (current.add(tagId)) {
                                        patchTagIds.removeAll();
                                        current.forEach(patchTagIds::add);
                                        patch.set("tagIds", patchTagIds);
                                        patchHasTags = true;
                                        changed = true;
                                    }
                                }
                                break;
                            }
                            case "remove_tag": {
                                String tagName = args != null ? args.getOrDefault("tag", args.get("name")) : null;
                                if (tagName == null || tagName.isBlank()) break;
                                String norm = ClockifyClient.normalizeTagName(tagName);
                                String tagId = tagsByNorm.get(norm);
                                if (tagId != null) {
                                    java.util.Set<String> current = new java.util.LinkedHashSet<>();
                                    if (entry.has("tagIds") && entry.get("tagIds").isArray()) {
                                        entry.get("tagIds").forEach(n -> { if (n.isTextual()) current.add(n.asText()); });
                                    }
                                    if (!patchHasTags && patch.has("tagIds")) {
                                        patch.get("tagIds").forEach(n -> { if (n.isTextual()) current.add(n.asText()); });
                                    }
                                    if (current.remove(tagId)) {
                                        patchTagIds.removeAll();
                                        current.forEach(patchTagIds::add);
                                        patch.set("tagIds", patchTagIds);
                                        patchHasTags = true;
                                        changed = true;
                                    }
                                }
                                break;
                            }
                            case "set_description": {
                                String value = args != null ? args.get("value") : null;
                                if (value != null && !value.equals(entry.path("description").asText())) {
                                    patch.put("description", value);
                                    changed = true;
                                }
                                break;
                            }
                            case "set_billable": {
                                String value = args != null ? args.get("value") : null;
                                if (value != null) {
                                    boolean desired = "true".equalsIgnoreCase(value) || "1".equals(value);
                                    boolean current = entry.path("billable").asBoolean(false);
                                    if (desired != current) {
                                        patch.put("billable", desired);
                                        changed = true;
                                    }
                                }
                                break;
                            }
                            case "set_project_by_id": {
                                String pid = args != null ? args.get("projectId") : null;
                                if (pid != null && !pid.isBlank()) {
                                    String current = entry.path("projectId").asText("");
                                    if (!pid.equals(current)) {
                                        patch.put("projectId", pid);
                                        pendingProjectId = pid;
                                        changed = true;
                                    }
                                }
                                break;
                            }
                            case "set_project_by_name": {
                                String name = args != null ? args.get("name") : null;
                                if (name != null && !name.isBlank()) {
                                    String norm = name.trim().toLowerCase(java.util.Locale.ROOT);
                                    String pid = snap.projectsByNameNorm.get(norm);
                                    if (pid != null) {
                                        String current = entry.path("projectId").asText("");
                                        if (!pid.equals(current)) {
                                            patch.put("projectId", pid);
                                            pendingProjectId = pid;
                                            changed = true;
                                        }
                                    }
                                }
                                break;
                            }
                            case "set_task_by_id": {
                                String tid = args != null ? args.get("taskId") : null;
                                if (tid != null && !tid.isBlank()) {
                                    String current = entry.path("taskId").asText("");
                                    if (!tid.equals(current)) {
                                        patch.put("taskId", tid);
                                        changed = true;
                                    }
                                }
                                break;
                            }
                            case "set_task_by_name": {
                                String tname = args != null ? args.get("name") : null;
                                if (tname != null && !tname.isBlank()) {
                                    String taskId = null;
                                    // Determine target project to resolve task under: prefer pendingProjectId; else entry.project.name
                                    String projectName = null;
                                    if (pendingProjectId != null) {
                                        projectName = snap.projectsById.getOrDefault(pendingProjectId, null);
                                    }
                                    if (projectName == null || projectName.isBlank()) {
                                        var projNode = entry.path("project");
                                        if (projNode.has("name") && projNode.get("name").isTextual()) {
                                            projectName = projNode.get("name").asText("");
                                        }
                                    }
                                    if (projectName != null && !projectName.isBlank()) {
                                        String pnorm = projectName.trim().toLowerCase(java.util.Locale.ROOT);
                                        var tmap = snap.tasksByProjectNameNorm.get(pnorm);
                                        if (tmap != null) {
                                            taskId = tmap.get(tname.trim().toLowerCase(java.util.Locale.ROOT));
                                        }
                                    }
                                    // Fallback: scan any project map
                                    if (taskId == null) {
                                        String tnorm = tname.trim().toLowerCase(java.util.Locale.ROOT);
                                        for (var e : snap.tasksByProjectNameNorm.entrySet()) {
                                            var id = e.getValue().get(tnorm);
                                            if (id != null) { taskId = id; break; }
                                        }
                                    }
                                    if (taskId != null) {
                                        String current = entry.path("taskId").asText("");
                                        if (!taskId.equals(current)) {
                                            patch.put("taskId", taskId);
                                            changed = true;
                                        }
                                    }
                                }
                                break;
                            }
                            default:
                                // Unknown action type — skip
                                break;
                        }
                    }

                    if (changed) {
                        api.updateTimeEntry(workspaceId, timeEntry.path("id").asText(), patch);
                        return createResponse(eventType, "actions_applied", actionsToApply);
                    } else {
                        return createResponse(eventType, "no_changes", new ArrayList<>());
                    }

                } catch (Exception e) {
                    logger.error("Error processing webhook", e);
                    return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}",
                            "application/json");
                }
            });
        }
    }

    private static String extractWorkspaceId(JsonNode payload) {
        if (payload.has("workspaceId")) {
            return payload.get("workspaceId").asText(null);
        }
        return null;
    }

    private static JsonNode extractTimeEntry(JsonNode payload) {
        if (payload.has("timeEntry")) {
            return payload.get("timeEntry");
        }
        return payload;
    }

    private static void logActions(List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            logger.info("No actions to apply");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Actions to apply (demo)\n");
        for (Action action : actions) {
            sb.append("  type=").append(action.getType());
            if (action.getArgs() != null && !action.getArgs().isEmpty()) {
                sb.append(", args=").append(action.getArgs());
            }
            sb.append('\n');
        }
        sb.append("Note: In production, apply via Clockify API");
        logger.info(sb.toString());
    }

    private static HttpResponse createResponse(String eventType, String status, List<Action> actions)
            throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("event", eventType);
        response.put("status", status);
        response.put("actionsCount", actions.size());

        ArrayNode actionsArray = objectMapper.createArrayNode();
        for (Action action : actions) {
            ObjectNode actionNode = objectMapper.createObjectNode();
            actionNode.put("type", action.getType());
            if (action.getArgs() != null) {
                ObjectNode argsNode = objectMapper.createObjectNode();
                action.getArgs().forEach(argsNode::put);
                actionNode.set("args", argsNode);
            }
            actionsArray.add(actionNode);
        }
        response.set("actions", actionsArray);

        return HttpResponse.ok(response.toString(), "application/json");
    }

    private static JsonNode parseRequestBody(HttpServletRequest request) throws Exception {
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
