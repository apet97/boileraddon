package com.clockify.addon.sdk.metrics;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Simple Prometheus metrics endpoint. Exposes a singleton PrometheusMeterRegistry
 * and scrapes it on each request. Content type matches Prometheus text format.
 */
public class MetricsHandler implements RequestHandler {
    private static final PrometheusMeterRegistry REGISTRY = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    public static PrometheusMeterRegistry registry() {
        return REGISTRY;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String scrape = REGISTRY.scrape();
        return HttpResponse.ok(scrape, "text/plain; version=0.0.4; charset=utf-8");
    }
}

