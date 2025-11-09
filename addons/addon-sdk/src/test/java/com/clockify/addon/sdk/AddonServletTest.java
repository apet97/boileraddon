package com.clockify.addon.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddonServletTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void lifecyclePostByExplicitPathRoutesToHandler() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        // Register lifecycle handler explicitly at /lifecycle/installed
        addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
            // Ensure body can be read if present
            Object raw = request.getAttribute("clockify.rawBody");
            String body = raw instanceof String ? (String) raw : "";
            return HttpResponse.ok(OBJECT_MAPPER.createObjectNode()
                    .put("status", "installed")
                    .put("echo", body).toString(), "application/json");
        });

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            String body = OBJECT_MAPPER.createObjectNode()
                    .put("workspaceId", "ws-1")
                    .put("userId", "u-1")
                    .toString();

            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/lifecycle/installed").openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(200, status);
            JsonNode json = OBJECT_MAPPER.readTree(responseBody);
            assertEquals("installed", json.get("status").asText());
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookUsesHeaderEventTypeWhenPresent() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_UPDATED", request -> {
            Object cachedJson = request.getAttribute("clockify.jsonBody");
            JsonNode json = cachedJson instanceof JsonNode ? (JsonNode) cachedJson : null;
            String payloadValue = json != null && json.has("payload") ? json.get("payload").asText() : "missing";
            return HttpResponse.ok("header-handler:" + payloadValue);
        });
        addon.registerWebhookHandler("TIME_ENTRY_DELETED", request -> HttpResponse.ok("json-handler"));

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            String body = OBJECT_MAPPER.createObjectNode()
                    .put("event", "TIME_ENTRY_DELETED")
                    .put("payload", "cached")
                    .toString();

            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            connection.setRequestProperty("clockify-webhook-event-type", "TIME_ENTRY_UPDATED");
            writeRequestBody(connection, body);

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(200, status);
            assertEquals("header-handler:cached", responseBody);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookFallsBackToJsonEventWhenHeaderMissing() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_DELETED", request -> HttpResponse.ok("json-handler"));

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            String body = OBJECT_MAPPER.createObjectNode()
                    .put("event", "TIME_ENTRY_DELETED")
                    .put("payload", "value")
                    .toString();

            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            writeRequestBody(connection, body);

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(200, status);
            assertEquals("json-handler", responseBody);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookRoutesByPathAndUpdatesManifest() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> HttpResponse.ok("default-handler"));
        addon.registerWebhookHandler("PROJECT_CREATED", "/project-webhook", request -> HttpResponse.ok("project-handler"));

        assertEquals(2, manifest.getWebhooks().size());
        ClockifyManifest.WebhookEndpoint defaultEndpoint = manifest.getWebhooks().stream()
                .filter(endpoint -> "TIME_ENTRY_CREATED".equals(endpoint.getEvent()))
                .findFirst()
                .orElseThrow();
        assertEquals("/webhook", defaultEndpoint.getPath());

        ClockifyManifest.WebhookEndpoint projectEndpoint = manifest.getWebhooks().stream()
                .filter(endpoint -> "PROJECT_CREATED".equals(endpoint.getEvent()))
                .findFirst()
                .orElseThrow();
        assertEquals("/project-webhook", projectEndpoint.getPath());

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            HttpURLConnection projectConnection = openWebhookConnection(baseUrl + "/project-webhook");
            projectConnection.setRequestProperty("clockify-webhook-event-type", "PROJECT_CREATED");
            writeRequestBody(projectConnection, "{}");

            int projectStatus = projectConnection.getResponseCode();
            String projectResponse = readBody(projectConnection, projectStatus);

            assertEquals(200, projectStatus);
            assertEquals("project-handler", projectResponse);

            HttpURLConnection defaultConnection = openWebhookConnection(baseUrl + "/webhook");
            defaultConnection.setRequestProperty("clockify-webhook-event-type", "TIME_ENTRY_CREATED");
            writeRequestBody(defaultConnection, "{}");

            int defaultStatus = defaultConnection.getResponseCode();
            String defaultResponse = readBody(defaultConnection, defaultStatus);

            assertEquals(200, defaultStatus);
            assertEquals("default-handler", defaultResponse);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookReturnsBadRequestWhenEventMissing() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            String body = OBJECT_MAPPER.createObjectNode()
                    .put("payload", "value")
                    .toString();

            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            writeRequestBody(connection, body);

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(400, status);
            assertEquals("Missing webhook event type", responseBody);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookRejectsInvalidCharactersInEventType() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> HttpResponse.ok("ok"));

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            connection.setRequestProperty("clockify-webhook-event-type", "DROP TABLE");
            writeRequestBody(connection, "{}");

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(400, status);
            JsonNode json = OBJECT_MAPPER.readTree(responseBody);
            assertEquals("Event type contains invalid characters", json.get("message").asText());
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookRejectsUnregisteredEventType() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> HttpResponse.ok("ok"));

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            connection.setRequestProperty("clockify-webhook-event-type", "NOT_REGISTERED");
            writeRequestBody(connection, "{}");

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(400, status);
            JsonNode json = OBJECT_MAPPER.readTree(responseBody);
            assertEquals("Event type not registered in addon manifest", json.get("message").asText());
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void webhookRejectsOverlyLongEventType() throws Exception {
        int port = findFreePort();
        String contextPath = "/auto-tag-assistant";
        String baseUrl = "http://localhost:" + port + contextPath;

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Test manifest")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> HttpResponse.ok("ok"));

        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> serverFuture = executor.submit(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForServer(port);

        try {
            String longEvent = "EVENT_" + "X".repeat(300);
            HttpURLConnection connection = openWebhookConnection(baseUrl + "/webhook");
            connection.setRequestProperty("clockify-webhook-event-type", longEvent);
            writeRequestBody(connection, "{}");

            int status = connection.getResponseCode();
            String responseBody = readBody(connection, status);

            assertEquals(400, status);
            JsonNode json = OBJECT_MAPPER.readTree(responseBody);
            assertEquals("Event type exceeds maximum length (255 characters)", json.get("message").asText());
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    private static HttpURLConnection openWebhookConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }

    private static void writeRequestBody(HttpURLConnection connection, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpURLConnection connection, int status) throws IOException {
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static void stopServer(EmbeddedServer server, Future<?> serverFuture, ExecutorService executor) throws Exception {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            serverFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            serverFuture.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void waitForServer(int port) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException ignored) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Server did not start listening on port " + port);
    }
}
