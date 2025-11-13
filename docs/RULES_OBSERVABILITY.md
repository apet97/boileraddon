# Rules Add-on Observability Guide

Operators can monitor the Rules add-on via dedicated health endpoints, Prometheus metrics, and structured logs. This document summarizes what each signal means, how to interpret it, and what to alert on.

## Endpoints

### `/health`
- **Purpose:** Liveness check. Confirms the JVM is serving requests and (when configured) can open a JDBC connection to the rules/token stores.
- **Response:** `200 {"status":"healthy"}` when everything is reachable; `503` otherwise.
- **Usage:** Wire to container liveness probes. Failing `/health` should restart the pod.

### `/ready`
- **Purpose:** Readiness check implemented by `ReadinessHandler`.
- **Checks:** Executes `rulesStore.getAll("health-probe")` and `tokenStore.count()` (when DB token store is enabled). Missing stores are reported as `"SKIPPED"`.
- **Response:** `200 {"status":"READY","checks":{"rulesStore":"UP","tokenStore":"UP"}}` when both dependencies are reachable; `503` with `"status":"DEGRADED"` if either fails.
- **Usage:** Kubernetes/Docker readiness probe. Alert if readiness flaps or stays degraded &gt;1 minute.

### `/metrics`
- **Purpose:** Prometheus exposition endpoint via `MetricsHandler`.
- **Content:** Plain text metrics with `rules_*` prefixes (see below). Scrape with Prometheus (or compatible) every 15–30s.
- **Security:** Same base URL as the add-on; protect behind your ingress auth or private network.

## Key Metrics

| Metric | Type | Meaning / Guidance |
| --- | --- | --- |
| `rules_webhook_latency_ms{event, outcome}` | Timer (histogram) | End-to-end latency for webhook handlers. Alert if P95 &gt; a few seconds or outcome=`error` spikes. |
| `rules_evaluated_total{event}` | Counter | Number of enabled rules evaluated for each event. High counts without matching rules may indicate misconfigured automations. |
| `rules_matched_total{event}` | Counter | Number of rules whose conditions matched. Use alongside `rules_evaluated_total` to estimate match rate. |
| `rules_actions_total{type, result}` | Counter | Action execution attempts (e.g., `add_tag`, `set_description`) and whether they succeeded. Alert on `result="failure"` spikes. |
| `rules_webhook_dedup_hits_total{event}` | Counter | Count of duplicate webhooks ignored by `WebhookIdempotencyCache`. A sudden spike usually means upstream retry storms; verify `RULES_WEBHOOK_DEDUP_SECONDS` and ingress health. |

> ℹ️ **Custom metrics:** The SDK also exposes Jetty/JVM metrics (heap, threads) via Micrometer. Scrape them from the same `/metrics` endpoint to round out dashboards.

## Logging

- **Structure:** All requests flow through `DiagnosticContextFilter` / `LoggingContext`, which attaches `requestId`, `workspaceId`, and (when available) `userId`. These fields appear in every log line.
- **Redaction:** `SensitiveHeaderFilter` strips `Authorization`, `Clockify-Signature`, cookies, and other secrets before any loggers or exception handlers touch them.
- **Notable log lines:**
  - `Rules Add-on starting | ... env=... applyChanges=...` &mdash; startup summary, useful for change tracking.
  - `PlatformAuthFilter...` &mdash; indicates bearer enforcement is active on `/api/**` and `/status`.
  - `Duplicate dynamic webhook suppressed` &mdash; idempotency hit; expect a corresponding increase in `rules_webhook_dedup_hits_total`.
  - `Rules store readiness failed` / `Token store readiness failed` &mdash; emitted from `/ready` when dependencies throw.
- **Levels:** INFO for lifecycle events, DEBUG for expected races (e.g., accepting previous token during rotation), WARN for recoverable issues, ERROR only when a request cannot be fulfilled.

## Alerting Suggestions

1. **Readiness degradation:** `/ready` fails for more than 60 seconds or toggles repeatedly → likely DB outage or credential issue.
2. **HTTP 5xx / webhook errors:** Outcome label `error` or `partial` dominates `rules_webhook_latency_ms`. Combine with log sampling to pinpoint failing actions.
3. **Duplicate storm:** `rules_webhook_dedup_hits_total` increases sharply → investigate upstream retries or incorrect webhook retries.
4. **Token store failures:** Look for WARN/ERROR lines from `RotatingTokenStore` or `PooledDatabaseTokenStore`. Alert if they recur to avoid auth outages.
5. **Metrics scrape gaps:** Alert when Prometheus stops scraping `/metrics` (no data for &gt;5 minutes) to catch networking issues early.

Pair these alerts with dashboards that plot request rates, dedupe rate, action failures, and readiness history. For full runbooks (how to fix each alert), embed links back to this repository’s docs so on-call engineers know which environment variables and commands to check.
