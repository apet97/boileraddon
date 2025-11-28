package com.clockify.addon.rules;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class SmokeIT {
    private EmbeddedServer server;
    private Thread serverThread;
    private int port;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.join(2000);
        }
    }

    @Test
    void healthAndMetricsRespond() throws Exception {
        this.port = randomPort();
        String baseUrl = "http://localhost:" + port + "/rules";

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules (IT)")
                .description("Smoke test")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/health", new HealthCheck("rules", "it"));
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());

        AddonServlet servlet = new AddonServlet(addon);
        server = new EmbeddedServer(servlet, "/rules");
        serverThread = new Thread(() -> {
            try {
                server.start(port);
            } catch (Exception ignored) {
            }
        });
        serverThread.start();

        HttpClient client = HttpClient.newHttpClient();
        awaitReady(client, URI.create(baseUrl + "/health"));

        HttpResponse<String> health = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, health.statusCode(), "health should return 200");

        HttpResponse<String> metrics = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/metrics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, metrics.statusCode(), "metrics should return 200");
    }

    private static void awaitReady(HttpClient client, URI uri) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse<Void> response = client.send(
                        HttpRequest.newBuilder(uri).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(100);
        }
    }

    private static int randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
