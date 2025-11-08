package com.example.autotagassistant;

import com.clockify.addon.sdk.*;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.ServerSocket;

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
        String baseUrl = "http://localhost:" + port + "/auto-tag-assistant";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant (IT)")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        addon.registerCustomEndpoint("/health", new HealthCheck("auto-tag-assistant", "it"));
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());

        AddonServlet servlet = new AddonServlet(addon);
        server = new EmbeddedServer(servlet, "/auto-tag-assistant");
        serverThread = new Thread(() -> {
            try {
                server.start(port);
            } catch (Exception ignored) {}
        });
        serverThread.start();

        // Probe /health
        HttpClient client = HttpClient.newHttpClient();
        awaitReady(client, URI.create(baseUrl + "/health"));

        HttpResponse<String> health = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/health")).GET().build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, health.statusCode(), "health should return 200");

        HttpResponse<String> metrics = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/metrics")).GET().build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, metrics.statusCode(), "metrics should return 200");
    }

    private static void awaitReady(HttpClient client, URI uri) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse<Void> r = client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.discarding());
                if (r.statusCode() == 200) return;
            } catch (Exception ignored) {}
            Thread.sleep(100);
        }
    }

    private static int randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

