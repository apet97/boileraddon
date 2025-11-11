package com.example.rules.cache;

import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStoreSPI;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cache for enabled rules to improve performance.
 * Automatically refreshes rules periodically and on demand.
 */
public final class RuleCache {
    private RuleCache() {}

    private static final Map<String, List<Rule>> RULES_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_REFRESH = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5); // 5 minutes TTL

    private static final ScheduledExecutorService REFRESH_SCHEDULER = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "rule-cache-refresher");
        t.setDaemon(true);
        return t;
    });

    private static volatile RulesStoreSPI rulesStore;

    /**
     * Initialize the rule cache with the rules store.
     */
    public static void initialize(RulesStoreSPI store) {
        rulesStore = store;

        // Schedule periodic cache refresh
        REFRESH_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                refreshAll();
            } catch (Exception e) {
                // Log but don't crash the scheduler
                System.err.println("Error refreshing rule cache: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES); // Check every minute
    }

    /**
     * Get enabled rules for a workspace, using cache if available and fresh.
     */
    public static List<Rule> getEnabledRules(String workspaceId) {
        if (rulesStore == null) {
            return Collections.emptyList();
        }

        Long lastRefresh = LAST_REFRESH.get(workspaceId);
        List<Rule> cachedRules = RULES_CACHE.get(workspaceId);

        // Return cached rules if they exist and are fresh
        if (cachedRules != null && lastRefresh != null &&
            System.currentTimeMillis() - lastRefresh < CACHE_TTL_MS) {
            return cachedRules;
        }

        // Cache miss or stale, refresh synchronously
        return refreshRules(workspaceId);
    }

    /**
     * Refresh rules for a specific workspace.
     */
    public static List<Rule> refreshRules(String workspaceId) {
        if (rulesStore == null) {
            return Collections.emptyList();
        }

        try {
            List<Rule> enabledRules = rulesStore.getEnabled(workspaceId);
            RULES_CACHE.put(workspaceId, Collections.unmodifiableList(enabledRules));
            LAST_REFRESH.put(workspaceId, System.currentTimeMillis());
            return enabledRules;
        } catch (Exception e) {
            // On error, return empty list but don't update cache
            System.err.println("Error refreshing rules for workspace " + workspaceId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Refresh all cached rules.
     */
    public static void refreshAll() {
        if (rulesStore == null) return;

        // Get all workspace IDs that have cached rules
        for (String workspaceId : RULES_CACHE.keySet()) {
            try {
                refreshRules(workspaceId);
            } catch (Exception e) {
                System.err.println("Error refreshing rules for workspace " + workspaceId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Invalidate cache for a specific workspace.
     */
    public static void invalidate(String workspaceId) {
        RULES_CACHE.remove(workspaceId);
        LAST_REFRESH.remove(workspaceId);
    }

    /**
     * Clear all cached rules.
     */
    public static void clear() {
        RULES_CACHE.clear();
        LAST_REFRESH.clear();
    }

    /**
     * Get cache statistics.
     */
    public static Map<String, Object> getStats() {
        return Map.of(
            "cachedWorkspaces", RULES_CACHE.size(),
            "totalRules", RULES_CACHE.values().stream().mapToInt(List::size).sum(),
            "cacheTTL", CACHE_TTL_MS
        );
    }

    /**
     * Shutdown the cache scheduler.
     */
    public static void shutdown() {
        REFRESH_SCHEDULER.shutdown();
        try {
            if (!REFRESH_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                REFRESH_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            REFRESH_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}