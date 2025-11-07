package com.example.autotagassistant;

import com.example.autotagassistant.sdk.AddonServlet;
import com.example.autotagassistant.sdk.ClockifyAddon;
import com.example.autotagassistant.sdk.ClockifyManifest;
import com.example.autotagassistant.sdk.EmbeddedServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"})
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
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"})
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

    private static void assertManifestRequestSucceeds(String manifestUrl, String expectedBaseUrl) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
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
