package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies that when RULES_APPLY_CHANGES is enabled, WebhookHandlers applies actions
 * by calling ClockifyClient.updateTimeEntry with an updated tagIds array.
 */
class WebhookApplyActionsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private RulesStore store;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        store = new RulesStore();
        request = Mockito.mock(HttpServletRequest.class);
        System.setProperty("RULES_APPLY_CHANGES", "true");
        com.clockify.addon.sdk.security.TokenStore.clear();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("RULES_APPLY_CHANGES");
        com.clockify.addon.sdk.security.TokenStore.clear();
        WebhookHandlers.setClientFactory(null); // reset
    }

    @Test
    void appliesAddTagAction() throws Exception {
        String workspaceId = "ws-1";
        String authToken = "test-token";
        com.clockify.addon.sdk.security.TokenStore.save(workspaceId, authToken, "https://api.clockify.me/api");

        // Rule: if description contains "meeting", add tag "billable"
        Rule rule = new Rule(
                "r1",
                "Tag meetings",
                true,
                "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable")))
        );
        store.save(workspaceId, rule);

        String payload = """
            {
              "workspaceId": "ws-1",
              "event": "TIME_ENTRY_CREATED",
              "timeEntry": {"id":"e1","description":"Client meeting","tagIds":[]}
            }
            """;

        // Prepare request with signature and body
        setupWebhookRequest(payload, authToken);

        // Fake ClockifyClient to avoid network and assert calls
        ClockifyClient fake = Mockito.mock(ClockifyClient.class);
        // getTimeEntry returns entry with no tags
        ObjectNode entry = mapper.createObjectNode();
        entry.put("id", "e1");
        entry.put("description", "Client meeting");
        entry.set("tagIds", mapper.createArrayNode());
        when(fake.getTimeEntry(eq(workspaceId), eq("e1"))).thenReturn(entry);
        // getTags returns empty list so createTag path is used
        when(fake.getTags(eq(workspaceId))).thenReturn(mapper.createArrayNode());
        // createTag returns id t1
        ObjectNode tag = mapper.createObjectNode();
        tag.put("id", "t1");
        when(fake.createTag(eq(workspaceId), eq("billable"))).thenReturn(tag);
        // updateTimeEntry returns updated entry
        when(fake.updateTimeEntry(eq(workspaceId), eq("e1"), any(ObjectNode.class)))
                .thenAnswer(inv -> {
                    ObjectNode patch = inv.getArgument(2);
                    // ensure tagIds present in patch
                    ArrayNode ids = (ArrayNode) patch.get("tagIds");
                    // Should contain t1
                    assertEquals("t1", ids.get(0).asText());
                    return entry;
                });

        // Inject factory that returns our fake client
        WebhookHandlers.setClientFactory((base, token) -> fake);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules").name("Rules").description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ","TIME_ENTRY_WRITE","TAG_READ","TAG_WRITE"})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, store);

        HttpResponse resp = addon.getWebhookHandlers().get("TIME_ENTRY_CREATED").handle(request);
        assertEquals(200, resp.getStatusCode());
        JsonNode json = mapper.readTree(resp.getBody());
        assertEquals("actions_applied", json.get("status").asText());

        // Verify that updateTimeEntry was invoked once
        verify(fake, times(1)).updateTimeEntry(eq(workspaceId), eq("e1"), any(ObjectNode.class));
    }

    private void setupWebhookRequest(String payload, String token) throws Exception {
        String signature = WebhookSignatureValidator.computeSignature(token, payload);
        when(request.getHeader("clockify-webhook-signature")).thenReturn(signature);
        when(request.getAttribute("clockify.rawBody")).thenReturn(payload);
        JsonNode node = mapper.readTree(payload);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(node);

        byte[] bytes = payload.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(jakarta.servlet.ReadListener readListener) {}
        };
        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }
}

