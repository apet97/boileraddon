# Status Notes

## Module map

### addon-sdk (`addons/addon-sdk`)
- **Entry points:** `ClockifyAddon` + `AddonServlet` wire endpoint routing, `EmbeddedServer` boots Jetty.
- **Security / middleware:** `middleware/*` (SecurityHeadersFilter, SensitiveHeaderFilter, PlatformAuthFilter + ScopedPlatformAuthFilter, WorkspaceContextFilter, RateLimiter, CorsFilter, RequestLoggingFilter, DiagnosticContextFilter).
- **Security helpers:** `security/*` (TokenStore + Database/Pooled implementations, WebhookSignatureValidator, JwtVerifier/JwtBootstrapLoader, SecretsPolicy, Platform auth filters).
- **Observability:** `metrics/MetricsHandler` (Micrometer registry + Prometheus scrape), `health/*` (HealthCheck + DatabaseHealthCheck), logging helpers in `logging/*`.

### Rules add-on (`addons/rules`)
- **Entry point:** `RulesApp` wires manifest, controllers (`SettingsController`, `SimpleSettingsController`, `ManifestController`, `IftttController`), health/readiness handlers, metrics, lifecycle + webhook handlers.
- **Security / middleware:** WorkspaceContextFilter + ScopedPlatformAuthFilter (protects `/api/**` + `/status`), SecurityHeadersFilter, SensitiveHeaderFilter, optional RateLimiter, CorsFilter, RequestLoggingFilter. `RuntimeFlags` centralized dev flag gating.
- **Webhook controllers:** `WebhookHandlers`, `DynamicWebhookHandlers`, `LifecycleHandlers`, debug/test endpoints, explorer APIs under `api/explorer`.
- **Storage & idempotency:** `DatabaseRulesStore` vs in-memory fallback, `WebhookIdempotencyCache` with `DatabaseWebhookIdempotencyStore` or `InMemoryWebhookIdempotencyStore`, `RuleCache`, `WorkspaceCache`.
- **Metrics & health:** `RulesMetrics`, `/metrics` Prom endpoint, `/health`, `/ready`, additional DatabaseHealthCheck and `ReadinessHandler`, plus dev-only `/debug/config`.

### Auto-Tag Assistant (`addons/auto-tag-assistant`)
- **Entry point:** `AutoTagAssistantApp` configures manifest, lifecycle/webhook handlers, `/settings`, `/status`, `/metrics`, `/health`.
- **Security:** WorkspaceContextFilter + ScopedPlatformAuthFilter (protects `/status` + `/api`), SensitiveHeaderFilter, SecurityHeadersFilter, optional rate limiting, CORS, request logging. JWT bootstrap enforced unless `ENV=dev`.
- **Webhook/controllers:** `SettingsController`, `WebhookHandlers`, `LifecycleHandlers`, `ClockifyApiClient`.
- **Storage:** Shared TokenStore with optional database-backed persistence if DB env vars supplied; no idempotency store.
- **Observability:** Health check includes optional DB probe, `/metrics` uses shared MetricsHandler; status endpoint exposes workspace/token presence.

### Overtime add-on (`addons/overtime`)
- **Entry point:** `OvertimeApp` registers manifest, settings endpoints (`/settings`, `/api/settings`), `/status`, `/metrics`, `/health`, lifecycle + webhook handlers.
- **Security:** Similar filters as Auto-Tag (WorkspaceContextFilter, ScopedPlatformAuthFilter for `/status` + `/api`), SecurityHeadersFilter, SensitiveHeaderFilter, optional RateLimiter/Cors/RequestLogging.
- **Storage:** `SettingsStore` (in-memory JSON), TokenStore integration for workspace tokens; no DB/idempotency yet.
- **Observability:** `/health`, `/status`, `/metrics` endpoints, but `/metrics` currently unauthenticated.

### Template add-on (`addons/_template-addon`)
- **Entry point:** `TemplateAddonApp` (manifest, `/health`, `/status`, `/api/test`, lifecycle/webhook handlers).
- **Security:** WorkspaceContextFilter + ScopedPlatformAuthFilter (same `/status` + `/api` coverage), SecurityHeadersFilter, SensitiveHeaderFilter.
- **Controllers:** `SettingsController`, `ManifestController`, `TestController` (dry-run helper).
- **Storage/observability:** TokenStore used for status check; no persistence by default; `/health` basic OK response only.

## Build / test runs (Phase 0)
- `mvn -q -pl addons/rules -am test -DtrimStackTrace=false` → **Initial FAIL**: `DebugConfigControllerTest` asserted `skipSignatureVerify=false` but saw host env override. **Current status:** PASS after hardening runtime flag overrides and rerunning (latest @ 18:27 local).
- `mvn -q clean verify -DtrimStackTrace=false` → **FAIL** before fixes (blocked by the same rules test). Needs rerun once repo-wide tasks settle.
- `mvn -q -Pci,security-scan verify -DtrimStackTrace=false` → **FAIL** before fixes (same root cause). Pending re-run after all phases.

## Issue backlog (initial)
1. **Idempotency backend clarity:** Startup logs mention backend choice, but we still need stronger runtime indicators + tests verifying DB preference when `RULES_DB_*` present (per phase requirements).
2. **Async executor overload handling:** `RulesMetrics.recordAsyncBacklog` exists, but fallback logs/tests around queue rejection are thin; need coverage ensuring overload doesn't drop work silently.
3. **Other add-ons security consistency:** Auto-Tag, Overtime, Template expose `/metrics` unauthenticated and have inconsistent status endpoints; PlatformAuthFilter coverage + docs must match Rules standard.
4. **CI workflows/docs audit pending:** Need to inspect `.github/workflows` + README/doc freshness once code fixes land.

_Last updated: after Phase 3 dev-config endpoints + 18:37 module test run._
