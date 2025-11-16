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
 * CRUD controller for Tasks management.
 * Provides endpoints for creating, reading, updating, and deleting tasks.
 */
public class TasksController {

    private static final Logger logger = LoggerFactory.getLogger(TasksController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MEDIA_JSON = "application/json";

    private final ClockifyClientFactory clientFactory;

    public TasksController(ClockifyClientFactory clientFactory) {
        this.clientFactory = clientFactory == null ? ClockifyClient::new : clientFactory;
    }

    public TasksController() {
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
     * GET /api/tasks - List all tasks for a workspace and project
     */
    public RequestHandler listTasks() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String projectId = getProjectId(request);
                if (projectId == null) {
                    return ErrorResponse.of(400, "TASKS.PROJECT_ID_REQUIRED", "projectId is required", request, false);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }
                JsonNode tasks = workspaceClient.get().getTasks(workspaceId, projectId);
                return HttpResponse.ok(tasks.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TASKS.LIST_FAILED", "Failed to list tasks", e, true);
            }
        };
    }

    /**
     * POST /api/tasks - Create a new task
     */
    public RequestHandler createTask() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String projectId = getProjectId(request);
                if (projectId == null) {
                    return ErrorResponse.of(400, "TASKS.PROJECT_ID_REQUIRED", "projectId is required", request, false);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                JsonNode body = parseRequestBody(request);

                // Validate required fields
                if (!body.has("name") || body.get("name").asText().isBlank()) {
                    return ErrorResponse.of(400, "TASKS.NAME_REQUIRED", "Task name is required", request, false);
                }

                // Build task creation payload
                ObjectNode taskPayload = objectMapper.createObjectNode();
                taskPayload.set("name", body.get("name"));

                // Optional fields
                if (body.has("assigneeIds")) {
                    taskPayload.set("assigneeIds", body.get("assigneeIds"));
                }
                if (body.has("estimate")) {
                    taskPayload.set("estimate", body.get("estimate"));
                }
                if (body.has("status")) {
                    taskPayload.set("status", body.get("status"));
                }

                // Use openapiCall for POST to /workspaces/{workspaceId}/projects/{projectId}/tasks
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.POST,
                    "/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks",
                    taskPayload.toString()
                );

                // Refresh workspace cache to reflect new task
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TASKS.CREATE_FAILED", "Failed to create task", e, true);
            }
        };
    }

    /**
     * PUT /api/tasks - Update an existing task
     */
    public RequestHandler updateTask() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String taskId = extractTaskId(request);
                if (taskId == null) {
                    return ErrorResponse.of(400, "TASKS.TASK_ID_REQUIRED", "taskId is required", request, false);
                }

                JsonNode body = parseRequestBody(request);

                // Build task update payload
                ObjectNode taskPayload = objectMapper.createObjectNode();

                // Update only provided fields
                if (body.has("name")) {
                    taskPayload.set("name", body.get("name"));
                }
                if (body.has("assigneeIds")) {
                    taskPayload.set("assigneeIds", body.get("assigneeIds"));
                }
                if (body.has("estimate")) {
                    taskPayload.set("estimate", body.get("estimate"));
                }
                if (body.has("status")) {
                    taskPayload.set("status", body.get("status"));
                }
                if (body.has("archived")) {
                    taskPayload.set("archived", body.get("archived"));
                }

                // Use openapiCall for PUT to /workspaces/{workspaceId}/tasks/{taskId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.PUT,
                    "/workspaces/" + workspaceId + "/tasks/" + taskId,
                    taskPayload.toString()
                );

                // Refresh workspace cache to reflect updated task
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TASKS.UPDATE_FAILED", "Failed to update task", e, true);
            }
        };
    }

    /**
     * DELETE /api/tasks - Delete a task by id
     */
    public RequestHandler deleteTask() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String taskId = extractTaskId(request);
                if (taskId == null) {
                    return ErrorResponse.of(400, "TASKS.TASK_ID_REQUIRED", "taskId is required", request, false);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                // Use openapiCall for DELETE to /workspaces/{workspaceId}/tasks/{taskId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.DELETE,
                    "/workspaces/" + workspaceId + "/tasks/" + taskId,
                    null
                );

                // Refresh workspace cache to reflect deleted task
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                ObjectNode result = objectMapper.createObjectNode();
                result.put("deleted", true);
                result.put("taskId", taskId);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TASKS.DELETE_FAILED", "Failed to delete task", e, true);
            }
        };
    }

    /**
     * POST /api/tasks/bulk - Bulk operations on tasks
     * Supports bulk create, update, and delete operations
     */
    public RequestHandler bulkTasks() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                JsonNode body = parseRequestBody(request);

                // Validate required fields
                if (!body.has("operations") || !body.get("operations").isArray()) {
                    return ErrorResponse.of(400, "TASKS.BULK_OPERATIONS_REQUIRED", "operations array is required", request, false);
                }

                ObjectNode result = objectMapper.createObjectNode();
                var operationsArray = objectMapper.createArrayNode();

                // Process each operation
                for (JsonNode operation : body.get("operations")) {
                    if (!operation.has("type") || !operation.has("data")) {
                        continue; // Skip invalid operations
                    }

                    String type = operation.get("type").asText();
                    JsonNode data = operation.get("data");

                    ObjectNode operationResult = objectMapper.createObjectNode();
                    operationResult.put("type", type);

                    try {
                        switch (type) {
                            case "create":
                                // Create task
                                if (!data.has("name") || data.get("name").asText().isBlank()) {
                                    operationResult.put("status", "error");
                                    operationResult.put("error", "Task name is required");
                                } else {
                                    String projectId = data.has("projectId") ? data.get("projectId").asText() : null;
                                    if (projectId == null) {
                                        operationResult.put("status", "error");
                                        operationResult.put("error", "projectId is required for task creation");
                                    } else {
                                        ObjectNode taskPayload = objectMapper.createObjectNode();
                                        taskPayload.set("name", data.get("name"));

                                        if (data.has("assigneeIds")) {
                                            taskPayload.set("assigneeIds", data.get("assigneeIds"));
                                        }
                                        if (data.has("estimate")) {
                                            taskPayload.set("estimate", data.get("estimate"));
                                        }
                                        if (data.has("status")) {
                                            taskPayload.set("status", data.get("status"));
                                        }

                                        var response = workspaceClient.get().openapiCall(
                                            OpenApiCallConfig.HttpMethod.POST,
                                            "/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks",
                                            taskPayload.toString()
                                        );

                                        JsonNode createdTask = objectMapper.readTree(response.body());
                                        operationResult.put("status", "success");
                                        operationResult.set("result", createdTask);
                                    }
                                }
                                break;

                            case "update":
                                // Update task
                                if (!data.has("id")) {
                                    operationResult.put("status", "error");
                                    operationResult.put("error", "Task id is required for update");
                                } else {
                                    String taskId = data.get("id").asText();
                                    ObjectNode taskPayload = objectMapper.createObjectNode();

                                    if (data.has("name")) {
                                        taskPayload.set("name", data.get("name"));
                                    }
                                    if (data.has("assigneeIds")) {
                                        taskPayload.set("assigneeIds", data.get("assigneeIds"));
                                    }
                                    if (data.has("estimate")) {
                                        taskPayload.set("estimate", data.get("estimate"));
                                    }
                                    if (data.has("status")) {
                                        taskPayload.set("status", data.get("status"));
                                    }
                                    if (data.has("archived")) {
                                        taskPayload.set("archived", data.get("archived"));
                                    }

                                        var response = workspaceClient.get().openapiCall(
                                        OpenApiCallConfig.HttpMethod.PUT,
                                        "/workspaces/" + workspaceId + "/tasks/" + taskId,
                                        taskPayload.toString()
                                    );

                                    JsonNode updatedTask = objectMapper.readTree(response.body());
                                    operationResult.put("status", "success");
                                    operationResult.set("result", updatedTask);
                                }
                                break;

                            case "delete":
                                // Delete task
                                if (!data.has("id")) {
                                    operationResult.put("status", "error");
                                    operationResult.put("error", "Task id is required for deletion");
                                } else {
                                    String taskId = data.get("id").asText();
                                    workspaceClient.get().openapiCall(
                                        OpenApiCallConfig.HttpMethod.DELETE,
                                        "/workspaces/" + workspaceId + "/tasks/" + taskId,
                                        null
                                    );

                                    operationResult.put("status", "success");
                                    operationResult.put("taskId", taskId);
                                }
                                break;

                            default:
                                operationResult.put("status", "error");
                                operationResult.put("error", "Unknown operation type: " + type);
                                break;
                        }
                    } catch (Exception e) {
                        operationResult.put("status", "error");
                        operationResult.put("error", e.getMessage());
                    }

                    operationsArray.add(operationResult);
                }

                // Refresh workspace cache after bulk operations
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                result.set("operations", operationsArray);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "TASKS.BULK_FAILED", "Failed to process bulk operations", e, true);
            }
        };
    }

    private String getProjectId(HttpServletRequest request) {
        // Try to get from query parameter
        String projectId = request.getParameter("projectId");
        if (projectId != null && !projectId.trim().isEmpty()) {
            return projectId.trim();
        }

        // Try JSON body if available
        Object cachedJson = request.getAttribute("clockify.jsonBody");
        if (cachedJson instanceof JsonNode json && json.hasNonNull("projectId")) {
            String id = json.get("projectId").asText("");
            if (!id.isBlank()) return id;
        }

        return null;
    }

    private String extractTaskId(HttpServletRequest request) throws Exception {
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
            if (segments.length >= 4 && "api".equals(segments[1]) && "tasks".equals(segments[2])) {
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
        return ErrorResponse.of(400, "TASKS.WORKSPACE_REQUIRED", hint, request, false);
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
