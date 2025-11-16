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
 * CRUD controller for Clients management.
 * Provides endpoints for creating, reading, updating, and deleting clients.
 */
public class ClientsController {

    private static final Logger logger = LoggerFactory.getLogger(ClientsController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MEDIA_JSON = "application/json";

    private final ClockifyClientFactory clientFactory;

    public ClientsController(ClockifyClientFactory clientFactory) {
        this.clientFactory = clientFactory == null ? ClockifyClient::new : clientFactory;
    }

    public ClientsController() {
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
     * GET /api/clients - List all clients for a workspace
     */
    public RequestHandler listClients() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                boolean archived = Boolean.parseBoolean(request.getParameter("archived"));
                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }
                JsonNode clients = workspaceClient.get().getClients(workspaceId, archived);
                return HttpResponse.ok(clients.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "CLIENTS.LIST_FAILED", "Failed to list clients", e, true);
            }
        };
    }

    /**
     * POST /api/clients - Create a new client
     */
    public RequestHandler createClient() {
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
                    return ErrorResponse.of(400, "CLIENTS.NAME_REQUIRED", "Client name is required", request, false);
                }

                // Build client creation payload
                ObjectNode clientPayload = objectMapper.createObjectNode();
                clientPayload.set("name", body.get("name"));

                // Optional fields
                if (body.has("email")) {
                    clientPayload.set("email", body.get("email"));
                }
                if (body.has("address")) {
                    clientPayload.set("address", body.get("address"));
                }
                if (body.has("note")) {
                    clientPayload.set("note", body.get("note"));
                }

                // Use openapiCall for POST to /workspaces/{workspaceId}/clients
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.POST,
                    "/workspaces/" + workspaceId + "/clients",
                    clientPayload.toString()
                );

                // Refresh workspace cache to reflect new client
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "CLIENTS.CREATE_FAILED", "Failed to create client", e, true);
            }
        };
    }

    /**
     * PUT /api/clients - Update an existing client
     */
    public RequestHandler updateClient() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String clientId = extractClientId(request);
                if (clientId == null) {
                    return ErrorResponse.of(400, "CLIENTS.CLIENT_ID_REQUIRED", "clientId is required", request, false);
                }

                JsonNode body = parseRequestBody(request);

                // Build client update payload
                ObjectNode clientPayload = objectMapper.createObjectNode();

                // Update only provided fields
                if (body.has("name")) {
                    clientPayload.set("name", body.get("name"));
                }
                if (body.has("email")) {
                    clientPayload.set("email", body.get("email"));
                }
                if (body.has("address")) {
                    clientPayload.set("address", body.get("address"));
                }
                if (body.has("note")) {
                    clientPayload.set("note", body.get("note"));
                }
                if (body.has("archived")) {
                    clientPayload.set("archived", body.get("archived"));
                }

                // Use openapiCall for PUT to /workspaces/{workspaceId}/clients/{clientId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.PUT,
                    "/workspaces/" + workspaceId + "/clients/" + clientId,
                    clientPayload.toString()
                );

                // Refresh workspace cache to reflect updated client
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                return HttpResponse.ok(response.body(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "CLIENTS.UPDATE_FAILED", "Failed to update client", e, true);
            }
        };
    }

    /**
     * DELETE /api/clients - Delete a client by id
     */
    public RequestHandler deleteClient() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                String clientId = extractClientId(request);
                if (clientId == null) {
                    return ErrorResponse.of(400, "CLIENTS.CLIENT_ID_REQUIRED", "clientId is required", request, false);
                }

                Optional<ClockifyClient> workspaceClient = getWorkspaceClockifyClient(workspaceId);
                if (workspaceClient.isEmpty()) {
                    return tokenMissing(request);
                }

                // Use openapiCall for DELETE to /workspaces/{workspaceId}/clients/{clientId}
                var response = workspaceClient.get().openapiCall(
                    OpenApiCallConfig.HttpMethod.DELETE,
                    "/workspaces/" + workspaceId + "/clients/" + clientId,
                    null
                );

                // Refresh workspace cache to reflect deleted client
                com.clockify.addon.sdk.security.TokenStore.get(workspaceId)
                        .ifPresent(token -> WorkspaceCache.refreshAsync(workspaceId, token.apiBaseUrl(), token.token()));

                ObjectNode result = objectMapper.createObjectNode();
                result.put("deleted", true);
                result.put("clientId", clientId);
                return HttpResponse.ok(result.toString(), MEDIA_JSON);

            } catch (Exception e) {
                return internalError(request, "CLIENTS.DELETE_FAILED", "Failed to delete client", e, true);
            }
        };
    }

    private String extractClientId(HttpServletRequest request) throws Exception {
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
            if (segments.length >= 4 && "api".equals(segments[1]) && "clients".equals(segments[2])) {
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
        return ErrorResponse.of(400, "CLIENTS.WORKSPACE_REQUIRED", hint, request, false);
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
