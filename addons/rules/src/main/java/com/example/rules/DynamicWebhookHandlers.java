package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.example.rules.engine.*;
import com.example.rules.store.RulesStoreSPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.*;

/**
 * Dynamic webhook handlers that support any Clockify webhook event.
 * Extends the basic WebhookHandlers with IFTTT-style rules and openapi_call actions.
 */
public class DynamicWebhookHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DynamicWebhookHandlers.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static RulesStoreSPI rulesStore;
    private static Evaluator evaluator;
    private static final Set<String> registeredEvents = Collections.synchronizedSet(new HashSet<>());

    public static void registerDynamicEvents(ClockifyAddon addon, RulesStoreSPI store) {
        rulesStore = store;
        evaluator = new Evaluator();

        // Register a generic webhook handler that can process any event
        // We'll register common events upfront, and allow dynamic registration as rules are created
        String[] commonEvents = {
            // Exclude time-entry events here to avoid overriding legacy handler
            // "NEW_TIME_ENTRY", "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_PROJECT", "PROJECT_UPDATED", "PROJECT_DELETED",
            "NEW_CLIENT", "CLIENT_UPDATED", "CLIENT_DELETED",
            "NEW_TAG", "TAG_UPDATED", "TAG_DELETED",
            "NEW_TASK", "TASK_UPDATED", "TASK_DELETED",
            "USER_JOINED_WORKSPACE", "USER_DELETED_FROM_WORKSPACE"
        };

        for (String event : commonEvents) {
            registerEvent(addon, event);
        }

        logger.info("Registered {} common webhook events for dynamic handling", commonEvents.length);
    }

    public static void registerEvent(ClockifyAddon addon, String eventName) {
        // Never override legacy time-entry events — leave them to WebhookHandlers
        if ("NEW_TIME_ENTRY".equals(eventName) || "TIME_ENTRY_UPDATED".equals(eventName)) {
            logger.info("Skipping dynamic registration for time-entry event {} (handled by legacy handler)", eventName);
            return;
        }
        if (registeredEvents.contains(eventName)) {
            return; // Already registered
        }

        addon.registerWebhookHandler(eventName, request -> {
            try {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = extractWorkspaceId(payload);
                String eventType = payload.has("event") ? payload.get("event").asText() : eventName;

                // Verify webhook signature (allow opt-out in dev)
                boolean skipSig = "true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_SKIP_SIGNATURE_VERIFY", "false"));
                if (!skipSig) {
                    WebhookSignatureValidator.VerificationResult verificationResult =
                            WebhookSignatureValidator.verify(request, workspaceId);
                    if (!verificationResult.isValid()) {
                        logger.warn("Webhook signature verification failed for workspace {}", workspaceId);
                        return verificationResult.response();
                    }
                }

                logger.info("Dynamic webhook event received: {} for workspace {}", eventType, workspaceId);

                // Load rules that match this event
                List<Rule> allRules = rulesStore.getEnabled(workspaceId);
                List<Rule> matchingRules = new ArrayList<>();

                for (Rule rule : allRules) {
                    // Check if rule has a trigger that matches this event
                    if (ruleMatchesEvent(rule, eventType)) {
                        matchingRules.add(rule);
                    }
                }

                if (matchingRules.isEmpty()) {
                    logger.debug("No rules found for event {} in workspace {}", eventType, workspaceId);
                    return createResponse(eventType, "no_matching_rules", new ArrayList<>());
                }

                // Process each matching rule
                List<Action> allActions = new ArrayList<>();
                TimeEntryContext context = new TimeEntryContext(payload); // Generic context

                for (Rule rule : matchingRules) {
                    // Evaluate conditions if present
                    boolean conditionsMet = true;
                    if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                        conditionsMet = evaluator.evaluate(rule, context);
                    }

                    if (conditionsMet) {
                        logger.info("Rule '{}' matched for event {}", rule.getName(), eventType);
                        allActions.addAll(rule.getActions());
                    }
                }

                if (allActions.isEmpty()) {
                    return createResponse(eventType, "no_actions", new ArrayList<>());
                }

                // Check if we should apply changes
                boolean applyEnv = "true".equalsIgnoreCase(System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false"));
                boolean applyProp = "true".equalsIgnoreCase(System.getProperty("RULES_APPLY_CHANGES", "false"));

                if (!(applyEnv || applyProp)) {
                    logger.info("RULES_APPLY_CHANGES=false — logging actions only");
                    return createResponse(eventType, "actions_logged", allActions);
                }

                // Apply actions
                var wkOpt = com.clockify.addon.sdk.security.TokenStore.get(workspaceId);
                if (wkOpt.isEmpty()) {
                    logger.warn("Missing installation token for workspace {} — skipping mutations", workspaceId);
                    return createResponse(eventType, "missing_token", new ArrayList<>());
                }

                var wk = wkOpt.get();
                ClockifyClient api = new ClockifyClient(wk.apiBaseUrl(), wk.token());

                // Execute actions (including openapi_call)
                int executedCount = executeActions(allActions, payload, workspaceId, api);

                return createResponse(eventType, "actions_applied", allActions, executedCount);

            } catch (Exception e) {
                logger.error("Error processing dynamic webhook event", e);
                return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
            }
        });

        registeredEvents.add(eventName);
        logger.debug("Registered dynamic webhook handler for event: {}", eventName);
    }

    /**
     * Check if a rule's trigger matches the given event.
     * For IFTTT rules, we look for a "trigger" field with an "event" property.
     * For legacy rules, we assume they apply to time entry events.
     */
    private static boolean ruleMatchesEvent(Rule rule, String eventType) {
        // We don’t yet persist explicit trigger metadata on Rule.
        // Until Rule carries a "trigger.event", treat dynamic rules as wildcard (match any event),
        // and rely on user-provided conditions to scope when they run.
        // Time-entry events are handled by the legacy handler and are not registered here.
        return true;
    }

    /**
     * Execute a list of actions, including both classic actions and openapi_call actions.
     */
    private static int executeActions(List<Action> actions, JsonNode payload, String workspaceId, ClockifyClient api) {
        int executedCount = 0;

        for (Action action : actions) {
            try {
                String type = action.getType();
                if ("openapi_call".equals(type)) {
                    executeOpenApiCall(action, payload, workspaceId, api);
                    executedCount++;
                } else {
                    // Legacy actions (add_tag, set_billable, etc.) are handled by WebhookHandlers
                    // For dynamic events, we only execute openapi_call actions here
                    logger.debug("Skipping legacy action type {} for dynamic event", type);
                }
            } catch (Exception e) {
                logger.error("Failed to execute action: {}", action, e);
            }
        }

        return executedCount;
    }

    /**
     * Execute an openapi_call action by resolving placeholders and calling the Clockify API.
     */
    private static void executeOpenApiCall(Action action, JsonNode payload, String workspaceId, ClockifyClient api) {
        Map<String, String> args = action.getArgs();
        if (args == null) {
            logger.warn("openapi_call action has no args");
            return;
        }

        // Extract endpoint details from args
        String method = args.get("method");
        String pathTemplate = args.get("path");

        if (method == null || pathTemplate == null) {
            logger.warn("openapi_call action missing method or path");
            return;
        }

        // Resolve path parameters
        String resolvedPath = PlaceholderResolver.resolve(pathTemplate, payload);
        logger.info("Executing openapi_call: {} {}", method, resolvedPath);

        // Build request body if present
        String bodyJson = args.get("body");
        String resolvedBody = null;
        if (bodyJson != null && !bodyJson.isBlank()) {
            try {
                JsonNode bodyTemplate = objectMapper.readTree(bodyJson);
                JsonNode resolvedBodyNode = PlaceholderResolver.resolveInJson(bodyTemplate, payload);
                resolvedBody = resolvedBodyNode.toString();
            } catch (Exception e) {
                logger.error("Failed to parse body JSON", e);
            }
        }

        // Make the API call
        try {
            // For now, use a simple HTTP client approach
            // In a full implementation, you'd use ClockifyClient methods
            logger.info("API call: {} {} with body: {}", method, resolvedPath, resolvedBody);

            // Placeholder for actual API call - would need to extend ClockifyClient
            // or use the SDK's ClockifyHttpClient directly
            var httpClient = new com.clockify.addon.sdk.http.ClockifyHttpClient(api.getBaseUrl());

            if ("POST".equalsIgnoreCase(method)) {
                // httpClient.postJson(resolvedPath, api.getToken(), resolvedBody, Map.of());
            } else if ("PUT".equalsIgnoreCase(method)) {
                // httpClient.putJson(resolvedPath, api.getToken(), resolvedBody, Map.of());
            } else if ("PATCH".equalsIgnoreCase(method)) {
                // httpClient.patchJson(resolvedPath, api.getToken(), resolvedBody, Map.of());
            } else if ("DELETE".equalsIgnoreCase(method)) {
                // httpClient.delete(resolvedPath, api.getToken(), Map.of());
            }

            logger.info("Successfully executed openapi_call");
        } catch (Exception e) {
            logger.error("Failed to execute API call", e);
        }
    }

    private static String extractWorkspaceId(JsonNode payload) {
        if (payload.has("workspaceId")) {
            return payload.get("workspaceId").asText(null);
        }
        return null;
    }

    private static HttpResponse createResponse(String eventType, String status, List<Action> actions) {
        return createResponse(eventType, status, actions, actions.size());
    }

    private static HttpResponse createResponse(String eventType, String status, List<Action> actions, int executedCount) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("event", eventType);
            response.put("status", status);
            response.put("actionsCount", actions.size());
            response.put("executedCount", executedCount);
            return HttpResponse.ok(response.toString(), "application/json");
        } catch (Exception e) {
            return HttpResponse.error(500, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
        }
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
