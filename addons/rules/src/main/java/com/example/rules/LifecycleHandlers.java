package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.example.rules.api.ErrorResponse;
import com.example.rules.store.RulesStoreSPI;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

/**
 * Handles add-on lifecycle events.
 *
 * INSTALLED event:
 * - Sent when workspace admin installs the add-on
 * - Payload includes workspace-specific auth token
 * - CRITICAL: Store this token securely - it's needed for all Clockify API calls
 *
 * DELETED event:
 * - Sent when workspace admin uninstalls the add-on
 * - Clean up any stored data for this workspace
 */
public class LifecycleHandlers {
    private static final Logger logger = LoggerFactory.getLogger(LifecycleHandlers.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static RulesStoreSPI rulesStore;

    public static void register(ClockifyAddon addon, RulesStoreSPI store) {
        rulesStore = store;

        // Handle INSTALLED event
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            // Verify lifecycle signature (JWT) before processing
            com.clockify.addon.sdk.security.WebhookSignatureValidator.VerificationResult sig =
                    com.clockify.addon.sdk.security.WebhookSignatureValidator.verifyLifecycle(request, addon.getManifest().getKey());
            if (!sig.isValid()) {
                return sig.response();
            }
            try (LoggingContext loggingContext = LoggingContext.create()) {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                String userId = payload.has("userId") ? payload.get("userId").asText(null) : null;
                String authToken = payload.has("authToken") ? payload.get("authToken").asText(null) : null;
                String apiUrl = payload.has("apiUrl") ? payload.get("apiUrl").asText(null) : null;

                RequestContext.attachWorkspace(request, loggingContext, workspaceId);
                RequestContext.attachUser(request, loggingContext, userId);

                logger.info("Lifecycle INSTALLED received for workspace {} (user={})", workspaceId, userId);

                // SECURITY: Redact sensitive fields before logging payload for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("INSTALLED payload (sanitized): {}", redactSensitiveFields(payload));
                }

                if (workspaceId == null || workspaceId.isBlank()) {
                    return ErrorResponse.of(400, "RULES.MISSING_WORKSPACE", "workspaceId missing in payload", request, false);
                }
                if (authToken == null || authToken.isBlank()) {
                    logger.warn("Missing auth token in INSTALLED payload for workspace {}", workspaceId);
                    return ErrorResponse.of(400, "RULES.MISSING_TOKEN", "authToken missing in payload", request, false);
                }

                com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, apiUrl);
                logger.info("Stored installation token for workspace {}", workspaceId);
                try {
                    var wk = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).orElse(null);
                    if (wk != null) {
                        com.example.rules.cache.WorkspaceCache.refreshAsync(workspaceId, wk.apiBaseUrl(), wk.token());
                    }
                } catch (Exception ignored) {}

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "installed")
                        .put("message", "Rules add-on installed successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                logger.error("Error handling INSTALLED event", e);
                return ErrorResponse.of(500, "RULES.LIFECYCLE_ERROR",
                        "Lifecycle handler failed", request, true, e.getMessage());
            }
        });

        // Handle DELETED event
        addon.registerLifecycleHandler("DELETED", "/lifecycle/deleted", request -> {
            com.clockify.addon.sdk.security.WebhookSignatureValidator.VerificationResult sig =
                    com.clockify.addon.sdk.security.WebhookSignatureValidator.verifyLifecycle(request, addon.getManifest().getKey());
            if (!sig.isValid()) {
                return sig.response();
            }
            try (LoggingContext loggingContext = LoggingContext.create()) {
                JsonNode payload = parseRequestBody(request);
                String workspaceId = payload.has("workspaceId") ? payload.get("workspaceId").asText(null) : null;
                RequestContext.attachWorkspace(request, loggingContext, workspaceId);
                logger.info("Lifecycle DELETED received for workspace {}", workspaceId);

                if (workspaceId == null || workspaceId.isBlank()) {
                    return ErrorResponse.of(400, "RULES.MISSING_WORKSPACE", "workspaceId missing in payload", request, false);
                }

                boolean removed = com.clockify.addon.sdk.security.TokenStore.delete(workspaceId);
                if (removed) {
                    logger.info("Removed stored auth token for workspace {}", workspaceId);
                } else {
                    logger.info("No stored auth token found for workspace {}", workspaceId);
                }

                int deletedRules = rulesStore.deleteAll(workspaceId);
                logger.info("Deleted {} rules for workspace {}", deletedRules, workspaceId);

                String responseBody = objectMapper.createObjectNode()
                        .put("status", "uninstalled")
                        .put("message", "Rules add-on uninstalled successfully")
                        .toString();

                return HttpResponse.ok(responseBody, "application/json");

            } catch (Exception e) {
                logger.error("Error handling DELETED event", e);
                return ErrorResponse.of(500, "RULES.LIFECYCLE_ERROR",
                        "Lifecycle handler failed", request, true, e.getMessage());
            }
        });
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

    /**
     * Redacts sensitive fields from lifecycle payloads to prevent token leakage in logs.
     *
     * SECURITY NOTE: Installation tokens have full workspace access and never expire.
     * Per Clockify addon guide (line 1749): "The installation token should never be logged."
     *
     * @param payload The original lifecycle event payload
     * @return A sanitized copy with sensitive fields redacted
     */
    private static JsonNode redactSensitiveFields(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return payload;
        }

        ObjectNode sanitized = ((ObjectNode) payload).deepCopy();

        // Redact installation token (critical security requirement)
        if (sanitized.has("authToken")) {
            sanitized.put("authToken", "[REDACTED]");
        }

        // Redact webhook tokens if present
        if (sanitized.has("webhooks") && sanitized.get("webhooks").isArray()) {
            for (JsonNode webhook : sanitized.get("webhooks")) {
                if (webhook.isObject() && webhook.has("authToken")) {
                    ((ObjectNode) webhook).put("authToken", "[REDACTED]");
                }
            }
        }

        return sanitized;
    }
}
