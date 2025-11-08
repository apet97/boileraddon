package com.example.rules.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a rule with conditions and actions.
 * A rule evaluates conditions using AND/OR combinator and executes actions when matched.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rule {

    private final String id;
    private final String name;
    private final boolean enabled;
    private final String combinator; // "AND" or "OR"
    private final List<Condition> conditions;
    private final List<Action> actions;

    @JsonCreator
    public Rule(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("enabled") Boolean enabled,
            @JsonProperty("combinator") String combinator,
            @JsonProperty("conditions") List<Condition> conditions,
            @JsonProperty("actions") List<Action> actions) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.enabled = enabled != null ? enabled : true;
        this.combinator = combinator != null ? combinator : "AND";
        this.conditions = conditions;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCombinator() {
        return combinator;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Action> getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return enabled == rule.enabled &&
                Objects.equals(id, rule.id) &&
                Objects.equals(name, rule.name) &&
                Objects.equals(combinator, rule.combinator) &&
                Objects.equals(conditions, rule.conditions) &&
                Objects.equals(actions, rule.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, combinator, conditions, actions);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", combinator='" + combinator + '\'' +
                ", conditions=" + conditions +
                ", actions=" + actions +
                '}';
    }
}
