package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.example.rules.api.ErrorResponse;
import com.example.rules.cache.WorkspaceCache;
import com.example.rules.engine.OpenApiCallConfig;
import com.example.rules.security.PermissionChecker;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

/**
 * CRUD controller for Tags management.
 * Provides endpoints for creating, reading, updating, and deleting tags.
 */
public class TagsController {

    private static final Logger logger = LoggerFactory.getLogger(TagsController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MEDIA_JSON = "application/json";

    private final ClockifyClient clockifyClient;

    public TagsController(ClockifyClient clockifyClient) {
        this.clockifyClient = clockifyClient;
    }

    private ClockifyClient getWorkspaceClockifyClient(String workspaceId) {
        // Get the workspace token from TokenStore
        var tokenOpt = com.clockify.addon.sdk.security.TokenStore.get(workspaceId);
        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("No installation token found for workspace: " + workspaceId);
        }
        var token = tokenOpt.get();
        return new ClockifyClient(token.apiBaseUrl(), token.token());
    }

    /**
     * GET /api/tags - List all tags for a workspace
     */
    public RequestHandler listTags() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                // Check read permissions
                if (!PermissionChecker.canReadTags(workspaceId)) {
                    return ErrorResponse.of(403, "TAGS.INSUFFICIENT_PERMISSIONS",
                        "Insufficient permissions to read tags", request, false);
                }

                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                JsonNode tags = workspaceClient.getTags(workspaceId);
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
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                // Check write permissions
                if (!PermissionChecker.canWriteTags(workspaceId)) {
                    return ErrorResponse.of(403, "TAGS.INSUFFICIENT_PERMISSIONS",
                        "Insufficient permissions to create tags", request, false);
                }

                JsonNode body = parseRequestBody(request);

                // Validate required fields
                if (!body.has("name") || body.get("name").asText().isBlank()) {
                    return ErrorResponse.of(400, "TAGS.NAME_REQUIRED", "Tag name is required", request, false);
                }

                String tagName = body.get("name").asText();

                // Use createTag method for POST to /workspaces/{workspaceId}/tags
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                JsonNode tag = workspaceClient.createTag(workspaceId, tagName);

                // Refresh workspace cache to reflect new tag
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

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
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                // Check write permissions
                if (!PermissionChecker.canWriteTags(workspaceId)) {
                    return ErrorResponse.of(403, "TAGS.INSUFFICIENT_PERMISSIONS",
                        "Insufficient permissions to update tags", request, false);
                }

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
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                var response = workspaceClient.openapiCall(
                    OpenApiCallConfig.HttpMethod.PUT,
                    "/workspaces/" + workspaceId + "/tags/" + tagId,
                    tagPayload.toString()
                );

                // Refresh workspace cache to reflect updated tag
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

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
                String workspaceId = getWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                // Check write permissions
                if (!PermissionChecker.canWriteTags(workspaceId)) {
                    return ErrorResponse.of(403, "TAGS.INSUFFICIENT_PERMISSIONS",
                        "Insufficient permissions to delete tags", request, false);
                }

                String tagId = extractTagId(request);
                if (tagId == null) {
                    return ErrorResponse.of(400, "TAGS.TAG_ID_REQUIRED", "tagId is required", request, false);
                }

                // Use openapiCall for DELETE to /workspaces/{workspaceId}/tags/{tagId}
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                var response = workspaceClient.openapiCall(
                    OpenApiCallConfig.HttpMethod.DELETE,
                    "/workspaces/" + workspaceId + "/tags/" + tagId,
                    null
                );

                // Refresh workspace cache to reflect deleted tag
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

                ObjectNode result = objectMapper.createObjectNode();
                result.put("deleted", true);
                result.put("tagId", tagId);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TAGS.DELETE_FAILED", "Failed to delete tag", e, true);
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
        return ErrorResponse.of(400, "TAGS.WORKSPACE_REQUIRED", "workspaceId is required", request, false);
    }

    private HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e, boolean retryable) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, retryable, e.getMessage());
    }
}