package com.example.rules.api.explorer;

import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceExplorerControllerTest {
    private static final ObjectMapper OM = new ObjectMapper();

    private WorkspaceExplorerService service;
    private WorkspaceExplorerController controller;

    @BeforeEach
    void setUp() {
        RequestContext.configureWorkspaceFallback(true);
        service = mock(WorkspaceExplorerService.class);
        controller = new WorkspaceExplorerController(service);
    }

    @Test
    void timeOffEndpointPassesBalancesFilters() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-1"},
                "view", new String[]{"balances"},
                "policyId", new String[]{"pol-9"},
                "sort", new String[]{"BALANCE"}
        ));
        when(service.getTimeOff(eq("ws-1"), any())).thenReturn(emptyResponse());

        HttpResponse response = controller.timeOff().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getTimeOff(eq("ws-1"), captor.capture());
        WorkspaceExplorerService.ExplorerQuery query = captor.getValue();
        assertEquals("balances", query.filters().get("view"));
        assertEquals("pol-9", query.filters().get("policyId"));
        assertEquals("BALANCE", query.filters().get("sort"));
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void invoicesEndpointJoinsStatuses() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-2"},
                "status", new String[]{"PAID", "SENT"}
        ));
        when(service.getInvoices(eq("ws-2"), any())).thenReturn(emptyResponse());

        controller.invoices().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getInvoices(eq("ws-2"), captor.capture());
        WorkspaceExplorerService.ExplorerQuery query = captor.getValue();
        assertEquals("PAID,SENT", query.filters().get("statuses"));
    }

    @Test
    void tasksEndpointPassesFilters() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-7"},
                "projectId", new String[]{"proj-1"},
                "search", new String[]{"design"},
                "archived", new String[]{"true"},
                "clientId", new String[]{"client-9"}
        ));
        when(service.getTasks(eq("ws-7"), any())).thenReturn(emptyResponse());

        controller.tasks().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getTasks(eq("ws-7"), captor.capture());
        Map<String, String> filters = captor.getValue().filters();
        assertEquals("proj-1", filters.get("projectId"));
        assertEquals("design", filters.get("search"));
        assertEquals("true", filters.get("archived"));
        assertEquals("client-9", filters.get("clientId"));
    }

    @Test
    void snapshotParsesIncludeFlags() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-3"},
                "includeTimeOff", new String[]{"true"},
                "includeTasks", new String[]{"true"},
                "includeWebhooks", new String[]{"1"},
                "pageSizePerDataset", new String[]{"10"},
                "maxPagesPerDataset", new String[]{"2"},
                "timeEntryLookbackDays", new String[]{"45"}
        ));
        when(service.getSnapshot(eq("ws-3"), any())).thenReturn(emptyResponse());

        controller.snapshot().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.SnapshotRequest> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.SnapshotRequest.class);
        verify(service).getSnapshot(eq("ws-3"), captor.capture());
        WorkspaceExplorerService.SnapshotRequest snapshot = captor.getValue();
        assertEquals(10, snapshot.pageSizePerDataset());
        assertEquals(2, snapshot.maxPagesPerDataset());
        assertEquals(45, snapshot.timeEntryLookbackDays());
        assertTrue(snapshot.includeTimeOff());
        assertTrue(snapshot.includeTasks());
        assertTrue(snapshot.includeWebhooks());
    }

    @Test
    void timeOffRequestStatusesAreNormalized() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-4"},
                "status", new String[]{"pending", "APPROVED", "ignored"}
        ));
        when(service.getTimeOff(eq("ws-4"), any())).thenReturn(emptyResponse());

        controller.timeOff().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getTimeOff(eq("ws-4"), captor.capture());
        assertEquals("PENDING,APPROVED", captor.getValue().filters().get("statuses"));
    }

    @Test
    void timeOffPoliciesDropInvalidStatusValues() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-5"},
                "view", new String[]{"policies"},
                "status", new String[]{"approved"}
        ));
        when(service.getTimeOff(eq("ws-5"), any())).thenReturn(emptyResponse());

        controller.timeOff().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getTimeOff(eq("ws-5"), captor.capture());
        Map<String, String> filters = captor.getValue().filters();
        assertTrue(filters.containsKey("view"));
        assertTrue(!filters.containsKey("status"));
    }

    @Test
    void invoiceFiltersNormalizeStatusesAndSortOptions() throws Exception {
        HttpServletRequest request = requestWithParams(Map.of(
                "workspaceId", new String[]{"ws-6"},
                "status", new String[]{"paid", "sent", "noop"},
                "sort", new String[]{"balance"},
                "sortOrder", new String[]{"descending"}
        ));
        when(service.getInvoices(eq("ws-6"), any())).thenReturn(emptyResponse());

        controller.invoices().handle(request);

        ArgumentCaptor<WorkspaceExplorerService.ExplorerQuery> captor =
                ArgumentCaptor.forClass(WorkspaceExplorerService.ExplorerQuery.class);
        verify(service).getInvoices(eq("ws-6"), captor.capture());
        Map<String, String> filters = captor.getValue().filters();
        assertEquals("PAID,SENT", filters.get("statuses"));
        assertEquals("BALANCE", filters.get("sort-column"));
        assertEquals("DESCENDING", filters.get("sort-order"));
    }

    private ObjectNode emptyResponse() {
        ObjectNode node = OM.createObjectNode();
        node.set("items", OM.createArrayNode());
        ObjectNode pagination = OM.createObjectNode();
        pagination.put("page", 1);
        pagination.put("pageSize", 25);
        pagination.put("hasMore", false);
        pagination.put("nextPage", 1);
        node.set("pagination", pagination);
        return node;
    }

    private HttpServletRequest requestWithParams(Map<String, String[]> params) {
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(org.mockito.ArgumentMatchers.anyString()))
                .then(invocation -> attributes.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        when(request.getHeader(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(request.getParameter(org.mockito.ArgumentMatchers.anyString()))
                .then(invocation -> {
                    String name = invocation.getArgument(0);
                    String[] values = params.get(name);
                    return (values == null || values.length == 0) ? null : values[0];
                });
        when(request.getParameterValues(org.mockito.ArgumentMatchers.anyString()))
                .then(invocation -> params.get(invocation.getArgument(0)));
        return request;
    }
}
