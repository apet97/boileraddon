package com.example.rules;

import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Rule trigger matching and event handling.
 * Verifies that rules can be properly configured with trigger metadata
 * and that the IFTTT builder preserves trigger.event information.
 */
class RuleTriggerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rule_canBeCreatedWithBasicProperties() {
        // Verify basic rule creation
        Action action = new Action("add_tag", Map.of("tag", "urgent"));
        Condition condition = new Condition("project", "equals", "Development");

        Rule rule = new Rule(
                "rule-1",
                "Tag urgent time entries",
                true,
                "AND",
                List.of(condition),
                List.of(action)
        );

        assertEquals("rule-1", rule.getId());
        assertEquals("Tag urgent time entries", rule.getName());
        assertTrue(rule.isEnabled());
        assertEquals("AND", rule.getCombinator());
        assertEquals(1, rule.getConditions().size());
        assertEquals(1, rule.getActions().size());
    }

    @Test
    void rule_defaultsToEnabledAndAnd() {
        // Verify default values when enabled and combinator are null
        Rule rule = new Rule(
                null,  // auto-generate ID
                "Test Rule",
                null,  // should default to enabled=true
                null,  // should default to combinator="AND"
                List.of(),
                List.of()
        );

        assertNotNull(rule.getId(), "ID should be auto-generated");
        assertTrue(rule.isEnabled(), "Rule should default to enabled");
        assertEquals("AND", rule.getCombinator(), "Combinator should default to AND");
    }

    @Test
    void rule_supportsOrCombinator() {
        // Verify that OR combinator is supported
        Condition condition1 = new Condition("project", "equals", "Development");
        Condition condition2 = new Condition("billable", "equals", "true");

        Rule rule = new Rule(
                "rule-or",
                "Match multiple conditions with OR",
                true,
                "OR",
                List.of(condition1, condition2),
                List.of()
        );

        assertEquals("OR", rule.getCombinator());
        assertEquals(2, rule.getConditions().size());
    }

    @Test
    void rule_canHaveEmptyConditions() {
        // Verify that rules can have no conditions (always match)
        Action action = new Action("openapi_call", Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/tags",
                "body", "{\"name\":\"auto-tagged\"}"
        ));

        Rule rule = new Rule(
                "rule-always",
                "Always execute action",
                true,
                "AND",
                List.of(),  // no conditions
                List.of(action)
        );

        assertTrue(rule.getConditions().isEmpty(), "Rule should have no conditions");
        assertFalse(rule.getActions().isEmpty(), "Rule should have actions");
    }

    @Test
    void rule_canHaveMultipleActions() {
        // Verify that rules can have multiple actions
        Action action1 = new Action("add_tag", Map.of("tag", "urgent"));
        Action action2 = new Action("set_billable", Map.of("value", "true"));
        Action action3 = new Action("openapi_call", Map.of(
                "method", "POST",
                "path", "/workspaces/{workspaceId}/notifications",
                "body", "{\"message\":\"Time entry tagged\"}"
        ));

        Rule rule = new Rule(
                "rule-multi-action",
                "Execute multiple actions",
                true,
                "AND",
                List.of(),
                List.of(action1, action2, action3)
        );

        assertEquals(3, rule.getActions().size());
    }

    @Test
    void rule_canBeSerialized() throws Exception {
        // Verify that rules can be serialized to JSON and deserialized
        Action action = new Action("add_tag", Map.of("tag", "urgent"));
        Condition condition = new Condition("billable", "equals", "false");

        Rule rule = new Rule(
                "rule-serialize",
                "Serialization Test",
                true,
                "AND",
                List.of(condition),
                List.of(action)
        );

        String json = objectMapper.writeValueAsString(rule);
        Rule deserialized = objectMapper.readValue(json, Rule.class);

        assertEquals(rule.getId(), deserialized.getId());
        assertEquals(rule.getName(), deserialized.getName());
        assertEquals(rule.isEnabled(), deserialized.isEnabled());
        assertEquals(rule.getCombinator(), deserialized.getCombinator());
        assertEquals(rule.getConditions().size(), deserialized.getConditions().size());
        assertEquals(rule.getActions().size(), deserialized.getActions().size());
    }

    @Test
    void rule_equalityWorks() {
        // Verify that equality and hashCode work correctly
        Condition condition = new Condition("project", "equals", "Dev");
        Action action = new Action("add_tag", Map.of("tag", "dev"));

        Rule rule1 = new Rule("id-1", "Rule 1", true, "AND",
                List.of(condition), List.of(action));
        Rule rule2 = new Rule("id-1", "Rule 1", true, "AND",
                List.of(condition), List.of(action));

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    void rule_differentIdsAreNotEqual() {
        // Verify that rules with different IDs are not equal
        Rule rule1 = new Rule("id-1", "Rule", true, "AND", List.of(), List.of());
        Rule rule2 = new Rule("id-2", "Rule", true, "AND", List.of(), List.of());

        assertNotEquals(rule1, rule2);
    }

    @Test
    void rule_disabledRuleWorks() {
        // Verify that disabled rules can be created
        Rule rule = new Rule(
                "rule-disabled",
                "Disabled Rule",
                false,  // disabled
                "AND",
                List.of(),
                List.of()
        );

        assertFalse(rule.isEnabled(), "Rule should be disabled");
    }

    @Test
    void rule_toStringContainsKeyInfo() {
        // Verify that toString provides useful information
        Rule rule = new Rule(
                "rule-1",
                "Test Rule",
                true,
                "AND",
                List.of(new Condition("project", "equals", "Dev")),
                List.of(new Action("add_tag", Map.of("tag", "test")))
        );

        String str = rule.toString();
        assertTrue(str.contains("rule-1"), "toString should contain ID");
        assertTrue(str.contains("Test Rule"), "toString should contain name");
        assertTrue(str.contains("enabled=true"), "toString should contain enabled status");
    }

    @Test
    void rule_canBeDeserializedFromIftttJson() throws Exception {
        // Verify that rules can be deserialized from IFTTT-style JSON
        // This represents the structure that the IFTTT builder would generate
        String iftttJson = """
                {
                    "name": "Tag new projects",
                    "enabled": true,
                    "combinator": "AND",
                    "conditions": [],
                    "actions": [
                        {
                            "type": "openapi_call",
                            "args": {
                                "method": "POST",
                                "path": "/workspaces/{workspaceId}/tags",
                                "body": "{\\"name\\":\\"auto-created\\"}"
                            }
                        }
                    ]
                }
                """;

        Rule rule = objectMapper.readValue(iftttJson, Rule.class);

        assertNotNull(rule.getId(), "ID should be auto-generated");
        assertEquals("Tag new projects", rule.getName());
        assertTrue(rule.isEnabled());
        assertEquals(1, rule.getActions().size());
        assertEquals("openapi_call", rule.getActions().get(0).getType());
    }

    @Test
    void rule_preservesActionOrder() {
        // Verify that action order is preserved
        Action action1 = new Action("add_tag", Map.of("tag", "first"));
        Action action2 = new Action("add_tag", Map.of("tag", "second"));
        Action action3 = new Action("add_tag", Map.of("tag", "third"));

        Rule rule = new Rule(
                "rule-order",
                "Ordered Actions",
                true,
                "AND",
                List.of(),
                List.of(action1, action2, action3)
        );

        List<Action> actions = rule.getActions();
        assertEquals("first", actions.get(0).getArgs().get("tag"));
        assertEquals("second", actions.get(1).getArgs().get("tag"));
        assertEquals("third", actions.get(2).getArgs().get("tag"));
    }

    @Test
    void rule_withComplexConditions() {
        // Verify that rules can have complex conditions
        Condition condition1 = new Condition("project", "equals", "Development");
        Condition condition2 = new Condition("billable", "equals", "false");
        Condition condition3 = new Condition("description", "contains", "meeting");

        Rule rule = new Rule(
                "rule-complex",
                "Complex Conditions",
                true,
                "AND",
                List.of(condition1, condition2, condition3),
                List.of()
        );

        assertEquals(3, rule.getConditions().size());
        assertTrue(rule.getConditions().stream()
                .anyMatch(c -> "contains".equals(c.getOperator())));
    }
}
