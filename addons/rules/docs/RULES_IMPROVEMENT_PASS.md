# Rules Improvement Pass (2025-11-16)

## Changes by Theme

### Idempotency visibility
- `WebhookIdempotencyCache` now tracks the active backend (`database` vs `in_memory`), logs it at startup, exposes `backendLabel()` for other components, and reports it through a dedicated Prometheus gauge `rules_webhook_idempotency_backend{backend=...}`.
- `RulesApp.configureDedupStore` remains deterministic and is now unit-tested alongside `WebhookIdempotencyCache`/`RulesMetrics` to ensure DB-backed dedupe is selected when credentials are present.
- README / observability docs explain how to confirm the backend via logs, metrics, or the new debug endpoint.

### Async executor behavior
- `scheduleAsyncProcessing` records `rules_async_backlog_total` outcomes for `submitted`, `rejected`, and `fallback` so dashboards can differentiate normal flow vs saturation.
- Added success + rejection coverage in `WebhookHandlersTest` to assert metrics/logging when the executor is saturated and when async work is accepted, plus docs describing the new outcomes.

### Dev flags & configuration safety
- Added integration + unit tests (`WebhookHandlersTest.skipSignatureFlagIgnoredOutsideDev`, `RuntimeFlagsTest`) proving `ADDON_SKIP_SIGNATURE_VERIFY` is ignored whenever `ENV` isn’t dev/local.
- Status/README guidance now highlights that dev toggles remain locked down in prod.

### Config introspection / debug tooling
- Introduced `/debug/config` (dev-only) via `DebugConfigController`: reports environment label, dedupe backend, token store mode, DB wiring, JWT mode, and live runtime flags without leaking secrets.
- Hooked `RulesApp.registerDebugEndpoint` so the route is only registered in dev variants, and verified output via `RulesAppTest` + `DebugConfigControllerTest`.
- Documented the endpoint in README and `docs/RULES_OBSERVABILITY.md`.

### Small refactors
- Extracted `RulesApp.registerDebugEndpoint` + helper `isDevEnvironment`, trimming the main bootstrap method and making endpoint registration testable.
- Consolidated `WorkspaceExplorerController` error-handling switches into reusable helpers for more readable failure paths.

### CI / verification
- No workflow changes were required; `mvn -q -pl addons/rules -am test -DtrimStackTrace=false` and `mvn -q clean verify -DtrimStackTrace=false` both pass locally after the new gauges/endpoints/tests.

## Known Limitations & Follow-ups
- Async handling still relies on upstream retries—consider a durable queue or back-pressure feedback channel if webhook load grows beyond the executor’s capacity.
- `/debug/config` is dev-only by design; staging/prod operators still rely on logs + metrics for config introspection.
- Idempotency persistence remains TTL based; multi-day dedupe windows or per-workspace overrides may be needed for very large tenants.

## Suggested PR
- **Title:** `feat: expose dedupe backend + async backlog signals`
- **Description:**
  - Track webhook idempotency backend (log/gauge/debug endpoint) and document how ops can confirm DB vs in-memory modes.
  - Make async executor backlog observable (`submitted|rejected|fallback` metrics) with tests plus docs.
  - Lock down dev flags via new tests and add the `/debug/config` dev endpoint for safe introspection.
  - Include light refactors (RulesApp/Explorer) and full Maven test proof: `mvn -q -pl addons/rules -am test` & `mvn -q clean verify`.
