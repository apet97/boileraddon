# Rules Add-on  
[AI START HERE](../../docs/AI_START_HERE.md)

![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)
[![Validate](https://github.com/apet97/boileraddon/actions/workflows/validate.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/validate.yml)
[![Docs](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml)
[![Coverage](https://apet97.github.io/boileraddon/coverage/badge.svg)](https://apet97.github.io/boileraddon/coverage/)
[![Docs Index](https://img.shields.io/badge/Docs-Index-blue)](../../docs/README.md)

Automation add-on that applies rule-driven actions to time entries (e.g., tagging entries that match conditions). Includes lifecycle handlers, a settings page, and webhook processing.

See also: [Manifest Recipes](../../docs/MANIFEST_RECIPES.md) and [Permissions Matrix](../../docs/PERMISSIONS_MATRIX.md) for choosing plan/scopes and wiring routes.

## Workspace Explorer UI

`/settings` now renders an interactive workspace explorer that stays entirely within the secured add-on surface. The iframe:

- Bootstraps via the signed `auth_token` JWT and never calls Clockify APIs directly from the browser.
- Surfaces the active environment, base URL, signature mode, and resolved workspace so reviewers can immediately confirm which tenant they are inspecting.
- Uses a left-hand navigation rail plus dataset-aware toolbars (search, filters, page-size selector, reset) for every dataset (users, projects, clients, tags, **tasks**, time entries, invoices, time-off, webhooks, custom fields).
- Streams data from backend routes under `/api/rules/explorer/**`, which wrap the Clockify OpenAPI GET endpoints using the installation token—including the new tasks feed that walks project/task GET pages without any browser-side secrets.
- Adds per-section presets that live in `localStorage`—each browser can save multiple named filters per dataset and reload them later.
- Upgrades the snapshot workspace with dataset toggles (users through invoices **plus tasks**), configurable page size/max pages (5–100 × 1–20), and a selectable time-entry lookback (UI presets 7/30/90 days, clamped 1–90 via `timeEntryLookbackDays`). Progress is rendered inline, each dataset gets an expandable JSON preview, and the download button stays disabled until a run finishes cleanly.
- Provides contextual “Create rule from this” actions on time entries (with copy-to-clipboard rule seeds) and quick project/tag shortcuts into the simple builder via `ruleName`, `prefillDescription`, `prefillProjectId`, and `prefillTagIds` query params. When the builder opens via a deep link it now shows a prefill banner and highlights every condition it injected so reviewers can see exactly what came from the explorer.

### Explorer API (backend-only)

| Endpoint | Purpose | Supported query params |
| --- | --- | --- |
| `GET /api/rules/explorer/overview` | Aggregated counts + recent activity | `sampleSize` (default 5), `recentDays` (default 7) |
| `GET /api/rules/explorer/users` | Paginated workspace users | `page`, `pageSize`, `search`, `status` |
| `GET /api/rules/explorer/projects` | Paginated projects | `page`, `pageSize`, `search`, `archived`, `billable`, `clientId` |
| `GET /api/rules/explorer/clients` | Paginated clients | `page`, `pageSize`, `search`, `archived` |
| `GET /api/rules/explorer/tags` | Paginated tags | `page`, `pageSize`, `search`, `archived` |
| `GET /api/rules/explorer/tasks` | Workspace tasks gathered via project-scoped GET loops | `page`, `pageSize`, `search`, `projectId`, `clientId`, `archived` (`true\|false\|all`) |
| `GET /api/rules/explorer/time-entries` | Recent time entries (hydrated) | `page`, `pageSize`, `from`, `to`, `userId`, `projectId`, `tagIds` |
| `GET /api/rules/explorer/time-off` | PTO requests/policies/balances | `page`, `pageSize`, `view` (`requests\|policies\|balances`). Requests view: `status` (`PENDING\|APPROVED\|REJECTED\|ALL`), `userId`, `groupId`, `from`, `to`. Policies view: `status` (`ACTIVE\|ARCHIVED\|ALL`). Balances view: `policyId` or `userId` (required), `sort` (`USER\|POLICY\|USED\|BALANCE\|TOTAL`), `sortOrder` (`ASCENDING\|DESCENDING`). |
| `GET /api/rules/explorer/webhooks` | Workspace webhook inventory | `page`, `pageSize`, `type` (server-side), `event`, `enabled`, `search` (client-side filters on the fetched payload) |
| `GET /api/rules/explorer/custom-fields` | Custom field registry | `page`, `pageSize`, `search`, `status`, `entityType` |
| `GET /api/rules/explorer/invoices` | Invoice list | `page`, `pageSize`, `status` (CSV of `UNSENT\|SENT\|PAID\|PARTIALLY_PAID\|VOID\|OVERDUE`), `sort` (`ID\|CLIENT\|DUE_ON\|ISSUE_DATE\|AMOUNT\|BALANCE`), `sortOrder` (`ASCENDING\|DESCENDING`), `clientId` |
| `GET /api/rules/explorer/snapshot` | On-demand aggregate snapshot | `include{Users|Projects|Clients|Tags|Tasks|TimeEntries|TimeOff|Webhooks|CustomFields|Invoices}`, `pageSizePerDataset` (5–100), `maxPagesPerDataset` (1–20) |

> 🔁 **Archived tasks:** The `archived` query parameter on `/api/rules/explorer/tasks` now talks directly to Clockify’s per-project task endpoints, so selecting `true` or `false` returns only the requested state. Use `archived=all` to lift the filter entirely—even when walking the workspace-wide project scan.

Snapshots clamp each dataset to the specified `pageSizePerDataset`/`maxPagesPerDataset` bounds (defaults 25 × 3) and stop early if pagination stops advancing. Time entries default to a 30‑day lookback but honor the `timeEntryLookbackDays` input (clamped 1–90) so you can zoom into smaller ranges before exporting. Time-off snapshots reuse the active `policies` view to avoid hammering PTO APIs, and the tasks fetcher walks project + task GET pages with a capped scan (5K items) so “fetch everything” stays safe even on large workspaces. Webhook `event`/`enabled`/`search` filters are applied after fetching the workspace inventory, and the pagination metadata now reflects the filtered subset so `hasMore`/`totalItems` stay accurate. The UI renders per-dataset progress, summary rows, expandable JSON previews, and only enables the download button after a run completes.

All requests inherit workspace context from `PlatformAuthFilter`. When running in local dev mode you can still provide `workspaceId` as a query parameter, but production traffic **must** rely on the signed JWT headers.

### QA on developer.clockify.me

1. Build and run the Rules add-on locally (`make dev-rules`) or via Docker, expose it through ngrok, and install the manifest under **Admin → Add-ons → Install Custom Add-on** with the HTTPS base URL.
2. Open the Rules sidebar inside the Clockify workspace. Confirm the hero badges match your `ENV` label, base URL, and signature mode.
3. Click through every dataset on the left nav (users → custom fields). For each section—including the new Tasks panel—exercise the toolbar (search, filters, page-size selector), save at least one preset, reload the iframe, and ensure the presets remain available.
4. Run a snapshot with small limits (`pageSizePerDataset=10`, `maxPagesPerDataset=1`) while only selecting one or two datasets. Watch the inline progress list update, expand the JSON preview cards, then rerun with more datasets and a different lookback (7/30/90 days). Use the download button and inspect the JSON to ensure each requested dataset is present (the button stays disabled until a run completes).
5. From the Time Entries tab, pick any entry with a project/tags and click “Create rule from this.” The simple builder should open with the name, description, project, and tags pre-filled and show the new “Prefilled from explorer” banner. Use the “Copy rule seed” action if you prefer to start from raw JSON.
6. Confirm snapshot and explorer requests succeed only when the Clockify iframe passes a valid `auth_token` (try opening the iframe URL directly in a new tab to verify `PlatformAuthFilter` rejects unauthenticated calls).

## Quick Start  

```bash
# 1) Build once (fat JAR lives under addons/rules/target/)
mvn -q -pl addons/rules -am package -DskipTests

# 2) Copy the env template (RulesConfiguration reads this automatically via make dev-rules)
cp .env.rules.example .env.rules

# 3) Start the add-on (loads .env.rules, including JWT bootstrap + DB config if present)
make dev-rules

# 4) Expose via ngrok in a separate terminal, then restart with the HTTPS base URL
ngrok http 8080
ADDON_BASE_URL=https://YOUR.ngrok-free.app/rules make run-rules

# 5) Install using: https://YOUR.ngrok-free.app/rules/manifest.json
```

Docker workflow:
```bash
# Build-only: multi-stage Dockerfile with ADDON_DIR=addons/rules baked in
ADDON_BASE_URL=https://YOUR.ngrok-free.app/rules make docker-build TEMPLATE=rules

# Build and run (for local verification)
ADDON_BASE_URL=https://YOUR.ngrok-free.app/rules make docker-run TEMPLATE=rules
```

## Configuration

`RulesConfiguration.fromEnvironment()` is the single source of truth for configuration. It reads `.env.rules` (when using `make dev-rules`) or real env vars, validates everything via `ConfigValidator`, and exposes typed records to the rest of the add-on. Use `.env.rules.example` as a starting point.

### Configuration profiles

- **Local dev (`.env.rules.dev.example`)** — `ENV=dev`, `RULES_APPLY_CHANGES=false`, and optional helpers such as `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, and `ADDON_SKIP_SIGNATURE_VERIFY`. `RuntimeFlags` enforces that these helpers are ignored automatically when `ENV!=dev`.
- **Staging (`.env.rules.staging.example`)** — `ENV=staging`, real JWT bootstrap values (JWKS, PEM map, or individual PEM), no dev helpers. Start with `RULES_APPLY_CHANGES=false`, then flip to `true` only after verifying automations end-to-end.
- **Production (`.env.rules.prod.example`)** — `ENV=prod`, `RULES_APPLY_CHANGES=true`, persistent token store enabled, and monitoring/alerting env vars configured. Stage/staging envs should mirror this file as closely as possible.

Each example file includes all required keys plus inline notes linking back to this README and the production launch checklist.

### Core runtime

| Variable | Purpose | Default / Notes |
| --- | --- | --- |
| `ADDON_BASE_URL` | External URL used in the runtime manifest, redirects, and iframe embeds. | `http://localhost:8080/rules` |
| `ADDON_PORT` | Embedded Jetty listener. | `8080` |
| `CLOCKIFY_API_BASE_URL` | Base URL for the upstream Clockify API client. | `https://api.clockify.me/api` |
| `ENV` | Labels the environment (`dev`, `prod`, etc.); used by `RuntimeFlags` to guard risky toggles. | `prod` |
| `RULES_APPLY_CHANGES` | Enables mutations instead of dry-run logging. Backed by `RuntimeFlags.applyChangesEnabled()` so you can flip it without reloading config. | `false` |
| `RULES_WEBHOOK_DEDUP_SECONDS` | TTL (seconds) for `WebhookIdempotencyCache`. | `600` (must be between 60 seconds and 24 hours) |

### Persistence (rules + token store)

| Variable(s) | Purpose | Default / Notes |
| --- | --- | --- |
| `RULES_DB_URL`, `RULES_DB_USERNAME`, `RULES_DB_PASSWORD` | JDBC settings for `DatabaseRulesStore`. | Optional; falls back to `DB_*` if unset. |
| `DB_URL`, `DB_USER`/`DB_USERNAME`, `DB_PASSWORD` | Shared DB config for the pooled `TokenStore` (and as fallback for rules storage). | Required when enabling persistence. |
| `ENABLE_DB_TOKEN_STORE` | Forces persistent token storage on/off. | Default: `false` unless `ENV=prod`; fails fast if DB credentials are missing. |

### JWT bootstrap

| Variable(s) | Purpose | Notes |
| --- | --- | --- |
| `CLOCKIFY_JWT_PUBLIC_KEY`, `CLOCKIFY_JWT_PUBLIC_KEY_PEM` | Single PEM key used to verify settings iframe JWTs (and PlatformAuthFilter if wired). | Provide either `CLOCKIFY_JWT_PUBLIC_KEY` or `_PEM`; **required in non-dev envs**. |
| `CLOCKIFY_JWT_PUBLIC_KEY_MAP` | JSON map of `{kid: pem}` for key rotation. | Pair with `CLOCKIFY_JWT_DEFAULT_KID` to define a fallback. |
| `CLOCKIFY_JWT_JWKS_URI` | JWKS endpoint for self-serve key rotation. | Preferred in production; overrides PEM inputs when set. |
| `CLOCKIFY_JWT_DEFAULT_KID` | Default key ID to fall back to when the JWT header omits `kid`. | Optional. |
| `CLOCKIFY_JWT_EXPECT_ISS`, `CLOCKIFY_JWT_EXPECT_AUD` | Issuer/audience claims enforced by `JwtVerifier.Constraints`. | Defaults: issuer `clockify`, audience `rules` (the addon key). |
| `CLOCKIFY_JWT_LEEWAY_SECONDS` | Allowed clock skew when validating temporal claims. | `60` seconds. |

SDK-level `JwtBootstrapConfig` feeds all of the above into `JwtVerifier` (loaded by `RulesConfiguration`). JWKS > PEM map > single PEM is enforced explicitly, and extra env vars are logged then ignored so there is never ambiguity about which key source is in use. Non-dev environments fail fast (with actionable error messages) unless at least one of the three key sources is configured. Dev environments may omit the keys entirely, but `/api/**` endpoints will run without bearer enforcement in that case.

Platform authorization flows through `ScopedPlatformAuthFilter` + `PlatformAuthFilter` once a verifier is configured; see “Security: JWT Verification” below for architecture details.

### Middleware & networking

| Variable | Purpose | Default / Notes |
| --- | --- | --- |
| `ADDON_FRAME_ANCESTORS` | Value injected into the CSP `frame-ancestors` directive. | `'self' https://*.clockify.me https://*.clockify.com https://developer.clockify.me` |
| `ADDON_RATE_LIMIT`, `ADDON_LIMIT_BY` | Enables `RateLimiter` per IP or workspace. | Disabled unless `ADDON_RATE_LIMIT` is set. |
| `ADDON_CORS_ORIGINS`, `ADDON_CORS_ALLOW_CREDENTIALS` | Enables `CorsFilter` with the provided comma-separated allowlist. | Disabled unless origins list is set. |
| `ADDON_REQUEST_LOGGING` | Enables scrubbed request logging. | `false`. |

### Dev-only helpers (never set in production)

| Variable | Purpose | Notes |
| --- | --- | --- |
| `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN` | Preload a workspace/token pair into the in-memory TokenStore for local smoke tests. | Parsed via `RulesConfiguration.LocalDevSecrets`, logged once, and ignored in prod. |
| `ADDON_SKIP_SIGNATURE_VERIFY` | Skips webhook signature verification. | Only honored when `ENV=dev`; blocked in other environments by `RuntimeFlags.skipSignatureVerification()`. |
| `ADDON_ACCEPT_JWT_SIGNATURE` | Allows Developer JWT (`Clockify-Signature`) headers instead of HMAC. | Requires `ENV=dev`; default is disabled so production always expects `clockify-webhook-signature`. |

Developer signatures: webhooks always include an HMAC header `clockify-webhook-signature` (and case variants). When you opt into `ADDON_ACCEPT_JWT_SIGNATURE=true` **and** run with `ENV=dev`, the SDK also accepts the JWT header `Clockify-Signature`. Keep HMAC verification enabled in every production/staging deployment.

## Security: JWT Verification

This addon implements JWT verification at the **addon level** rather than in the shared SDK. This architectural decision allows each addon to:

- **Customize verification constraints** (issuer, audience, allowed algorithms, clock skew) based on its specific security requirements
- **Manage keys independently** using addon-specific JWKS endpoints or environment-configured key maps
- **Evolve security policies** without requiring SDK updates across all addons

`JwtBootstrapConfig` builds the `JwtVerifier` instance on startup. Constraints (`expectedIssuer`, `expectedAudience`, `clockSkewSeconds`) come straight from env vars, while the expected subject is derived from the manifest key (`rules`). Key material is sourced in this priority order:

1. **JWKS (`CLOCKIFY_JWT_JWKS_URI`)** — best for production key rotation.
2. **PEM map (`CLOCKIFY_JWT_PUBLIC_KEY_MAP` + optional `CLOCKIFY_JWT_DEFAULT_KID`)** — supports multiple `kid` entries in a single env var.
3. **Single PEM (`CLOCKIFY_JWT_PUBLIC_KEY` or `_PEM`)** — simplest local setup.

The same verifier powers `WorkspaceContextFilter` (Settings iframe JWT), `PlatformAuthFilter` (portal-authenticated APIs), and any other component that needs to validate Clockify-issued tokens—controllers never touch `System.getenv` directly.

### Key Features

The `JwtVerifier` class (`src/main/java/com/example/rules/security/JwtVerifier.java`) provides:

1. **Strict kid handling**: When a JWT header includes `kid`, only that specific key is used—no fallback to default keys
2. **Algorithm intersection**: Enforces intersection of configured algorithms with a safe built-in set (`RS256`, `ES256`)
3. **Temporal validation**: Enforces `iat`, `nbf`, `exp` with configurable clock skew (default 60s) and max TTL (24h)
4. **Audience any-of matching**: Supports both string and array audience claims with any-of semantics
5. **JWKS integration**: Optional `JwksKeySource` for dynamic key rotation
6. **Environment configuration**: Configure via `CLOCKIFY_JWT_PUBLIC_KEY_MAP`, `CLOCKIFY_JWT_DEFAULT_KID`, `CLOCKIFY_JWT_EXPECT_AUD`, `CLOCKIFY_JWT_LEEWAY_SECONDS`

When you need bearer-style platform authentication (e.g., developer portal or custom UI), instantiate `PlatformAuthFilter` with the same verifier reference that `RulesConfiguration` produced. The `devFromEnv()` helper is intended for ad-hoc experiments only; production filters should never read secrets directly from `System.getenv`.

### Configuration Examples

**Single public key** (simple case):
```bash
export CLOCKIFY_JWT_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
```

**Multiple keys with rotation** (production):
```bash
export CLOCKIFY_JWT_PUBLIC_KEY_MAP='{"key-2024":"-----BEGIN PUBLIC KEY-----\n...","key-2025":"-----BEGIN PUBLIC KEY-----\n..."}'
export CLOCKIFY_JWT_DEFAULT_KID=key-2024
export CLOCKIFY_JWT_EXPECT_AUD=rules
export CLOCKIFY_JWT_LEEWAY_SECONDS=60
```

**When to use SDK-level vs Addon-level JWT verification**:
- **Addon-level** (current pattern): When addons have different trust domains, key management, or security policies
- **SDK-level** (future consideration): When all addons share identical JWT verification requirements and key infrastructure

## UI controllers, CSP nonce, and base URL injection

- `SettingsController`, `SimpleSettingsController`, and `IftttController` accept the resolved `baseUrl` via constructor parameters. That keeps redirects, iframe assets, and relative links consistent with the manifest without ever calling `System.getenv`.
- Each controller reads the CSP nonce stored in `SecurityHeadersFilter.CSP_NONCE_ATTR`. If the filter did not run (unit tests that exercise controllers directly), the controllers generate a fallback nonce before rendering HTML.
- When JWT bootstrap is configured, `WorkspaceContextFilter` runs ahead of `SecurityHeadersFilter`. It verifies the iframe JWT using the configured `JwtVerifier`, attaches workspace/user attributes to the `HttpServletRequest`, and lets controllers personalize responses without decoding JWTs on their own.
- The settings UI automatically forwards the Clockify-provided `auth_token` as an `Authorization: Bearer` header on every `/api/**` call so `PlatformAuthFilter` can enforce workspace scoping. In dev you may still opt into fallback via `ENV=dev`, but production requests must include a valid bearer token.

## HTTP filters & logging hygiene

- **SecurityHeadersFilter** sets security headers (`Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `Cache-Control`) and injects the CSP nonce that UI controllers consume. Override `ADDON_FRAME_ANCESTORS` to embed outside the Clockify defaults.
- **PlatformAuthFilter** now guards `/api/**` and `/status`, rejecting requests that lack a valid `Authorization: Bearer <auth_token>` header. Tokens must include both `installation_id` and `workspace_id` claims or the request is rejected with 403. `RequestContext.resolveWorkspaceId` only falls back to `workspaceId` query params in `ENV=dev`, so production callers cannot spoof another workspace by tweaking the URL.
- **SensitiveHeaderFilter** redacts `Authorization`, `X-Addon-Token`, `X-Addon-Lifecycle-Token`, `Clockify-Signature`, `Cookie`, and `Set-Cookie` before any logging occurs so request logs never leak installation tokens.
- **Optional filters** (RateLimiter, CorsFilter, RequestLoggingFilter) are attached only when the corresponding env vars are set in `RulesConfiguration`. This avoids divergent behavior between local dev and production.

### Persistent Token Storage with DatabaseTokenStore

For production deployments, configure **DatabaseTokenStore** to persist workspace tokens across service restarts:

```bash
# Required environment variables
export DB_URL="jdbc:postgresql://localhost:5432/clockify_addons"
export DB_USER="postgres"
export DB_PASSWORD="your-secure-password"
```

**Setup**:
1. Create database: `createdb clockify_addons`
2. Load schema: `psql clockify_addons < extras/sql/token_store.sql`
3. Set environment variables above
4. Restart Rules addon - logs should show: `✓ TokenStore configured with database persistence`

**Migration from InMemoryTokenStore**:
1. Set database environment variables
2. Restart addon
3. Reinstall addon in each workspace (triggers new `INSTALLED` lifecycle event)
4. Tokens will be persisted in database going forward

See: [Database Token Store Guide](../../docs/DATABASE_TOKEN_STORE.md) for complete setup, troubleshooting, and production tuning.

> 🧪 **Local smoke tests**: When `CLOCKIFY_WORKSPACE_ID` and `CLOCKIFY_INSTALLATION_TOKEN` are present (and `ENV=dev`), `RulesConfiguration.LocalDevSecrets` preloads the in-memory TokenStore so you can call `/rules/status` or `/rules/api/test` without reinstalling. These env vars are ignored automatically in non-dev environments.
>
> ⚠️ **Fail-fast on misconfiguration:** If any `RULES_DB_*`/`DB_*` variables are set but the database cannot be reached, `RulesApp` throws during startup instead of quietly falling back to in-memory storage. Fix credentials or unset the variables before redeploying.

### Workspace Cache (ID ↔ Name)

Rules preloads workspace entities after install so rules and UI can map names to IDs:
- Tags, Projects, Clients, Users, Tasks (by project)
- Inspect: `GET /rules/api/cache?workspaceId=<ws>` → counts
- Refresh: `POST /rules/api/cache/refresh?workspaceId=<ws>`

See also: docs/WEBHOOK_IFTTT.md for event→action patterns.

## Manifest (Scopes and Plan)

Rules needs to read and optionally modify time entries, and it uses tags. It targets the PRO plan by default so Clockify’s automation features are available; raise the minimum plan further only if your deployment requires enterprise-only capabilities.

```java
ClockifyManifest manifest = ClockifyManifest
    .v1_3Builder()
    .key("rules")
    .name("Rules")
    .baseUrl(baseUrl)
    .minimalSubscriptionPlan("PRO")
    .scopes(new String[]{
        "TIME_ENTRY_READ", "TIME_ENTRY_WRITE",
        "TAG_READ", "TAG_WRITE",
        "PROJECT_READ", "PROJECT_WRITE",
        "CLIENT_READ", "CLIENT_WRITE",
        "TASK_READ", "TASK_WRITE",
        "WORKSPACE_READ"
    })
    .build();
```

Register UI and endpoints in your app wiring; the runtime manifest is served at `/{addon}/manifest.json` and stays synchronized:

```java
addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));
addon.registerCustomEndpoint("/settings", new SettingsController());
addon.registerLifecycleHandler("INSTALLED", handler);
addon.registerLifecycleHandler("DELETED",   handler);
// Default path ("/webhook"): register time entry events used by Rules
addon.registerWebhookHandler("NEW_TIME_ENTRY", handler);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", handler);
// Or supply a custom path, e.g.: addon.registerWebhookHandler("NEW_TIME_ENTRY", "/webhooks/entries", handler);
```

See docs/MANIFEST_AND_LIFECYCLE.md for manifest/lifecycle patterns and docs/REQUEST-RESPONSE-EXAMPLES.md for full HTTP exchanges.

## Route → Manifest Mapping

| Route | Purpose | Manifest Entry |
|------|---------|----------------|
| `/manifest.json` | Serve runtime manifest | n/a (content of manifest itself) |
| `/settings` | Settings UI | `components[]` item with `type: SETTINGS_SIDEBAR`, `url: /settings` |
| `/lifecycle/installed` | Lifecycle install callback | `lifecycle[]` item `{ type: "INSTALLED", path: "/lifecycle/installed" }` |
| `/lifecycle/deleted` | Lifecycle uninstall callback | `lifecycle[]` item `{ type: "DELETED", path: "/lifecycle/deleted" }` |
| `/webhook` (default) | Time entry webhooks (NEW_TIME_ENTRY, TIME_ENTRY_UPDATED) | One `webhooks[]` item per event with `path: "/webhook"` |
| `/health` | Health endpoint (includes DB probe when DB_URL/DB_USER set) | Not listed in manifest |
| `/ready` | Readiness probe (rules store + token store checks) | Not listed in manifest |
| `/metrics` | Prometheus metrics scrape | Not listed in manifest |
| Custom (e.g. `/webhooks/entries`) | Alternative webhook mount | One `webhooks[]` item per event with `path: "/webhooks/entries"` |

## Health, readiness & metrics

- **`/health` (liveness)** &mdash; backed by `HealthCheck`, reports the JVM version plus rule/token store connectivity. Returns `200` with JSON like `{"status":"healthy","checks":[...]}` when Jetty is up and any configured database connections succeed; otherwise `503`.
- **`/ready` (readiness)** &mdash; served by `ReadinessHandler`, which treats missing stores as `SKIPPED`, calls `rulesStore.getAll("health-probe")`, and executes `tokenStore.count()` when persistence is enabled. Returns `200` only when both checks are UP, otherwise `503` with `"status":"DEGRADED"`.
- **`/metrics`** &mdash; exposes Prometheus-format counters/timers (webhook dedupes, rule evaluations, executor latency). Scrape it from the same base URL (e.g., `http://service/rules/metrics`).
- **Probe guidance** &mdash; point Kubernetes or Docker health probes at `/ready` for startup + readiness gates, and use `/health` for liveness. Both endpoints are lightweight and safe to call every few seconds.

## Webhook idempotency & duplicate detection

- `WebhookIdempotencyCache` hashes `(workspaceId, eventType, preferred payload ID)` per delivery. `RULES_WEBHOOK_DEDUP_SECONDS` must stay between **60 seconds and 24 hours**; invalid values now fail fast during startup and are clamped/logged defensively at runtime.
- Preferred IDs include `payloadId`, `eventId`, `timeEntry.id`, etc. If no stable field exists, the cache falls back to hashing the entire payload body.
- **Store modes:** When `RULES_DB_*` (or the shared `DB_*`) is configured, dedupe entries live in the new `webhook_dedup` table so every pod sees the same suppression window. Without a database configuration, the add-on automatically falls back to the in-memory store (node-local behavior identical to previous releases).
- When a duplicate arrives within the TTL, handlers short-circuit, emit a `duplicate` log line, and increment the Prometheus counter `rules_webhook_dedup_hits_total`. First-seen payloads increment `rules_webhook_dedup_misses_total`. Alert on spikes in either counter to spot upstream retry storms or unexpectedly noisy tenants.

## Operational guardrails

- **Workspace cache cap** &mdash; Refreshes load at most 5,000 tasks per workspace to keep memory bounded. When a workspace exceeds the cap, log lines include `workspaceId`, `tasksLoaded`, and the observed total, and the metric `rules_workspace_cache_truncated_total{dataset="tasks"}` increments so dashboards can flag truncated caches.
- **Async webhook backlog** &mdash; Any time the async executor rejects work, the handler falls back to synchronous processing and increments `rules_async_backlog_total{outcome="fallback"}`. Alerting on this counter helps catch sustained overload before queue depth impacts webhook SLAs.
- **Dedupe visibility** &mdash; With database settings in place, webhook dedupe entries are shared across all replicas. When running purely in-memory (local dev), duplicates are only caught on the current JVM; track `rules_webhook_dedup_hits_total` vs `rules_webhook_dedup_misses_total` to understand retry ratios and decide when to enable persistence.

### openapi_call safety

- Only `GET` and `POST` methods are accepted for `openapi_call` actions; other verbs are rejected during rule validation.
- Paths must start with `/workspaces/{workspaceId}/...` to keep automation scoped to the invoking workspace’s public APIs.
- Treat `openapi_call` as a privileged escape hatch: test rules in a sandbox workspace, prefer read-only endpoints first, and keep payloads small enough to inspect in logs/metrics when something misfires.

## Checklist: Plan, Scopes, Events

- Plan (minimalSubscriptionPlan)
  - Set to `PRO`. Explorer datasets (invoices, time off) and rule actions rely on Pro-tier automations, so staying on Pro avoids false failures during review.
- Scopes (least privilege while covering explorer/builder)
  - Rules + actions: `TIME_ENTRY_READ`, `TIME_ENTRY_WRITE`, `TAG_READ`, `TAG_WRITE`
  - Explorer CRUD helpers: `PROJECT_READ`, `PROJECT_WRITE`, `CLIENT_READ`, `CLIENT_WRITE`, `TASK_READ`, `TASK_WRITE`
  - Context: `WORKSPACE_READ` (surface workspace metadata in explorer/builder)
- Webhook events (allowed by schema)
  - `NEW_TIME_ENTRY`, `TIME_ENTRY_UPDATED`
  - Add more only if your rules need additional signals.
- References
  - Event payloads: docs/REQUEST-RESPONSE-EXAMPLES.md
  - Full catalog: dev-docs-marketplace-cake-snapshot/
  - Manifest fields: docs/CLOCKIFY_PARAMETERS.md

### Database schema & migrations

The JDBC stores create tables automatically for local smoke tests, but production deployments should manage DDL explicitly. See [`docs/RULES_DB_SCHEMA.md`](../../docs/RULES_DB_SCHEMA.md) for canonical table definitions (`rules` + `addon_tokens`) and migration guidance.

### Monitoring & observability

`/health`, `/ready`, and `/metrics` expose everything needed for SRE dashboards. [`docs/RULES_OBSERVABILITY.md`](../../docs/RULES_OBSERVABILITY.md) lists the exact checks, Micrometer metrics, and alert suggestions (readiness flaps, webhook latency, dedupe spikes, etc.).

### Production launch checklist

Ready to ship Rules to a new environment? Follow [`RULES_PROD_LAUNCH_CHECKLIST.md`](../../RULES_PROD_LAUNCH_CHECKLIST.md) for pre-flight validation, build/deploy steps, dedupe smoke tests, and monitoring hand-off items.
