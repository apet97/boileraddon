package com.example.rules.store;

import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RulesStoreTest {

    private RulesStore store;

    @BeforeEach
    void setUp() {
        store = new RulesStore();
    }

    @Test
    void testSaveAndGet() {
        Rule rule = createTestRule("test-rule", "Test Rule");

        Rule saved = store.save("workspace-1", rule);
        assertNotNull(saved);
        assertEquals(rule.getId(), saved.getId());
        assertEquals(rule.getName(), saved.getName());

        Optional<Rule> retrieved = store.get("workspace-1", saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
    }

    @Test
    void testGetAll() {
        Rule rule1 = createTestRule("rule-1", "Rule 1");
        Rule rule2 = createTestRule("rule-2", "Rule 2");

        store.save("workspace-1", rule1);
        store.save("workspace-1", rule2);

        List<Rule> rules = store.getAll("workspace-1");
        assertEquals(2, rules.size());
    }

    @Test
    void testGetEnabled() {
        Rule enabled = new Rule("rule-1", "Enabled", true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);

        Rule disabled = new Rule("rule-2", "Disabled", false, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);

        store.save("workspace-1", enabled);
        store.save("workspace-1", disabled);

        List<Rule> enabledRules = store.getEnabled("workspace-1");
        assertEquals(1, enabledRules.size());
        assertTrue(enabledRules.get(0).isEnabled());
    }

    @Test
    void testDelete() {
        Rule rule = createTestRule("test-rule", "Test Rule");
        Rule saved = store.save("workspace-1", rule);

        boolean deleted = store.delete("workspace-1", saved.getId());
        assertTrue(deleted);

        Optional<Rule> retrieved = store.get("workspace-1", saved.getId());
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleted = store.delete("workspace-1", "non-existent");
        assertFalse(deleted);
    }

    @Test
    void testDeleteAll() {
        Rule rule1 = createTestRule("rule-1", "Rule 1");
        Rule rule2 = createTestRule("rule-2", "Rule 2");

        store.save("workspace-1", rule1);
        store.save("workspace-1", rule2);

        int deleted = store.deleteAll("workspace-1");
        assertEquals(2, deleted);

        List<Rule> rules = store.getAll("workspace-1");
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExists() {
        Rule rule = createTestRule("test-rule", "Test Rule");
        Rule saved = store.save("workspace-1", rule);

        assertTrue(store.exists("workspace-1", saved.getId()));
        assertFalse(store.exists("workspace-1", "non-existent"));
    }

    @Test
    void testCount() {
        assertEquals(0, store.count("workspace-1"));

        store.save("workspace-1", createTestRule("rule-1", "Rule 1"));
        assertEquals(1, store.count("workspace-1"));

        store.save("workspace-1", createTestRule("rule-2", "Rule 2"));
        assertEquals(2, store.count("workspace-1"));
    }

    @Test
    void testWorkspaceIsolation() {
        Rule rule1 = createTestRule("rule-1", "Rule 1");
        Rule rule2 = createTestRule("rule-2", "Rule 2");

        store.save("workspace-1", rule1);
        store.save("workspace-2", rule2);

        List<Rule> ws1Rules = store.getAll("workspace-1");
        List<Rule> ws2Rules = store.getAll("workspace-2");

        assertEquals(1, ws1Rules.size());
        assertEquals(1, ws2Rules.size());
        assertNotEquals(ws1Rules.get(0).getId(), ws2Rules.get(0).getId());
    }

    @Test
    void testValidation_missingName() {
        Rule rule = new Rule("test-rule", null, true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);

        assertThrows(IllegalArgumentException.class, () -> store.save("workspace-1", rule));
    }

    @Test
    void testValidation_emptyConditions() {
        Rule rule = new Rule("test-rule", "Test", true, "AND",
                Collections.emptyList(),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);

        assertThrows(IllegalArgumentException.class, () -> store.save("workspace-1", rule));
    }

    @Test
    void testValidation_emptyActions() {
        Rule rule = new Rule("test-rule", "Test", true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.emptyList(), null, 0);

        assertThrows(IllegalArgumentException.class, () -> store.save("workspace-1", rule));
    }

    @Test
    void testClear() {
        store.save("workspace-1", createTestRule("rule-1", "Rule 1"));
        store.save("workspace-2", createTestRule("rule-2", "Rule 2"));

        store.clear();

        assertEquals(0, store.count("workspace-1"));
        assertEquals(0, store.count("workspace-2"));
    }

    private Rule createTestRule(String id, String name) {
        return new Rule(id, name, true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);
    }
}
