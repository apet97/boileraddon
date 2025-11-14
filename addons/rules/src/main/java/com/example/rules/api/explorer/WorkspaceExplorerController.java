package com.example.rules.api.explorer;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.logging.LoggingContext;
import com.example.rules.api.ErrorResponse;
import com.example.rules.config.RuntimeFlags;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * HTTP surface for the workspace explorer API set.
 */
public class WorkspaceExplorerController {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceExplorerController.class);
    private static final String MEDIA_JSON = "application/json";
    private static final Set<String> TIME_OFF_REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "ALL");
    private static final Set<String> TIME_OFF_POLICY_STATUSES = Set.of("ACTIVE", "ARCHIVED", "ALL");
    private static final Set<String> TIME_OFF_BALANCE_SORTS = Set.of("USER", "POLICY", "USED", "BALANCE", "TOTAL");
    private static final Set<String> SORT_ORDERS = Set.of("ASCENDING", "DESCENDING");
    private static final Set<String> INVOICE_STATUSES = Set.of("UNSENT", "SENT", "PAID", "PARTIALLY_PAID", "VOID", "OVERDUE");
    private static final Set<String> INVOICE_SORT_COLUMNS = Set.of("ID", "CLIENT", "DUE_ON", "ISSUE_DATE", "AMOUNT", "BALANCE");

    private final WorkspaceExplorerService service;

    public WorkspaceExplorerController(WorkspaceExplorerService service) {
        this.service = service;
    }

    public RequestHandler overview() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);

                WorkspaceExplorerService.OverviewRequest overviewRequest = parseOverviewRequest(request);
                ObjectNode payload = service.getOverview(workspaceId, overviewRequest);
                ObjectNode runtime = payload.putObject("runtime");
                runtime.put("environment", RuntimeFlags.environmentLabel());
                runtime.put("applyChanges", RuntimeFlags.applyChangesEnabled());
                runtime.put("signatureVerification", RuntimeFlags.skipSignatureVerification() ? "skipped" : "enforced");

                return HttpResponse.ok(payload.toString(), MEDIA_JSON);
            } catch (WorkspaceExplorerService.ExplorerException e) {
                return handleExplorerException(e, request);
            } catch (Exception e) {
                return internalError(request, "EXPLORER.OVERVIEW_FAILED", "Failed to load workspace overview", e);
            }
        };
    }

    public RequestHandler users() {
        return request -> handleCollection(
                request,
                25,
                this::userFilters,
                WorkspaceExplorerService.ExplorerDataset.USERS
        );
    }

    public RequestHandler projects() {
        return request -> handleCollection(
                request,
                25,
                this::projectFilters,
                WorkspaceExplorerService.ExplorerDataset.PROJECTS
        );
    }

    public RequestHandler clients() {
        return request -> handleCollection(
                request,
                25,
                this::clientFilters,
                WorkspaceExplorerService.ExplorerDataset.CLIENTS
        );
    }

    public RequestHandler tags() {
        return request -> handleCollection(
                request,
                50,
                this::tagFilters,
                WorkspaceExplorerService.ExplorerDataset.TAGS
        );
    }

    public RequestHandler timeEntries() {
        return request -> handleCollection(
                request,
                20,
                this::timeEntryFilters,
                WorkspaceExplorerService.ExplorerDataset.TIME_ENTRIES
        );
    }

    public RequestHandler timeOff() {
        return request -> handleCollection(
                request,
                25,
                this::timeOffFilters,
                WorkspaceExplorerService.ExplorerDataset.TIME_OFF
        );
    }

    public RequestHandler webhooks() {
        return request -> handleCollection(
                request,
                20,
                this::webhookFilters,
                WorkspaceExplorerService.ExplorerDataset.WEBHOOKS
        );
    }

    public RequestHandler customFields() {
        return request -> handleCollection(
                request,
                25,
                this::customFieldFilters,
                WorkspaceExplorerService.ExplorerDataset.CUSTOM_FIELDS
        );
    }

    public RequestHandler invoices() {
        return request -> handleCollection(
                request,
                25,
                this::invoiceFilters,
                WorkspaceExplorerService.ExplorerDataset.INVOICES
        );
    }

    public RequestHandler snapshot() {
        return request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String workspaceId = RequestContext.resolveWorkspaceId(request);
                if (workspaceId == null) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, workspaceId);
                WorkspaceExplorerService.SnapshotRequest snapshotRequest = parseSnapshotRequest(request);
                ObjectNode payload = service.getSnapshot(workspaceId, snapshotRequest);
                return HttpResponse.ok(payload.toString(), MEDIA_JSON);
            } catch (WorkspaceExplorerService.ExplorerException e) {
                return handleExplorerException(e, request);
            } catch (Exception e) {
                return internalError(request, "EXPLORER.SNAPSHOT_FAILED", "Failed to build snapshot", e);
            }
        };
    }

    private HttpResponse handleCollection(HttpServletRequest request,
                                          int defaultPageSize,
                                          Function<HttpServletRequest, Map<String, String>> filtersBuilder,
                                          WorkspaceExplorerService.ExplorerDataset dataset) {
        try (LoggingContext ctx = RequestContext.logging(request)) {
            String workspaceId = RequestContext.resolveWorkspaceId(request);
            if (workspaceId == null) {
                return workspaceRequired(request);
            }
            RequestContext.attachWorkspace(request, ctx, workspaceId);

            Map<String, String> filters = filtersBuilder == null ? Map.of() : filtersBuilder.apply(request);
            WorkspaceExplorerService.ExplorerQuery query = buildQuery(request, defaultPageSize, filters);
            ObjectNode payload = switch (dataset) {
                case USERS -> service.getUsers(workspaceId, query);
                case PROJECTS -> service.getProjects(workspaceId, query);
                case CLIENTS -> service.getClients(workspaceId, query);
                case TAGS -> service.getTags(workspaceId, query);
                case TIME_ENTRIES -> service.getTimeEntries(workspaceId, query);
                case TIME_OFF -> service.getTimeOff(workspaceId, query);
                case WEBHOOKS -> service.getWebhooks(workspaceId, query);
                case CUSTOM_FIELDS -> service.getCustomFields(workspaceId, query);
                case INVOICES -> service.getInvoices(workspaceId, query);
            };
            return HttpResponse.ok(payload.toString(), MEDIA_JSON);
        } catch (WorkspaceExplorerService.ExplorerException e) {
            return handleExplorerException(e, request);
        } catch (Exception e) {
            String code = switch (dataset) {
                case USERS -> "EXPLORER.USERS_FAILED";
                case PROJECTS -> "EXPLORER.PROJECTS_FAILED";
                case CLIENTS -> "EXPLORER.CLIENTS_FAILED";
                case TAGS -> "EXPLORER.TAGS_FAILED";
                case TIME_ENTRIES -> "EXPLORER.TIME_ENTRIES_FAILED";
                case TIME_OFF -> "EXPLORER.TIME_OFF_FAILED";
                case WEBHOOKS -> "EXPLORER.WEBHOOKS_FAILED";
                case CUSTOM_FIELDS -> "EXPLORER.CUSTOM_FIELDS_FAILED";
                case INVOICES -> "EXPLORER.INVOICES_FAILED";
            };
            String message = switch (dataset) {
                case USERS -> "Failed to load workspace users";
                case PROJECTS -> "Failed to load workspace projects";
                case CLIENTS -> "Failed to load workspace clients";
                case TAGS -> "Failed to load workspace tags";
                case TIME_ENTRIES -> "Failed to load time entries";
                case TIME_OFF -> "Failed to load time off data";
                case WEBHOOKS -> "Failed to load webhooks";
                case CUSTOM_FIELDS -> "Failed to load custom fields";
                case INVOICES -> "Failed to load invoices";
            };
            return internalError(request, code, message, e);
        }
    }

    private WorkspaceExplorerService.OverviewRequest parseOverviewRequest(HttpServletRequest request) {
        int sampleSize = parseInt(request.getParameter("sampleSize"), 5, 1, 50);
        int recentDays = parseInt(request.getParameter("recentDays"), 7, 1, 90);
        return new WorkspaceExplorerService.OverviewRequest(sampleSize, recentDays);
    }

    private WorkspaceExplorerService.SnapshotRequest parseSnapshotRequest(HttpServletRequest request) {
        boolean includeUsers = parseBoolean(request.getParameter("includeUsers"), true);
        boolean includeProjects = parseBoolean(request.getParameter("includeProjects"), true);
        boolean includeClients = parseBoolean(request.getParameter("includeClients"), true);
        boolean includeTags = parseBoolean(request.getParameter("includeTags"), true);
        boolean includeTimeEntries = parseBoolean(request.getParameter("includeTimeEntries"), true);
        boolean includeTimeOff = parseBoolean(request.getParameter("includeTimeOff"), false);
        boolean includeWebhooks = parseBoolean(request.getParameter("includeWebhooks"), false);
        boolean includeCustomFields = parseBoolean(request.getParameter("includeCustomFields"), false);
        boolean includeInvoices = parseBoolean(request.getParameter("includeInvoices"), false);
        int pageSize = parseInt(request.getParameter("pageSizePerDataset"), 25, 5, 100);
        int maxPages = parseInt(request.getParameter("maxPagesPerDataset"), 3, 1, 20);
        return new WorkspaceExplorerService.SnapshotRequest(
                includeUsers,
                includeProjects,
                includeClients,
                includeTags,
                includeTimeEntries,
                includeTimeOff,
                includeWebhooks,
                includeCustomFields,
                includeInvoices,
                pageSize,
                maxPages
        );
    }

    private WorkspaceExplorerService.ExplorerQuery buildQuery(HttpServletRequest request,
                                                              int defaultPageSize,
                                                              Map<String, String> filters) {
        int page = parseInt(request.getParameter("page"), 1, 1, 1000);
        int pageSize = parseInt(request.getParameter("pageSize"), defaultPageSize, 1, 100);
        Map<String, String> effectiveFilters = filters == null ? Map.of() : filters;
        return new WorkspaceExplorerService.ExplorerQuery(page, pageSize, effectiveFilters);
    }

    private Map<String, String> projectFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("name", search);
        }
        String archived = trim(request.getParameter("archived"));
        if (archived != null) {
            filters.put("archived", archived);
        }
        String billable = trim(request.getParameter("billable"));
        if (billable != null) {
            filters.put("billable", billable);
        }
        String clientId = trim(request.getParameter("clientId"));
        if (clientId != null) {
            filters.put("clients", clientId);
        }
        return filters;
    }

    private Map<String, String> userFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("name", search);
        }
        String status = trim(request.getParameter("status"));
        if (status != null) {
            filters.put("status", status);
        }
        return filters;
    }

    private Map<String, String> clientFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("name", search);
        }
        String archived = trim(request.getParameter("archived"));
        if (archived != null) {
            filters.put("archived", archived);
        }
        return filters;
    }

    private Map<String, String> tagFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("name", search);
        }
        String archived = trim(request.getParameter("archived"));
        if (archived != null) {
            filters.put("archived", archived);
        }
        return filters;
    }

    private Map<String, String> timeEntryFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String from = trim(request.getParameter("from"));
        if (from != null) {
            filters.put("start", from);
        }
        String to = trim(request.getParameter("to"));
        if (to != null) {
            filters.put("end", to);
        }
        String userId = trim(request.getParameter("userId"));
        if (userId != null) {
            filters.put("userId", userId);
        }
        String projectId = trim(request.getParameter("projectId"));
        if (projectId != null) {
            filters.put("projectId", projectId);
        }
        String tagIds = trim(request.getParameter("tagIds"));
        if (tagIds != null) {
            filters.put("tagIds", tagIds);
        }
        return filters;
    }

    private Map<String, String> timeOffFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String view = trim(request.getParameter("view"));
        String normalizedView = normalizeView(view);
        filters.put("view", normalizedView);
        switch (normalizedView) {
            case "policies" -> {
                String search = trim(request.getParameter("search"));
                if (search != null) {
                    filters.put("name", search);
                }
                String status = normalizeEnum(request.getParameter("status"), TIME_OFF_POLICY_STATUSES);
                if (status != null) {
                    filters.put("status", status);
                }
            }
            case "balances" -> {
                String policyId = trim(request.getParameter("policyId"));
                if (policyId != null) {
                    filters.put("policyId", policyId);
                }
                String userId = trim(request.getParameter("userId"));
                if (userId != null) {
                    filters.put("userId", userId);
                }
                String sort = normalizeEnum(request.getParameter("sort"), TIME_OFF_BALANCE_SORTS);
                if (sort != null) {
                    filters.put("sort", sort);
                }
                String sortOrder = normalizeEnum(request.getParameter("sortOrder"), SORT_ORDERS);
                if (sortOrder != null) {
                    filters.put("sort-order", sortOrder);
                }
            }
            default -> {
                String statuses = filterCsvValues(joinMulti(request, "status"), TIME_OFF_REQUEST_STATUSES);
                if (statuses != null) {
                    filters.put("statuses", statuses);
                }
                String users = joinMulti(request, "userId");
                if (users != null) {
                    filters.put("users", users);
                }
                String groups = joinMulti(request, "groupId");
                if (groups != null) {
                    filters.put("userGroups", groups);
                }
                String start = trim(request.getParameter("from"));
                if (start != null) {
                    filters.put("start", start);
                }
                String end = trim(request.getParameter("to"));
                if (end != null) {
                    filters.put("end", end);
                }
            }
        }
        return filters;
    }

    private Map<String, String> webhookFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String type = trim(request.getParameter("type"));
        if (type != null) {
            filters.put("type", type);
        }
        String event = trim(request.getParameter("event"));
        if (event != null) {
            filters.put("event", event);
        }
        String enabled = trim(request.getParameter("enabled"));
        if (enabled != null) {
            filters.put("enabled", enabled);
        }
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("search", search);
        }
        return filters;
    }

    private Map<String, String> customFieldFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String search = trim(request.getParameter("search"));
        if (search != null) {
            filters.put("name", search);
        }
        String status = trim(request.getParameter("status"));
        if (status != null) {
            filters.put("status", status);
        }
        String entityType = trim(request.getParameter("entityType"));
        if (entityType != null) {
            filters.put("entity-type", entityType);
        }
        return filters;
    }

    private Map<String, String> invoiceFilters(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        String statuses = filterCsvValues(joinMulti(request, "status"), INVOICE_STATUSES);
        if (statuses != null) {
            filters.put("statuses", statuses);
        }
        String sort = normalizeEnum(request.getParameter("sort"), INVOICE_SORT_COLUMNS);
        if (sort != null) {
            filters.put("sort-column", sort);
        }
        String sortOrder = normalizeEnum(request.getParameter("sortOrder"), SORT_ORDERS);
        if (sortOrder != null) {
            filters.put("sort-order", sortOrder);
        }
        String clientId = trim(request.getParameter("clientId"));
        if (clientId != null) {
            filters.put("client", clientId);
        }
        return filters;
    }

    private HttpResponse handleExplorerException(WorkspaceExplorerService.ExplorerException e, HttpServletRequest request) {
        if (e.getCause() != null) {
            logger.warn("Explorer exception {}: {}", e.code(), e.getMessage(), e);
        } else {
            logger.warn("Explorer exception {}: {}", e.code(), e.getMessage());
        }
        return ErrorResponse.of(e.status(), e.code(), e.getMessage(), request, e.retryable(), e.details());
    }

    private HttpResponse workspaceRequired(HttpServletRequest request) {
        String hint = RequestContext.workspaceFallbackAllowed()
                ? "workspaceId is required"
                : "workspaceId is required (Authorization bearer token missing or expired)";
        return ErrorResponse.of(400, "EXPLORER.WORKSPACE_REQUIRED", hint, request, false);
    }

    private HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, true, e.getMessage());
    }

    private static int parseInt(String raw, int defaultValue, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < min) {
                return min;
            }
            if (parsed > max) {
                return max;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String normalizeView(String view) {
        if (view == null || view.isBlank()) {
            return "requests";
        }
        String normalized = view.trim().toLowerCase();
        return switch (normalized) {
            case "policies", "balances" -> normalized;
            default -> "requests";
        };
    }

    private static String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank() || allowed == null || allowed.isEmpty()) {
            return null;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(upper) ? upper : null;
    }

    private static String filterCsvValues(String rawCsv, Set<String> allowed) {
        if (rawCsv == null || rawCsv.isBlank() || allowed == null || allowed.isEmpty()) {
            return null;
        }
        String[] tokens = rawCsv.split(",");
        Set<String> normalized = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (allowed.contains(upper)) {
                normalized.add(upper);
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(",", normalized);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> defaultValue;
        };
    }

    private static String joinMulti(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return trim(request.getParameter(name));
        }
        List<String> collected = new ArrayList<>();
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) {
                collected.add(trimmed);
            }
        }
        if (collected.isEmpty()) {
            return null;
        }
        return String.join(",", collected);
    }

}
