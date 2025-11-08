# Metrics (Prometheus)

This boilerplate exposes `/metrics` via a Micrometer Prometheus registry in the SDK. You can add counters/timers in your handlers and scrape with Prometheus.

## Enabling the endpoint

In your app wiring (already present in examples):

```
addon.registerCustomEndpoint("/metrics", new MetricsHandler());
```

## Built‑in webhook metrics (SDK)

The SDK records:
- `webhook_requests_total{event,path}` — number of webhook requests handled
- `webhook_request_seconds{event,path}` — duration timer
- `webhook_not_handled_total{event}` — event received with no handler
- `webhook_errors_total{reason}` — invalid payloads, missing body/event

## Adding your own metrics

Use the shared registry:

```
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import com.clockify.addon.sdk.metrics.MetricsHandler;

Counter.builder("rules_actions_matched_total")
    .tag("action", "add_tag")
    .register(MetricsHandler.registry())
    .increment();

Timer.Sample sample = Timer.start(MetricsHandler.registry());
try {
  // ... your logic
} finally {
  Timer.builder("rules_processing_seconds")
      .tag("path", "/webhook")
      .register(MetricsHandler.registry())
      .record(sample.stop(MetricsHandler.registry().timer("noop")));
}
```

## Scraping

- Scrape `/metrics` with Prometheus. Add a job pointed at your add‑on’s public URL.
- Do not list `/metrics` in the manifest; it’s an operational endpoint.

## Dashboards

- Useful charts: request rate by event, request latency by event, not‑handled events, and error rates.
- Add counters for your domain events (e.g., rules applied, tags created) and chart them.

