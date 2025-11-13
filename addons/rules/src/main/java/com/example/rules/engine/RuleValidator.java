package com.example.rules.engine;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates rule objects for correctness and safety.
 */
public class RuleValidator {

    private static final Set<String> SUPPORTED_ACTION_TYPES = Set.of(
            "add_tag",
            "remove_tag",
            "set_description",
            "set_billable",
            "set_project_by_id",
            "set_project_by_name",
            "set_task_by_id",
            "set_task_by_name",
            "openapi_call"
    );

    /**
     * Validates a rule object for correctness.
     *
     * @param rule the rule to validate
     * @throws RuleValidationException if the rule is invalid
     */
    public static void validate(Rule rule) throws RuleValidationException {
        if (rule == null) {
            throw new RuleValidationException("Rule cannot be null");
        }

        // Validate name
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new RuleValidationException("Rule name cannot be empty");
        }

        if (rule.getName().length() > 100) {
            throw new RuleValidationException("Rule name cannot exceed 100 characters");
        }

        // Validate combinator
        if (rule.getCombinator() != null &&
            !"AND".equals(rule.getCombinator()) && !"OR".equals(rule.getCombinator())) {
            throw new RuleValidationException("Combinator must be 'AND' or 'OR'");
        }

        // Validate priority
        if (rule.getPriority() < -100 || rule.getPriority() > 100) {
            throw new RuleValidationException("Priority must be between -100 and 100");
        }

        boolean hasConditions = rule.getConditions() != null && !rule.getConditions().isEmpty();
        boolean hasTrigger = rule.getTrigger() != null && !rule.getTrigger().isEmpty();
        if (!hasConditions && !hasTrigger) {
            throw new RuleValidationException("Rule must include at least one condition or trigger");
        }

        // Validate trigger
        validateTrigger(rule.getTrigger());

        // Validate conditions
        validateConditions(rule.getConditions());

        // Validate actions
        validateActions(rule.getActions());
    }

    private static void validateTrigger(Map<String, Object> trigger) throws RuleValidationException {
        if (trigger == null) {
            return; // Trigger is optional
        }

        // Validate trigger event if present
        Object event = trigger.get("event");
        if (event != null && !(event instanceof String)) {
            throw new RuleValidationException("Trigger event must be a string");
        }

        // Validate trigger parameters
        for (Map.Entry<String, Object> entry : trigger.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Prevent injection attacks in trigger keys
            if (key.contains("..") || key.contains("/") || key.contains("\\")) {
                throw new RuleValidationException("Invalid trigger key: " + key);
            }

            // Only allow string, number, boolean, or null values
            if (value != null &&
                !(value instanceof String) &&
                !(value instanceof Number) &&
                !(value instanceof Boolean)) {
                throw new RuleValidationException("Trigger values must be strings, numbers, or booleans");
            }
        }
    }

    private static void validateConditions(List<Condition> conditions) throws RuleValidationException {
        if (conditions == null) {
            return; // Conditions are optional
        }

        for (Condition condition : conditions) {
            validateCondition(condition);
        }
    }

    private static void validateCondition(Condition condition) throws RuleValidationException {
        if (condition == null) {
            throw new RuleValidationException("Condition cannot be null");
        }

        String type = condition.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new RuleValidationException("Condition type cannot be empty");
        }

        // Validate condition type
        if (!isValidConditionType(type)) {
            throw new RuleValidationException("Invalid condition type: " + type);
        }

        // Validate operator
        Condition.Operator operator = condition.getOperator();
        if (operator != null && !isValidOperator(operator)) {
            throw new RuleValidationException("Invalid operator: " + operator);
        }

        // Validate values
        String value = condition.getValue();
        if (value != null && value.length() > 1000) {
            throw new RuleValidationException("Condition value too long");
        }

        List<String> values = condition.getValues();
        if (values != null) {
            for (String val : values) {
                if (val != null && val.length() > 1000) {
                    throw new RuleValidationException("Condition value in list too long");
                }
            }
        }

        // Validate path for jsonPath conditions
        String path = condition.getPath();
        if (path != null && path.length() > 500) {
            throw new RuleValidationException("Condition path too long");
        }
    }

    private static void validateActions(List<Action> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("Rule must have at least one action");
        }

        for (Action action : actions) {
            validateAction(action);
        }
    }

    private static void validateAction(Action action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Action cannot be null");
        }

        String type = action.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new RuleValidationException("Action type cannot be empty");
        }

        // Validate action type
        if (!isValidActionType(type)) {
            throw new RuleValidationException("Invalid action type: " + type);
        }

        // Validate arguments
        Map<String, String> args = action.getArgs();
        if (args != null) {
            for (Map.Entry<String, String> entry : args.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Prevent injection attacks
                if (key.contains("..") || key.contains("/") || key.contains("\\")) {
                    throw new RuleValidationException("Invalid action argument key: " + key);
                }

                // Validate value length
                if (value != null && value.length() > 10000) {
                    throw new RuleValidationException("Action value too long: " + key);
                }
            }
        }
    }

    private static boolean isValidConditionType(String type) {
        return List.of(
            "descriptionContains", "descriptionEquals",
            "hasTag", "projectIdEquals", "projectNameContains",
            "clientIdEquals", "clientNameContains",
            "isBillable", "jsonPathContains", "jsonPathEquals"
        ).contains(type);
    }

    private static boolean isValidOperator(Condition.Operator operator) {
        return operator != null;
    }

    private static boolean isValidActionType(String type) {
        return SUPPORTED_ACTION_TYPES.contains(type);
    }

    /**
     * Exception thrown when rule validation fails.
     */
    public static class RuleValidationException extends Exception {
        public RuleValidationException(String message) {
            super(message);
        }
    }
}
