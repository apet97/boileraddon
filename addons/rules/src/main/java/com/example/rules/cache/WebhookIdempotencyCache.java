package com.example.rules.cache;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deduplication cache for webhook payloads. Stores a short-lived hash of each
 * workspace/event combination so retries or duplicate deliveries do not re-run business logic.
 */
public final class WebhookIdempotencyCache {

    private static final Logger logger = LoggerFactory.getLogger(WebhookIdempotencyCache.class);
    private static final ConcurrentMap<String, Long> CACHE = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CLEANER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "webhook-idempotency-cleaner");
        t.setDaemon(true);
        return t;
    });
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

    static {
        CLEANER.scheduleAtFixedRate(WebhookIdempotencyCache::purgeExpired, 1, 1, TimeUnit.MINUTES);
    }

    private static final long MIN_TTL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long MAX_TTL_MILLIS = Duration.ofHours(24).toMillis();
    private static volatile long ttlMillis = Duration.ofMinutes(10).toMillis();

    private WebhookIdempotencyCache() {
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

    /**
     * @return true when the payload has already been processed within the configured TTL.
     */
    public static boolean isDuplicate(String workspaceId, String eventType, JsonNode payload) {
        String dedupKey = deriveDedupKey(payload);
        if (dedupKey == null) {
            return false;
        }
        String key = buildKey(workspaceId, eventType, dedupKey);
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        Long previous = CACHE.putIfAbsent(key, expiresAt);
        if (previous == null) {
            return false;
        }
        if (previous < System.currentTimeMillis()) {
            CACHE.put(key, expiresAt);
            return false;
        }
        return true;
    }

    public static void clear() {
        CACHE.clear();
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

    private static String buildKey(String workspaceId, String eventType, String dedupKey) {
        String ws = workspaceId == null ? "unknown" : workspaceId;
        String event = eventType == null ? "unknown" : eventType;
        return ws + '|' + event + '|' + dedupKey;
    }

    private static void purgeExpired() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(entry -> entry.getValue() < now);
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
}
