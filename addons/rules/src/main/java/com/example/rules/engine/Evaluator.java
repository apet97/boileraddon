package com.example.rules.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Evaluates rules against time entry contexts.
 */
public class Evaluator {

    private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);

    /**
     * Evaluates a rule against a time entry context.
     *
     * @param rule the rule to evaluate
     * @param context the time entry context
     * @return true if the rule matches, false otherwise
     */
    public boolean evaluate(Rule rule, TimeEntryContext context) {
        if (!rule.isEnabled()) {
            return false;
        }

        List<Condition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        String combinator = rule.getCombinator();
        boolean isAnd = "AND".equalsIgnoreCase(combinator);

        for (Condition condition : conditions) {
            boolean matches = evaluateCondition(condition, context);

            if (isAnd && !matches) {
                // AND: fail fast if any condition fails
                return false;
            } else if (!isAnd && matches) {
                // OR: succeed fast if any condition succeeds
                return true;
            }
        }

        // AND: all conditions passed
        // OR: no conditions passed
        return isAnd;
    }

    private boolean evaluateCondition(Condition condition, TimeEntryContext context) {
        String type = condition.getType();
        if (type == null) {
            return false;
        }

        switch (type) {
            case "descriptionContains":
                return evaluateDescriptionContains(condition, context);
            case "descriptionEquals":
                return evaluateDescriptionEquals(condition, context);
            case "hasTag":
                return evaluateHasTag(condition, context);
            case "projectIdEquals":
                return evaluateProjectIdEquals(condition, context);
            case "isBillable":
                return evaluateIsBillable(condition, context);
            default:
                logger.warn("Unknown condition type: {}", type);
                return false;
        }
    }

    private boolean evaluateDescriptionContains(Condition condition, TimeEntryContext context) {
        String description = context.getDescription();
        String value = condition.getValue();

        if (description == null || value == null) {
            return false;
        }

        boolean contains = description.toLowerCase().contains(value.toLowerCase());
        return applyOperator(condition.getOperator(), contains);
    }

    private boolean evaluateDescriptionEquals(Condition condition, TimeEntryContext context) {
        String description = context.getDescription();
        String value = condition.getValue();

        if (description == null || value == null) {
            return false;
        }

        boolean equals = description.equalsIgnoreCase(value);
        return applyOperator(condition.getOperator(), equals);
    }

    private boolean evaluateHasTag(Condition condition, TimeEntryContext context) {
        List<String> tagIds = context.getTagIds();
        String value = condition.getValue();

        if (tagIds == null || value == null) {
            return false;
        }

        boolean hasTag = tagIds.contains(value);
        return applyOperator(condition.getOperator(), hasTag);
    }

    private boolean evaluateProjectIdEquals(Condition condition, TimeEntryContext context) {
        String projectId = context.getProjectId();
        String value = condition.getValue();

        if (projectId == null || value == null) {
            return false;
        }

        boolean equals = projectId.equals(value);
        return applyOperator(condition.getOperator(), equals);
    }

    private boolean evaluateIsBillable(Condition condition, TimeEntryContext context) {
        boolean isBillable = context.isBillable();
        String value = condition.getValue();

        if (value == null) {
            return false;
        }

        boolean expectedBillable = "true".equalsIgnoreCase(value);
        boolean equals = isBillable == expectedBillable;
        return applyOperator(condition.getOperator(), equals);
    }

    private boolean applyOperator(Condition.Operator operator, boolean matches) {
        switch (operator) {
            case EQUALS:
            case CONTAINS:
            case IN:
                return matches;
            case NOT_EQUALS:
            case NOT_CONTAINS:
            case NOT_IN:
                return !matches;
            default:
                return matches;
        }
    }
}
