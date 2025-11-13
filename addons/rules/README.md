# Rules Add-on  
[AI START HERE](../../docs/AI_START_HERE.md)

![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)
[![Validate](https://github.com/apet97/boileraddon/actions/workflows/validate.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/validate.yml)
[![Docs](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml)
[![Coverage](https://apet97.github.io/boileraddon/coverage/badge.svg)](https://apet97.github.io/boileraddon/coverage/)
[![Docs Index](https://img.shields.io/badge/Docs-Index-blue)](../../docs/README.md)

Automation add-on that applies rule-driven actions to time entries (e.g., tagging entries that match conditions). Includes lifecycle handlers, a settings page, and webhook processing.

See also: [Manifest Recipes](../../docs/MANIFEST_RECIPES.md) and [Permissions Matrix](../../docs/PERMISSIONS_MATRIX.md) for choosing plan/scopes and wiring routes.

## Quick Start  

```
mvn -q -pl addons/rules -am package -DskipTests
ADDON_BASE_URL=http://localhost:8080/rules java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
# In another terminal:
ngrok http 8080
# Restart with HTTPS base URL
ADDON_BASE_URL=https://YOUR.ngrok-free.app/rules java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
# Install using: https://YOUR.ngrok-free.app/rules/manifest.json
```

## Configuration

`addons/rules/src/main/java/com/example/rules/config/RulesConfiguration.java` loads and validates every environment variable once at startup. Copy `.env.rules.example`, tweak the values, and these keys will be wired automatically:

| Variable | Purpose | Default |
| --- | --- | --- |
| `ADDON_BASE_URL` | External URL surfaced in the manifest and iframe redirects | `http://localhost:8080/rules` |
| `ADDON_PORT` | Embedded Jetty listener | `8080` |
| `CLOCKIFY_API_BASE_URL` | Upstream API base for `ClockifyClient` | `https://api.clockify.me/api` |
| `RULES_APPLY_CHANGES` | Toggle between dry-run and mutation modes | `false` |
| `RULES_WEBHOOK_DEDUP_SECONDS` | TTL for webhook idempotency cache | `600` |
| `ENABLE_DB_TOKEN_STORE` + `DB_URL/DB_USER/DB_PASSWORD` | Optional persistent token store | disabled unless `ENV=prod` |
| `RULES_DB_URL` + `RULES_DB_USERNAME/RULES_DB_PASSWORD` | JDBC store for rule definitions | falls back to `DB_URL` |
| `CLOCKIFY_JWT_PUBLIC_KEY` / `CLOCKIFY_JWT_PUBLIC_KEY_PEM` / `CLOCKIFY_JWT_PUBLIC_KEY_MAP` / `CLOCKIFY_JWT_JWKS_URI` | Public keys (static PEM, PEM map, or JWKS) for iframe JWT validation | unset |
| `CLOCKIFY_JWT_EXPECT_ISS` / `CLOCKIFY_JWT_EXPECT_AUD` / `CLOCKIFY_JWT_LEEWAY_SECONDS` / `CLOCKIFY_JWT_DEFAULT_KID` | Optional claim enforcement + clock skew for JWT bootstrap | `clockify` / `rules` / `60` / unset |
| `ADDON_RATE_LIMIT` + `ADDON_LIMIT_BY` | SDK rate limiter for inbound requests | disabled |
| `ADDON_CORS_ORIGINS` + `ADDON_CORS_ALLOW_CREDENTIALS` | Explicit CORS allowlist for iframe AJAX calls | disabled |
| `ADDON_REQUEST_LOGGING` | Enables scrubbed request logs | `false` |

Dev helpers such as `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, and `ADDON_SKIP_SIGNATURE_VERIFY` remain available for local smoke tests but must never be enabled in production.

Developer signatures: webhooks include an HMAC header `clockify-webhook-signature` (and case variants). In Developer workspaces, Clockify may send a JWT header `Clockify-Signature`; the SDK accepts it by default. Toggle with `ADDON_ACCEPT_JWT_SIGNATURE=true|false`.

## Security: JWT Verification

This addon implements JWT verification at the **addon level** rather than in the shared SDK. This architectural decision allows each addon to:

- **Customize verification constraints** (issuer, audience, allowed algorithms, clock skew) based on its specific security requirements
- **Manage keys independently** using addon-specific JWKS endpoints or environment-configured key maps
- **Evolve security policies** without requiring SDK updates across all addons

### Key Features

The `JwtVerifier` class (`src/main/java/com/example/rules/security/JwtVerifier.java`) provides:

1. **Strict kid handling**: When a JWT header includes `kid`, only that specific key is used—no fallback to default keys
2. **Algorithm intersection**: Enforces intersection of configured algorithms with a safe built-in set (`RS256`, `ES256`)
3. **Temporal validation**: Enforces `iat`, `nbf`, `exp` with configurable clock skew (default 60s) and max TTL (24h)
4. **Audience any-of matching**: Supports both string and array audience claims with any-of semantics
5. **JWKS integration**: Optional `JwksKeySource` for dynamic key rotation
6. **Environment configuration**: Configure via `CLOCKIFY_JWT_PUBLIC_KEY_MAP`, `CLOCKIFY_JWT_DEFAULT_KID`, `CLOCKIFY_JWT_EXPECT_AUD`, `CLOCKIFY_JWT_LEEWAY_SECONDS`

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

### Workspace Cache (ID ↔ Name)

Rules preloads workspace entities after install so rules and UI can map names to IDs:
- Tags, Projects, Clients, Users, Tasks (by project)
- Inspect: `GET /rules/api/cache?workspaceId=<ws>` → counts
- Refresh: `POST /rules/api/cache/refresh?workspaceId=<ws>`

See also: docs/WEBHOOK_IFTTT.md for event→action patterns.

## Manifest (Scopes and Plan)

Rules needs to read and optionally modify time entries, and it uses tags. By default it targets the FREE plan; raise the minimum plan and adapt scopes as needed.

```java
ClockifyManifest manifest = ClockifyManifest
    .v1_3Builder()
    .key("rules")
    .name("Rules")
    .baseUrl(baseUrl)
    .minimalSubscriptionPlan("FREE")
    .scopes(new String[]{
        "TIME_ENTRY_READ", "TIME_ENTRY_WRITE",
        "TAG_READ", "TAG_WRITE"
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

## Checklist: Plan, Scopes, Events

- Plan (minimalSubscriptionPlan)
  - Start with `FREE`; move to `STANDARD`/`PRO` if enterprise features or higher quotas are needed.
- Scopes (least privilege)
  - Core: `TIME_ENTRY_READ` (evaluate rules), `TIME_ENTRY_WRITE` (apply changes)
  - Helpful: `TAG_READ`, `TAG_WRITE` (when adding/removing tags)
- Webhook events (allowed by schema)
  - `NEW_TIME_ENTRY`, `TIME_ENTRY_UPDATED`
  - Add more only if your rules need additional signals.
- References
  - Event payloads: docs/REQUEST-RESPONSE-EXAMPLES.md
  - Full catalog: dev-docs-marketplace-cake-snapshot/
  - Manifest fields: docs/CLOCKIFY_PARAMETERS.md
