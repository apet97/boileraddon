package com.example.autotagassistant;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.EmbeddedServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoTagAssistantAppTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void manifestEndpointRespondsWhenBaseUrlHasTrailingSlash() throws Exception {
        int port = findFreePort();
        String baseUrl = "http://localhost:" + port + "/auto-tag-assistant/";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        String contextPath = AutoTagAssistantApp.sanitizeContextPath(baseUrl);
        assertEquals("/auto-tag-assistant", contextPath);

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

        String manifestUrl = baseUrl + "manifest.json";
        String expectedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        try {
            assertManifestRequestSucceeds(manifestUrl, expectedBaseUrl);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void manifestEndpointHonorsForwardedHeader() throws Exception {
        int port = findFreePort();
        String baseUrl = "http://localhost:" + port + "/auto-tag-assistant";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        String contextPath = AutoTagAssistantApp.sanitizeContextPath(baseUrl);
        assertEquals("/auto-tag-assistant", contextPath);

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

        String manifestUrl = baseUrl + "/manifest.json";
        String forwardedHost = "example.ngrok-free.app";
        String expectedBaseUrl = "https://" + forwardedHost + contextPath;

        try {
            assertManifestRequestSucceeds(manifestUrl, expectedBaseUrl, connection -> {
                connection.setRequestProperty("Forwarded", "proto=https;host=" + forwardedHost);
            });
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    void manifestEndpointUsesRequestHostWhenConfiguredBaseUrlIsOutdated() throws Exception {
        int port = findFreePort();
        String configuredBaseUrl = "https://should-be-overridden.ngrok-free.app/auto-tag-assistant";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(configuredBaseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        String contextPath = AutoTagAssistantApp.sanitizeContextPath(configuredBaseUrl);
        assertEquals("/auto-tag-assistant", contextPath);

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

        String runtimeBaseUrl = "http://localhost:" + port + "/auto-tag-assistant";
        String manifestUrl = runtimeBaseUrl + "/manifest.json";

        try {
            assertManifestRequestSucceeds(manifestUrl, runtimeBaseUrl);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    @Test
    void lifecycleInstalledEndpointAcceptsDocumentedPayload() throws Exception {
        int port = findFreePort();
        String baseUrl = "http://localhost:" + port + "/auto-tag-assistant";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));
        LifecycleHandlers.register(addon);

        String contextPath = AutoTagAssistantApp.sanitizeContextPath(baseUrl);
        assertEquals("/auto-tag-assistant", contextPath);

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

        String lifecycleUrl = baseUrl + "/lifecycle/installed";

        try {
            assertLifecycleInstalledRequestSucceeds(lifecycleUrl);
        } finally {
            stopServer(server, serverFuture, executor);
        }
    }

    private static void assertManifestRequestSucceeds(String manifestUrl, String expectedBaseUrl) throws Exception {
        assertManifestRequestSucceeds(manifestUrl, expectedBaseUrl, connection -> { });
    }

    private static void assertManifestRequestSucceeds(String manifestUrl, String expectedBaseUrl, Consumer<HttpURLConnection> requestCustomizer) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                requestCustomizer.accept(connection);
                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String body = reader.lines().collect(Collectors.joining());
                        assertTrue(!body.isEmpty(), "Manifest body should not be empty");
                        JsonNode json = OBJECT_MAPPER.readTree(body);
                        assertEquals("auto-tag-assistant", json.get("key").asText());
                        assertEquals(expectedBaseUrl, json.get("baseUrl").asText());
                    }
                    return;
                }
                lastException = new IOException("Unexpected status code: " + status);
            } catch (IOException e) {
                lastException = e;
                Thread.sleep(200);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new AssertionError("Manifest endpoint did not respond successfully");
    }

    private static void assertLifecycleInstalledRequestSucceeds(String lifecycleUrl) throws Exception {
        Exception lastException = null;
        String payload = documentedInstalledLifecyclePayload();
        for (int attempt = 0; attempt < 30; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(lifecycleUrl).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String body = reader.lines().collect(Collectors.joining());
                        assertTrue(!body.isEmpty(), "Lifecycle response should not be empty");
                        JsonNode json = OBJECT_MAPPER.readTree(body);
                        assertEquals("installed", json.get("status").asText());
                        assertEquals("Add-on installed successfully", json.get("message").asText());
                    }
                    return;
                }
                lastException = new IOException("Unexpected status code: " + status);
            } catch (IOException e) {
                lastException = e;
                Thread.sleep(200);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new AssertionError("Lifecycle endpoint did not respond successfully");
    }

    private static String documentedInstalledLifecyclePayload() {
        return OBJECT_MAPPER.createObjectNode()
                .put("addonId", "auto-tag-assistant")
                .put("authToken", "auth-token-example")
                .put("workspaceId", "workspace-123")
                .put("workspaceName", "Example Workspace")
                .put("userId", "user-456")
                .put("userEmail", "admin@example.com")
                .put("timestamp", "2024-01-01T12:00:00Z")
                .toString();
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
}
