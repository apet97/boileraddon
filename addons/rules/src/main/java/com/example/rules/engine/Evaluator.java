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
            case "projectNameContains":
                return evaluateProjectNameContains(condition, context);
            case "clientIdEquals":
                return evaluateClientIdEquals(condition, context);
            case "clientNameContains":
                return evaluateClientNameContains(condition, context);
            case "isBillable":
                return evaluateIsBillable(condition, context);
            default:
                logger.warn("Unknown condition type: {}", type);
                return false;
        }
    }

    private boolean evaluateDescriptionContains(Condition condition, TimeEntryContext context) {
        String description = context.getDescription();
        if (description == null) return false;

        // Single value
        if (condition.getValue() != null) {
            boolean contains = description.toLowerCase().contains(condition.getValue().toLowerCase());
            return applyOperator(condition.getOperator(), contains);
        }
        // Multi-value for IN/NOT_IN
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().stream()
                    .filter(v -> v != null)
                    .anyMatch(v -> description.toLowerCase().contains(v.toLowerCase()));
            return applyOperator(condition.getOperator(), any);
        }
        return false;
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
        if (tagIds == null) return false;

        if (condition.getValue() != null) {
            boolean hasTag = tagIds.contains(condition.getValue());
            return applyOperator(condition.getOperator(), hasTag);
        }
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().stream().anyMatch(tagIds::contains);
            return applyOperator(condition.getOperator(), any);
        }
        return false;
    }

    private boolean evaluateProjectIdEquals(Condition condition, TimeEntryContext context) {
        String projectId = context.getProjectId();
        if (projectId == null) return false;

        if (condition.getValue() != null) {
            boolean equals = projectId.equals(condition.getValue());
            return applyOperator(condition.getOperator(), equals);
        }
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().contains(projectId);
            return applyOperator(condition.getOperator(), any);
        }
        return false;
    }

    private boolean evaluateProjectNameContains(Condition condition, TimeEntryContext context) {
        // project.name may be populated in webhook payloads
        var projectNode = context.getTimeEntry().path("project");
        String name = projectNode.has("name") && projectNode.get("name").isTextual()
                ? projectNode.get("name").asText("") : "";
        if (name.isEmpty()) return false;
        String value = condition.getValue();
        if (value != null) {
            boolean contains = name.toLowerCase().contains(value.toLowerCase());
            return applyOperator(condition.getOperator(), contains);
        }
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().stream()
                    .filter(v -> v != null)
                    .anyMatch(v -> name.toLowerCase().contains(v.toLowerCase()));
            return applyOperator(condition.getOperator(), any);
        }
        return false;
    }

    private boolean evaluateClientIdEquals(Condition condition, TimeEntryContext context) {
        var projectNode = context.getTimeEntry().path("project");
        String clientId = projectNode.has("clientId") && projectNode.get("clientId").isTextual()
                ? projectNode.get("clientId").asText("") : "";
        if (clientId.isEmpty()) return false;
        if (condition.getValue() != null) {
            boolean equals = clientId.equals(condition.getValue());
            return applyOperator(condition.getOperator(), equals);
        }
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().contains(clientId);
            return applyOperator(condition.getOperator(), any);
        }
        return false;
    }

    private boolean evaluateClientNameContains(Condition condition, TimeEntryContext context) {
        var projectNode = context.getTimeEntry().path("project");
        String clientName = projectNode.has("clientName") && projectNode.get("clientName").isTextual()
                ? projectNode.get("clientName").asText("") : "";
        if (clientName.isEmpty()) return false;
        String value = condition.getValue();
        if (value != null) {
            boolean contains = clientName.toLowerCase().contains(value.toLowerCase());
            return applyOperator(condition.getOperator(), contains);
        }
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            boolean any = condition.getValues().stream()
                    .filter(v -> v != null)
                    .anyMatch(v -> clientName.toLowerCase().contains(v.toLowerCase()));
            return applyOperator(condition.getOperator(), any);
        }
        return false;
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
