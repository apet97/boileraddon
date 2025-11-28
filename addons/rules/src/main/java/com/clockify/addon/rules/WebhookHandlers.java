package com.clockify.addon.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.security.TokenStore;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import io.micrometer.core.instrument.Counter;

public class WebhookHandlers {
    private static final Logger logger = LoggerFactory.getLogger(WebhookHandlers.class);
    private static final ObjectMapper om = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Counter webhookReceived = Counter.builder("rules_webhook_received_total").register(MetricsHandler.registry());
    private static final Counter webhookApplied = Counter.builder("rules_webhook_applied_total").register(MetricsHandler.registry());
    private static final Counter webhookDryRun = Counter.builder("rules_webhook_dry_run_total").register(MetricsHandler.registry());
    private static final Counter webhookErrors = Counter.builder("rules_webhook_error_total").register(MetricsHandler.registry());

    public static void register(ClockifyAddon addon, boolean applyChanges) {
        String addonKey = addon.getManifest().getKey();
        addon.registerWebhookHandler("TIME_ENTRY_UPDATED", req -> handle(req, applyChanges, addonKey));
    }

    private static HttpResponse handle(HttpServletRequest req, boolean applyChanges, String addonKey) throws Exception {
        JsonNode payload = parse(req);
        String workspaceId = text(payload, "workspaceId");
        WebhookSignatureValidator.VerificationResult sig = WebhookSignatureValidator.verify(req, workspaceId, addonKey);
        if (!sig.isValid()) {
            return sig.response();
        }
        webhookReceived.increment();

        JsonNode timeEntry = payload.path("timeEntry");
        String description = text(timeEntry, "description");
        String timeEntryId = text(timeEntry, "id");
        if (workspaceId == null || workspaceId.isBlank() || timeEntryId == null || timeEntryId.isBlank()) {
            return HttpResponse.error(400, "{\"error\":\"workspaceId and timeEntry.id are required\"}", "application/json");
        }

        List<RuleStore.RuleDefinition> rules = RuleStore.getRules(workspaceId);
        if (rules.isEmpty()) {
            return HttpResponse.ok("{\"status\":\"no-rules\",\"workspaceId\":\"" + workspaceId + "\",\"timeEntryId\":\"" + timeEntryId + "\"}", "application/json");
        }
        List<String> appliedTags = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean attempted = false;
        for (RuleStore.RuleDefinition rule : rules) {
            if (matches(description, rule.matchText())) {
                attempted = true;
                ActionResult result = applyTag(workspaceId, timeEntryId, rule.tag(), applyChanges);
                if (result.applied()) {
                    appliedTags.add(rule.tag());
                } else {
                    logger.warn("Rule action failed for workspace {} entry {}: {}", workspaceId, timeEntryId, result.message());
                    if (result.message() != null && !result.message().isBlank()) {
                        errors.add(result.message());
                    }
                }
            }
        }

        ObjectNode response = om.createObjectNode();
        response.put("workspaceId", workspaceId);
        response.put("timeEntryId", timeEntryId);
        response.put("applyChanges", applyChanges);
        response.put("matchedRules", appliedTags.size());
        ArrayNode tagsNode = om.createArrayNode();
        appliedTags.forEach(tagsNode::add);
        response.set("appliedTags", tagsNode);
        if (!errors.isEmpty()) {
            response.put("status", "action-failed");
            response.set("errors", om.valueToTree(errors));
            webhookErrors.increment(errors.size());
        } else if (appliedTags.isEmpty()) {
            response.put("status", rules.isEmpty() ? "no-rules" : attempted ? "no-op" : "no-match");
            if (!attempted) {
                webhookDryRun.increment();
            }
        } else {
            response.put("status", "actions-triggered");
            if (applyChanges) {
                webhookApplied.increment(appliedTags.size());
            } else {
                webhookDryRun.increment(appliedTags.size());
            }
        }
        return HttpResponse.ok(response.toString(), "application/json");
    }

    private static boolean matches(String description, String matchText) {
        return description != null && matchText != null
                && description.toLowerCase(Locale.ROOT).contains(matchText.toLowerCase(Locale.ROOT));
    }

    private static ActionResult applyTag(String workspaceId, String timeEntryId, String tagName, boolean applyChanges) {
        if (!applyChanges) {
            logger.info("Dry-run: would apply tag '{}' to time entry {} in workspace {}", tagName, timeEntryId, workspaceId);
            return new ActionResult(true, null);
        }
        Optional<TokenStore.WorkspaceToken> tokenOpt = TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            return new ActionResult(false, "installation token missing");
        }
        TokenStore.WorkspaceToken token = tokenOpt.get();
        String target = token.apiBaseUrl()
                + "/workspaces/" + workspaceId
                + "/time-entries/" + timeEntryId
                + "/tags";
        ObjectNode body = om.createObjectNode();
        body.put("name", tagName);

        HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token.token())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            java.net.http.HttpResponse<String> apiResponse = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (apiResponse.statusCode() >= 200 && apiResponse.statusCode() < 300) {
                logger.info("Applied tag '{}' to time entry {} in workspace {}", tagName, timeEntryId, workspaceId);
                return new ActionResult(true, null);
            }
            return new ActionResult(false, "Clockify API status " + apiResponse.statusCode());
        } catch (Exception e) {
            logger.warn("Failed to apply tag '{}' for workspace {}: {}", tagName, workspaceId, e.getMessage());
            return new ActionResult(false, e.getMessage());
        }
    }

    private static JsonNode parse(HttpServletRequest r) throws Exception {
        Object c = r.getAttribute("clockify.jsonBody");
        if (c instanceof JsonNode json) {
            return json;
        }
        String raw = rawBody(r);
        JsonNode parsed = om.readTree(raw);
        r.setAttribute("clockify.jsonBody", parsed);
        return parsed;
    }

    private static String rawBody(HttpServletRequest r) throws Exception {
        Object cached = r.getAttribute("clockify.rawBody");
        if (cached instanceof String s) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = r.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        String body = sb.toString();
        r.setAttribute("clockify.rawBody", body);
        return body;
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.has(f) && !n.get(f).isNull() ? n.get(f).asText(null) : null;
    }

    private record ActionResult(boolean applied, String message) {
    }
}
