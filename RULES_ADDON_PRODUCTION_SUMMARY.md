# Rules Add-on — Production Summary

The hardened `rules` module is the canonical production example in this repository. It evaluates Clockify webhooks against declarative rules, persists state via PostgreSQL/MySQL, and exposes health/metrics endpoints for orchestration. Configuration flows exclusively through `RulesConfiguration`, so controllers and filters never reach into `System.getenv`.

## Local workflow (dev/staging)
1. **Build once**  
   ```bash
   mvn -q -pl addons/rules -am package -DskipTests
   ```
2. **Copy the env template** — `cp .env.rules.example .env.rules`, then reference the profile-specific overlays (`.env.rules.dev.example`, `.env.rules.staging.example`, `.env.rules.prod.example`) to ensure the right ENV label, JWT bootstrap, and dev-only toggles are in place for each environment.
3. **Run with Make (loads `.env.rules`)** — `make dev-rules` starts the fat JAR with all middleware (SecurityHeadersFilter, SensitiveHeaderFilter, RateLimiter/CORS if configured). Toggle mutations via `RULES_APPLY_CHANGES=true` only when exercising real Clockify data.
4. **Expose via ngrok and reinstall** — `ngrok http 8080`, restart with `ADDON_BASE_URL=https://<domain>/rules make run-rules`, then reinstall using `https://<domain>/rules/manifest.json`.
5. **Optional Docker smoke** — `ADDON_BASE_URL=https://<domain>/rules make docker-build TEMPLATE=rules` builds the multi-stage image; `make docker-run TEMPLATE=rules` runs it locally with the same env vars.

DEV-ONLY helpers (`CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, `ADDON_SKIP_SIGNATURE_VERIFY`) are honored only when `ENV=dev`; production/staging ignores them via `RuntimeFlags`. Likewise, `/api/**` endpoints now require `Authorization: Bearer <auth_token>` headers unless `ENV=dev`, so operators can no longer spoof `workspaceId` via query strings.

## Production runtime (Docker or fat JAR)
- **Build container**  
  ```bash
  DOCKER_IMAGE=registry.example.com/rules:latest \
  ADDON_BASE_URL=https://rules.example.com \
  make docker-build TEMPLATE=rules
  ```
  The Make target passes `ADDON_DIR=addons/rules` and `DEFAULT_BASE_URL` into the Dockerfile so the packaged fat JAR lands at `/opt/addon/app.jar` running as the non-root `addon` user.

- **Run container (sample)**  
  ```bash
  docker run -d --name rules \
    -e ADDON_PORT=8080 \
    -e ADDON_BASE_URL=https://rules.example.com \
    -e CLOCKIFY_JWT_JWKS_URI=https://clockify.example.com/.well-known/jwks.json \
    -e CLOCKIFY_JWT_EXPECT_ISS=clockify \
    -e CLOCKIFY_JWT_EXPECT_AUD=rules \
    -e RULES_WEBHOOK_DEDUP_SECONDS=600 \
    -e ENABLE_DB_TOKEN_STORE=true \
    -e DB_URL=jdbc:postgresql://db:5432/clockify_addons \
    -e DB_USER=addons \
    -e DB_PASSWORD=*** \
    -e RULES_DB_URL=jdbc:postgresql://db:5432/rules \
    -e RULES_DB_USERNAME=rules \
    -e RULES_DB_PASSWORD=*** \
    -p 8080:8080 \
    registry.example.com/rules:latest
  ```
For bare-metal or VM deployments, run the same fat JAR directly: `ADDON_BASE_URL=https://rules.example.com java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar`.

- **Mandatory envs/secrets**  
  - Base networking: `ADDON_PORT`, `ADDON_BASE_URL`, `CLOCKIFY_API_BASE_URL`
  - JWT bootstrap: JWKS URI or PEM map, plus `CLOCKIFY_JWT_EXPECT_ISS`/`AUD` (missing config causes startup failure outside dev)
  - Persistence: `ENABLE_DB_TOKEN_STORE=true`, `DB_*`, and `RULES_DB_*` when rules storage is also backing onto SQL
  - Security/middleware: `ADDON_FRAME_ANCESTORS`, `ADDON_RATE_LIMIT`/`ADDON_LIMIT_BY`, optional `ADDON_CORS_*`, `ADDON_REQUEST_LOGGING`
  - Never set `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, or `ADDON_SKIP_SIGNATURE_VERIFY` outside development.
  - Webhook dedupe: `RULES_WEBHOOK_DEDUP_SECONDS` between **60 seconds and 24 hours**; values outside that range cause startup failures (and are clamped/logged defensively at runtime).

## Plan & scope profile
- **Plan** — `minimalSubscriptionPlan("PRO")`. Explorer tabs (invoices, PTO) and automation features only exist on Pro, so production tenants must meet that bar to avoid partial UIs.
- **Scopes** — Runtime manifest includes `TIME_ENTRY_{READ,WRITE}`, `TAG_{READ,WRITE}`, `PROJECT_{READ,WRITE}`, `CLIENT_{READ,WRITE}`, `TASK_{READ,WRITE}`, and `WORKSPACE_READ`. CRUD helpers behind `/api/projects`, `/api/clients`, `/api/tasks`, and explorer routes depend on these scopes; they stay server-side so the browser never holds installation tokens.

## Health, readiness, and metrics
- `GET /rules/health` — Jetty + storage liveness. Includes database checks when `RULES_DB_*` or `DB_*` are configured and a `DatabaseHealthCheck` for the pooled token store.
- `GET /rules/ready` — `ReadinessHandler` calls `RulesStore.getAll("health-probe")` and `tokenStore.count()`. Returns HTTP 503 with `"status":"DEGRADED"` when either dependency is down, so point Kubernetes readiness probes here.
- `GET /rules/metrics` — Prometheus exposition (request counters, rule evaluations, webhook dedupe stats, executor latencies). Scrape on the same base URL as the app. See [`docs/RULES_OBSERVABILITY.md`](docs/RULES_OBSERVABILITY.md) for alert suggestions and metric details.

## Workspace explorer updates
- The `/settings` iframe now drives the entire explorer experience: a left-hand nav keeps admins oriented, every dataset (users, projects, clients, tags, **tasks**, time entries, invoices, time-off, webhooks, custom fields) gets a toolbar (search, filters, page-size selector, reset), and presets are stored per section in `localStorage` so each browser can maintain multiple saved filter bundles.
- `/api/rules/explorer/**` remains the only data plane—the browser never talks to Clockify directly. The new tasks endpoint walks project + task GET pages server-side (capped at 5K items per snapshot) while webhook `event`/`enabled`/`search` filters are applied client-side so pagination stats describe the filtered subset.
- `/api/rules/explorer/snapshot` powers an on-demand “fetch everything” pass (multiple paginated GETs). The UI exposes dataset toggles (users through invoices **plus tasks**), configurable limits (5–100 rows × 1–20 pages), and a selectable time-entry lookback (UI presets 7/30/90 days, clamped 1–90 via `timeEntryLookbackDays`). Inline progress cards keep operators informed, each dataset now has an expandable JSON preview, and the download button only enables once a run completes.
- Time entry rows expose both a “Create rule from this” link (deep-linking into `/simple?ruleName=...&prefillDescription=...&prefillProjectId=...&prefillTagIds=...`) and a copy-to-clipboard rule seed for admins who want to start from raw JSON. The builder now shows a prefill banner and highlights injected conditions so reviewers always know what arrived from the explorer.

## Explorer + builder quick reference
- **Datasets** — Users, projects, clients, tags, **tasks**, time entries, time off (requests/policies/balances), webhooks, custom fields, invoices, plus the aggregated overview endpoint. All traffic stays under `/api/rules/explorer/**`.
- **Snapshot limits** — `pageSizePerDataset` is clamped between 5–100 rows, `maxPagesPerDataset` between 1–20 pages, and `timeEntryLookbackDays` between 1–90 days. Presets (7/30/90) sit in the UI, but the controller enforces the clamps even if a caller tampers with the query parameters.
- **Builder prefill** — `/simple` honors `ruleName`, `prefillDescription`, `prefillProjectId`, and `prefillTagIds`. Deep links show a blue banner that lists injected fields and highlight each affected form control so reviewers can audit exactly what came from the explorer seed.
- **Presets** — Every dataset toolbar persists filters, search text, and pagination preferences to `localStorage`, with save/load/delete actions per dataset. Use this during QA to simulate admin review workflows.

## Webhook idempotency & filters
- `WebhookIdempotencyCache` stores `(workspaceId, eventType, payloadId)` tuples for `RULES_WEBHOOK_DEDUP_SECONDS` (60s–24h). The cache is per-pod and in-memory; duplicates are only caught on the same node, so treat the behavior as “at-most-once per JVM”. Duplicates short-circuit webhook handlers and increment `rules_webhook_dedup_hits_total`, while first-seen payloads increment `rules_webhook_dedup_misses_total`. For cross-node dedupe, back the cache with a persistent store.
- `SecurityHeadersFilter` emits CSP + security headers and shares a per-request nonce with the settings/IFTTT controllers.
- `SensitiveHeaderFilter` wraps the servlet request to redact `Authorization`, `X-Addon-Token`, `Clockify-Signature`, and cookies before any logging occurs.
- Workspace iframe JWTs flow through the SDK `JwtBootstrapConfig` (loaded by `RulesConfiguration`) → `JwtVerifier` → `WorkspaceContextFilter` / `PlatformAuthFilter`, so no controller calls `System.getenv`.

## Known operational limits
- **Task scanning** — Workspace-level task listings walk projects (`50` per page) × tasks (`200` per page) and cap the scan at 5,000 tasks to keep snapshots bounded. When the cap hits, refreshes log the workspace, observed totals, and increment `rules_workspace_cache_truncated_total{dataset="tasks"}` so dashboards can flag truncated caches; operators should narrow filters before exporting again.
- **Webhook dedupe** — Idempotency cache is JVM-local plus TTL-based. Track `rules_webhook_dedup_hits_total` vs `rules_webhook_dedup_misses_total` to understand retry ratios and consider an external store if you deploy multiple replicas or need >24h dedupe windows.
- **Async backlog** — If the async executor saturates, webhook requests fall back to synchronous execution and increment `rules_async_backlog_total{outcome="fallback"}`. Sustained growth here signals the need to scale workers or revisit rule complexity.

## openapi_call safety
- Only `GET` and `POST` methods are accepted for `openapi_call` actions; other verbs fail validation.
- Paths must start with `/workspaces/{workspaceId}/...` to prevent cross-tenant calls.
- Use a sandbox workspace (and prefer read-only endpoints) when testing `openapi_call` rules; production alerts should include IDs from the structured logs to correlate who invoked which path.

For the authoritative runbook (env-by-env), see `addons/rules/README.md` and the new [`RULES_PROD_LAUNCH_CHECKLIST.md`](RULES_PROD_LAUNCH_CHECKLIST.md). Keep `PRODUCTION_CHECKLIST.md` and [`docs/RULES_DB_SCHEMA.md`](docs/RULES_DB_SCHEMA.md) in sync with any schema or ops changes.
