package com.example.rules.api.explorer;

import com.clockify.addon.sdk.security.TokenStore;
import com.example.rules.ClockifyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockifyExplorerGatewayTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TokenStore.clear();
    }

    @AfterEach
    void tearDown() {
        TokenStore.clear();
    }

    @Test
    void projectTaskFetchPropagatesArchivedFlag() throws Exception {
        TokenStore.save("ws-project", "token", "https://api.clockify.me/api");
        RecordingClockifyClient client = new RecordingClockifyClient();
        WorkspaceExplorerService.ClockifyExplorerGateway gateway =
                new WorkspaceExplorerService.ClockifyExplorerGateway((baseUrl, token) -> client);

        Map<String, String> filters = Map.of(
                "projectId", "proj-1",
                "archived", "true"
        );
        WorkspaceExplorerService.ExplorerQuery query =
                new WorkspaceExplorerService.ExplorerQuery(1, 25, filters);

        gateway.fetch("ws-project", WorkspaceExplorerService.ExplorerDataset.TASKS, query);

        assertEquals(1, client.recordedTaskQueries.size());
        assertEquals("true", client.recordedTaskQueries.get(0).get("archived"));
    }

    @Test
    void workspaceTaskScanPropagatesArchivedFlag() throws Exception {
        TokenStore.save("ws-scan", "token", "https://api.clockify.me/api");
        RecordingClockifyClient client = new RecordingClockifyClient();
        WorkspaceExplorerService.ClockifyExplorerGateway gateway =
                new WorkspaceExplorerService.ClockifyExplorerGateway((baseUrl, token) -> client);

        Map<String, String> filters = Map.of(
                "archived", "false"
        );
        WorkspaceExplorerService.ExplorerQuery query =
                new WorkspaceExplorerService.ExplorerQuery(1, 25, filters);

        gateway.fetch("ws-scan", WorkspaceExplorerService.ExplorerDataset.TASKS, query);

        assertTrue(client.workspaceScanInvoked, "workspace scan should fetch projects");
        assertTrue(client.recordedTaskQueries.stream()
                .allMatch(map -> "false".equals(map.get("archived"))));
    }

    private static final class RecordingClockifyClient extends ClockifyClient {
        final List<Map<String, String>> recordedTaskQueries = new ArrayList<>();
        boolean workspaceScanInvoked = false;

        RecordingClockifyClient() {
            super("https://example.com", "token");
        }

        @Override
        public PageResult getProjectsPage(String workspaceId,
                                          Map<String, String> queryParams,
                                          int page,
                                          int pageSize) {
            workspaceScanInvoked = true;
            ArrayNode projects = MAPPER.createArrayNode();
            ObjectNode project = MAPPER.createObjectNode();
            project.put("id", "proj-1");
            project.put("name", "Example Project");
            projects.add(project);
            Pagination pagination = new Pagination(page, pageSize, false, page, projects.size());
            return new PageResult(projects, pagination);
        }

        @Override
        public PageResult getTasksPage(String workspaceId,
                                       String projectId,
                                       Map<String, String> queryParams,
                                       int page,
                                       int pageSize) {
            recordedTaskQueries.add(new LinkedHashMap<>(queryParams));
            ArrayNode tasks = MAPPER.createArrayNode();
            tasks.add(MAPPER.createObjectNode().put("id", "task-1").put("name", "Example Task"));
            Pagination pagination = new Pagination(page, pageSize, false, page, tasks.size());
            return new PageResult(tasks, pagination);
        }
    }
}
