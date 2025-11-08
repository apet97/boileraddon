package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.engine.Action;
import com.example.rules.engine.Evaluator;
import com.example.rules.engine.Rule;
import com.example.rules.engine.TimeEntryContext;
import com.example.rules.security.WebhookSignatureValidator;
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

    private static RulesStore rulesStore;
    private static Evaluator evaluator;

    public static void register(ClockifyAddon addon, RulesStore store) {
        rulesStore = store;
        evaluator = new Evaluator();

        // Register handlers for time entry events
        String[] events = {
            "TIME_ENTRY_CREATED",
            "TIME_ENTRY_UPDATED"
        };

        for (String event : events) {
            addon.registerWebhookHandler(event, request -> {
                try {
                    JsonNode payload = parseRequestBody(request);
                    String workspaceId = extractWorkspaceId(payload);
                    String eventType = payload.has("event") ? payload.get("event").asText() : event;

                    // Verify webhook signature
                    WebhookSignatureValidator.VerificationResult verificationResult =
                            WebhookSignatureValidator.verify(request, workspaceId);
                    if (!verificationResult.isValid()) {
                        logger.warn("Webhook signature verification failed for workspace {}", workspaceId);
                        return verificationResult.response();
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

                    // For demo, we log the actions that would be applied
                    // In production, you would call the Clockify API here to apply changes
                    logger.info("Would apply {} actions for time entry", actionsToApply.size());
                    logActions(actionsToApply);

                    return createResponse(eventType, "actions_logged", actionsToApply);

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
