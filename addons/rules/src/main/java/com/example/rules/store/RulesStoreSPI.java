package com.example.rules.store;

import com.example.rules.engine.Rule;

import java.util.List;
import java.util.Optional;

/**
 * Minimal contract for a rules store. Implementations must be thread-safe.
 */
public interface RulesStoreSPI {
    Rule save(String workspaceId, Rule rule);
    Optional<Rule> get(String workspaceId, String ruleId);
    List<Rule> getAll(String workspaceId);
    List<Rule> getEnabled(String workspaceId);
    boolean delete(String workspaceId, String ruleId);
    int deleteAll(String workspaceId);
    boolean exists(String workspaceId, String ruleId);
    int count(String workspaceId);
    void clear();

    default List<String> listWorkspaces() {
        return List.of();
    }
}

