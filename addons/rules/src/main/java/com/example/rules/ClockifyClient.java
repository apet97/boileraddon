package com.example.rules;

import com.clockify.addon.sdk.http.ClockifyHttpClient;
import com.example.rules.engine.OpenApiCallConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around the SDK ClockifyHttpClient for the Rules add-on use cases.
 */
public class ClockifyClient {
    private static final int PAGE_SIZE = 500;

    private final ClockifyHttpClient http;
    private final String token;
    private final ObjectMapper om = new ObjectMapper();

    public static final class PageResult {
        private final ArrayNode items;
        private final Pagination pagination;

        public PageResult(ArrayNode items, Pagination pagination) {
            this.items = items;
            this.pagination = pagination;
        }

        public ArrayNode items() {
            return items;
        }

        public Pagination pagination() {
            return pagination;
        }
    }

    public static final class Pagination {
        private final int page;
        private final int pageSize;
        private final boolean hasMore;
        private final int nextPage;
        private final long totalItems;

        public Pagination(int page, int pageSize, boolean hasMore, int nextPage, long totalItems) {
            this.page = page;
            this.pageSize = pageSize;
            this.hasMore = hasMore;
            this.nextPage = nextPage;
            this.totalItems = totalItems;
        }

        public int page() {
            return page;
        }

        public int pageSize() {
            return pageSize;
        }

        public boolean hasMore() {
            return hasMore;
        }

        public int nextPage() {
            return nextPage;
        }

        public long totalItems() {
            return totalItems;
        }
    }

    public ClockifyClient(String baseUrl, String addonToken) {
        this(new ClockifyHttpClient(baseUrl), addonToken);
    }

    ClockifyClient(ClockifyHttpClient httpClient, String addonToken) {
        this.http = httpClient;
        this.token = addonToken;
    }

    public HttpResponse<String> openapiCall(OpenApiCallConfig.HttpMethod method, String path, String jsonBody) throws Exception {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        String normalizedBody = (jsonBody == null || jsonBody.isBlank()) ? "{}" : jsonBody;
        return switch (method) {
            case GET -> http.get(path, token, Map.of());
            case POST -> http.postJsonWithIdempotency(path, token, normalizedBody, Map.of());
            case PUT -> http.putJson(path, token, normalizedBody, Map.of());
            case PATCH -> http.patchJson(path, token, normalizedBody, Map.of());
            case DELETE -> http.delete(path, token, Map.of());
        };
    }

    public JsonNode getTags(String workspaceId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/tags", token, Map.of());
        ensure2xx(resp, 200);
        return om.readTree(resp.body());
    }

    public JsonNode createTag(String workspaceId, String tagName) throws Exception {
        ObjectNode body = om.createObjectNode().put("name", tagName);
        HttpResponse<String> resp = http.postJsonWithIdempotency("/workspaces/" + workspaceId + "/tags", token, body.toString(), Map.of());
        ensure2xx(resp, 201);
        return om.readTree(resp.body());
    }

    public JsonNode getProjects(String workspaceId, boolean archived) throws Exception {
        return fetchPaginatedArray(
                "/workspaces/" + workspaceId + "/projects",
                Map.of("archived", String.valueOf(archived))
        );
    }

    public JsonNode getClients(String workspaceId, boolean archived) throws Exception {
        return fetchPaginatedArray(
                "/workspaces/" + workspaceId + "/clients",
                Map.of("archived", String.valueOf(archived))
        );
    }

    public JsonNode getUsers(String workspaceId) throws Exception {
        return fetchPaginatedArray("/workspaces/" + workspaceId + "/users");
    }

    public JsonNode getTasks(String workspaceId, String projectId) throws Exception {
        return fetchPaginatedArray("/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks");
    }

    public ObjectNode getTimeEntry(String workspaceId, String timeEntryId) throws Exception {
        HttpResponse<String> resp = http.get("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId, token, Map.of());
        ensure2xx(resp, 200);
        JsonNode n = om.readTree(resp.body());
        if (!(n instanceof ObjectNode)) throw new IllegalStateException("Time entry is not an object");
        return (ObjectNode) n;
    }

    public ObjectNode updateTimeEntry(String workspaceId, String timeEntryId, ObjectNode patch) throws Exception {
        // The update endpoint expects certain top-level fields (start/end). Build from GET payload.
        ObjectNode existing = getTimeEntry(workspaceId, timeEntryId);
        ObjectNode req = existing.deepCopy();

        // Move timeInterval.start/end to root if not present (compat with v1 behavior)
        if (!req.has("start") && existing.has("timeInterval") && existing.get("timeInterval").has("start")) {
            req.set("start", existing.get("timeInterval").get("start"));
        }
        if (!req.has("end") && existing.has("timeInterval") && existing.get("timeInterval").has("end")) {
            req.set("end", existing.get("timeInterval").get("end"));
        }

        // Apply provided patch keys onto req
        patch.fields().forEachRemaining(e -> req.set(e.getKey(), e.getValue()));

        HttpResponse<String> resp = http.putJson("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId,
                token, req.toString(), Map.of());
        ensure2xx(resp, 200);
        JsonNode out = om.readTree(resp.body());
        if (!(out instanceof ObjectNode)) throw new IllegalStateException("Update response is not an object");
        return (ObjectNode) out;
    }

    public PageResult getUsersPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/users", queryParams, page, pageSize);
    }

    public PageResult getProjectsPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/projects", queryParams, page, pageSize);
    }

    public PageResult getClientsPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/clients", queryParams, page, pageSize);
    }

    public PageResult getTagsPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/tags", queryParams, page, pageSize);
    }

    public PageResult getTasksPage(String workspaceId,
                                   String projectId,
                                   Map<String, String> queryParams,
                                   int page,
                                   int pageSize) throws Exception {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId is required to list tasks");
        }
        return fetchPage("/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks", queryParams, page, pageSize);
    }

    public PageResult getTimeEntriesPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/time-entries", queryParams, page, pageSize);
    }

    public PageResult getTimeOffPoliciesPage(String workspaceId,
                                             Map<String, String> queryParams,
                                             int page,
                                             int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/time-off/policies", queryParams, page, pageSize);
    }

    public PageResult getTimeOffRequestsPage(String workspaceId,
                                             Map<String, String> queryParams,
                                             int page,
                                             int pageSize) throws Exception {
        ObjectNode body = om.createObjectNode();
        body.put("page", page);
        body.put("pageSize", pageSize);
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        if (sanitized.containsKey("start")) {
            body.put("start", sanitized.get("start"));
        }
        if (sanitized.containsKey("end")) {
            body.put("end", sanitized.get("end"));
        }
        if (sanitized.containsKey("statuses")) {
            body.set("statuses", toArrayNode(splitCsv(sanitized.get("statuses"))));
        }
        if (sanitized.containsKey("users")) {
            body.set("users", toArrayNode(splitCsv(sanitized.get("users"))));
        }
        if (sanitized.containsKey("userGroups")) {
            body.set("userGroups", toArrayNode(splitCsv(sanitized.get("userGroups"))));
        }

        HttpResponse<String> resp = http.postJson(
                "/workspaces/" + workspaceId + "/time-off/requests",
                token,
                body.toString(),
                Map.of()
        );
        ensure2xx(resp, 200);

        JsonNode root = om.readTree(resp.body());
        ArrayNode items = root.path("requests").isArray()
                ? (ArrayNode) root.get("requests")
                : om.createArrayNode();
        long total = root.path("count").asLong(items.size());
        Pagination pagination = paginationFromTotal(total, page, pageSize, items.size());
        return new PageResult(items, pagination);
    }

    public PageResult getTimeOffBalancesPage(String workspaceId,
                                             String policyId,
                                             String userId,
                                             Map<String, String> queryParams,
                                             int page,
                                             int pageSize) throws Exception {
        if ((policyId == null || policyId.isBlank()) && (userId == null || userId.isBlank())) {
            throw new IllegalArgumentException("policyId or userId is required to fetch balances");
        }
        String basePath = policyId != null && !policyId.isBlank()
                ? "/workspaces/" + workspaceId + "/time-off/balance/policy/" + policyId
                : "/workspaces/" + workspaceId + "/time-off/balance/user/" + userId;
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        sanitized.remove("policyId");
        sanitized.remove("userId");

        String path = buildPaginatedPath(basePath, sanitized, page, pageSize);
        HttpResponse<String> resp = http.get(path, token, Map.of());
        ensure2xx(resp, 200);

        JsonNode root = om.readTree(resp.body());
        ArrayNode balances = root.path("balances").isArray()
                ? (ArrayNode) root.get("balances")
                : om.createArrayNode();
        long total = root.path("count").asLong(balances.size());
        Pagination pagination = paginationFromTotal(total, page, pageSize, balances.size());
        return new PageResult(balances, pagination);
    }

    public PageResult getWebhooksPage(String workspaceId,
                                      Map<String, String> queryParams,
                                      int page,
                                      int pageSize) throws Exception {
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        Map<String, String> requestQuery = new LinkedHashMap<>();
        if (sanitized.containsKey("type")) {
            requestQuery.put("type", sanitized.get("type"));
        }
        String path = appendQueryParams("/workspaces/" + workspaceId + "/webhooks", requestQuery);
        HttpResponse<String> resp = http.get(path, token, Map.of());
        ensure2xx(resp, 200);

        JsonNode root = om.readTree(resp.body());
        ArrayNode raw = root.path("webhooks").isArray()
                ? (ArrayNode) root.get("webhooks")
                : om.createArrayNode();
        ArrayNode filtered = filterWebhooks(raw, sanitized);
        boolean clientFiltered = sanitized.containsKey("event")
                || sanitized.containsKey("enabled")
                || sanitized.containsKey("search");
        long totalFromApi = root.has("workspaceWebhookCount")
                ? root.get("workspaceWebhookCount").asLong(filtered.size())
                : filtered.size();
        long total = clientFiltered ? filtered.size() : totalFromApi;
        ArrayNode slice = sliceArray(filtered, page, pageSize);
        Pagination pagination = paginationFromTotal(total, page, pageSize, slice.size());
        return new PageResult(slice, pagination);
    }

    public PageResult getCustomFieldsPage(String workspaceId,
                                          Map<String, String> queryParams,
                                          int page,
                                          int pageSize) throws Exception {
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        String path = appendQueryParams("/workspaces/" + workspaceId + "/custom-fields", sanitized);
        HttpResponse<String> resp = http.get(path, token, Map.of());
        ensure2xx(resp, 200);

        JsonNode node = om.readTree(resp.body());
        ArrayNode all = node != null && node.isArray() ? (ArrayNode) node : om.createArrayNode();
        ArrayNode slice = sliceArray(all, page, pageSize);
        Pagination pagination = paginationFromTotal(all.size(), page, pageSize, slice.size());
        return new PageResult(slice, pagination);
    }

    public PageResult getInvoicesPage(String workspaceId,
                                      Map<String, String> queryParams,
                                      int page,
                                      int pageSize) throws Exception {
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        // Support comma-separated statuses while preserving Clockify's query name.
        if (sanitized.containsKey("statuses")) {
            sanitized.put("statuses", String.join(",", splitCsv(sanitized.get("statuses"))));
        }
        String path = buildPaginatedPath("/workspaces/" + workspaceId + "/invoices", sanitized, page, pageSize);
        HttpResponse<String> resp = http.get(path, token, Map.of());
        ensure2xx(resp, 200);

        JsonNode root = om.readTree(resp.body());
        ArrayNode invoices = root.path("invoices").isArray()
                ? (ArrayNode) root.get("invoices")
                : om.createArrayNode();
        long total = root.path("total").asLong(invoices.size());
        Pagination pagination = paginationFromTotal(total, page, pageSize, invoices.size());
        return new PageResult(invoices, pagination);
    }

    public static Map<String, String> mapTagsByNormalizedName(JsonNode tagsArray) {
        Map<String, String> map = new LinkedHashMap<>();
        if (tagsArray != null && tagsArray.isArray()) {
            for (JsonNode t : tagsArray) {
                if (t != null && t.has("name") && t.has("id")) {
                    String norm = normalizeTagName(t.get("name").asText());
                    String id = t.get("id").asText();
                    if (norm != null && !map.containsKey(norm)) map.put(norm, id);
                }
            }
        }
        return map;
    }

    public static String normalizeTagName(String name) {
        if (name == null) return null;
        String t = name.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }

    public static ArrayNode ensureTagIdsArray(ObjectNode timeEntry, ObjectMapper om) {
        if (timeEntry.has("tagIds") && timeEntry.get("tagIds").isArray()) {
            return (ArrayNode) timeEntry.get("tagIds");
        }
        ArrayNode arr = om.createArrayNode();
        timeEntry.set("tagIds", arr);
        return arr;
    }

    private static void ensure2xx(HttpResponse<String> resp, int expected) {
        int code = resp.statusCode();
        if (code != expected && (code / 100) != 2) {
            throw new RuntimeException("Clockify API error: status=" + code + " body=" + resp.body());
        }
    }

    private ArrayNode fetchPaginatedArray(String basePath) throws Exception {
        return fetchPaginatedArray(basePath, Map.of());
    }

    private ArrayNode fetchPaginatedArray(String basePath, Map<String, String> queryParams) throws Exception {
        ArrayNode aggregated = om.createArrayNode();
        int page = 1;
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        while (true) {
            String pathWithQuery = buildPaginatedPath(basePath, sanitized, page, PAGE_SIZE);
            HttpResponse<String> resp = http.get(pathWithQuery, token, Map.of());
            ensure2xx(resp, 200);
            JsonNode node = om.readTree(resp.body());
            if (node == null || node.isNull()) {
                break;
            }
            if (!node.isArray()) {
                throw new IllegalStateException("Clockify API response for " + basePath + " is not an array");
            }
            ArrayNode pageItems = (ArrayNode) node;
            if (pageItems.isEmpty()) {
                break;
            }
            aggregated.addAll(pageItems);

            Optional<String> nextPage = resp.headers().firstValue("X-Next-Page");
            if (nextPage.isPresent() && !nextPage.get().isBlank()) {
                try {
                    int parsed = Integer.parseInt(nextPage.get());
                    if (parsed <= page) {
                        break;
                    }
                    page = parsed;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Fallback to size-based pagination below
                }
            }

            if (pageItems.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }
        return aggregated;
    }

    public PageResult fetchPage(String basePath, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        Map<String, String> sanitized = sanitizeQuery(queryParams);
        String pathWithQuery = buildPaginatedPath(basePath, sanitized, page, pageSize);
        HttpResponse<String> resp = http.get(pathWithQuery, token, Map.of());
        ensure2xx(resp, 200);
        JsonNode node = om.readTree(resp.body());
        if (node == null || node.isNull()) {
            node = om.createArrayNode();
        }
        if (!node.isArray()) {
            throw new IllegalStateException("Clockify API response for " + basePath + " is not an array");
        }
        ArrayNode items = (ArrayNode) node;
        Pagination pagination = paginationFrom(resp, page, pageSize, items.size());
        return new PageResult(items, pagination);
    }

    private String buildPaginatedPath(String basePath, Map<String, String> queryParams, int page) {
        return buildPaginatedPath(basePath, queryParams, page, PAGE_SIZE);
    }

    private String buildPaginatedPath(String basePath, Map<String, String> queryParams, int page, int pageSize) {
        StringBuilder sb = new StringBuilder(basePath);
        if (!basePath.contains("?")) {
            sb.append('?');
        } else if (!basePath.endsWith("?") && !basePath.endsWith("&")) {
            sb.append('&');
        }
        sb.append("page-size=").append(pageSize);
        sb.append("&page=").append(page);
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (e.getValue() == null) continue;
                sb.append('&')
                        .append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private Map<String, String> sanitizeQuery(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            String trimmed = entry.getValue().trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            sanitized.put(entry.getKey(), trimmed);
        }
        return sanitized;
    }

    private Pagination paginationFrom(HttpResponse<String> resp, int page, int pageSize, int itemCount) {
        boolean hasMore = false;
        int nextPage = page;
        Optional<String> nextPageHeader = resp.headers().firstValue("X-Next-Page");
        if (nextPageHeader.isPresent()) {
            try {
                int parsed = Integer.parseInt(nextPageHeader.get());
                if (parsed > page) {
                    hasMore = true;
                    nextPage = parsed;
                }
            } catch (NumberFormatException ignored) {
                // fall back to size-based detection
            }
        }
        if (!hasMore && itemCount >= pageSize) {
            hasMore = true;
            nextPage = page + 1;
        }

        long totalItems = -1L;
        Optional<String> totalHeader = resp.headers().firstValue("X-Total-Count");
        if (totalHeader.isPresent()) {
            try {
                totalItems = Long.parseLong(totalHeader.get());
            } catch (NumberFormatException ignored) {
                totalItems = -1L;
            }
        }

        return new Pagination(page, pageSize, hasMore, hasMore ? nextPage : page, totalItems);
    }

    private Pagination paginationFromTotal(long total, int page, int pageSize, int returnedItems) {
        long effectiveTotal = total >= 0 ? total : (long) (page - 1) * pageSize + returnedItems;
        boolean hasMore = total >= 0
                ? ((long) page * pageSize) < total
                : returnedItems >= pageSize;
        int nextPage = hasMore ? page + 1 : page;
        return new Pagination(page, pageSize, hasMore, nextPage, effectiveTotal);
    }

    private ArrayNode sliceArray(ArrayNode source, int page, int pageSize) {
        ArrayNode slice = om.createArrayNode();
        if (source == null || source.isEmpty()) {
            return slice;
        }
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= source.size()) {
            return slice;
        }
        int toIndex = Math.min(source.size(), fromIndex + pageSize);
        for (int i = fromIndex; i < toIndex; i++) {
            slice.add(source.get(i));
        }
        return slice;
    }

    private ArrayNode filterWebhooks(ArrayNode raw, Map<String, String> filters) {
        if (raw == null || raw.isEmpty() || filters == null || filters.isEmpty()) {
            return raw == null ? om.createArrayNode() : raw;
        }
        String eventFilter = normalize(filters.get("event"));
        String enabledFilter = normalize(filters.get("enabled"));
        String searchFilter = normalize(filters.get("search"));
        if (eventFilter == null && enabledFilter == null && searchFilter == null) {
            return raw;
        }
        ArrayNode filtered = om.createArrayNode();
        for (JsonNode node : raw) {
            if (!node.isObject()) {
                continue;
            }
            if (eventFilter != null) {
                String event = node.path("webhookEvent").asText("");
                if (!eventFilter.equalsIgnoreCase(event)) {
                    continue;
                }
            }
            if (enabledFilter != null) {
                boolean expected = "true".equalsIgnoreCase(enabledFilter);
                if (node.path("enabled").asBoolean(false) != expected) {
                    continue;
                }
            }
            if (searchFilter != null) {
                String haystack = (node.path("name").asText("") + "|" + node.path("targetUrl").asText("")).toLowerCase(Locale.ROOT);
                if (!haystack.contains(searchFilter.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }
            filtered.add(node);
        }
        return filtered;
    }

    private String appendQueryParams(String basePath, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return basePath;
        }
        StringBuilder sb = new StringBuilder(basePath);
        boolean first = !basePath.contains("?");
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            sb.append(first ? '?' : '&');
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] tokens = raw.split(",");
        List<String> values = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String trimmed = token == null ? null : token.trim();
            if (trimmed != null && !trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private ArrayNode toArrayNode(List<String> values) {
        ArrayNode array = om.createArrayNode();
        if (values == null) {
            return array;
        }
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
