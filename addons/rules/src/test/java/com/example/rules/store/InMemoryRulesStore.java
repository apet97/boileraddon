package com.example.rules.store;

import com.example.rules.engine.Rule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RulesStoreSPI for testing purposes.
 * Thread-safe and suitable for integration tests.
 */
public class InMemoryRulesStore implements RulesStoreSPI {
    private final Map<String, Map<String, Rule>> workspaceRules = new ConcurrentHashMap<>();

    @Override
    public Rule save(String workspaceId, Rule rule) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("workspaceId cannot be null or blank");
        }
        if (rule == null) {
            throw new IllegalArgumentException("rule cannot be null");
        }

        // Generate ID if not present
        Rule ruleToSave = rule;
        if (rule.getId() == null || rule.getId().isBlank()) {
            String newId = UUID.randomUUID().toString();
            ruleToSave = new Rule(newId, rule.getName(), rule.isEnabled(),
                                 rule.getCombinator(), rule.getConditions(), rule.getActions(),
                                 rule.getTrigger(), rule.getPriority());
        }

        workspaceRules.computeIfAbsent(workspaceId, k -> new ConcurrentHashMap<>())
                     .put(ruleToSave.getId(), ruleToSave);

        return ruleToSave;
    }

    @Override
    public Optional<Rule> get(String workspaceId, String ruleId) {
        if (workspaceId == null || workspaceId.isBlank() || ruleId == null || ruleId.isBlank()) {
            return Optional.empty();
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(rules.get(ruleId));
    }

    @Override
    public List<Rule> getAll(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return List.of();
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return List.of();
        }

        return new ArrayList<>(rules.values());
    }

    @Override
    public List<Rule> getEnabled(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return List.of();
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return List.of();
        }

        return rules.values().stream()
                   .filter(Rule::isEnabled)
                   .toList();
    }

    @Override
    public boolean delete(String workspaceId, String ruleId) {
        if (workspaceId == null || workspaceId.isBlank() || ruleId == null || ruleId.isBlank()) {
            return false;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return false;
        }

        return rules.remove(ruleId) != null;
    }

    @Override
    public int deleteAll(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return 0;
        }

        Map<String, Rule> rules = workspaceRules.remove(workspaceId);
        if (rules == null) {
            return 0;
        }

        return rules.size();
    }

    @Override
    public boolean exists(String workspaceId, String ruleId) {
        if (workspaceId == null || workspaceId.isBlank() || ruleId == null || ruleId.isBlank()) {
            return false;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return false;
        }

        return rules.containsKey(ruleId);
    }

    @Override
    public int count(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return 0;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return 0;
        }

        return rules.size();
    }

    @Override
    public void clear() {
        workspaceRules.clear();
    }
}