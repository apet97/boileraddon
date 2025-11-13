package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicWebhookHandlersTest {

    @Test
    void computeDelayUsesRetryAfterCap() {
        long delay = DynamicWebhookHandlers.computeDelay(1, 7_000L);
        assertEquals(5_000L, delay);
    }

    @Test
    void computeDelayAppliesJitter() {
        long delay = DynamicWebhookHandlers.computeDelay(2, null);
        assertTrue(delay >= 550 && delay <= 649, "Delay within jitter window");
    }

    @Test
    void registersPersistedRuleEventsOnStartup() {
        String eventName = "STARTUP_DYNAMIC_EVENT";
        RulesStore store = new RulesStore();
        Rule rule = new Rule(
                "rule-startup",
                "Startup Rule",
                true,
                "AND",
                List.of(new Condition("descriptionContains", Condition.Operator.CONTAINS, "init", null)),
                List.of(new Action("add_tag", Map.of("tag", "boot"))),
                Map.of("event", eventName),
                0);
        store.save("workspace-startup", rule);

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules-startup")
                .name("Rules Startup Test")
                .baseUrl("http://localhost/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);

        DynamicWebhookHandlers.registerDynamicEvents(addon, store);

        assertTrue(addon.getWebhookPathsByEvent().containsKey(eventName));
        assertTrue(addon.getManifest().getWebhooks().stream()
                .anyMatch(endpoint -> eventName.equals(endpoint.getEvent())));
    }
}
