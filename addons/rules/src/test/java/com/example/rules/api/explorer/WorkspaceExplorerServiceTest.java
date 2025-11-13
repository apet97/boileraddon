package com.example.rules.api.explorer;

import com.example.rules.ClockifyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
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

    private static ObjectNode item(String field, String value) {
        ObjectNode node = OM.createObjectNode();
        node.put(field, value);
        return node;
    }

    private static ClockifyClient.PageResult pageResult(int page, int pageSize, ObjectNode... items) {
        ArrayNode array = OM.createArrayNode();
        if (items != null) {
            for (ObjectNode item : items) {
                array.add(item);
            }
        }
        ClockifyClient.Pagination pagination = new ClockifyClient.Pagination(page, pageSize, false, page, array.size());
        return new ClockifyClient.PageResult(array, pagination);
    }

    private static class StubGateway implements WorkspaceExplorerService.ExplorerGateway {
        private final Map<WorkspaceExplorerService.ExplorerDataset, ClockifyClient.PageResult> responses =
                new EnumMap<>(WorkspaceExplorerService.ExplorerDataset.class);

        private WorkspaceExplorerService.ExplorerDataset lastDataset;
        private WorkspaceExplorerService.ExplorerQuery lastQuery;

        void setResponse(WorkspaceExplorerService.ExplorerDataset dataset, ClockifyClient.PageResult result) {
            responses.put(dataset, result);
        }

        @Override
        public ClockifyClient.PageResult fetch(String workspaceId,
                                               WorkspaceExplorerService.ExplorerDataset dataset,
                                               WorkspaceExplorerService.ExplorerQuery query) {
            lastDataset = dataset;
            lastQuery = query;
            return responses.getOrDefault(
                    dataset,
                    new ClockifyClient.PageResult(OM.createArrayNode(), new ClockifyClient.Pagination(1, 1, false, 1, 0))
            );
        }
    }
}
