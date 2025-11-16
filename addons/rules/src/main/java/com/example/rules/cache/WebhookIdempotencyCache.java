package com.example.rules.cache;

import com.example.rules.metrics.RulesMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Facade over the configurable webhook idempotency store.
 */
public final class WebhookIdempotencyCache {
    private static final Logger logger = LoggerFactory.getLogger(WebhookIdempotencyCache.class);
    public enum Backend {
        IN_MEMORY,
        DATABASE,
        UNKNOWN
    }

    private static final String[] PREFERRED_FIELDS = new String[]{
            "payloadId",
            "eventId",
            "id",
            "timeEntryId",
            "timeEntry.id",
            "assignmentId",
            "projectId",
            "clientId",
            "targetId",
            "taskId",
            "userId",
            "webhookId",
            "invoiceId"
    };

    private static final long MIN_TTL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long MAX_TTL_MILLIS = Duration.ofHours(24).toMillis();

    private static volatile long ttlMillis = Duration.ofMinutes(10).toMillis();
    private static volatile WebhookIdempotencyStore store = new InMemoryWebhookIdempotencyStore();
    private static volatile Backend backend = Backend.IN_MEMORY;

    private WebhookIdempotencyCache() {
    }

    public static void configureStore(WebhookIdempotencyStore newStore) {
        if (newStore == null) {
            newStore = new InMemoryWebhookIdempotencyStore();
            logger.info("Webhook idempotency store reset to in-memory mode");
        }
        WebhookIdempotencyStore previous = store;
        store = newStore;
        backend = resolveBackend(newStore);
        RulesMetrics.recordIdempotencyBackend(backendLabel());
        logger.info("Webhook idempotency backend ready | backend={}", backendLabel());
        closeQuietly(previous);
    }

    public static void reset() {
        configureStore(new InMemoryWebhookIdempotencyStore());
    }

    public static WebhookIdempotencyStore currentStore() {
        return store;
    }
    public static Backend backendMode() {
        return backend;
    }

    public static String backendLabel() {
        return backend.name().toLowerCase(Locale.ROOT);
    }

    public static void configureTtl(long newTtlMillis) {
        long clamped = Math.max(MIN_TTL_MILLIS, Math.min(MAX_TTL_MILLIS, newTtlMillis));
        if (newTtlMillis < MIN_TTL_MILLIS) {
            logger.warn("Requested webhook dedupe TTL {} ms is below minimum {}; clamping to minimum.",
                    newTtlMillis, MIN_TTL_MILLIS);
        } else if (newTtlMillis > MAX_TTL_MILLIS) {
            logger.warn("Requested webhook dedupe TTL {} ms exceeds maximum {}; clamping to maximum.",
                    newTtlMillis, MAX_TTL_MILLIS);
        }
        ttlMillis = clamped;
        logger.info("Webhook idempotency TTL set to {} ms", ttlMillis);
    }

    public static boolean isDuplicate(String workspaceId, String eventType, JsonNode payload) {
        String dedupKey = deriveDedupKey(payload);
        if (dedupKey == null) {
            RulesMetrics.recordDedupMiss(eventType);
            return false;
        }
        try {
            boolean duplicate = store.isDuplicate(workspaceId, eventType, dedupKey, ttlMillis);
            if (duplicate) {
                RulesMetrics.recordDeduplicatedEvent(eventType);
            } else {
                RulesMetrics.recordDedupMiss(eventType);
            }
            return duplicate;
        } catch (Exception e) {
            logger.warn("Webhook idempotency store failure (workspace={}, event={}): {}", workspaceId, eventType, e.getMessage());
            RulesMetrics.recordDedupMiss(eventType);
            return false;
        }
    }

    public static void clear() {
        store.clear();
    }

    static String deriveDedupKey(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        for (String field : PREFERRED_FIELDS) {
            String value = extractField(payload, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        String body = payload.toString();
        return body.isBlank() ? null : sha256(body);
    }

    private static String extractField(JsonNode payload, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = payload;
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            current = current.get(part);
        }
        if (current == null) {
            return null;
        }
        if (current.isTextual() || current.isNumber()) {
            String value = current.asText();
            return value == null || value.isBlank() ? null : value;
        }
        return null;
    }

    private static String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void closeQuietly(WebhookIdempotencyStore oldStore) {
        if (oldStore == null) {
            return;
        }
        try {
            oldStore.close();
        } catch (Exception ignored) {
        }
    }

    private static Backend resolveBackend(WebhookIdempotencyStore current) {
        if (current instanceof DatabaseWebhookIdempotencyStore) {
            return Backend.DATABASE;
        }
        if (current instanceof InMemoryWebhookIdempotencyStore) {
            return Backend.IN_MEMORY;
        }
        return Backend.UNKNOWN;
    }
}
