package com.example.rules.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleValidatorTest {

    @Test
    void acceptsRulesWithProjectAndTaskActions() {
        Rule rule = new Rule(
                "rule-1",
                "Project assignment",
                true,
                "AND",
                List.of(new Condition("descriptionContains", Condition.Operator.CONTAINS, "client", null)),
                List.of(
                        new Action("set_project_by_id", Map.of("projectId", "proj-1")),
                        new Action("set_task_by_name", Map.of("name", "Kickoff"))
                ),
                null,
                0
        );

        assertDoesNotThrow(() -> RuleValidator.validate(rule));
    }

    @Test
    void ruleWithoutConditionsOrTriggerFails() {
        Rule rule = new Rule(
                "rule-empty",
                "No conditions",
                true,
                "AND",
                List.of(),
                List.of(new Action("add_tag", Map.of("tag", "ops"))),
                null,
                0
        );

        assertThrows(RuleValidator.RuleValidationException.class, () -> RuleValidator.validate(rule));
    }

    @Test
    void invalidActionTypeFailsValidation() {
        Rule rule = new Rule(
                "rule-invalid-action",
                "Bad action",
                true,
                "AND",
                List.of(new Condition("descriptionContains", Condition.Operator.CONTAINS, "meeting", null)),
                List.of(new Action("zap_project", Map.of("id", "123"))),
                null,
                0
        );

        assertThrows(RuleValidator.RuleValidationException.class, () -> RuleValidator.validate(rule));
    }
}
