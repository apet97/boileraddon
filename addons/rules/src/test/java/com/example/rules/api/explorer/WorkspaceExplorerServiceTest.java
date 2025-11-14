package com.example.rules.api.explorer;

import com.example.rules.ClockifyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceExplorerServiceTest {
    private static final ObjectMapper OM = new ObjectMapper();

    private WorkspaceExplorerService service;
    private StubGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new StubGateway();
        service = new WorkspaceExplorerService(gateway);
    }

    @Test
    void getUsersReturnsItemsAndPagination() throws Exception {
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.USERS,
                pageResult(1, 25, item("id", "u-1"), item("id", "u-2"))
        );

        ObjectNode response = service.getUsers(
                "ws",
                new WorkspaceExplorerService.ExplorerQuery(1, 25, Map.of())
        );

        assertEquals(2, response.get("items").size());
        assertEquals(25, response.get("pagination").get("pageSize").asInt());
        assertEquals(WorkspaceExplorerService.ExplorerDataset.USERS, gateway.lastDataset);
    }

    @Test
    void overviewCombinesSummaryAndSamples() throws Exception {
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.USERS, pageResult(1, 5, item("id", "u-1")));
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.PROJECTS, pageResult(1, 5, item("id", "p-1")));
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.CLIENTS, pageResult(1, 5, item("id", "c-1")));
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.TAGS, pageResult(1, 5, item("id", "t-1")));
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.TIME_ENTRIES, pageResult(1, 5, item("id", "te-1")));

        ObjectNode overview = service.getOverview("ws", new WorkspaceExplorerService.OverviewRequest(5, 3));

        assertEquals("ws", overview.get("workspaceId").asText());
        assertEquals(1, overview.path("summary").path("users").path("total").asInt());
        assertEquals(1, overview.path("samples").path("projects").path("items").size());
        assertTrue(overview.has("recentTimeEntries"));
        assertEquals(1, overview.path("recentTimeEntries").path("items").size());
    }

    @Test
    void timeEntriesAlwaysIncludeHydratedFilter() throws Exception {
        gateway.setResponse(WorkspaceExplorerService.ExplorerDataset.TIME_ENTRIES, pageResult(1, 20));

        service.getTimeEntries("ws", new WorkspaceExplorerService.ExplorerQuery(1, 20, Map.of()));

        assertEquals("true", gateway.lastQuery.filters().get("hydrated"));
    }

    @Test
    void snapshotAggregatesMultiplePagesPerDataset() throws Exception {
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.USERS,
                pageResult(1, 2, true, 2, 4, item("id", "u-1"), item("id", "u-2"))
        );
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.USERS,
                2,
                pageResult(2, 2, false, 2, 4, item("id", "u-3"))
        );
        WorkspaceExplorerService.SnapshotRequest request = new WorkspaceExplorerService.SnapshotRequest(
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                2,
                2
        );

        ObjectNode snapshot = service.getSnapshot("ws", request);

        assertEquals(3, snapshot.path("summary").path("users").path("items").asInt());
        assertEquals(2, snapshot.path("summary").path("users").path("pages").asInt());
        assertEquals(3, snapshot.path("datasets").path("users").size());
    }

    @Test
    void timeOffDefaultsToRequestsView() throws Exception {
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.TIME_OFF,
                pageResult(1, 10, item("id", "req-1"))
        );

        ObjectNode response = service.getTimeOff("ws", null);

        assertEquals(WorkspaceExplorerService.ExplorerDataset.TIME_OFF, gateway.lastDataset);
        assertEquals(1, response.get("items").size());
    }

    @Test
    void webhooksDatasetReturnsItems() throws Exception {
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.WEBHOOKS,
                pageResult(1, 5, item("id", "wh-1"))
        );

        ObjectNode response = service.getWebhooks("ws", null);

        assertEquals(1, response.get("items").size());
        assertEquals(WorkspaceExplorerService.ExplorerDataset.WEBHOOKS, gateway.lastDataset);
    }

    @Test
    void invoicesDatasetReturnsItems() throws Exception {
        gateway.setResponse(
                WorkspaceExplorerService.ExplorerDataset.INVOICES,
                pageResult(1, 5, item("id", "inv-1"))
        );

        ObjectNode response = service.getInvoices("ws", null);

        assertEquals(1, response.get("items").size());
        assertEquals(WorkspaceExplorerService.ExplorerDataset.INVOICES, gateway.lastDataset);
    }

    private static ObjectNode item(String field, String value) {
        ObjectNode node = OM.createObjectNode();
        node.put(field, value);
        return node;
    }

    private static ClockifyClient.PageResult pageResult(int page, int pageSize, ObjectNode... items) {
        return pageResult(page, pageSize, false, page, items == null ? 0 : items.length, items);
    }

    private static ClockifyClient.PageResult pageResult(int page,
                                                        int pageSize,
                                                        boolean hasMore,
                                                        int nextPage,
                                                        long totalItems,
                                                        ObjectNode... items) {
        ArrayNode array = OM.createArrayNode();
        if (items != null) {
            for (ObjectNode item : items) {
                array.add(item);
            }
        }
        ClockifyClient.Pagination pagination = new ClockifyClient.Pagination(
                page,
                pageSize,
                hasMore,
                hasMore ? nextPage : page,
                totalItems
        );
        return new ClockifyClient.PageResult(array, pagination);
    }

    private static class StubGateway implements WorkspaceExplorerService.ExplorerGateway {
        private final Map<WorkspaceExplorerService.ExplorerDataset, Map<Integer, ClockifyClient.PageResult>> responses =
                new EnumMap<>(WorkspaceExplorerService.ExplorerDataset.class);

        private WorkspaceExplorerService.ExplorerDataset lastDataset;
        private WorkspaceExplorerService.ExplorerQuery lastQuery;

        void setResponse(WorkspaceExplorerService.ExplorerDataset dataset, ClockifyClient.PageResult result) {
            setResponse(dataset, 1, result);
        }

        void setResponse(WorkspaceExplorerService.ExplorerDataset dataset,
                         int page,
                         ClockifyClient.PageResult result) {
            responses.computeIfAbsent(dataset, d -> new HashMap<>()).put(page, result);
        }

        @Override
        public ClockifyClient.PageResult fetch(String workspaceId,
                                               WorkspaceExplorerService.ExplorerDataset dataset,
                                               WorkspaceExplorerService.ExplorerQuery query) {
            lastDataset = dataset;
            lastQuery = query;
            Map<Integer, ClockifyClient.PageResult> perPage = responses.get(dataset);
            if (perPage != null) {
                ClockifyClient.PageResult direct = perPage.get(query.page());
                if (direct != null) {
                    return direct;
                }
                ClockifyClient.PageResult any = perPage.get(0);
                if (any != null) {
                    return any;
                }
            }
            return new ClockifyClient.PageResult(
                    OM.createArrayNode(),
                    new ClockifyClient.Pagination(query.page(), query.pageSize(), false, query.page(), 0)
            );
        }
    }
}
