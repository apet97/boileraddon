package com.example.rules.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Represents a single condition in a rule.
 * Examples:
 * - descriptionContains: check if description contains a string
 * - hasTag: check if time entry has a specific tag
 * - projectIdEquals: check if project ID matches
 */
public class Condition {

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        NOT_IN
    }

    private final String type;
    private final Operator operator;
    private final String value;
    private final List<String> values;

    @JsonCreator
    public Condition(
            @JsonProperty("type") String type,
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") String value,
            @JsonProperty("values") List<String> values) {
        this.type = type;
        this.operator = operator != null ? operator : Operator.EQUALS;
        this.value = value;
        this.values = values;
    }

    public String getType() {
        return type;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return Objects.equals(type, condition.type) &&
                operator == condition.operator &&
                Objects.equals(value, condition.value) &&
                Objects.equals(values, condition.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, operator, value, values);
    }

    @Override
    public String toString() {
        return "Condition{" +
                "type='" + type + '\'' +
                ", operator=" + operator +
                ", value='" + value + '\'' +
                ", values=" + values +
                '}';
    }
}
