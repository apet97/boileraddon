package com.example.rules;

import com.clockify.addon.sdk.*;
import com.clockify.addon.sdk.health.HealthCheck;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Smoke test for /status endpoint shape: tokenPresent/applyChanges/skipSignatureVerify/baseUrl.
 * Registers a minimal /status handler (mirrors RulesApp) and exercises it over HTTP.
 */
class StatusEndpointIT {
    private EmbeddedServer server;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) server.stop();
        if (serverThread != null) serverThread.join(2000);
        com.clockify.addon.sdk.security.TokenStore.clear();
    }

    @Test
    void statusReportsFlagsAndTokenPresence() throws Exception {
        int port = randomPort();
        String base = "http://localhost:" + port + "/rules";

        // Seed a token so tokenPresent=true
        String ws = "ws-smoke";
        com.clockify.addon.sdk.security.TokenStore.save(ws, "tok", "https://api.clockify.me/api");

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules (status)")
                .baseUrl(base)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        // Register a handler equivalent to RulesApp’s /status lambda
        addon.registerCustomEndpoint("/status", request -> {
            try {
                String qws = request.getParameter("workspaceId");
                boolean tokenPresent = qws != null && !qws.isBlank() && com.clockify.addon.sdk.security.TokenStore.get(qws).isPresent();
                boolean apply = "true".equalsIgnoreCase(System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false"));
                boolean skipSig = "true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_SKIP_SIGNATURE_VERIFY", "false"));
                String json = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                        .put("workspaceId", qws == null ? "" : qws)
                        .put("tokenPresent", tokenPresent)
                        .put("applyChanges", apply)
                        .put("skipSignatureVerify", skipSig)
                        .put("baseUrl", base)
                        .toString();
                return com.clockify.addon.sdk.HttpResponse.ok(json, "application/json");
            } catch (Exception e) {
                return com.clockify.addon.sdk.HttpResponse.error(500, e.getMessage());
            }
        });

        AddonServlet servlet = new AddonServlet(addon);
        server = new EmbeddedServer(servlet, "/rules");
        serverThread = new Thread(() -> { try { server.start(port); } catch (Exception ignored) {} });
        serverThread.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + "/status?workspaceId=" + ws)).GET().build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, r.statusCode());

        com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.body());
        Assertions.assertTrue(json.get("tokenPresent").asBoolean());
        Assertions.assertFalse(json.get("applyChanges").asBoolean());
        Assertions.assertFalse(json.get("skipSignatureVerify").asBoolean());
        Assertions.assertEquals(base, json.get("baseUrl").asText());
    }

    private static int randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }
}
