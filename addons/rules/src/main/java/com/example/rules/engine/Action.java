package com.example.rules.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an action to be executed when a rule matches.
 * Examples:
 * - add_tag: {"tag": "urgent"}
 * - remove_tag: {"tag": "later"}
 * - set_description: {"value": "Meeting with client"}
 * - set_billable: {"value": "true"}
 */
public class Action {

    private final String type;
    private final Map<String, String> args;

    @JsonCreator
    public Action(
            @JsonProperty("type") String type,
            @JsonProperty("args") Map<String, String> args) {
        this.type = type;
        this.args = args;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return Objects.equals(type, action.type) &&
                Objects.equals(args, action.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, args);
    }

    @Override
    public String toString() {
        return "Action{" +
                "type='" + type + '\'' +
                ", args=" + args +
                '}';
    }
}
