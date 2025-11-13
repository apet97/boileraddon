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
import java.util.LinkedHashMap;
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

    public PageResult getTimeEntriesPage(String workspaceId, Map<String, String> queryParams, int page, int pageSize) throws Exception {
        return fetchPage("/workspaces/" + workspaceId + "/time-entries", queryParams, page, pageSize);
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
}
