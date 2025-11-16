package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.clockify.addon.sdk.testutil.SignatureTestUtil;
import com.example.rules.ClockifyClient;
import com.example.rules.cache.WebhookIdempotencyCache;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class WebhookHandlersTest {

    private RulesStore store;
    private ObjectMapper mapper;
    private HttpServletRequest request;

    private static final String EXECUTOR_CORE_PROP = "com.example.rules.webhook.executor.corePoolSize";
    private static final String EXECUTOR_MAX_PROP = "com.example.rules.webhook.executor.maxPoolSize";
    private static final String EXECUTOR_QUEUE_PROP = "com.example.rules.webhook.executor.queueCapacity";

    @BeforeEach
    void setUp() {
        store = new RulesStore();
        mapper = new ObjectMapper();
        request = Mockito.mock(HttpServletRequest.class);
        WebhookIdempotencyCache.reset();
        MetricsHandler.registry().clear();

        // Clear token store
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.setProperty("ADDON_ACCEPT_JWT_SIGNATURE", "true");
        System.setProperty("ADDON_AUTH_COMPAT", "HMAC");
        System.setProperty("ENV", "dev");
        System.clearProperty("RULES_APPLY_CHANGES");
        System.clearProperty("ADDON_SKIP_SIGNATURE_VERIFY");
        System.clearProperty(EXECUTOR_CORE_PROP);
        System.clearProperty(EXECUTOR_MAX_PROP);
        System.clearProperty(EXECUTOR_QUEUE_PROP);
        WebhookHandlers.setClientFactory(null);
        WebhookHandlers.resetAsyncExecutorForTesting();
    }

    @AfterEach
    void tearDown() {
        com.clockify.addon.sdk.security.TokenStore.clear();
        System.clearProperty("ADDON_ACCEPT_JWT_SIGNATURE");
        System.clearProperty("ADDON_AUTH_COMPAT");
        System.clearProperty("ENV");
        System.clearProperty("ADDON_SKIP_SIGNATURE_VERIFY");
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECT_ISS");
        System.clearProperty("RULES_APPLY_CHANGES");
        System.clearProperty(EXECUTOR_CORE_PROP);
        System.clearProperty(EXECUTOR_MAX_PROP);
        System.clearProperty(EXECUTOR_QUEUE_PROP);
        WebhookHandlers.setClientFactory(null);
        WebhookHandlers.resetAsyncExecutorForTesting();
        WebhookIdempotencyCache.reset();
    }

    @Test
    void testWebhook_noRules() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Test entry",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequestJwt(payload, workspaceId);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("no_rules", json.get("status").asText());
    }

    @Test
    void testWebhook_withMatchingRule() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        // Create a rule
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Action action = new Action("add_tag", Collections.singletonMap("tag", "billable"));
        Rule rule = new Rule("rule-1", "Tag meetings", true, "AND",
                Collections.singletonList(condition),
                Collections.singletonList(action), null, 0);
        store.save(workspaceId, rule);

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Client meeting",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequestJwt(payload, workspaceId);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("actions_logged", json.get("status").asText());
        assertEquals(1, json.get("actionsCount").asInt());
    }

    @Test
    void duplicateWebhookIsIgnored() throws Exception {
        String workspaceId = "workspace-dup";
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, "token", "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-dup",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-dup",
                    "description": "First call",
                    "tagIds": []
                }
            }
            """;
        setupWebhookRequestJwt(payload, workspaceId);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse first = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        JsonNode firstBody = mapper.readTree(first.getBody());
        assertEquals("no_rules", firstBody.get("status").asText());

        HttpResponse second = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        JsonNode secondBody = mapper.readTree(second.getBody());
        assertEquals("duplicate", secondBody.get("status").asText());

        Counter dedupHits = MetricsHandler.registry()
                .find("rules_webhook_dedup_hits_total")
                .tag("event", "NEW_TIME_ENTRY")
                .counter();
        assertNotNull(dedupHits);
        assertEquals(1.0, dedupHits.count(), "Duplicate metric should record exactly one hit");
    }

    @Test
    void testWebhook_invalidSignature() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Test entry",
                    "tagIds": []
                }
            }
            """;

        // Setup request with WRONG signature
        setupWebhookRequest(payload, "wrong-token");

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(403, response.getStatusCode());
    }

    @Test
    void testWebhook_noMatchingRules() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Setup token store
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        // Create a rule that won't match
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Action action = new Action("add_tag", Collections.singletonMap("tag", "billable"));
        Rule rule = new Rule("rule-1", "Tag meetings", true, "AND",
                Collections.singletonList(condition),
                Collections.singletonList(action), null, 0);
        store.save(workspaceId, rule);

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-1",
                    "description": "Development work",
                    "tagIds": []
                }
            }
            """;

        setupWebhookRequestJwt(payload, workspaceId);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        // Get the registered handler
        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("no_match", json.get("status").asText());
        assertEquals(0, json.get("actionsCount").asInt());
    }

    private void setupWebhookRequest(String payload, String authToken) throws Exception {
        String signature = WebhookSignatureValidator.computeSignature(authToken, payload);

        when(request.getHeader("clockify-webhook-signature")).thenReturn(signature);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);

        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            @Override
            public int read() {
                return bis.read();
            }

            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
            }
        };

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }

    @Test
    void asyncExecutorUsesBoundedQueueWhenConfigured() {
        System.setProperty(EXECUTOR_CORE_PROP, "1");
        System.setProperty(EXECUTOR_MAX_PROP, "2");
        System.setProperty(EXECUTOR_QUEUE_PROP, "3");
        WebhookHandlers.resetAsyncExecutorForTesting();

        ThreadPoolExecutor executor = WebhookHandlers.getAsyncExecutor();
        assertEquals(1, executor.getCorePoolSize());
        assertEquals(2, executor.getMaximumPoolSize());
        assertTrue(executor.getQueue() instanceof ArrayBlockingQueue);
        int capacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
        assertEquals(3, capacity);
    }

    @Test
    void oversizedWorkloadsFallbackToSynchronousWhenQueueSaturated() throws Exception {
        String workspaceId = "workspace-async";
        String authToken = "token";
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        System.setProperty("RULES_APPLY_CHANGES", "true");
        System.setProperty(EXECUTOR_CORE_PROP, "1");
        System.setProperty(EXECUTOR_MAX_PROP, "1");
        System.setProperty(EXECUTOR_QUEUE_PROP, "1");
        WebhookHandlers.resetAsyncExecutorForTesting();

        ThreadPoolExecutor executor = WebhookHandlers.getAsyncExecutor();
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);

        executor.execute(() -> {
            running.countDown();
            awaitUninterruptibly(blocker);
        });
        try {
            assertTrue(running.await(5, TimeUnit.SECONDS));
            executor.execute(() -> awaitUninterruptibly(blocker));
            awaitQueueSize(executor, 1);

            List<Action> actions = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                actions.add(new Action("set_description", Map.of("value", "desc-" + i)));
            }
            Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "trigger", null);
            Rule rule = new Rule("async-rule", "Async rule", true, "AND",
                    Collections.singletonList(condition), actions, null, 0);
            store.save(workspaceId, rule);

            String payload = """
                {
                    "workspaceId": "workspace-async",
                    "event": "NEW_TIME_ENTRY",
                    "timeEntry": {
                        "id": "entry-async",
                        "description": "trigger action",
                        "tagIds": []
                    }
                }
                """;
            setupWebhookRequestJwt(payload, workspaceId);

            RecordingClockifyClient fake = new RecordingClockifyClient();
            WebhookHandlers.setClientFactory((base, token) -> fake);

            ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                    .key("rules").name("Rules").description("Test")
                    .baseUrl("http://localhost:8080/rules")
                    .minimalSubscriptionPlan("FREE")
                    .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE"})
                    .build();
            ClockifyAddon addon = new ClockifyAddon(manifest);
            WebhookHandlers.register(addon, store);

            HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
            assertEquals(200, response.getStatusCode());
            JsonNode json = mapper.readTree(response.getBody());
            assertEquals("actions_applied", json.get("status").asText());
            assertTrue(fake.getUpdateCount() >= 1, "Expected synchronous update invocation");

            Counter backlogCounter = MetricsHandler.registry()
                    .find("rules_async_backlog_total")
                    .tag("outcome", "fallback")
                    .counter();
            assertNotNull(backlogCounter);
            assertEquals(1.0, backlogCounter.count());

            Counter rejectedCounter = MetricsHandler.registry()
                    .find("rules_async_backlog_total")
                    .tag("outcome", "rejected")
                    .counter();
            assertNotNull(rejectedCounter);
            assertEquals(1.0, rejectedCounter.count());
        } finally {
            blocker.countDown();
        }
    }

    @Test
    void asyncSchedulingEmitsSubmittedMetric() throws Exception {
        String workspaceId = "workspace-async-success";
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, "token", "https://api.clockify.me/api");

        System.setProperty("RULES_APPLY_CHANGES", "true");
        WebhookHandlers.resetAsyncExecutorForTesting();

        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            actions.add(new Action("set_description", Map.of("value", "desc-" + i)));
        }
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "trigger", null);
        Rule rule = new Rule("async-rule-success", "Async rule", true, "AND",
                Collections.singletonList(condition), actions, null, 0);
        store.save(workspaceId, rule);

        String payload = """
            {
                "workspaceId": "workspace-async-success",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-async-success",
                    "description": "trigger action",
                    "tagIds": []
                }
            }
            """;
        setupWebhookRequestJwt(payload, workspaceId);

        CountDownLatch latch = new CountDownLatch(1);
        LatchingClockifyClient fake = new LatchingClockifyClient(latch);
        WebhookHandlers.setClientFactory((base, token) -> fake);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules").description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE"})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("scheduled", json.get("status").asText());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Async update never completed");
        assertEquals(1, fake.getUpdateCount());

        Counter submittedCounter = MetricsHandler.registry()
                .find("rules_async_backlog_total")
                .tag("outcome", "submitted")
                .counter();
        assertNotNull(submittedCounter);
        assertEquals(1.0, submittedCounter.count());

        Counter fallbackCounter = MetricsHandler.registry()
                .find("rules_async_backlog_total")
                .tag("outcome", "fallback")
                .counter();
        assertNull(fallbackCounter);
    }

    @Test
    void skipSignatureFlagIgnoredOutsideDev() throws Exception {
        String workspaceId = "workspace-prod";
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, "prod-token", "https://api.clockify.me/api");

        System.setProperty("ENV", "prod");
        System.setProperty("ADDON_SKIP_SIGNATURE_VERIFY", "true");

        String payload = """
            {
                "workspaceId": "workspace-prod",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": {
                    "id": "entry-prod",
                    "description": "noop",
                    "tagIds": []
                }
            }
            """;
        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);
        when(request.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(request.getHeader("Clockify-Signature")).thenReturn(null);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules").description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        assertEquals(401, response.getStatusCode(), "Prod env must enforce signatures even when skip flag is set");
    }

    private void setupWebhookRequestJwt(String payload, String workspaceId) throws Exception {
        // Generate a temporary RSA keypair for signing a JWT used by the validator
        var key = SignatureTestUtil.RsaFixture.generate("kid-webhook");
        // Configure validator with the public key and expected issuer
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        String jwt = SignatureTestUtil.rs256Jwt(
                key,
                new SignatureTestUtil.Builder()
                        .sub("rules")
                        .workspaceId(workspaceId)
        );

        // Canonical header path
        when(request.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(request.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);

        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(jakarta.servlet.ReadListener readListener) { }
        };
        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }

    private void awaitQueueSize(ThreadPoolExecutor executor, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (executor.getQueue().size() < expected) {
            if (System.nanoTime() > deadline) {
                fail("Executor queue did not reach expected size: " + expected);
            }
            Thread.sleep(10);
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    latch.await();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class RecordingClockifyClient extends ClockifyClient {
        private final ObjectMapper om = new ObjectMapper();
        private final AtomicInteger updates = new AtomicInteger();

        RecordingClockifyClient() {
            super("http://localhost", "token");
        }

        @Override
        public ObjectNode getTimeEntry(String workspaceId, String timeEntryId) {
            ObjectNode node = om.createObjectNode();
            node.put("id", timeEntryId);
            node.put("description", "original");
            node.set("tagIds", om.createArrayNode());
            return node;
        }

        @Override
        public JsonNode getTags(String workspaceId) {
            return om.createArrayNode();
        }

        @Override
        public ObjectNode updateTimeEntry(String workspaceId, String timeEntryId, ObjectNode patch) {
            updates.incrementAndGet();
            ObjectNode node = om.createObjectNode();
            node.put("id", timeEntryId);
            return node;
        }

        int getUpdateCount() {
            return updates.get();
        }
    }

    private static final class LatchingClockifyClient extends RecordingClockifyClient {
        private final CountDownLatch latch;

        LatchingClockifyClient(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public ObjectNode updateTimeEntry(String workspaceId, String timeEntryId, ObjectNode patch) {
            ObjectNode node = super.updateTimeEntry(workspaceId, timeEntryId, patch);
            latch.countDown();
            return node;
        }
    }

    @Test
    void testWebhook_acceptsDeveloperJwtSignature() throws Exception {
        String workspaceId = "workspace-1";
        String authToken = "test-token";

        // Provide stored token (required by validator even when accepting JWT header)
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        String payload = """
            {
                "workspaceId": "workspace-1",
                "event": "NEW_TIME_ENTRY",
                "timeEntry": { "id": "e1", "description": "hello", "tagIds": [] }
            }
            """;

        // Create an RSA keypair and sign a JWT per the guide
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair kp = kpg.generateKeyPair();
        java.security.interfaces.RSAPublicKey pub = (java.security.interfaces.RSAPublicKey) kp.getPublic();
        java.security.interfaces.RSAPrivateKey priv = (java.security.interfaces.RSAPrivateKey) kp.getPrivate();

        String headerJson = "{\\\"alg\\\":\\\"RS256\\\",\\\"typ\\\":\\\"JWT\\\"}";
        long exp = java.time.Instant.now().plusSeconds(300).getEpochSecond();
        String payloadJson = "{" +
                "\\\"iss\\\":\\\"clockify\\\"," +
                "\\\"type\\\":\\\"addon\\\"," +
                "\\\"sub\\\":\\\"rules\\\"," +
                "\\\"workspaceId\\\":\\\"" + workspaceId + "\\\"," +
                "\\\"exp\\\":" + exp +
                "}";
        java.util.Base64.Encoder urlEnc = java.util.Base64.getUrlEncoder().withoutPadding();
        String headerB64 = urlEnc.encodeToString(headerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String payloadB64 = urlEnc.encodeToString(payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
        sig.initSign(priv);
        sig.update(signingInput.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        String signatureB64 = urlEnc.encodeToString(sig.sign());
        String jwt = signingInput + "." + signatureB64;

        // Configure validator with public key
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", toPem(pub));
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        // Mock request with JWT header instead of HMAC header
        when(request.getHeader("clockify-webhook-signature")).thenReturn(null);
        when(request.getHeader("Clockify-Signature")).thenReturn(jwt);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);
        JsonNode jsonNode = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(jakarta.servlet.ReadListener readListener) { }
        };
        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));

        // Wire add-on and handler
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules").description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse response = addon.getWebhookHandlers().get("NEW_TIME_ENTRY").handle(request);
        assertEquals(200, response.getStatusCode());
        JsonNode out = mapper.readTree(response.getBody());
        assertTrue(out.has("status"));
    }

    private static String toPem(java.security.interfaces.RSAPublicKey publicKey) {
        byte[] der = publicKey.getEncoded();
        String b64 = java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
    }
}
