package com.example.rules.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceCachePaginationTest {
    private static final ObjectMapper OM = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;

    private List<ObjectNode> projects;
    private Map<String, List<ObjectNode>> tasksByProject;
    private List<ObjectNode> clients;
    private List<ObjectNode> users;

    @BeforeEach
    void setUp() throws Exception {
        projects = new ArrayList<>();
        tasksByProject = new HashMap<>();
        clients = new ArrayList<>();
        users = new ArrayList<>();

        // Create >500 entries for each dataset to require pagination.
        for (int i = 1; i <= 502; i++) {
            ObjectNode project = OM.createObjectNode();
            project.put("id", "project-" + i);
            project.put("name", "Project " + i);
            projects.add(project);
            tasksByProject.put("project-" + i, new ArrayList<>());
        }

        // Provide >500 tasks for the first project to cover task pagination.
        List<ObjectNode> projectOneTasks = tasksByProject.get("project-1");
        for (int i = 1; i <= 501; i++) {
            ObjectNode task = OM.createObjectNode();
            task.put("id", "task-1-" + i);
            task.put("name", "Task " + i);
            projectOneTasks.add(task);
        }

        for (int i = 1; i <= 501; i++) {
            ObjectNode client = OM.createObjectNode();
            client.put("id", "client-" + i);
            client.put("name", "Client " + i);
            clients.add(client);

            ObjectNode user = OM.createObjectNode();
            user.put("id", "user-" + i);
            user.put("name", "User " + i);
            user.put("email", "user" + i + "@example.com");
            users.add(user);
        }

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/workspaces/ws1/tags", exchange -> respond(exchange, Collections.emptyList()));
        server.createContext("/workspaces/ws1/projects", new ProjectsHandler());
        server.createContext("/workspaces/ws1/clients", exchange -> respond(exchange, clients));
        server.createContext("/workspaces/ws1/users", exchange -> respond(exchange, users));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void refreshMergesPaginatedResults() {
        WorkspaceCache.Snapshot snapshot = WorkspaceCache.refresh("ws1", baseUrl, "token");

        assertEquals(502, snapshot.projectsById.size());
        assertEquals("Project 502", snapshot.projectsById.get("project-502"));

        assertEquals(501, snapshot.clientsById.size());
        assertEquals("client-501", snapshot.clientsByNameNorm.get("client 501"));

        assertEquals(501, snapshot.usersById.size());
        assertEquals("user-501", snapshot.usersByNameNorm.get("user501@example.com"));

        Map<String, String> tasksForProjectOne = snapshot.tasksByProjectNameNorm.get("project 1");
        assertNotNull(tasksForProjectOne);
        assertEquals(501, tasksForProjectOne.size());
        assertEquals("Task 501", snapshot.taskNamesById.get("task-1-501"));
    }

    private class ProjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/workspaces/ws1/projects")) {
                respond(exchange, projects);
                return;
            }
            if (path.startsWith("/workspaces/ws1/projects/") && path.endsWith("/tasks")) {
                String projectId = path.substring("/workspaces/ws1/projects/".length(), path.length() - "/tasks".length());
                respond(exchange, tasksByProject.getOrDefault(projectId, Collections.emptyList()));
                return;
            }
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, List<ObjectNode> allItems) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        int pageSize = Integer.parseInt(query.getOrDefault("page-size", "500"));
        int page = Integer.parseInt(query.getOrDefault("page", "1"));
        int start = Math.max((page - 1) * pageSize, 0);
        int end = Math.min(start + pageSize, allItems.size());

        List<ObjectNode> slice = start >= allItems.size() ? Collections.emptyList() : allItems.subList(start, end);
        byte[] body = OM.writeValueAsBytes(slice);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        if (end < allItems.size()) {
            exchange.getResponseHeaders().add("X-Next-Page", String.valueOf(page + 1));
        }
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        String raw = uri.getQuery();
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> params = new HashMap<>();
        String[] parts = raw.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            params.put(key, value);
        }
        return params;
    }
}
