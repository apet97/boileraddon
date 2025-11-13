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

/**
 * CRUD controller for Projects management.
 * Provides endpoints for creating, reading, updating, and deleting projects.
 */
public class ProjectsController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectsController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MEDIA_JSON = "application/json";

    private final ClockifyClient clockifyClient;

    public ProjectsController(ClockifyClient clockifyClient) {
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
     * GET /api/projects - List all projects for a workspace
     */
    public RequestHandler listProjects() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                boolean archived = Boolean.parseBoolean(request.getParameter("archived"));
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                JsonNode projects = workspaceClient.getProjects(workspaceId, archived);
                return HttpResponse.ok(projects.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "PROJECTS.LIST_FAILED", "Failed to list projects", e, true);
            }
        };
    }

    /**
     * POST /api/projects - Create a new project
     */
    public RequestHandler createProject() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                JsonNode body = parseRequestBody(request);

                // Validate required fields
                if (!body.has("name") || body.get("name").asText().isBlank()) {
                    return ErrorResponse.of(400, "PROJECTS.NAME_REQUIRED", "Project name is required", request, false);
                }

                // Build project creation payload
                ObjectNode projectPayload = objectMapper.createObjectNode();
                projectPayload.set("name", body.get("name"));

                // Optional fields
                if (body.has("clientId")) {
                    projectPayload.set("clientId", body.get("clientId"));
                }
                if (body.has("color")) {
                    projectPayload.set("color", body.get("color"));
                }
                if (body.has("note")) {
                    projectPayload.set("note", body.get("note"));
                }
                if (body.has("isPublic")) {
                    projectPayload.set("public", body.get("isPublic"));
                }
                if (body.has("estimate")) {
                    projectPayload.set("estimate", body.get("estimate"));
                }

                // Use openapiCall for POST to /workspaces/{workspaceId}/projects
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                var response = workspaceClient.openapiCall(
                    OpenApiCallConfig.HttpMethod.POST,
                    "/workspaces/" + workspaceId + "/projects",
                    projectPayload.toString()
                );

                // Refresh workspace cache to reflect new project
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "PROJECTS.CREATE_FAILED", "Failed to create project", e, true);
            }
        };
    }

    /**
     * PUT /api/projects - Update an existing project
     */
    public RequestHandler updateProject() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String projectId = extractProjectId(request);
                if (projectId == null) {
                    return ErrorResponse.of(400, "PROJECTS.PROJECT_ID_REQUIRED", "projectId is required", request, false);
                }

                JsonNode body = parseRequestBody(request);

                // Build project update payload
                ObjectNode projectPayload = objectMapper.createObjectNode();

                // Update only provided fields
                if (body.has("name")) {
                    projectPayload.set("name", body.get("name"));
                }
                if (body.has("clientId")) {
                    projectPayload.set("clientId", body.get("clientId"));
                }
                if (body.has("color")) {
                    projectPayload.set("color", body.get("color"));
                }
                if (body.has("note")) {
                    projectPayload.set("note", body.get("note"));
                }
                if (body.has("isPublic")) {
                    projectPayload.set("public", body.get("isPublic"));
                }
                if (body.has("archived")) {
                    projectPayload.set("archived", body.get("archived"));
                }
                if (body.has("estimate")) {
                    projectPayload.set("estimate", body.get("estimate"));
                }

                // Use openapiCall for PUT to /workspaces/{workspaceId}/projects/{projectId}
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                var response = workspaceClient.openapiCall(
                    OpenApiCallConfig.HttpMethod.PUT,
                    "/workspaces/" + workspaceId + "/projects/" + projectId,
                    projectPayload.toString()
                );

                // Refresh workspace cache to reflect updated project
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "PROJECTS.UPDATE_FAILED", "Failed to update project", e, true);
            }
        };
    }

    /**
     * DELETE /api/projects - Delete a project by id
     */
    public RequestHandler deleteProject() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String projectId = extractProjectId(request);
                if (projectId == null) {
                    return ErrorResponse.of(400, "PROJECTS.PROJECT_ID_REQUIRED", "projectId is required", request, false);
                }

                // Use openapiCall for DELETE to /workspaces/{workspaceId}/projects/{projectId}
                ClockifyClient workspaceClient = getWorkspaceClockifyClient(workspaceId);
                var response = workspaceClient.openapiCall(
                    OpenApiCallConfig.HttpMethod.DELETE,
                    "/workspaces/" + workspaceId + "/projects/" + projectId,
                    null
                );

                // Refresh workspace cache to reflect deleted project
                var token = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).get();
                WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token());

                ObjectNode result = objectMapper.createObjectNode();
                result.put("deleted", true);
                result.put("projectId", projectId);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "PROJECTS.DELETE_FAILED", "Failed to delete project", e, true);
            }
        };
    }

    private String extractProjectId(HttpServletRequest request) throws Exception {
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
            if (segments.length >= 4 && "api".equals(segments[1]) && "projects".equals(segments[2])) {
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
        return ErrorResponse.of(400, "PROJECTS.WORKSPACE_REQUIRED", hint, request, false);
    }

    private HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e, boolean retryable) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, retryable, e.getMessage());
    }
}
