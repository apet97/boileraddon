package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.cache.RuleCache;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Integration-style test that verifies dynamic webhook handling continues to
 * respect condition-bearing rules served from {@link RuleCache}.
 */
class DynamicWebhookHandlersRuleCacheIT {

    private static final ObjectMapper mapper = new ObjectMapper();

    private RulesStore store;
    private ClockifyAddon addon;

    @BeforeEach
    void setUp() {
        System.setProperty("ADDON_SKIP_SIGNATURE_VERIFY", "true");
        store = new RulesStore();
        RuleCache.initialize(store);
        RuleCache.clear();

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules Dynamic Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{})
                .build();
        addon = new ClockifyAddon(manifest);
        DynamicWebhookHandlers.registerDynamicEvents(addon, store);
    }

    @AfterEach
    void tearDown() {
        RuleCache.clear();
        System.clearProperty("ADDON_SKIP_SIGNATURE_VERIFY");
    }

    @Test
    void dynamicWebhookHandlerMatchesCachedRuleConditions() throws Exception {
        String workspaceId = "ws-cache";

        Rule conditionalRule = new Rule(
                "dyn-1",
                "Dynamic rule",
                true,
                "AND",
                Collections.singletonList(new Condition(
                        "descriptionContains",
                        Condition.Operator.CONTAINS,
                        "sync",
                        null
                )),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "meeting"))),
                Map.of("event", "NEW_PROJECT"),
                0
        );

        Rule saved = store.save(workspaceId, conditionalRule);
        assertNotNull(saved.getId());

        RuleCache.refreshRules(workspaceId);

        HttpServletRequest request = buildRequest(workspaceId, "NEW_PROJECT", "Weekly sync review");

        HttpResponse response = addon.getWebhookHandlers().get("NEW_PROJECT").handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("actions_logged", json.get("status").asText());
        assertEquals(1, json.get("actionsCount").asInt());
        assertEquals("NEW_PROJECT", json.get("event").asText());
    }

    private HttpServletRequest buildRequest(String workspaceId, String eventType, String description) throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        String payload = """
                {
                  "workspaceId": "%s",
                  "event": "%s",
                  "description": "%s"
                }
                """.formatted(workspaceId, eventType, description);

        JsonNode json = mapper.readTree(payload);
        attributes.put("clockify.rawBody", payload);
        attributes.put("clockify.jsonBody", json);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());

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
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(anyString())).thenReturn(null);

        return request;
    }
}

