# Rules Add‑on — Declarative Automations

The Rules add‑on lets admins define “if … then …” automations for Clockify time entries (AND/OR logic), with a simple no‑code builder available at `/rules/settings`.

- Conditions: descriptionContains, descriptionEquals, hasTag (by ID), projectIdEquals, projectNameContains, clientIdEquals, clientNameContains, isBillable
- Actions: add_tag, remove_tag, set_description, set_billable, set_project_by_id, set_project_by_name, set_task_by_id, set_task_by_name, openapi_call (IFTTT placeholders → Clockify API)
- Manifest key: `rules`; base path: `/rules`
- Minimal subscription plan: `PRO` (tasks, invoices, and automation actions require the Pro tier)

## Zero‑to‑Run

```bash
# 0) Requirements: Java 17 + Maven; ngrok (optional, for public install)
java -version && mvn -version

# 1) Build
make build-rules

# 2) Run locally
ADDON_BASE_URL=http://localhost:8080/rules make run-rules

# 3) Expose via ngrok (separate terminal)
ngrok http 8080

# 4) Restart with ngrok base URL (either):
#   ADDON_BASE_URL=https://<ngrok-domain>/rules make run-rules
#   bash scripts/run-rules.sh --use-ngrok   # auto‑detects https public URL via 127.0.0.1:4040
#   bash scripts/run-rules.sh --base-url "https://<ngrok-domain>/rules"  # reliable fallback (quoted)

# 5) Install in Clockify Developer using:
#   https://<ngrok-domain>/rules/manifest.json
```

## Install and Run

```bash
# Build the module
make build-rules

# Run locally
ADDON_BASE_URL=http://localhost:8080/rules make run-rules

# Expose via ngrok (separate terminal)
ngrok http 8080
# Restart with ngrok base URL
#   ADDON_BASE_URL=https://<ngrok-domain>/rules make run-rules
# Install in Clockify using: https://<ngrok-domain>/rules/manifest.json
```

## Manifest (Admins‑only component)

Rules ships with `minimalSubscriptionPlan("PRO")` so Clockify’s automation features remain available during installs. Adjust the plan only if your deployment needs to restrict access further.

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

// Admins‑only sidebar component
manifest.getComponents().add(
    new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Rules", "ADMINS")
);

Explorer datasets and the simple builder both create/update projects, clients, and tasks on behalf of admins, so the manifest purposely includes the matching read/write scopes alongside `WORKSPACE_READ` for context banners.
```

Optional runtime safeguards:
- Security headers: `export ADDON_FRAME_ANCESTORS="'self' https://*.clockify.me"`
- Rate limiting: `export ADDON_RATE_LIMIT=10` and `export ADDON_LIMIT_BY=ip|workspace`
- CORS allowlist: `export ADDON_CORS_ORIGINS=https://app.clockify.me` (optional credentials: `export ADDON_CORS_ALLOW_CREDENTIALS=true`)
- Request logging: `export ADDON_REQUEST_LOGGING=true`

## Security Hardening Features

This boilerplate includes comprehensive enterprise security hardening features enabled by default:

- **JWT Security**: Algorithm enforcement, strict kid handling, JWKS-based key management
- **CSRF Protection**: Token-based validation with constant-time comparison for state-changing operations
- **RFC-7807 Error Handling**: Standardized problem+json error responses
- **Request ID Propagation**: Distributed tracing for all requests with X-Request-Id headers
- **Security Headers**: Comprehensive HTTP security headers (CSP, HSTS, XSS protection)
- **Input Validation**: Comprehensive parameter and payload validation
- **Path Sanitization**: URL path validation and sanitization
- **Rate Limiting**: IP and workspace-based request throttling
- **PlatformAuthFilter**: `/api/**` + `/status` require `Authorization: Bearer <auth_token>` with both `installation_id` and `workspace_id` claims; missing claims return `403` so callers cannot spoof tenancy.

All security features are validated through comprehensive testing (307 tests across 5 layers) and are automatically enabled in production deployments.

## API

- `GET /rules/manifest.json` — runtime manifest (v1.3; no `$schema`)
- `GET /rules/settings` — sidebar UI
- Access: The Rules sidebar component is registered for `ADMINS` only (manifest component access level).
- `GET /rules/status?workspaceId=...` — runtime status (token present, apply mode, skip‑sig flag, baseUrl)
- `GET /rules/api/rules?workspaceId=...` — list rules
- `POST /rules/api/rules?workspaceId=...` — create/update rule (id auto‑generated if omitted)
- `DELETE /rules/api/rules?id=<id>&workspaceId=...` — delete rule by id
- `POST /rules/api/test` — evaluate rules against provided payload; no side effects
- `GET /rules/api/cache?workspaceId=...` — cache summary (tags/projects/clients/users/tasks)
- `GET /rules/api/cache/data?workspaceId=...` — lists for autocompletes (names+ids)
- `POST /rules/api/cache/refresh?workspaceId=...` — refresh cache immediately

### Rule JSON schema (example)

```json
{
  "name": "Tag client meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"},
    {"type": "hasTag", "operator": "EQUALS", "value": "client"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}
```

### Quick curl examples

```bash
# Create/update a rule (OR logic by default when combinator omitted)
curl -s -X POST \
  "http://localhost:8080/rules/api/rules?workspaceId=workspace-1" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Tag client meetings",
    "enabled":true,
    "conditions":[
      {"type":"descriptionContains","operator":"CONTAINS","value":"meeting"},
      {"type":"projectNameContains","operator":"CONTAINS","value":"Client"}
    ],
    "actions":[
      {"type":"add_tag","args":{"tag":"billable"}},
      {"type":"set_billable","args":{"value":"true"}}
    ]
  }'

# List rules
curl -s "http://localhost:8080/rules/api/rules?workspaceId=workspace-1"

# Delete rule by id
curl -s -X DELETE "http://localhost:8080/rules/api/rules?id=<ID>&workspaceId=workspace-1"

# Dry‑run evaluation (no side effects). Provide a minimal timeEntry skeleton.
curl -s -X POST http://localhost:8080/rules/api/test \
  -H 'Content-Type: application/json' \
  -d '{
    "workspaceId":"workspace-1",
    "timeEntry":{"id":"e1","description":"Client meeting","tagIds":[],"projectId":"proj-1"}
  }'

## No‑Code Builder (UI)

- Open: `<BASE>/rules/settings` (e.g., `http://localhost:8080/rules/settings`).
- Enter your `workspaceId` (required for API calls).
- Add Conditions from dropdowns; choose AND/OR combinator (optional).
- Add Actions and provide required args (tag/name, projectId/taskId/value). The UI autocompletes names and converts them to IDs where appropriate.
- Click “Save Rule”; use “Existing Rules” to refresh or delete.
- When you arrive via the explorer’s “Create rule from this” action, the builder highlights every injected condition and shows a prefill banner/copyable rule seed so admins know exactly what was preloaded.

## Workspace Explorer & Snapshot

- `/rules/settings` also hosts the workspace explorer iframe used inside Clockify (install via **developer.clockify.me → Admin → Add-ons → Install Custom Add-on** with `https://<domain>/rules/manifest.json`).
- The left nav covers all read-only datasets required for audits (users, projects, clients, tags, **tasks**, time entries, invoices, time-off, webhooks, custom fields). Each panel ships with search/filter inputs, page-size controls, and a preset dropdown backed by `localStorage` so every browser can save multiple named filter bundles.
- Explorer APIs (`/api/rules/explorer/**`) run server-side and never expose the Clockify token to the browser. The new tasks endpoint walks project/task GET pages with a capped scan (5K items) so “fetch everything” remains safe even on large workspaces; when the cap hits, refreshes log the workspace and increment `rules_workspace_cache_truncated_total{dataset="tasks"}` so operators can spot truncated caches.
- Snapshot mode (`/api/rules/explorer/snapshot`) supports dataset toggles (users through invoices + tasks), configurable limits (5–100 rows × 1–20 pages), and a selectable time-entry lookback (UI presets 7/30/90 days, clamped 1–90). The UI renders progress per dataset, shows expandable JSON previews, and only enables the download button when a run completes without errors.
- Time entry rows surface both a “Create rule from this” link (deep-linking into `/simple?ruleName=...&prefillDescription=...&prefillProjectId=...&prefillTagIds=...`) and a copy-to-clipboard rule seed for admins who prefer to start from JSON.

## Developer Workspace Notes

- Install After Start: Start the add‑on and confirm the Base URL banner matches your ngrok URL, then install the manifest. Installing first can cache an old URL and cause 401/404s.
- Only inside Clockify: Configure security so the UI is embeddable only in Clockify (see `ADDON_FRAME_ANCESTORS`) and allow CORS only for Clockify origins.
- Helper defaults: `scripts/run-rules.sh` sets security defaults automatically when not provided:
  - `ADDON_FRAME_ANCESTORS='self' https://*.clockify.me`
  - `ADDON_CORS_ORIGINS=https://app.clockify.me,https://developer.clockify.me`
  - `ADDON_CORS_ALLOW_CREDENTIALS=false` (you can override if needed)

## IFTTT Builder (Advanced Automations)

The Rules add-on now includes an IFTTT-style automation builder at `/rules/ifttt` that lets you:

- **Pick Any Webhook Trigger**: Select from all Clockify webhook events (not just time entries)
  - Time tracking: NEW_TIME_ENTRY, TIME_ENTRY_UPDATED, TIMER_STOPPED, etc.
  - Projects: NEW_PROJECT, PROJECT_UPDATED, PROJECT_DELETED
  - Clients, Tags, Tasks, Users, and more

- **Compose Custom API Actions**: Build actions using any Clockify API endpoint
  - Endpoints are loaded dynamically from the OpenAPI spec
  - Create dynamic forms with required/optional fields
  - Use placeholder templating: `{{field.path}}` to insert values from the webhook payload
  - Examples: `{{timeEntry.id}}`, `{{project.name}}`, `{{user.email}}`

- **Filter with Conditions**: Add optional filter conditions to limit when actions fire
  - JSON path conditions: `jsonPathContains`, `jsonPathEquals`
  - Support for dotted paths like `project.name`, `user.id`, etc.

- **See Live Previews**: View HTTP method, path, and body for each action before saving

### IFTTT API Endpoints

- `GET /rules/api/catalog/triggers` — list all available webhook triggers
- `GET /rules/api/catalog/actions` — list all Clockify API endpoints (from OpenAPI spec)
- `GET /rules/ifttt` — IFTTT builder UI

Note: Legacy time‑entry behavior is preserved. The dynamic handler intentionally does not register
`NEW_TIME_ENTRY`/`TIME_ENTRY_UPDATED` to avoid overriding the existing time‑entry webhook handler
that powers classic actions (add_tag, set_billable, etc.).

### Example IFTTT Rule

```json
{
  "name": "Auto-tag billable client projects",
  "enabled": true,
  "trigger": {
    "event": "NEW_TIME_ENTRY",
    "conditions": [
      {"type": "jsonPathContains", "path": "project.clientName", "value": "ACME"}
    ]
  },
  "actions": [
    {
      "type": "openapi_call",
      "endpoint": {
        "method": "POST",
        "path": "/v1/workspaces/{{workspaceId}}/projects/{{projectId}}/tasks"
      },
      "body": {
        "name": "Follow-up for {{user.name}}",
        "projectId": "{{projectId}}"
      }
    }
  ]
}
```

## Supported Conditions and Actions (Rules engine)

- Conditions
  - descriptionContains, descriptionEquals
  - hasTag (by tagId)
  - projectIdEquals
  - projectNameContains
  - clientIdEquals, clientNameContains
  - isBillable (true/false)
  - **jsonPathContains, jsonPathEquals** (IFTTT: query any payload field with dotted paths)

- Actions
  - add_tag, remove_tag (by name; creates tag if missing)
  - set_description
  - set_billable (true/false)
  - set_project_by_id, set_project_by_name (name resolved via cache)
  - set_task_by_id, set_task_by_name (name resolved under current or newly set project via cache)
  - **openapi_call** (IFTTT: execute any Clockify API endpoint with placeholder resolution)

## Operational guardrails & metrics

- **Workspace cache cap** — Refreshes load at most 5,000 tasks per workspace. When the cap fires, the service logs the workspace, observed totals, and increments `rules_workspace_cache_truncated_total{dataset="tasks"}` so dashboards can flag partial caches.
- **Webhook dedupe storage** — When `RULES_DB_*`/`DB_*` is configured the cache persists entries in the `webhook_dedup` table so every pod shares the same suppression window. Local/dev runs fall back to the in-memory store (at-most-once per JVM). Track `rules_webhook_dedup_hits_total` vs `rules_webhook_dedup_misses_total` to monitor retries, and scrape `rules_webhook_idempotency_backend{backend="database|in_memory"}` (single tag reports `1`) to confirm which backend is active.
- **Async backlog** — Oversized rule batches fall back to synchronous processing when the async executor is saturated. `rules_async_backlog_total{outcome="submitted"}` counts hand-offs, `outcome="rejected"` fires when the queue is full, and `outcome="fallback"` means we ran synchronously. Alert on sustained `rejected`/`fallback` growth.

### openapi_call safety

- Only `GET` and `POST` methods are accepted; other verbs fail validation.
- Paths must start with `/workspaces/{workspaceId}/...` to keep automation scoped to the invoking tenant.
- Test `openapi_call` rules in a sandbox workspace first, prefer read-only endpoints when possible, and keep payloads small enough to audit via logs/metrics if something goes wrong.

- Signatures: Developer webhooks are signed. The validator accepts `clockify-webhook-signature`, `x-clockify-webhook-signature` (case variants), and Developer’s JWT header `Clockify-Signature` (enable with `ADDON_ACCEPT_JWT_SIGNATURE=true`; default is `false`). If your environment still 401s, use the dev bypass below to prove E2E and share one sample header so we can adapt.
- Dev bypass: To test end‑to‑end without signature problems and apply changes:
  - `ADDON_SKIP_SIGNATURE_VERIFY=true RULES_APPLY_CHANGES=true bash scripts/run-rules.sh --base-url "https://<ngrok>/rules"`
  - Create a rule in the UI, then create/update a matching time entry in the installed workspace.
  - Switch back to signed mode by removing `ADDON_SKIP_SIGNATURE_VERIFY` once verified.

Tips:
- Only one ngrok agent session is allowed on the free plan. If you see `ERR_NGROK_108`, stop stray sessions (`killall ngrok`), or reuse the existing one. You can always read the current public URL from `http://127.0.0.1:4040/api/tunnels`.
- Quote your `--base-url` value to avoid stray spaces (the run script rejects URLs containing spaces and prints a hint).

## Troubleshooting
- Double slash (//) 400 errors: If you see `Ambiguous URI empty segment`, you likely posted to `/rules//api/rules`. Hard‑refresh to load the latest UI; it computes baseUrl safely and won’t generate `//`.
- Wrong base URL: Quote the URL when using `--base-url`. If the banner shows a space in the URL or a context path of `/`, stop and relaunch with a clean quoted URL.
- Single ngrok agent: Free plan allows one agent. Kill stray agents (`killall ngrok`) and start a fresh one (`ngrok http 8080`).
```

## Security
- Webhook signature verification enforced; missing/invalid signature → 401/403
- Token storage is in‑memory for demo; use a persistent store before production
- See SECURITY.md and THREAT_MODEL.md for the full posture

## Notes
- The demo logs matched actions by default; to apply changes, set `RULES_APPLY_CHANGES=true`.
- In production, call Clockify APIs to apply changes and ensure idempotence (e.g., don’t re‑add existing tags).

### Applying actions (example)

Use the SDK `ClockifyHttpClient` with the stored workspace token:

```java
var wk = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).orElseThrow();
var http = new com.clockify.addon.sdk.http.ClockifyHttpClient(wk.apiBaseUrl());

// Example: add a tag to a time entry (pseudo-path; consult API-COOKBOOK for exact endpoint)
String body = "{\"tagIds\":[\"" + tagId + "\"]}";
var resp = http.putJson("/v1/workspaces/" + workspaceId + "/time-entries/" + timeEntryId, wk.token(), body, java.util.Map.of());
if (resp.statusCode() / 100 != 2) {
  // handle error, retry logic embedded for 429/5xx
}
```

```
make validate           # basic manifest checks
pip install jsonschema  # once
make schema-validate    # strong schema checks
```
## Local demo and webhook simulation

Seed a demo rule and exercise a dry‑run evaluation (no side effects):

```
export WORKSPACE_ID=your-workspace-id
make rules-seed-demo
```

If you want to apply actions on real webhooks locally, start the add‑on with `RULES_APPLY_CHANGES=true`, preload the installation token (or simulate one), and send a signed webhook:

```
export WORKSPACE_ID=your-workspace-id
export CLOCKIFY_INSTALLATION_TOKEN=raw-installation-jwt

# Run with apply-changes enabled (use your ngrok url if exposing externally)
RULES_APPLY_CHANGES=true ADDON_BASE_URL=http://localhost:8080/rules make run-rules

# In another terminal, simulate a signed webhook
make rules-webhook-sim
```
