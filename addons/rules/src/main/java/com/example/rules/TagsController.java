package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.example.rules.api.ErrorResponse;
import com.example.rules.cache.WorkspaceCache;
import com.example.rules.engine.OpenApiCallConfig;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.Optional;

/**
 * CRUD controller for Tags management.
 * Provides endpoints for creating, reading, updating, and deleting tags.
 */
public class TagsController {

    private static final Logger logger = LoggerFactory.getLogger(TagsController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MEDIA_JSON = "application/json";

    private final ClockifyClientFactory clientFactory;

    public TagsController(ClockifyClientFactory clientFactory) {
        this.clientFactory = clientFactory == null ? ClockifyClient::new : clientFactory;
    }

    public TagsController() {
        this(ClockifyClient::new);
    }

    private Optional<ClockifyClient> getWorkspaceClockifyClient(String workspaceId) {
        var tokenOpt = com.clockify.addon.sdk.security.TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        var token = tokenOpt.get();
        return Optional.of(clientFactory.create(token.apiBaseUrl(), token.token()));
    }

    /**
     * GET /api/tags - List all tags for a workspace
     */
    public RequestHandler listTags() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }
                JsonNode tags = workspaceClient.get().getTags(workspaceId);
                return HttpResponse.ok(tags.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TAGS.LIST_FAILED", "Failed to list tags", e, true);
            }
        };
    }

    /**
     * POST /api/tags - Create a new tag
     */
    public RequestHandler createTag() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                JsonNode body = parseRequestBody(request);

                // Validate required fields
                if (!body.has("name") || body.get("name").asText().isBlank()) {
                    return ErrorResponse.of(400, "TAGS.NAME_REQUIRED", "Tag name is required", request, false);
                }

                String tagName = body.get("name").asText();

                // Use createTag method for POST to /workspaces/{workspaceId}/tags
                JsonNode tag = workspaceClient.get().createTag(workspaceId, tagName);

                // Refresh workspace cache to reflect new tag
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(tag.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TAGS.CREATE_FAILED", "Failed to create tag", e, true);
            }
        };
    }

    /**
     * PUT /api/tags - Update an existing tag
     */
    public RequestHandler updateTag() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String tagId = extractTagId(request);
                if (tagId == null) {
                    return ErrorResponse.of(400, "TAGS.TAG_ID_REQUIRED", "tagId is required", request, false);
                }

                JsonNode body = parseRequestBody(request);

                // Build tag update payload
                ObjectNode tagPayload = objectMapper.createObjectNode();

                // Update only provided fields
                if (body.has("name")) {
                    tagPayload.set("name", body.get("name"));
                }
                if (body.has("archived")) {
                    tagPayload.set("archived", body.get("archived"));
                }

                // Use openapiCall for PUT to /workspaces/{workspaceId}/tags/{tagId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.PUT,
                    "/workspaces/" + workspaceId + "/tags/" + tagId,
                    tagPayload.toString()
                );

                // Refresh workspace cache to reflect updated tag
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TAGS.UPDATE_FAILED", "Failed to update tag", e, true);
            }
        };
    }

    /**
     * DELETE /api/tags - Delete a tag by id
     */
    public RequestHandler deleteTag() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String tagId = extractTagId(request);
                if (tagId == null) {
                    return ErrorResponse.of(400, "TAGS.TAG_ID_REQUIRED", "tagId is required", request, false);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                // Use openapiCall for DELETE to /workspaces/{workspaceId}/tags/{tagId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.DELETE,
                    "/workspaces/" + workspaceId + "/tags/" + tagId,
                    null
                );

                // Refresh workspace cache to reflect deleted tag
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                ObjectNode result = objectMapper.createObjectNode();
                result.put("deleted", true);
                result.put("tagId", tagId);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TAGS.DELETE_FAILED", "Failed to delete tag", e, true);
            }
        };
    }

    private String extractTagId(HttpServletRequest request) throws Exception {
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
            if (segments.length >= 4 && "api".equals(segments[1]) && "tags".equals(segments[2])) {
                return segments[3];
            }
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

    private HttpResponse workspaceRequired(HttpServletRequest request) {
        String hint = RequestContext.workspaceFallbackAllowed()
                ? "workspaceId is required"
                : "workspaceId is required (Authorization bearer token missing or expired)";
        return ErrorResponse.of(400, "TAGS.WORKSPACE_REQUIRED", hint, request, false);
    }

    private HttpResponse tokenMissing(HttpServletRequest request) {
        return ErrorResponse.of(
                412,
                "RULES.MISSING_TOKEN",
                "Workspace installation token not found",
                request,
                false
        );
    }

    private HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e, boolean retryable) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, retryable, e.getMessage());
    }
}
