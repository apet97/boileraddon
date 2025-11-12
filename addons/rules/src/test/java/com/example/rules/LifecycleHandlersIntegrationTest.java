package com.example.rules;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.EmbeddedServer;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.clockify.addon.sdk.testutil.SignatureTestUtil;

import java.io.BufferedReader;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleHandlersIntegrationTest {
    private static final ObjectMapper OM = new ObjectMapper();

    @Test
    void installedAndDeletedRoundTripStoresAndRemovesToken() throws Exception {
        // Configure JWT public key for lifecycle signature verification
        var key = SignatureTestUtil.RsaFixture.generate("kid-lc");
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");
        int port = findFreePort();
        String baseUrl = "http://localhost:" + port + "/rules";

        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Automation")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ","TIME_ENTRY_WRITE","TAG_READ","TAG_WRITE"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        LifecycleHandlers.register(addon, new RulesStore());

        EmbeddedServer server = new EmbeddedServer(new AddonServlet(addon), "/rules");
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> f = exec.submit(() -> { try { server.start(port); } catch (Exception e) { throw new RuntimeException(e);} });
        waitForServer(port);
        try {
            // Install
            String payload = OM.createObjectNode()
                    .put("workspaceId", "ws-lc")
                    .put("userId", "u1")
                    .put("authToken", "tkn")
                    .put("apiUrl", "https://api.clockify.me/api/v1")
                    .toString();
            String jwtInstall = SignatureTestUtil.rs256Jwt(
                    key,
                    new SignatureTestUtil.Builder().sub("rules").workspaceId("ws-lc")
            );
            HttpURLConnection c1 = post(baseUrl + "/lifecycle/installed", payload, jwtInstall);
            assertEquals(200, c1.getResponseCode());
            String body1 = readBody(c1);
            JsonNode j1 = OM.readTree(body1);
            assertEquals("installed", j1.get("status").asText());
            assertTrue(com.clockify.addon.sdk.security.TokenStore.get("ws-lc").isPresent());

            // Delete
            String delPayload = OM.createObjectNode().put("workspaceId", "ws-lc").toString();
            String jwtDelete = SignatureTestUtil.rs256Jwt(
                    key,
                    new SignatureTestUtil.Builder().sub("rules").workspaceId("ws-lc")
            );
            HttpURLConnection c2 = post(baseUrl + "/lifecycle/deleted", delPayload, jwtDelete);
            assertEquals(200, c2.getResponseCode());
            String body2 = readBody(c2);
            JsonNode j2 = OM.readTree(body2);
            assertEquals("uninstalled", j2.get("status").asText());
            assertTrue(com.clockify.addon.sdk.security.TokenStore.get("ws-lc").isEmpty());
        } finally {
            server.stop();
            f.cancel(true);
            exec.shutdownNow();
            System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
            System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
        }
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) { s.setReuseAddress(true); return s.getLocalPort(); }
    }

    private static void waitForServer(int port) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            try (java.net.Socket s = new java.net.Socket("127.0.0.1", port)) { return; } catch (Exception ignored) {}
            Thread.sleep(50);
        }
        throw new IllegalStateException("Server did not start");
    }

    private static HttpURLConnection post(String url, String body, String jwt) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        if (jwt != null && !jwt.isBlank()) c.setRequestProperty("Clockify-Signature", jwt);
        c.setDoOutput(true);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        c.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
        return c;
    }

    private static String readBody(HttpURLConnection c) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
