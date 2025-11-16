package com.example.rules.api.explorer;

import com.clockify.addon.sdk.security.TokenStore;
import com.example.rules.ClockifyClient;
import com.example.rules.ClockifyClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Aggregates workspace data into explorer-friendly payloads and exposes collection helpers.
 */
public class WorkspaceExplorerService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExplorerGateway gateway;
    private final Supplier<OffsetDateTime> clock;

    public WorkspaceExplorerService() {
        this(new ClockifyExplorerGateway(ClockifyClient::new));
    }

    public WorkspaceExplorerService(ExplorerGateway gateway) {
        this(gateway, () -> OffsetDateTime.now(ZoneOffset.UTC));
    }

    WorkspaceExplorerService(ExplorerGateway gateway, Supplier<OffsetDateTime> clock) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.clock = Objects.requireNonNull(clock, "clock");
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

        ExplorerQuery policiesQuery = new ExplorerQuery(1, effective.sampleSize(), Map.of("view", "policies"));
        ClockifyClient.PageResult timeOffPolicies = gateway.fetch(workspaceId, ExplorerDataset.TIME_OFF, policiesQuery);
        addSample("timeOffPolicies", timeOffPolicies, summary, samples);

        Map<String, String> timeFilters = new LinkedHashMap<>();
        OffsetDateTime now = clock.get();
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

    public ObjectNode getTasks(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery effective = query == null ? new ExplorerQuery(1, 25, Map.of()) : query;
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.TASKS, effective));
    }

    public ObjectNode getTimeOff(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery effective = query == null ? new ExplorerQuery(1, 25, Map.of()) : query;
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.TIME_OFF, effective));
    }

    public ObjectNode getWebhooks(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery effective = query == null ? new ExplorerQuery(1, 25, Map.of()) : query;
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.WEBHOOKS, effective));
    }

    public ObjectNode getCustomFields(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery effective = query == null ? new ExplorerQuery(1, 25, Map.of()) : query;
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.CUSTOM_FIELDS, effective));
    }

    public ObjectNode getInvoices(String workspaceId, ExplorerQuery query) throws ExplorerException {
        ExplorerQuery effective = query == null ? new ExplorerQuery(1, 25, Map.of()) : query;
        return toResponse(gateway.fetch(workspaceId, ExplorerDataset.INVOICES, effective));
    }

    public ObjectNode getSnapshot(String workspaceId, SnapshotRequest request) throws ExplorerException {
        SnapshotRequest effective = request == null ? SnapshotRequest.defaults() : request;
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode summary = MAPPER.createObjectNode();
        ObjectNode datasets = MAPPER.createObjectNode();
        root.set("summary", summary);
        root.set("datasets", datasets);

        Map<String, String> timeEntryFilters = new LinkedHashMap<>();
        OffsetDateTime now = clock.get();
        timeEntryFilters.put("end", now.toString());
        timeEntryFilters.put("start", now.minusDays(effective.timeEntryLookbackDays()).toString());
        Map<String, String> timeOffFilters = Map.of("view", "policies", "status", "ACTIVE");

        if (effective.includeUsers()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.USERS, Map.of(), effective);
            summary.set("users", dataset.summary());
            datasets.set("users", dataset.items());
        }
        if (effective.includeProjects()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.PROJECTS, Map.of(), effective);
            summary.set("projects", dataset.summary());
            datasets.set("projects", dataset.items());
        }
        if (effective.includeClients()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.CLIENTS, Map.of(), effective);
            summary.set("clients", dataset.summary());
            datasets.set("clients", dataset.items());
        }
        if (effective.includeTags()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.TAGS, Map.of(), effective);
            summary.set("tags", dataset.summary());
            datasets.set("tags", dataset.items());
        }
        if (effective.includeTasks()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.TASKS, Map.of(), effective);
            summary.set("tasks", dataset.summary());
            datasets.set("tasks", dataset.items());
        }
        if (effective.includeTimeEntries()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.TIME_ENTRIES, timeEntryFilters, effective);
            summary.set("timeEntries", dataset.summary());
            datasets.set("timeEntries", dataset.items());
        }
        if (effective.includeTimeOff()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.TIME_OFF, timeOffFilters, effective);
            summary.set("timeOff", dataset.summary());
            datasets.set("timeOff", dataset.items());
        }
        if (effective.includeWebhooks()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.WEBHOOKS, Map.of(), effective);
            summary.set("webhooks", dataset.summary());
            datasets.set("webhooks", dataset.items());
        }
        if (effective.includeCustomFields()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.CUSTOM_FIELDS, Map.of(), effective);
            summary.set("customFields", dataset.summary());
            datasets.set("customFields", dataset.items());
        }
        if (effective.includeInvoices()) {
            SnapshotDataset dataset = collectDataset(workspaceId, ExplorerDataset.INVOICES, Map.of(), effective);
            summary.set("invoices", dataset.summary());
            datasets.set("invoices", dataset.items());
        }
        root.put("workspaceId", workspaceId);
        return root;
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

    private SnapshotDataset collectDataset(String workspaceId,
                                           ExplorerDataset dataset,
                                           Map<String, String> filters,
                                           SnapshotRequest request) throws ExplorerException {
        int page = 1;
        int pagesFetched = 0;
        boolean hadMore = false;
        long totalItems = -1;
        ArrayNode aggregated = MAPPER.createArrayNode();
        Map<String, String> baseFilters = filters == null ? Map.of() : filters;

        while (pagesFetched < request.maxPagesPerDataset()) {
            ExplorerQuery query = new ExplorerQuery(page, request.pageSizePerDataset(), baseFilters);
            ExplorerQuery effectiveQuery = dataset == ExplorerDataset.TIME_ENTRIES ? ensureHydrated(query) : query;
            ClockifyClient.PageResult pageResult = gateway.fetch(workspaceId, dataset, effectiveQuery);
            aggregated.addAll(pageResult.items());
            pagesFetched++;
            if (pageResult.pagination().totalItems() >= 0) {
                totalItems = pageResult.pagination().totalItems();
            }
            if (!pageResult.pagination().hasMore()) {
                hadMore = false;
                break;
            }
            hadMore = true;
            int nextPage = pageResult.pagination().nextPage();
            if (nextPage <= page) {
                break;
            }
            page = nextPage;
        }

        ObjectNode stats = MAPPER.createObjectNode();
        stats.put("items", aggregated.size());
        stats.put("pages", pagesFetched);
        stats.put("hadMore", hadMore);
        if (totalItems >= 0) {
            stats.put("total", totalItems);
        }
        return new SnapshotDataset(aggregated, stats);
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
        TASKS,
        TIME_ENTRIES,
        TIME_OFF,
        WEBHOOKS,
        CUSTOM_FIELDS,
        INVOICES
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
                    412,
                    "EXPLORER.TOKEN_NOT_FOUND",
                    "Workspace installation token not found for " + workspaceId,
                    false
            );
        }
    }

    public static class ClockifyExplorerGateway implements ExplorerGateway {
        private static final int PROJECT_PAGE_SIZE = 50;
        private static final int TASK_PAGE_SIZE = 200;
        private static final int MAX_TASK_SCAN = 5000;
        private final ClockifyClientFactory clientFactory;

        public ClockifyExplorerGateway() {
            this(ClockifyClient::new);
        }

        ClockifyExplorerGateway(ClockifyClientFactory clientFactory) {
            this.clientFactory = clientFactory;
        }

        @Override
        public ClockifyClient.PageResult fetch(String workspaceId, ExplorerDataset dataset, ExplorerQuery query) throws ExplorerException {
            ClockifyClient client = resolve(workspaceId);
            try {
                return switch (dataset) {
                    case USERS -> client.getUsersPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case PROJECTS -> client.getProjectsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case CLIENTS -> client.getClientsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case TAGS -> client.getTagsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case TASKS -> fetchTasks(client, workspaceId, query);
                    case TIME_ENTRIES -> client.getTimeEntriesPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case TIME_OFF -> fetchTimeOff(client, workspaceId, query);
                    case WEBHOOKS -> client.getWebhooksPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case CUSTOM_FIELDS -> client.getCustomFieldsPage(workspaceId, query.filters(), query.page(), query.pageSize());
                    case INVOICES -> client.getInvoicesPage(workspaceId, query.filters(), query.page(), query.pageSize());
                };
            } catch (ExplorerException e) {
                throw e;
            } catch (Exception e) {
                throw new ExplorerException(502, "EXPLORER.CLOCKIFY_ERROR", "Clockify API request failed", true, e);
            }
        }

        private ClockifyClient.PageResult fetchTasks(ClockifyClient client,
                                                     String workspaceId,
                                                     ExplorerQuery query) throws Exception {
            Map<String, String> filters = new LinkedHashMap<>(query.filters());
            String singleProject = cleanedId(filters.get("projectId"));
            if (singleProject != null && !singleProject.isBlank()) {
                return fetchProjectTasks(client, workspaceId, singleProject, filters, query.page(), query.pageSize());
            }
            return fetchWorkspaceTasks(client, workspaceId, filters, query.page(), query.pageSize());
        }

        private ClockifyClient.PageResult fetchTimeOff(ClockifyClient client,
                                                       String workspaceId,
                                                       ExplorerQuery query) throws ExplorerException, Exception {
            Map<String, String> filters = new LinkedHashMap<>(query.filters());
            String rawView = filters.getOrDefault("view", "requests");
            String view = rawView == null ? "requests" : rawView.trim().toLowerCase();
            filters.remove("view");
            return switch (view) {
                case "policies" -> client.getTimeOffPoliciesPage(workspaceId, filters, query.page(), query.pageSize());
                case "balances" -> {
                    String policyId = filters.get("policyId");
                    String userId = filters.get("userId");
                    if ((policyId == null || policyId.isBlank()) && (userId == null || userId.isBlank())) {
                        throw new ExplorerException(
                                400,
                                "EXPLORER.TIME_OFF_BALANCE_FILTER_REQUIRED",
                                "policyId or userId is required when view=balances",
                                false
                        );
                    }
                    yield client.getTimeOffBalancesPage(workspaceId, policyId, userId, filters, query.page(), query.pageSize());
                }
                case "requests", "" -> client.getTimeOffRequestsPage(workspaceId, filters, query.page(), query.pageSize());
                default -> throw new ExplorerException(
                        400,
                        "EXPLORER.TIME_OFF_VIEW_UNSUPPORTED",
                        "Unknown time off view: " + view,
                        false
                );
            };
        }

        private ClockifyClient resolve(String workspaceId) throws ExplorerException {
            var tokenOpt = TokenStore.get(workspaceId);
            if (tokenOpt.isEmpty()) {
                throw ExplorerException.tokenMissing(workspaceId);
            }
            var token = tokenOpt.get();
            return clientFactory.create(token.apiBaseUrl(), token.token());
        }

        private ClockifyClient.PageResult fetchProjectTasks(ClockifyClient client,
                                                            String workspaceId,
                                                            String projectId,
                                                            Map<String, String> filters,
                                                            int page,
                                                            int pageSize) throws Exception {
            String search = normalize(filters.get("search"));
            Map<String, String> taskQuery = taskQuery(filters);
            ClockifyClient.PageResult result = client.getTasksPage(workspaceId, projectId, taskQuery, page, pageSize);
            if (search == null) {
                return result;
            }
            ArrayNode filtered = MAPPER.createArrayNode();
            for (JsonNode node : result.items()) {
                if (matchesSearch(node, search)) {
                    filtered.add(node);
                }
            }
            ClockifyClient.Pagination pagination = new ClockifyClient.Pagination(
                    result.pagination().page(),
                    result.pagination().pageSize(),
                    result.pagination().hasMore(),
                    result.pagination().nextPage(),
                    result.pagination().totalItems()
            );
            return new ClockifyClient.PageResult(filtered, pagination);
        }

        private ClockifyClient.PageResult fetchWorkspaceTasks(ClockifyClient client,
                                                              String workspaceId,
                                                              Map<String, String> filters,
                                                              int page,
                                                              int pageSize) throws Exception {
            int offset = Math.max(0, (page - 1) * pageSize);
            int skipped = 0;
            int produced = 0;
            int scanned = 0;
            ArrayNode items = MAPPER.createArrayNode();
            String search = normalize(filters.get("search"));
            Map<String, String> taskQuery = taskQuery(filters);

            Map<String, String> projectFilters = new LinkedHashMap<>();
            String archived = normalize(filters.get("archived"));
            if ("all".equals(archived)) {
                // do not constrain archived flag
            } else if ("true".equals(archived) || "false".equals(archived)) {
                projectFilters.put("archived", archived);
            } else {
                projectFilters.put("archived", "false");
            }
            String clientFilter = cleanedId(filters.get("clientId"));
            if (clientFilter != null) {
                projectFilters.put("clients", clientFilter);
            }

            int projectPage = 1;
            boolean moreData = false;

            while (scanned < MAX_TASK_SCAN && produced < pageSize) {
                ClockifyClient.PageResult projects = client.getProjectsPage(
                        workspaceId,
                        projectFilters,
                        projectPage,
                        PROJECT_PAGE_SIZE
                );
                ArrayNode projectItems = projects.items();
                if (projectItems.isEmpty()) {
                    break;
                }
                for (JsonNode project : projectItems) {
                    if (!project.hasNonNull("id")) {
                        continue;
                    }
                    String projectId = project.get("id").asText();
                    String projectName = project.path("name").asText("");
                    int taskPage = 1;
                    boolean projectHasMore = true;
                    while (projectHasMore && scanned < MAX_TASK_SCAN && produced < pageSize) {
                        ClockifyClient.PageResult taskPageResult = client.getTasksPage(
                                workspaceId,
                                projectId,
                                taskQuery,
                                taskPage,
                                TASK_PAGE_SIZE
                        );
                        ArrayNode tasks = taskPageResult.items();
                        if (tasks.isEmpty()) {
                            break;
                        }
                        for (JsonNode task : tasks) {
                            scanned++;
                            if (scanned >= MAX_TASK_SCAN) {
                                moreData = true;
                                break;
                            }
                            if (search != null && !matchesSearch(task, search)) {
                                continue;
                            }
                            if (skipped < offset) {
                                skipped++;
                                continue;
                            }
                            if (produced < pageSize) {
                                ObjectNode enriched;
                                if (task.isObject()) {
                                    enriched = (ObjectNode) task.deepCopy();
                                } else {
                                    enriched = MAPPER.createObjectNode();
                                    enriched.set("value", task);
                                }
                                enriched.put("projectId", projectId);
                                enriched.put("projectName", projectName);
                                items.add(enriched);
                                produced++;
                            } else {
                                moreData = true;
                                break;
                            }
                        }
                        if (produced >= pageSize || moreData) {
                            break;
                        }
                        projectHasMore = taskPageResult.pagination().hasMore();
                        if (projectHasMore) {
                            taskPage = taskPageResult.pagination().nextPage();
                        }
                    }
                    if (produced >= pageSize || moreData) {
                        break;
                    }
                }
                if (produced >= pageSize || moreData) {
                    break;
                }
                if (projects.pagination().hasMore()) {
                    projectPage = projects.pagination().nextPage();
                } else {
                    break;
                }
            }

            ClockifyClient.Pagination pagination = new ClockifyClient.Pagination(
                    page,
                    pageSize,
                    moreData,
                    moreData ? page + 1 : page,
                    -1
            );
            return new ClockifyClient.PageResult(items, pagination);
        }

        private Map<String, String> taskQuery(Map<String, String> filters) {
            if (filters == null || filters.isEmpty()) {
                return Map.of();
            }
            String archived = normalize(filters.get("archived"));
            if ("true".equals(archived) || "false".equals(archived)) {
                return Map.of("archived", archived);
            }
            return Map.of();
        }

        private String normalize(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return trimmed.toLowerCase(Locale.ROOT);
        }

        private boolean matchesSearch(JsonNode node, String search) {
            if (search == null) {
                return true;
            }
            String name = node.path("name").asText("");
            return name.toLowerCase(Locale.ROOT).contains(search);
        }

        private String cleanedId(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
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

    public record SnapshotRequest(
            boolean includeUsers,
            boolean includeProjects,
            boolean includeClients,
            boolean includeTags,
            boolean includeTasks,
            boolean includeTimeEntries,
            boolean includeTimeOff,
            boolean includeWebhooks,
            boolean includeCustomFields,
            boolean includeInvoices,
            int pageSizePerDataset,
            int maxPagesPerDataset,
            int timeEntryLookbackDays
    ) {
        public SnapshotRequest {
            int normalizedPageSize = Math.min(Math.max(pageSizePerDataset, 5), 100);
            int normalizedPages = Math.min(Math.max(maxPagesPerDataset, 1), 20);
            int normalizedLookback = Math.min(Math.max(timeEntryLookbackDays, 1), 90);
            pageSizePerDataset = normalizedPageSize;
            maxPagesPerDataset = normalizedPages;
            timeEntryLookbackDays = normalizedLookback;
        }

        public static SnapshotRequest defaults() {
            return new SnapshotRequest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
                    false,
                    25,
                    3,
                    30
            );
        }
    }

    private record SnapshotDataset(ArrayNode items, ObjectNode summary) {
    }
}
