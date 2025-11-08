package com.example.rules.cache;

import com.example.rules.ClockifyClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Small in-memory cache of Clockify workspace entities to map IDs <-> names for UX and quick lookups.
 * Populated on lifecycle install and refreshable via API.
 */
public final class WorkspaceCache {
    private WorkspaceCache() {}

    public static final class Snapshot {
        public final Map<String, String> tagsById;
        public final Map<String, String> tagsByNameNorm; // nameNorm -> id
        public final Map<String, String> projectsById;
        public final Map<String, String> projectsByNameNorm;
        public final Map<String, String> clientsById;
        public final Map<String, String> clientsByNameNorm;
        public final Map<String, String> usersById;
        public final Map<String, String> usersByNameNorm; // nameNorm/emailNorm -> id
        public final Map<String, Map<String, String>> tasksByProjectNameNorm; // projectNameNorm -> (taskNameNorm -> id)
        public final Map<String, String> taskNamesById; // id -> display name

        Snapshot(Map<String, String> tagsById,
                 Map<String, String> tagsByNameNorm,
                 Map<String, String> projectsById,
                 Map<String, String> projectsByNameNorm,
                 Map<String, String> clientsById,
                 Map<String, String> clientsByNameNorm,
                 Map<String, String> usersById,
                 Map<String, String> usersByNameNorm,
                 Map<String, Map<String, String>> tasksByProjectNameNorm,
                 Map<String, String> taskNamesById) {
            this.tagsById = tagsById; this.tagsByNameNorm = tagsByNameNorm;
            this.projectsById = projectsById; this.projectsByNameNorm = projectsByNameNorm;
            this.clientsById = clientsById; this.clientsByNameNorm = clientsByNameNorm;
            this.usersById = usersById; this.usersByNameNorm = usersByNameNorm;
            this.tasksByProjectNameNorm = tasksByProjectNameNorm;
            this.taskNamesById = taskNamesById;
        }
    }

    private static final class Cache {
        volatile Snapshot snapshot = emptySnapshot();
    }

    private static final ConcurrentMap<String, Cache> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "workspace-cache-preloader"); t.setDaemon(true); return t; });

    public static Snapshot get(String workspaceId) {
        return CACHE.computeIfAbsent(workspaceId, k -> new Cache()).snapshot;
    }

    public static void refreshAsync(String workspaceId, String apiBaseUrl, String token) {
        EXEC.submit(() -> refresh(workspaceId, apiBaseUrl, token));
    }

    public static Snapshot refresh(String workspaceId, String apiBaseUrl, String token) {
        try {
            ClockifyClient api = new ClockifyClient(apiBaseUrl, token);
            Map<String, String> tagsById = new ConcurrentHashMap<>();
            Map<String, String> tagsByNameNorm = new ConcurrentHashMap<>();
            JsonNode tags = api.getTags(workspaceId);
            if (tags != null && tags.isArray()) {
                for (JsonNode t : tags) {
                    if (t.has("id") && t.has("name")) {
                        String id = t.get("id").asText(); String name = t.get("name").asText("");
                        tagsById.put(id, name); tagsByNameNorm.put(norm(name), id);
                    }
                }
            }

            Map<String, String> projectsById = new ConcurrentHashMap<>();
            Map<String, String> projectsByNameNorm = new ConcurrentHashMap<>();
            JsonNode projects = api.getProjects(workspaceId, false);
            if (projects != null && projects.isArray()) {
                for (JsonNode p : projects) {
                    if (p.has("id") && p.has("name")) {
                        String id = p.get("id").asText(); String name = p.get("name").asText("");
                        projectsById.put(id, name); projectsByNameNorm.put(norm(name), id);
                    }
                }
            }

            Map<String, String> clientsById = new ConcurrentHashMap<>();
            Map<String, String> clientsByNameNorm = new ConcurrentHashMap<>();
            JsonNode clients = api.getClients(workspaceId, false);
            if (clients != null && clients.isArray()) {
                for (JsonNode c : clients) {
                    if (c.has("id") && c.has("name")) {
                        String id = c.get("id").asText(); String name = c.get("name").asText("");
                        clientsById.put(id, name); clientsByNameNorm.put(norm(name), id);
                    }
                }
            }

            Map<String, String> usersById = new ConcurrentHashMap<>();
            Map<String, String> usersByNameNorm = new ConcurrentHashMap<>();
            JsonNode users = api.getUsers(workspaceId);
            if (users != null && users.isArray()) {
                for (JsonNode u : users) {
                    String id = u.has("id") ? u.get("id").asText() : null;
                    if (id == null) continue;
                    String name = u.has("name") ? u.get("name").asText("") : "";
                    String email = u.has("email") ? u.get("email").asText("") : "";
                    usersById.put(id, name.isEmpty() ? email : name);
                    if (!name.isEmpty()) usersByNameNorm.put(norm(name), id);
                    if (!email.isEmpty()) usersByNameNorm.put(norm(email), id);
                }
            }

            Map<String, Map<String, String>> tasksByProjectNameNorm = new ConcurrentHashMap<>();
            Map<String, String> taskNamesById = new ConcurrentHashMap<>();
            for (Map.Entry<String, String> e : projectsById.entrySet()) {
                String pid = e.getKey(); String pname = e.getValue();
                String pnorm = norm(pname);
                JsonNode tasks = api.getTasks(workspaceId, pid);
                Map<String, String> tmap = new ConcurrentHashMap<>();
                if (tasks != null && tasks.isArray()) {
                    for (JsonNode t : tasks) {
                        if (t.has("id") && t.has("name")) {
                            String id = t.get("id").asText();
                            String name = t.get("name").asText("");
                            tmap.put(norm(name), id);
                            taskNamesById.put(id, name);
                        }
                    }
                }
                tasksByProjectNameNorm.put(pnorm, tmap);
            }

            Snapshot snap = new Snapshot(
                    Collections.unmodifiableMap(tagsById),
                    Collections.unmodifiableMap(tagsByNameNorm),
                    Collections.unmodifiableMap(projectsById),
                    Collections.unmodifiableMap(projectsByNameNorm),
                    Collections.unmodifiableMap(clientsById),
                    Collections.unmodifiableMap(clientsByNameNorm),
                    Collections.unmodifiableMap(usersById),
                    Collections.unmodifiableMap(usersByNameNorm),
                    deepUnmodifiable(tasksByProjectNameNorm),
                    Collections.unmodifiableMap(taskNamesById)
            );
            CACHE.computeIfAbsent(workspaceId, k -> new Cache()).snapshot = snap;
            return snap;
        } catch (Exception e) {
            // Keep the old snapshot and rethrow
            throw new RuntimeException("Failed to refresh workspace cache: " + e.getMessage(), e);
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.toLowerCase(Locale.ROOT);
    }

    private static Snapshot emptySnapshot() {
        return new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static Map<String, Map<String, String>> deepUnmodifiable(Map<String, Map<String, String>> m) {
        ConcurrentHashMap<String, Map<String, String>> out = new ConcurrentHashMap<>();
        for (var e : m.entrySet()) out.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        return Collections.unmodifiableMap(out);
    }
}
