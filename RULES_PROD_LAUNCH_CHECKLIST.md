# Rules Add-on Production Launch Checklist

Use this checklist before the first deployment **and** during every rollout. Update links and commands with your environment-specific values.

> ℹ️ References: [`addons/rules/README.md`](addons/rules/README.md), [`docs/RULES_DB_SCHEMA.md`](docs/RULES_DB_SCHEMA.md), [`docs/RULES_OBSERVABILITY.md`](docs/RULES_OBSERVABILITY.md)

---

## A. Pre-flight (once per environment)

- [ ] **Schema applied:** Run the migrations from `docs/RULES_DB_SCHEMA.md` for both `rules` and `addon_tokens` tables. Document the schema version deployed.
- [ ] **Dedicated workspace:** Create a Clockify workspace for this environment and record its ID.
- [ ] **Addon registration:** Ensure the add-on is approved for the workspace and note the `addonKey` (should remain `rules`).
- [ ] **JWT configuration chosen:** Decide between JWKS / PEM map / single PEM.
  - [ ] `CLOCKIFY_JWT_JWKS_URI` **or** `CLOCKIFY_JWT_PUBLIC_KEY_MAP` **or** `CLOCKIFY_JWT_PUBLIC_KEY[_PEM]` is set.
  - [ ] `CLOCKIFY_JWT_EXPECT_ISS`, `CLOCKIFY_JWT_EXPECT_AUD`, and optional `CLOCKIFY_JWT_DEFAULT_KID` are set.
  - [ ] `CLOCKIFY_JWT_LEEWAY_SECONDS` kept within expected skew (default 60s).
- [ ] **Environment label correct:** `ENV=dev` only for local testing, `ENV=staging` or `ENV=prod` everywhere else.
- [ ] **Dev-only flags absent in staging/prod:** `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, `ADDON_SKIP_SIGNATURE_VERIFY`, and `ADDON_ACCEPT_JWT_SIGNATURE` unset.
- [ ] **Token store inputs:** When `ENABLE_DB_TOKEN_STORE=true`, confirm `DB_URL`, `DB_USER`/`DB_USERNAME`, `DB_PASSWORD` are populated.

## B. Build & deploy

- [ ] **Build artifacts:**
  - [ ] `mvn -pl addons/rules -am package` (or `mvn clean verify`) completes successfully.
  - [ ] Docker image built with the correct base URL, e.g.:
    ```bash
    ADDON_BASE_URL=https://rules.example.com/rules \
    make docker-build TEMPLATE=rules
    ```
- [ ] **Deploy container:** Inject the env vars from your profile (`.env.rules.prod.example` is a good starting point). Include:
  - `ADDON_BASE_URL`, `ADDON_PORT`
  - JWT envs from section A
  - Persistence envs (`ENABLE_DB_TOKEN_STORE`, `RULES_DB_*`, `DB_*`)
  - Optional middleware (`ADDON_RATE_LIMIT`, `ADDON_FRAME_ANCESTORS`, etc.)
- [ ] **Startup verification:**
  - [ ] Logs show “Rules Add-on starting” with the expected env label and `applyChanges` flag.
  - [ ] No ERROR lines about JWT bootstrap, database connectivity, or token store initialization.
  - [ ] `PlatformAuthFilter registered for /api/**` appears (non-dev only).

## C. Smoke tests

- [ ] **Health endpoints:**
  - [ ] `GET /rules/health` → `200`.
  - [ ] `GET /rules/ready` → `200` after databases are reachable.
  - [ ] `GET /rules/metrics` returns Prometheus text and includes `rules_webhook_latency_ms`.
- [ ] **Settings UI (inside Clockify):**
  - [ ] Sidebar loads, shows correct base URL + environment banner.
  - [ ] Developer tools show `/api/**` and `/status` requests with `Authorization: Bearer ...` headers (no 401s).
- [ ] **Webhook path sanity:**
  - [ ] Trigger a sample webhook (e.g., create a time entry) with known match conditions.
  - [ ] Confirm webhook log lines show `workspaceId` and `event` fields.
  - [ ] Metrics `rules_evaluated_total` and `rules_actions_total` increment.

## D. Idempotency & error paths

- [ ] **Duplicate detection:**
  - [ ] Replay the same webhook payload (same `payloadId` or `eventId`).
  - [ ] Handler returns duplicate status and logs “Duplicate webhook suppressed”.
  - [ ] `rules_webhook_dedup_hits_total` increments.
- [ ] **TTL semantics validated:** `RULES_WEBHOOK_DEDUP_SECONDS` is between 60 seconds and 24 hours, matching retry expectations (see README’s idempotency section).
- [ ] **Transient failure simulation:**
  - [ ] Temporarily block DB connectivity (e.g., revoke network or shut down replica).
  - [ ] `/ready` transitions to `503` with `rulesStore` or `tokenStore` reported as `DOWN`.
  - [ ] Logs show WARN (not ERROR spam) with the cause once per failure mode and recover when DB returns.

## E. Monitoring & alerts

- [ ] `/metrics` scraped by Prometheus (or equivalent) at the desired interval.
- [ ] Dashboards include:
  - Webhook throughput (`rules_evaluated_total`, `rules_matched_total`).
  - Action success/failure (`rules_actions_total`).
  - Latency histograms (`rules_webhook_latency_ms`).
  - Dedup hits vs. total events.
- [ ] Alerts configured per [`docs/RULES_OBSERVABILITY.md`](docs/RULES_OBSERVABILITY.md):
  1. Readiness degradation lasting &gt;1 minute.
  2. `rules_actions_total{result="failure"}` spike.
  3. `rules_webhook_dedup_hits_total` spike signaling retries.
  4. Metrics scrape gaps or `/metrics` returning non-200.
- [ ] Logs shipped to your aggregator with `workspaceId`, `requestId`, and `env` fields preserved.
- [ ] Runbooks linked from alerts point back to this checklist and relevant docs.

Once all items are checked, capture a snapshot of the command outputs (`mvn` build, docker tag, health checks) and attach it to your release ticket for traceability.
