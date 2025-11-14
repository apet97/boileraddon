package com.example.rules.metrics;

import com.clockify.addon.sdk.metrics.MetricsHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Micrometer helpers for the Rules add-on. Counters/timers are registered once and reused.
 */
public final class RulesMetrics {
    private static final MeterRegistry REGISTRY = MetricsHandler.registry();

    private RulesMetrics() {
    }

    public static Timer.Sample startWebhookTimer() {
        return Timer.start(REGISTRY);
    }

    public static void stopWebhookTimer(Timer.Sample sample, String event, String outcome) {
        if (sample == null) {
            return;
        }
        sample.stop(Timer.builder("rules_webhook_latency_ms")
                .description("Time to process a Clockify webhook event")
                .tag("event", sanitize(event))
                .tag("outcome", sanitize(outcome))
                .register(REGISTRY));
    }

    public static void recordRuleEvaluation(String event, int evaluated, int matched) {
        Counter.builder("rules_evaluated_total")
                .description("Number of rule evaluations per webhook event")
                .tag("event", sanitize(event))
                .register(REGISTRY)
                .increment(evaluated);

        Counter.builder("rules_matched_total")
                .description("Rules that matched during webhook evaluation")
                .tag("event", sanitize(event))
                .register(REGISTRY)
                .increment(matched);
    }

    public static void recordActionResult(String type, boolean success) {
        Counter.builder("rules_actions_total")
                .description("Actions executed by the Rules add-on")
                .tag("type", sanitize(type))
                .tag("result", success ? "success" : "failure")
                .register(REGISTRY)
                .increment();
    }

    public static void recordDeduplicatedEvent(String event) {
        Counter.builder("rules_webhook_dedup_hits_total")
                .description("Duplicate webhook requests ignored")
                .tag("event", sanitize(event))
                .register(REGISTRY)
                .increment();
    }

    public static void recordDedupMiss(String event) {
        Counter.builder("rules_webhook_dedup_misses_total")
                .description("Webhook payloads accepted as new work")
                .tag("event", sanitize(event))
                .register(REGISTRY)
                .increment();
    }

    public static void recordAsyncBacklog(String outcome) {
        Counter.builder("rules_async_backlog_total")
                .description("Async webhook backlog outcomes (fallback or drop)")
                .tag("outcome", sanitize(outcome))
                .register(REGISTRY)
                .increment();
    }

    public static void recordWorkspaceCacheTruncation(String dataset) {
        Counter.builder("rules_workspace_cache_truncated_total")
                .description("Workspace cache refreshes that hit safety caps")
                .tag("dataset", sanitize(dataset))
                .register(REGISTRY)
                .increment();
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 64 ? value.substring(0, 64) : value;
    }
}
