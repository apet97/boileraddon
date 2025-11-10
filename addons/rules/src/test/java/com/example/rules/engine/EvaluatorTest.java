package com.example.rules.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    private Evaluator evaluator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        evaluator = new Evaluator();
        mapper = new ObjectMapper();
    }

    @Test
    void testDescriptionContains_matches() {
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Client meeting tomorrow");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testDescriptionContains_noMatch() {
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Development work");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertFalse(evaluator.evaluate(rule, context));
    }

    @Test
    void testAndCombinator_allMatch() {
        Condition cond1 = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Condition cond2 = new Condition("hasTag", Condition.Operator.EQUALS, "client", null);

        Rule rule = new Rule(null, "Test", true, "AND", Arrays.asList(cond1, cond2),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Client meeting");
        ArrayNode tagIds = mapper.createArrayNode();
        tagIds.add("client");
        timeEntry.set("tagIds", tagIds);

        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testAndCombinator_oneDoesNotMatch() {
        Condition cond1 = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Condition cond2 = new Condition("hasTag", Condition.Operator.EQUALS, "client", null);

        Rule rule = new Rule(null, "Test", true, "AND", Arrays.asList(cond1, cond2),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Client meeting");
        timeEntry.set("tagIds", mapper.createArrayNode()); // No tags

        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertFalse(evaluator.evaluate(rule, context));
    }

    @Test
    void testOrCombinator_oneMatches() {
        Condition cond1 = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Condition cond2 = new Condition("hasTag", Condition.Operator.EQUALS, "urgent", null);

        Rule rule = new Rule(null, "Test", true, "OR", Arrays.asList(cond1, cond2),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "important"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Review code");
        ArrayNode tagIds = mapper.createArrayNode();
        tagIds.add("urgent");
        timeEntry.set("tagIds", tagIds);

        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testOrCombinator_noneMatch() {
        Condition cond1 = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Condition cond2 = new Condition("hasTag", Condition.Operator.EQUALS, "urgent", null);

        Rule rule = new Rule(null, "Test", true, "OR", Arrays.asList(cond1, cond2),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "important"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Review code");
        timeEntry.set("tagIds", mapper.createArrayNode());

        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertFalse(evaluator.evaluate(rule, context));
    }

    @Test
    void testProjectIdEquals_matches() {
        Condition condition = new Condition("projectIdEquals", Condition.Operator.EQUALS, "project-123", null);
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "project-work"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("projectId", "project-123");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testIsBillable_matches() {
        Condition condition = new Condition("isBillable", Condition.Operator.EQUALS, "true", null);
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "revenue"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("billable", true);
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testDisabledRule_doesNotEvaluate() {
        Condition condition = new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null);
        Rule rule = new Rule(null, "Test", false, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "billable"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Client meeting");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertFalse(evaluator.evaluate(rule, context));
    }

    @Test
    void testNotContainsOperator() {
        Condition condition = new Condition("descriptionContains", Condition.Operator.NOT_CONTAINS, "personal", null);
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "work"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Business meeting");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testDescriptionContains_IN_values() {
        Condition condition = new Condition("descriptionContains", Condition.Operator.IN, null, Arrays.asList("meeting", "review"));
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "any"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Code review");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testHasTag_IN_values() {
        Condition condition = new Condition("hasTag", Condition.Operator.IN, null, Arrays.asList("urgent", "client"));
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "hit"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        ArrayNode tagIds = mapper.createArrayNode();
        tagIds.add("client");
        timeEntry.set("tagIds", tagIds);
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }

    @Test
    void testProjectIdEquals_IN_values() {
        Condition condition = new Condition("projectIdEquals", Condition.Operator.IN, null, Arrays.asList("p1", "p2"));
        Rule rule = new Rule(null, "Test", true, "AND", Collections.singletonList(condition),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "proj"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("projectId", "p2");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertTrue(evaluator.evaluate(rule, context));
    }
    @Test
    void testEmptyConditions_returnsFalse() {
        Rule rule = new Rule(null, "Test", true, "AND", Collections.emptyList(),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))), null, 0);

        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("description", "Some work");
        TimeEntryContext context = new TimeEntryContext(timeEntry);

        assertFalse(evaluator.evaluate(rule, context));
    }
}
