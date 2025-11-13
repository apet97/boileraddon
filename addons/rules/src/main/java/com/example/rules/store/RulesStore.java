package com.example.rules.store;

import com.example.rules.engine.Rule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory storage for rules per workspace.
 * Thread-safe implementation using ConcurrentHashMap.
 * For production, replace with database-backed storage.
 */
public class RulesStore implements RulesStoreSPI {

    private static final Logger logger = LoggerFactory.getLogger(RulesStore.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Map<String, Rule>> workspaceRules = new ConcurrentHashMap<>();

    /**
     * Creates or updates a rule for a workspace.
     *
     * @param workspaceId the workspace ID
     * @param rule the rule to save
     * @return the saved rule
     */
    @Override
    public synchronized Rule save(String workspaceId, Rule rule) {
        if (workspaceId == null || rule == null) {
            throw new IllegalArgumentException("workspaceId and rule cannot be null");
        }

        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }

        boolean hasConditions = rule.getConditions() != null && !rule.getConditions().isEmpty();
        boolean hasTriggerMetadata = rule.getTrigger() != null && !rule.getTrigger().isEmpty();
        if (!hasConditions && !hasTriggerMetadata) {
            throw new IllegalArgumentException("Rule must have at least one condition or trigger metadata");
        }

        if (rule.getActions() == null || rule.getActions().isEmpty()) {
            throw new IllegalArgumentException("Rule must have at least one action");
        }

        workspaceRules.computeIfAbsent(workspaceId, k -> new ConcurrentHashMap<>())
                .put(rule.getId(), rule);

        logger.info("Saved rule {} for workspace {}", rule.getId(), workspaceId);
        return rule;
    }

    /**
     * Retrieves a specific rule by ID.
     *
     * @param workspaceId the workspace ID
     * @param ruleId the rule ID
     * @return Optional containing the rule if found
     */
    @Override
    public synchronized Optional<Rule> get(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) {
            return Optional.empty();
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(rules.get(ruleId));
    }

    /**
     * Retrieves all rules for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of rules (may be empty)
     */
    @Override
    public synchronized List<Rule> getAll(String workspaceId) {
        if (workspaceId == null) {
            return Collections.emptyList();
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(rules.values());
    }

    /**
     * Retrieves only enabled rules for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of enabled rules (may be empty)
     */
    @Override
    public synchronized List<Rule> getEnabled(String workspaceId) {
        return getAll(workspaceId).stream()
                .filter(Rule::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a rule.
     *
     * @param workspaceId the workspace ID
     * @param ruleId the rule ID
     * @return true if deleted, false if not found
     */
    @Override
    public synchronized boolean delete(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) {
            return false;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        if (rules == null) {
            return false;
        }

        Rule removed = rules.remove(ruleId);
        if (removed != null) {
            logger.info("Deleted rule {} from workspace {}", ruleId, workspaceId);
            return true;
        }
        return false;
    }

    /**
     * Deletes all rules for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return number of rules deleted
     */
    @Override
    public synchronized int deleteAll(String workspaceId) {
        if (workspaceId == null) {
            return 0;
        }

        Map<String, Rule> rules = workspaceRules.remove(workspaceId);
        if (rules != null) {
            int count = rules.size();
            logger.info("Deleted {} rules from workspace {}", count, workspaceId);
            return count;
        }
        return 0;
    }

    /**
     * Checks if a rule exists.
     *
     * @param workspaceId the workspace ID
     * @param ruleId the rule ID
     * @return true if exists, false otherwise
     */
    @Override
    public synchronized boolean exists(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) {
            return false;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        return rules != null && rules.containsKey(ruleId);
    }

    /**
     * Counts rules for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return number of rules
     */
    @Override
    public synchronized int count(String workspaceId) {
        if (workspaceId == null) {
            return 0;
        }

        Map<String, Rule> rules = workspaceRules.get(workspaceId);
        return rules != null ? rules.size() : 0;
    }

    /**
     * Clears all rules from all workspaces. Used for testing.
     */
    @Override
    public synchronized void clear() {
        workspaceRules.clear();
        logger.info("Cleared all rules from store");
    }
}
