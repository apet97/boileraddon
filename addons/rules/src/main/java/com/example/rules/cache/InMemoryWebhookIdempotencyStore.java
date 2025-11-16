package com.example.rules.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default in-memory implementation that keeps dedupe entries per-node.
 */
public final class InMemoryWebhookIdempotencyStore implements WebhookIdempotencyStore {
    private final ConcurrentMap<String, Long> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    public InMemoryWebhookIdempotencyStore() {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webhook-idempotency-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::purgeExpired, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public boolean isDuplicate(String workspaceId, String eventType, String dedupKey, long ttlMillis) {
        String key = buildKey(workspaceId, eventType, dedupKey);
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        Long previous = cache.putIfAbsent(key, expiresAt);
        if (previous == null) {
            return false;
        }
        if (previous < System.currentTimeMillis()) {
            cache.put(key, expiresAt);
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void close() {
        cleaner.shutdownNow();
        cache.clear();
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private static String buildKey(String workspaceId, String eventType, String dedupKey) {
        String ws = workspaceId == null ? "unknown" : workspaceId;
        String event = eventType == null ? "unknown" : eventType;
        return ws + '|' + event + '|' + dedupKey;
    }
}
