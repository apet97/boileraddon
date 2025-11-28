package com.clockify.addon.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class RuleStore {
    private RuleStore() {
    }

    record RuleDefinition(String id, String matchText, String tag) {
        ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", id);
            node.put("matchText", matchText);
            node.put("tag", tag);
            return node;
        }
    }

    private static final Map<String, List<RuleDefinition>> RULES = new ConcurrentHashMap<>();

    static List<RuleDefinition> getRules(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return List.of();
        }
        return RULES.getOrDefault(workspaceId, List.of());
    }

    static int ruleCount(String workspaceId) {
        return getRules(workspaceId).size();
    }

    static RuleDefinition addRule(String workspaceId, String matchText, String tag) {
        RuleDefinition definition = new RuleDefinition(java.util.UUID.randomUUID().toString(), matchText, tag);
        RULES.computeIfAbsent(workspaceId, id -> new CopyOnWriteArrayList<>()).add(definition);
        return definition;
    }

    static boolean deleteRule(String workspaceId, String id) {
        if (workspaceId == null || workspaceId.isBlank() || id == null || id.isBlank()) {
            return false;
        }
        List<RuleDefinition> rules = RULES.get(workspaceId);
        if (rules == null) {
            return false;
        }
        return rules.removeIf(rule -> id.equals(rule.id()));
    }
}
