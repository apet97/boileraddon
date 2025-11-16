package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.example.rules.config.RuntimeFlags;
import com.example.rules.engine.Action;
import com.example.rules.engine.Evaluator;
import com.example.rules.engine.OpenApiCallConfig;
import com.example.rules.engine.OpenApiCallException;
import com.example.rules.engine.PlaceholderResolver;
import com.example.rules.engine.Rule;
import com.example.rules.engine.TimeEntryContext;
import com.example.rules.cache.RuleCache;
import com.example.rules.cache.WebhookIdempotencyCache;
import com.example.rules.api.ErrorResponse;
import com.example.rules.metrics.RulesMetrics;
import com.example.rules.store.RulesStoreSPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
        // Note: Clockify only supports these webhook events (no *_UPDATED/*_DELETED variants)
        String[] commonEvents = {
            // Exclude time-entry events here to avoid overriding legacy handler
            // "NEW_TIME_ENTRY", "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_PROJECT",
            "NEW_CLIENT",
            "NEW_TAG",
            "NEW_TASK",
            "USER_JOINED_WORKSPACE", "USER_DELETED_FROM_WORKSPACE"
        };

        for (String event : commonEvents) {
            registerEvent(addon, event);
        }

        logger.info("Registered {} common webhook events for dynamic handling", commonEvents.length);

        int restored = registerExistingRuleEvents(addon);
        if (restored > 0) {
            logger.info("Registered {} persisted webhook events for dynamic handling", restored);
        } else {
            logger.debug("No persisted dynamic webhook events required registration");
        }
    }

    public static boolean registerEvent(ClockifyAddon addon, String eventName) {
        if (addon == null || eventName == null || eventName.isBlank()) {
            return false;
        }
        // Never override legacy time-entry events — leave them to WebhookHandlers
        if ("NEW_TIME_ENTRY".equals(eventName) || "TIME_ENTRY_UPDATED".equals(eventName)) {
            logger.info("Skipping dynamic registration for time-entry event {} (handled by legacy handler)", eventName);
            return false;
        }
        if (registeredEvents.contains(eventName) && addon.getWebhookPathsByEvent().containsKey(eventName)) {
            return false; // Already registered for this addon
        }

        addon.registerWebhookHandler(eventName, request -> {
            Timer.Sample sample = RulesMetrics.startWebhookTimer();
            String eventType = eventName;
            try (LoggingContext loggingContext = LoggingContext.create()) {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = extractWorkspaceId(payload);
                if (workspaceId == null || workspaceId.isBlank()) {
                    return respondWithMetrics(sample, eventType, "invalid_workspace",
                            ErrorResponse.of(400, "RULES.MISSING_WORKSPACE", "workspaceId missing in payload", request, false));
                }
                request.setAttribute(DiagnosticContextFilter.WORKSPACE_ID_ATTR, workspaceId);
                loggingContext.workspace(workspaceId);
                if (payload.has("event")) {
                    eventType = payload.get("event").asText();
                }
                String userId = payload.path("userId").asText(null);
                if (userId != null && !userId.isBlank()) {
                    request.setAttribute(DiagnosticContextFilter.USER_ID_ATTR, userId);
                    loggingContext.user(userId);
                }

                if (!RuntimeFlags.skipSignatureVerification()) {
                    WebhookSignatureValidator.VerificationResult verificationResult =
                            WebhookSignatureValidator.verify(request, workspaceId, addon.getManifest().getKey());
                    if (!verificationResult.isValid()) {
                        logger.warn("Webhook signature verification failed for workspace {}", workspaceId);
                        return respondWithMetrics(sample, eventType, "invalid_signature", verificationResult.response());
                    }
                }

                logger.info("Dynamic webhook event received: {} for workspace {}", eventType, workspaceId);

                if (WebhookIdempotencyCache.isDuplicate(workspaceId, eventType, payload)) {
                    logger.info("Duplicate dynamic webhook suppressed | workspace={} event={}", workspaceId, eventType);
                    return respondWithMetrics(sample, eventType, "duplicate",
                            createResponse(eventType, "duplicate", new ArrayList<>(), ActionExecutionSummary.none()));
                }

                List<Rule> allRules = RuleCache.getEnabledRules(workspaceId);
                List<Rule> matchingRules = new ArrayList<>();
                for (Rule rule : allRules) {
                    if (ruleMatchesEvent(rule, eventType)) {
                        matchingRules.add(rule);
                    }
                }
                matchingRules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

                if (matchingRules.isEmpty()) {
                    logger.debug("No rules found for event {} in workspace {} (total enabled rules: {})",
                            eventType, workspaceId, allRules.size());
                    RulesMetrics.recordRuleEvaluation(eventType, allRules.size(), 0);
                    return respondWithMetrics(sample, eventType, "no_rules",
                            createResponse(eventType, "no_matching_rules", new ArrayList<>(), ActionExecutionSummary.none()));
                }

                logger.info("Found {} matching rules for event {} in workspace {}",
                        matchingRules.size(), eventType, workspaceId);

                List<Action> allActions = new ArrayList<>();
                TimeEntryContext context = new TimeEntryContext(payload);
                int matchedRules = 0;
                for (Rule rule : matchingRules) {
                    boolean conditionsMet = rule.getConditions() == null || rule.getConditions().isEmpty()
                            || evaluator.evaluate(rule, context);
                    if (conditionsMet && rule.getActions() != null) {
                        matchedRules++;
                        logger.info("Rule '{}' matched for event {}", rule.getName(), eventType);
                        allActions.addAll(rule.getActions());
                    }
                }
                RulesMetrics.recordRuleEvaluation(eventType, allRules.size(), matchedRules);

                if (allActions.isEmpty()) {
                    return respondWithMetrics(sample, eventType, "no_actions",
                            createResponse(eventType, "no_actions", new ArrayList<>(), ActionExecutionSummary.none()));
                }

                if (!RuntimeFlags.applyChangesEnabled()) {
                    logger.info("RULES_APPLY_CHANGES=false — logging actions only");
                    return respondWithMetrics(sample, eventType, "logged",
                            createResponse(eventType, "actions_logged", allActions, ActionExecutionSummary.none()));
                }

                var wkOpt = com.clockify.addon.sdk.security.TokenStore.get(workspaceId);
                if (wkOpt.isEmpty()) {
                    logger.warn("Missing installation token for workspace {} — skipping mutations", workspaceId);
                    return respondWithMetrics(sample, eventType, "missing_token",
                            ErrorResponse.of(412, "RULES.MISSING_TOKEN", "Workspace installation token not found", request, false));
                }

                var wk = wkOpt.get();
                ClockifyClient api = new ClockifyClient(wk.apiBaseUrl(), wk.token());

                ActionExecutionSummary summary = executeActions(allActions, payload, workspaceId, api);
                String outcome = summary.failed() > 0 ? "partial" : "success";
                return respondWithMetrics(sample, eventType, outcome,
                        createResponse(eventType, "actions_applied", allActions, summary));

            } catch (Exception e) {
                logger.error("Error processing dynamic webhook event", e);
                return respondWithMetrics(sample, eventType, "error",
                        ErrorResponse.of(500, "RULES.UNHANDLED_ERROR",
                                "Unexpected webhook error", request, true, e.getMessage()));
            }
        });

        registeredEvents.add(eventName);
        logger.debug("Registered dynamic webhook handler for event: {}", eventName);
        return true;
    }

    public static boolean registerTriggerEvent(ClockifyAddon addon, Rule rule) {
        if (addon == null || rule == null || !rule.isEnabled()) {
            return false;
        }
        Map<String, Object> trigger = rule.getTrigger();
        if (trigger == null || trigger.isEmpty()) {
            return false;
        }
        Object eventValue = trigger.get("event");
        if (!(eventValue instanceof String event)) {
            return false;
        }
        String trimmed = event.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return registerEvent(addon, trimmed);
    }

    private static int registerExistingRuleEvents(ClockifyAddon addon) {
        if (addon == null || rulesStore == null) {
            return 0;
        }
        List<String> workspaceIds;
        try {
            workspaceIds = rulesStore.listWorkspaces();
        } catch (UnsupportedOperationException e) {
            logger.debug("Rules store does not support listing workspaces: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            logger.warn("Failed to enumerate workspaces for dynamic event registration", e);
            return 0;
        }

        if (workspaceIds == null || workspaceIds.isEmpty()) {
            return 0;
        }

        int registrations = 0;
        for (String workspaceId : workspaceIds) {
            if (workspaceId == null || workspaceId.isBlank()) {
                continue;
            }
            try {
                List<Rule> enabledRules = rulesStore.getEnabled(workspaceId);
                for (Rule rule : enabledRules) {
                    if (registerTriggerEvent(addon, rule)) {
                        registrations++;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to register dynamic webhook events for workspace {}", workspaceId, e);
            }
        }

        return registrations;
    }

    /**
     * Check if a rule's trigger matches the given event.
     * For IFTTT rules, we look for a "trigger" field with an "event" property.
     * For legacy rules, we assume they apply to time entry events.
     */
    private static boolean ruleMatchesEvent(Rule rule, String eventType) {
        // If rule has explicit trigger metadata, use it for matching
        if (rule.getTrigger() != null && !rule.getTrigger().isEmpty()) {
            Object triggerEvent = rule.getTrigger().get("event");
            if (triggerEvent instanceof String) {
                return eventType.equals(triggerEvent);
            }
            // If trigger has no specific event, it's a wildcard rule
            return true;
        }

        // Legacy rules without trigger metadata are considered wildcard
        // This maintains backward compatibility with existing rules
        return true;
    }

    private static ActionExecutionSummary executeActions(List<Action> actions, JsonNode payload, String workspaceId, ClockifyClient api) {
        int attempted = 0;
        int succeeded = 0;
        int failed = 0;

        for (Action action : actions) {
            String type = action.getType();
            if (!"openapi_call".equals(type)) {
                logger.debug("Skipping legacy action type {} for dynamic event", type);
                continue;
            }
            attempted++;
            boolean success = executeOpenApiCallWithRetry(action, payload, workspaceId, api);
            if (success) {
                succeeded++;
            } else {
                failed++;
            }
            RulesMetrics.recordActionResult(type, success);
        }

        return new ActionExecutionSummary(attempted, succeeded, failed);
    }

    /**
     * Execute an openapi_call action with retry mechanism.
     */
    private static boolean executeOpenApiCallWithRetry(Action action, JsonNode payload, String workspaceId, ClockifyClient api) {
        OpenApiCallConfig config;
        try {
            config = OpenApiCallConfig.from(action, objectMapper);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid openapi_call configuration for workspace {}: {}", workspaceId, ex.getMessage());
            return false;
        }

        final int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                performOpenApiCall(config, payload, workspaceId, api);
                logger.info("Successfully executed openapi_call on attempt {}/{}", attempt, maxAttempts);
                return true;
            } catch (OpenApiCallException ex) {
                logger.warn("openapi_call attempt {}/{} failed (status={}): {}", attempt, maxAttempts,
                        ex.getStatusCode(), ex.getMessage());

                if (!ex.isRetryable() || attempt == maxAttempts) {
                    logger.error("openapi_call failed after {} attempts (status={}): bodyLength={}",
                            maxAttempts, ex.getStatusCode(),
                            ex.getResponseBody() == null ? 0 : ex.getResponseBody().length());
                    return false;
                }

                long delay = computeDelay(attempt, ex.getRetryAfterMillis());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry interrupted for workspace {}", workspaceId, interruptedException);
                    return false;
                }
            } catch (Exception ex) {
                logger.error("Unexpected error executing openapi_call for workspace {}", workspaceId, ex);
                return false;
            }
        }
        return false;
    }

    private static void performOpenApiCall(OpenApiCallConfig config, JsonNode payload, String workspaceId, ClockifyClient api)
            throws OpenApiCallException {
        OpenApiCallConfig.ResolvedCall request = config.resolve(payload);
        logger.info("Executing openapi_call: {} {}", request.method(), request.path());
        if (request.body() != null && !request.body().isBlank()) {
            logger.debug("openapi_call payload bytes={}", request.body().length());
        }

        java.net.http.HttpResponse<String> response;
        try {
            response = api.openapiCall(request.method(), request.path(), request.body());
        } catch (Exception e) {
            throw new OpenApiCallException("Clockify API request failed", e, true);
        }

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        boolean retryable = status == 429 || status >= 500;
        throw new OpenApiCallException(
                "Clockify API returned status " + status,
                status,
                retryable,
                truncateBody(response.body()),
                parseRetryAfterMillis(response));
    }

    private static String extractWorkspaceId(JsonNode payload) {
        if (payload.has("workspaceId")) {
            return payload.get("workspaceId").asText(null);
        }
        return null;
    }

    static long computeDelay(int attempt, Long retryAfterMillis) {
        if (retryAfterMillis != null && retryAfterMillis > 0) {
            return Math.min(retryAfterMillis, 5_000L);
        }
        long base = 250L * (1L << Math.max(0, attempt - 1));
        base = Math.min(base, 2_000L);
        long jitter = ThreadLocalRandom.current().nextLong(50, 150);
        return base + jitter;
    }

    private static Long parseRetryAfterMillis(java.net.http.HttpResponse<String> response) {
        return response.headers()
                .firstValue("Retry-After")
                .map(value -> {
                    try {
                        return Long.parseLong(value) * 1000L;
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private static String truncateBody(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= 512 ? body : body.substring(0, 512);
    }

    private static HttpResponse respondWithMetrics(Timer.Sample sample, String eventType, String outcome, com.clockify.addon.sdk.HttpResponse response) {
        RulesMetrics.stopWebhookTimer(sample, eventType, outcome);
        return response;
    }

    private static HttpResponse createResponse(String eventType, String status, List<Action> actions, ActionExecutionSummary summary) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("event", eventType);
            response.put("status", status);
            response.put("actionsCount", actions.size());
            if (summary != null) {
                response.put("actionsAttempted", summary.attempted());
                response.put("executedCount", summary.succeeded());
                response.put("actionsFailed", summary.failed());
            } else {
                response.put("executedCount", 0);
            }
            return HttpResponse.ok(response.toString(), "application/json");
        } catch (Exception e) {
            logger.error("Failed to serialize dynamic webhook response", e);
            return ErrorResponse.of(500, "RULES.RESPONSE_BUILD_FAILED", "Failed to serialize response", null, true);
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

    private record ActionExecutionSummary(int attempted, int succeeded, int failed) {
        static ActionExecutionSummary none() {
            return new ActionExecutionSummary(0, 0, 0);
        }
    }
}
