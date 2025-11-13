package com.example.rules.api.explorer;

import com.clockify.addon.sdk.security.TokenStore;
import com.example.rules.ClockifyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates workspace data into explorer-friendly payloads and exposes collection helpers.
 */
public class WorkspaceExplorerService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExplorerGateway gateway;

    public WorkspaceExplorerService() {
        this(new ClockifyExplorerGateway());
    }

    public WorkspaceExplorerService(ExplorerGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public ObjectNode getOverview(String workspaceId, OverviewRequest request) throws ExplorerException {
        OverviewRequest effective = request == null ? new OverviewRequest(5, 7) : request;
        ExplorerQuery sampleQuery = new ExplorerQuery(1, effective.sampleSize(), Map.of());

        ClockifyClient.PageResult users = gateway.fetch(workspaceId, ExplorerDataset.USERS, sampleQuery);
        ClockifyClient.PageResult projects = gateway.fetch(workspaceId, ExplorerDataset.PROJECTS, sampleQuery);
        ClockifyClient.PageResult clients = gateway.fetch(workspaceId, ExplorerDataset.CLIENTS, sampleQuery);
        ClockifyClient.PageResult tags = gateway.fetch(workspaceId, ExplorerDataset.TAGS, sampleQuery);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("workspaceId", workspaceId);

        ObjectNode summary = MAPPER.createObjectNode();
        ObjectNode samples = MAPPER.createObjectNode();
        root.set("summary", summary);
        root.set("samples", samples);

        addSample("users", users, summary, samples);
        addSample("projects", projects, summary, samples);
        addSample("clients", clients, summary, samples);
        addSample("tags", tags, summary, samples);

        Map<String, String> timeFilters = new LinkedHashMap<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        timeFilters.put("end", now.toString());
        timeFilters.put("start", now.minusDays(effective.recentDays()).toString());
        ExplorerQuery timeEntriesQuery = ensureHydrated(new ExplorerQuery(1, effective.sampleSize(), timeFilters));
        ClockifyClient.PageResult timeEntries = gateway.fetch(workspaceId, ExplorerDataset.TIME_ENTRIES, timeEntriesQuery);
        root.set("recentTimeEntries", toResponse(timeEntries));

        return root;
    }

    public ObjectNode getUsers(String workspaceId, ExplorerQuery query) throws ExplorerException {
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.USERS, query));
    }

    public ObjectNode getProjects(String workspaceId, ExplorerQuery query) throws ExplorerException {
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.PROJECTS, query));
    }

    public ObjectNode getClients(String workspaceId, ExplorerQuery query) throws ExplorerException {
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.CLIENTS, query));
    }

    public ObjectNode getTags(String workspaceId, ExplorerQuery query) throws ExplorerException {
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.TAGS, query));
    }

    public ObjectNode getTimeEntries(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery hydrated = ensureHydrated(query);
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.TIME_ENTRIES, hydrated));
    }

    private ExplorerQuery ensureHydrated(ExplorerQuery query) {
        if (query.filters().containsKey("hydrated")) {
            return query;
        }
        Map<String, String> filters = new LinkedHashMap<>(query.filters());
        filters.put("hydrated", "true");
        return new ExplorerQuery(query.page(), query.pageSize(), filters);
    }

    private void addSample(String key,
                           ClockifyClient.PageResult page,
                           ObjectNode summaryNode,
                           ObjectNode samplesNode) {
        summaryNode.set(key, summaryFor(page));
        samplesNode.set(key, toResponse(page));
    }

    private ObjectNode toResponse(ClockifyClient.PageResult pageResult) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("items", pageResult.items());
        node.set("pagination", paginationNode(pageResult.pagination()));
        return node;
    }

    private ObjectNode paginationNode(ClockifyClient.Pagination pagination) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("page", pagination.page());
        node.put("pageSize", pagination.pageSize());
        node.put("hasMore", pagination.hasMore());
        node.put("nextPage", pagination.nextPage());
        if (pagination.totalItems() >= 0) {
            node.put("totalItems", pagination.totalItems());
        }
        return node;
    }

    private ObjectNode summaryFor(ClockifyClient.PageResult pageResult) {
        ObjectNode node = MAPPER.createObjectNode();
        long total = pageResult.pagination().totalItems();
        node.put("total", total >= 0 ? total : pageResult.items().size());
        node.put("page", pageResult.pagination().page());
        node.put("pageSize", pageResult.pagination().pageSize());
        node.put("hasMore", pageResult.pagination().hasMore());
        node.put("sampleSize", pageResult.items().size());
        return node;
    }

    public interface ExplorerGateway {
        ClockifyClient.PageResult fetch(String workspaceId, ExplorerDataset dataset, ExplorerQuery query) throws ExplorerException;
    }

    public enum ExplorerDataset {
        USERS,
        PROJECTS,
        CLIENTS,
        TAGS,
        TIME_ENTRIES
    }

    public static class ExplorerException extends Exception {
        private final int status;
        private final String code;
        private final boolean retryable;
        private final String details;

        public ExplorerException(int status, String code, String message, boolean retryable) {
            this(status, code, message, retryable, null, null);
        }

        public ExplorerException(int status, String code, String message, boolean retryable, Throwable cause) {
            this(status, code, message, retryable, cause != null ? cause.getMessage() : null, cause);
        }

        private ExplorerException(int status, String code, String message, boolean retryable, String details, Throwable cause) {
            super(message, cause);
            this.status = status;
            this.code = code;
            this.retryable = retryable;
            this.details = details;
        }

        public int status() {
            return status;
        }

        public String code() {
            return code;
        }

        public boolean retryable() {
            return retryable;
        }

        public String details() {
            return details;
        }

        public static ExplorerException tokenMissing(String workspaceId) {
            return new ExplorerException(
                    404,
                    "EXPLORER.TOKEN_NOT_FOUND",
                    "Workspace installation token not found for " + workspaceId,
                    false
            );
        }
    }

    public static class ClockifyExplorerGateway implements ExplorerGateway {
        @Override
        public ClockifyClient.PageResult fetch(String workspaceId, ExplorerDataset dataset, ExplorerQuery query) throws ExplorerException {
            ClockifyClient client = resolve(workspaceId);
            try {
                return switch (dataset) {
                    case USERS -> client.getUsersPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case PROJECTS -> client.getProjectsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case CLIENTS -> client.getClientsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case TAGS -> client.getTagsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case TIME_ENTRIES -> client.getTimeEntriesPage(workspaceId, query.filters(), query.page(), query.pageSize());
                };
            } catch (ExplorerException e) {
                throw e;
            } catch (Exception e) {
                throw new ExplorerException(502, "EXPLORER.CLOCKIFY_ERROR", "Clockify API request failed", true, e);
            }
        }

        private ClockifyClient resolve(String workspaceId) throws ExplorerException {
            var tokenOpt = TokenStore.get(workspaceId);
            if (tokenOpt.isEmpty()) {
                throw ExplorerException.tokenMissing(workspaceId);
            }
            var token = tokenOpt.get();
            return new ClockifyClient(token.apiBaseUrl(), token.token());
        }
    }

    public record ExplorerQuery(int page, int pageSize, Map<String, String> filters) {
        public ExplorerQuery {
            if (page < 1) {
                throw new IllegalArgumentException("page must be >= 1");
            }
            if (pageSize < 1) {
                throw new IllegalArgumentException("pageSize must be >= 1");
            }
            filters = filters == null ? Map.of() : Map.copyOf(filters);
        }
    }

    public record OverviewRequest(int sampleSize, int recentDays) {
        public OverviewRequest {
            int normalizedSample = sampleSize <= 0 ? 5 : Math.min(sampleSize, 50);
            int normalizedDays = recentDays <= 0 ? 7 : Math.min(recentDays, 90);
            sampleSize = normalizedSample;
            recentDays = normalizedDays;
        }
    }
}
