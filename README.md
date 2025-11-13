# Clockify Add-on Boilerplate

![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)
[![Smoke](https://github.com/apet97/boileraddon/actions/workflows/smoke.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/smoke.yml)
[![Validate](https://github.com/apet97/boileraddon/actions/workflows/validate.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/validate.yml)
[![Docs](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml)
![Java](https://img.shields.io/badge/Java-17+-informational)
![Maven](https://img.shields.io/badge/Maven-3.6+-informational)
[![Coverage](https://apet97.github.io/boileraddon/coverage/badge.svg)](https://apet97.github.io/boileraddon/coverage/)
[![Docs Index](https://img.shields.io/badge/Docs-Index-blue)](docs/README.md)

A clean, **self-contained** boilerplate for building Clockify add‑ons with **Maven Central dependencies only** — no private repos, no external SDK installs. It ships a lightweight in‑repo SDK (routing, middleware, security), a production‑ready Rules add‑on, and an Auto‑Tag example.

**🎉 Status: Production Ready** • All 638+ tests passing • Full security hardening • CI/CD automated

## Codebase overview
- **Repo purpose:** end-to-end Clockify add-on boilerplate with a hardened production target (`addons/rules`), plus smaller reference modules (`addons/auto-tag-assistant`, `addons/_template-addon`) you can inspect when prototyping.
- **Primary add-on – Rules (`addons/rules`)**: automation engine, settings/IFTTT UI, lifecycle/webhooks, persistence, readiness/metrics filters. All production docs point here.
- **In-repo SDK (`addons/addon-sdk`)**: manifest model, routing, middleware (SecurityHeadersFilter, RateLimiter, CorsFilter, WorkspaceContextFilter), TokenStore interfaces, metrics/health helpers.
- **Docs + tooling:** `docs/` (architecture, parameters, AI guides), `tools/` (manifest validators, schema), `_briefings/` (pinned references), `Makefile` helpers (`make dev-rules`, `make docker-build TEMPLATE=rules`).
- **Other modules:** Auto-Tag Assistant (demo), `addons/overtime` (policy sample), `_template-addon` for scaffolding; treat them as learning aids, not the canonical prod surface.
- **Configuration flow:** `RulesConfiguration` is the single source of truth for env vars, JWT bootstrap, rate limiting, and dev-only helpers. Runtime toggles (`RuntimeFlags`) guard dangerous flags so production never depends on `System.getenv` scattered across controllers.

## 🎯 New to the Project? Start Here!

### 🚀 How to Launch (Choose Your Path)

**Option 1: One-Command Launch (Recommended for First-Time Users)**
```bash
# Automatically builds, starts server, and shows you the manifest URL
./scripts/quick-start.sh
```

**Option 2: Manual Launch (Rules Add-on)**
```bash
# 1. Build the project
mvn clean package -DskipTests

# 2. Run locally (port 8080)
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# 3. In another terminal, expose via ngrok
ngrok http 8080

# 4. Copy the ngrok HTTPS URL (e.g., https://abc123.ngrok-free.app)

# 5. Restart with the public URL
ADDON_BASE_URL=https://abc123.ngrok-free.app/rules \
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# 6. Install in Clockify using: https://abc123.ngrok-free.app/rules/manifest.json
```

**Option 3: Quick Development Mode**
```bash
# Copy environment template and edit as needed
cp .env.rules.example .env.rules

# Run in dev mode (auto-loads .env.rules)
make dev-rules

# Health check
curl http://localhost:8080/rules/health
```

**Option 4: Docker**
```bash
# Build and run in Docker
ADDON_BASE_URL=https://your-ngrok.ngrok-free.app/rules make docker-run TEMPLATE=rules
# Build-only (image tagged via DOCKER_IMAGE, defaults to rules)
ADDON_BASE_URL=https://your-ngrok.ngrok-free.app/rules make docker-build TEMPLATE=rules
```

### 📝 What You'll Need

- **Java 17+** - Check with `java -version`
- **Maven 3.6+** - Check with `mvn -version`
- **ngrok** (optional) - Only needed to expose localhost to Clockify

### ✅ Verify Installation

After launching, verify everything works:

```bash
# Check health endpoint
curl http://localhost:8080/rules/health
# Expected: {"status":"healthy",...}

# Check manifest
curl http://localhost:8080/rules/manifest.json
# Expected: Valid JSON with "schemaVersion": "1.3"

# Run full test suite
mvn test
# Expected: BUILD SUCCESS, 638+ tests passing
```

### 📦 Install in Clockify

Once your add-on is running and exposed via ngrok:

1. **Go to Clockify** → **Admin** → **Add-ons**
2. Click **"Install Custom Add-on"**
3. **Enter your manifest URL**: `https://abc123.ngrok-free.app/rules/manifest.json`
   - Replace `abc123.ngrok-free.app` with your actual ngrok domain
   - Keep the `/rules/manifest.json` path
4. Click **"Install"**
5. **Look for the add-on** in your Clockify sidebar under time entries

**What happens next:**
- Clockify sends an `INSTALLED` lifecycle event with your workspace token
- The add-on stores the token and starts processing webhooks
- You can configure rules in the add-on settings page

**Troubleshooting:**
- ❌ **"Invalid manifest"** → Make sure the runtime manifest doesn't have `$schema` field
- ❌ **"Connection failed"** → Verify ngrok is forwarding port 8080 and the add-on is running
- ❌ **"No webhooks"** → Check server logs for `INSTALLED` event, restart add-on if URL changed

**📘 [Complete Setup Guide](FROM_ZERO_SETUP.md)** - Full walkthrough from zero to running
**📖 [Setup Script Guide](docs/SETUP_SCRIPT_GUIDE.md)** - All script options and examples

---

## Configuration (Rules add-on)

`RulesConfiguration` reads `.env.rules` (or real env vars) once at startup, validates every value via `ConfigValidator`, and fans out typed config to controllers, filters, and stores. Start from `.env.rules.example` (or `.env.example` for docker-compose) and adjust the following buckets:

| Category | Variables | Notes |
| --- | --- | --- |
| **Core runtime** | `ADDON_BASE_URL`, `ADDON_PORT`, `CLOCKIFY_API_BASE_URL`, `ENV`, `RULES_APPLY_CHANGES`, `RULES_WEBHOOK_DEDUP_SECONDS` | Base URL/port drive the runtime manifest and Jetty context path. `RULES_APPLY_CHANGES` is exposed through `RuntimeFlags` so you can flip it without rebuilding. Dedup TTL configures `WebhookIdempotencyCache`. |
| **Persistence** | `ENABLE_DB_TOKEN_STORE`, `DB_URL`, `DB_USER`/`DB_USERNAME`, `DB_PASSWORD`, `RULES_DB_URL`, `RULES_DB_USERNAME`, `RULES_DB_PASSWORD` | Rules storage prefers `RULES_DB_*` and falls back to shared `DB_*`. Token storage is enabled when `ENABLE_DB_TOKEN_STORE=true` (or `ENV=prod`) **and** `DB_URL` is set. Missing/invalid DB config fails fast instead of silently falling back to in-memory. |
| **JWT bootstrap (settings iframe + PlatformAuthFilter)** | `CLOCKIFY_JWT_PUBLIC_KEY`, `CLOCKIFY_JWT_PUBLIC_KEY_PEM`, `CLOCKIFY_JWT_PUBLIC_KEY_MAP`, `CLOCKIFY_JWT_JWKS_URI`, `CLOCKIFY_JWT_DEFAULT_KID`, `CLOCKIFY_JWT_EXPECT_ISS`, `CLOCKIFY_JWT_EXPECT_AUD`, `CLOCKIFY_JWT_LEEWAY_SECONDS` | `RulesConfiguration.JwtBootstrapConfig` feeds `JwtVerifier`, which prefers JWKS, then PEM map, then single PEM. `expectedAudience` defaults to the add-on key (“rules”) and `expectedIssuer` defaults to `clockify`. |
| **Network & middleware** | `ADDON_FRAME_ANCESTORS`, `ADDON_RATE_LIMIT`, `ADDON_LIMIT_BY`, `ADDON_CORS_ORIGINS`, `ADDON_CORS_ALLOW_CREDENTIALS`, `ADDON_REQUEST_LOGGING` | These inputs gate SDK middleware. `SecurityHeadersFilter` uses `ADDON_FRAME_ANCESTORS` when building CSP; RateLimiter and CorsFilter are added only when corresponding envs are present. |
| **Dev-only helpers (never set in prod)** | `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN`, `ADDON_SKIP_SIGNATURE_VERIFY` | Guarded by `RulesConfiguration.LocalDevSecrets` + `RuntimeFlags`. Use them to preload a workspace token locally or to skip signature verification during smoke tests. Production builds treat them as disabled even if someone sets them by mistake. |

`PlatformAuthFilter.devFromEnv()` remains a DEV-ONLY helper. Production JWT auth must flow through `RulesConfiguration` so key material never leaks from controllers calling `System.getenv`.

## Quick links
- **From Zero Setup**: FROM_ZERO_SETUP.md ⭐ (Start here!)
- **Setup Scripts**: docs/SETUP_SCRIPT_GUIDE.md 🚀 (One-command launch!)
- Quick Start (Local): docs/QUICK_START_LOCAL.md
- Zero‑Shot: docs/ZERO_SHOT.md
- Ngrok Testing: docs/NGROK_TESTING.md
- SDK Overview: docs/SDK_OVERVIEW.md
- Repo Structure: docs/REPO_STRUCTURE.md
- Build Environment (Java 17): docs/BUILD_ENVIRONMENT.md
- Make Targets: docs/MAKE_TARGETS.md
- CI Overview: docs/CI_OVERVIEW.md
  - Smoke tests: .github/workflows/smoke.yml (fast /health and /metrics)
- Security Checklist: docs/SECURITY_CHECKLIST.md
- AI Zero‑Shot Playbook: docs/AI_ZERO_SHOT_PLAYBOOK.md
- AI START HERE: docs/AI_START_HERE.md
- Docs Index: docs/README.md
- Manifest & Lifecycle: docs/MANIFEST_AND_LIFECYCLE.md
- Parameters Reference: docs/CLOCKIFY_PARAMETERS.md
- PostgreSQL Guide: docs/POSTGRESQL_GUIDE.md
- Rules Add‑on: docs/ADDON_RULES.md
- Architecture: docs/ARCHITECTURE.md

Table of contents
- Requirements
- Quickstart (Rules + Auto‑Tag)
- Why this boilerplate
- Documentation
- Coverage & CI
- Testing Guide
- Troubleshooting

## 🚀 Enterprise Security Hardening & Production-Ready Status

This boilerplate includes **comprehensive enterprise security hardening** and is **fully production-ready**:

### 🔒 Security Hardening Features (All Tested & Verified)
- ✅ **JWT Security**: Algorithm enforcement, strict kid handling, JWKS-based key management
- ✅ **CSRF Protection**: Token-based validation with constant-time comparison, deterministic test bypass
- ✅ **Security Headers**: Comprehensive HTTP security headers (CSP with nonce, HSTS, XSS protection)
- ✅ **RFC-7807 Error Handling**: Standardized problem+json error responses with request ID correlation
- ✅ **Request ID Propagation**: Distributed tracing for all requests across filter chain
- ✅ **Input Validation**: Comprehensive parameter and payload validation with XSS prevention
- ✅ **Path Sanitization**: URL path validation and sanitization
- ✅ **Rate Limiting**: IP and workspace-based request throttling with critical endpoint protection

### 🏗️ Production Enhancements
- ✅ **Persistence**: Database-backed token storage (PostgreSQL/MySQL)
- ✅ **Reliability**: HTTP idempotency, timeouts, retries, health checks
- ✅ **Observability**: Structured logging, metrics, monitoring
- ✅ **Testing**: **638+ tests passing** across 5 layers with CI/CD automation
  - 126 rules addon tests (unit, integration, smoke)
  - 496 SDK tests (filters, middleware, security)
  - 16 auto-tag-assistant tests
  - All CRUD endpoints tested with permission validation
- ✅ **Documentation**: Complete production deployment and security guides

### 🎯 Recent Security Enhancements (November 2025)

**JWT Verification for Lifecycle Handlers** 🔐
- ✅ RS256 signature verification for all `INSTALLED` and `DELETED` events
- ✅ Workspace context extraction in settings UI (JWT payload parsing)
- ✅ Automatic key rotation support with `CLOCKIFY_JWT_PUBLIC_KEY_MAP`
- ✅ Comprehensive JWT testing patterns with `SignatureTestUtil`

**Persistent Token Storage with DatabaseTokenStore** 💾
- ✅ Tokens survive service restarts, container crashes, and pod evictions
- ✅ PostgreSQL/MySQL support with automatic schema management
- ✅ HikariCP connection pooling for production-grade reliability
- ✅ Graceful fallback to in-memory storage if database unavailable

**CI/CD & Build Improvements** 🚀
- ✅ Fixed Maven reactor dependency resolution (verify phase instead of test)
- ✅ Integrated OWASP dependency check with NVD database caching
- ✅ Optimized build performance (13+ minutes → 1-3 minutes on cached runs)
- ✅ All 16 tests in auto-tag-assistant now pass with JWT verification

**Comprehensive Documentation**
- 📖 [JWT Verification Guide](docs/JWT_VERIFICATION_GUIDE.md) - Complete setup and troubleshooting
- 📖 [Database Token Store Guide](docs/DATABASE_TOKEN_STORE.md) - Persistence and migration
- 📖 [Security Guide](docs/SECURITY.md) - All 24 security features explained
- 📖 [Testing Guide](docs/TESTING.md) - 5-layer testing architecture

**See**: [Security Guide](docs/SECURITY.md) | [Testing Guide](docs/TESTING.md) | [Production Deployment Guide](docs/PRODUCTION-DEPLOYMENT.md) | [CHANGELOG](CHANGELOG.md)

## Requirements

| Tool | Version | Notes |
| --- | --- | --- |
| Java | 17 or newer | Verify with `java -version`. |
| Maven | 3.6 or newer | Verify with `mvn -version`. |
| ngrok | Latest | Optional; required only when exposing localhost to Clockify. |

## Quickstart

1. **Clone and enter the repo**
   ```bash
   git clone https://github.com/apet97/boileraddon.git
   cd boileraddon
   ```
2. **Build every module (downloads Maven Central dependencies on first run)**
   ```bash
   mvn clean package -DskipTests
   ```
3. **Expose the server to Clockify (second terminal)**
   ```bash
   ngrok http 8080
   ```
   Copy the HTTPS forwarding domain that ngrok prints (for example `https://abc123.ngrok-free.app`).
4. **Run the Rules add-on (primary production target)**

   The Rules add-on loads configuration from `.env.rules` via `RulesConfiguration`, so copy the template and either run via Make or java -jar:
   ```bash
   cp .env.rules.example .env.rules          # one-time setup
   make dev-rules                            # loads .env.rules automatically
   # or run the fat jar manually:
   ADDON_BASE_URL=https://abc123.ngrok-free.app/rules \
   java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
   ```
   `make dev-rules` wires health/readiness, JWT bootstrap, rate limits, and CSP defaults exactly like production. Use `RULES_APPLY_CHANGES=true` only when you are ready to mutate time entries.

5. **Install in Clockify** – Provide `https://abc123.ngrok-free.app/rules/manifest.json` when installing a custom add-on in **Admin → Add-ons**.

The runtime manifest served at `/rules/manifest.json` stays schema-compliant (no `$schema`, `"schemaVersion": "1.3"`) because it is generated on startup by `RulesApp`.

### Auto-Tag Assistant demo

Need the smaller sample instead? The Auto-Tag Assistant remains available as a demo:

```bash
# Native JVM demo
ADDON_PORT=8080 ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Dockerized demo
ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant \
make docker-run TEMPLATE=auto-tag-assistant
```

Stop and restart the process after starting ngrok so manifests always point at the public HTTPS URL.

### Rules add-on (automation)

Build and run the Rules add‑on to define "if … then …" automations for time entries.

```
make build-rules
# In another terminal: ngrok http 8080 (copy HTTPS URL)
ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/rules make run-rules
# Install using: https://YOUR-NGROK.ngrok-free.app/rules/manifest.json
```
To log actions without changing Clockify, leave `RULES_APPLY_CHANGES` unset (default) or set it to `false`.
To apply actions, set `RULES_APPLY_CHANGES=true`.

See docs/ADDON_RULES.md for API, schema, and examples. For faster local usage, try:

```
cp .env.rules.example .env.rules
make dev-rules
# Health
curl http://localhost:8080/rules/health
```

> 💡 **Secure settings bootstrap:** Provide `CLOCKIFY_JWT_PUBLIC_KEY` (Clockify Marketplace public key in PEM format) so the settings iframe can verify JWTs server-side. When present, the server injects a trusted bootstrap JSON and the browser never decodes JWTs on its own.
>
> 🔐 **Optional claim checks:** Set `CLOCKIFY_JWT_EXPECT_ISS`, `CLOCKIFY_JWT_EXPECT_AUD`, and (optionally) `CLOCKIFY_JWT_LEEWAY_SECONDS` to enforce issuer/audience claims and control clock skew on the JWT bootstrap. For key rotation, provide a JSON map via `CLOCKIFY_JWT_PUBLIC_KEY_MAP='{\"kid-1\":\"-----BEGIN PUBLIC KEY-----...\"}'` and set `CLOCKIFY_JWT_DEFAULT_KID` if a fallback is needed.
>
> ```bash
> # Example: two kid-specific keys with fallback
> export CLOCKIFY_JWT_PUBLIC_KEY_MAP='{"kid-1":"-----BEGIN PUBLIC KEY-----...","kid-2":"-----BEGIN PUBLIC KEY-----..."}'
> export CLOCKIFY_JWT_DEFAULT_KID=kid-1
> ```

### Use a database-backed token store (recommended)

For production, persist installation tokens. This boilerplate includes docs and a sample schema to implement a `DatabaseTokenStore` in your add-on. The demo module uses an in-memory `TokenStore` by default; wire your own persistent store before going to production.

Examples:

```bash
# Enable request logging (headers scrubbed) while testing
export ADDON_REQUEST_LOGGING=true

# Start a local Postgres (optional)
docker compose -f docker-compose.dev.yml up -d

# Export DB env and run the Rules add-on
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons
export ENABLE_DB_TOKEN_STORE=true        # or set ENV=prod
make dev-rules                           # loads .env.rules + overrides above
```

See also: docs/DATABASE_TOKEN_STORE.md and extras/sql/token_store.sql.

> 🔒 **Automatic enabling:** Set `ENABLE_DB_TOKEN_STORE=true` (or run with `ENV=prod` plus `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) and the Rules add-on will automatically switch to the pooled PostgreSQL token store. Misconfigured DB URLs fail fast instead of silently falling back to in-memory storage.

### Security Hardening Quick Start

All security features are enabled by default and validated through comprehensive testing:

```bash
# Verify security hardening is working
./scripts/quick-start.sh
# Run all tests to validate security features
mvn test
```

**Security Features Automatically Enabled:**
- JWT algorithm enforcement and key rotation
- CSRF protection with constant-time validation
- RFC-7807 standardized error responses
- Request ID propagation for distributed tracing
- Comprehensive security headers (CSP, HSTS, XSS protection)
- Input validation and path sanitization

### Optional runtime safeguards

- Security headers: enabled by default; set CSP frame-ancestors via `ADDON_FRAME_ANCESTORS`.
  - Example: `export ADDON_FRAME_ANCESTORS="'self' https://*.clockify.me"`
- Rate limiting: enable via env to throttle by IP or workspace.
  - `export ADDON_RATE_LIMIT=10` (requests/sec)
  - `export ADDON_LIMIT_BY=ip` (or `workspace`)

### Security filters & CSP behavior (Rules add-on)
- `SecurityHeadersFilter` (from the SDK) runs on every request, sets `X-Content-Type-Options`, `Referrer-Policy`, `Cache-Control`, `Permissions-Policy`, and conditionally `Strict-Transport-Security`, then renders a CSP with a per-request nonce. Override `frame-ancestors` via `ADDON_FRAME_ANCESTORS` if you embed outside Clockify.
- The nonce is exposed as `SecurityHeadersFilter.CSP_NONCE_ATTR` so `SettingsController`, `SimpleSettingsController`, and `IftttController` can include inline `<script>`/`<style>` tags safely. If the filter has not run (unit tests), the controllers generate a throwaway nonce.
- `SensitiveHeaderFilter` wraps `HttpServletRequest` so any logging (including `RequestLoggingFilter`) sees `[REDACTED]` for `Authorization`, `X-Addon-Token`, `X-Addon-Lifecycle-Token`, `Clockify-Signature`, `Cookie`, and `Set-Cookie`. Operators should expect those headers to be masked in logs.
- `WorkspaceContextFilter` only activates when `RulesConfiguration.JwtBootstrapConfig` is present. It uses `JwtVerifier` to parse the iframe JWT, attaches workspace/user attributes, and lets controllers read them without touching `System.getenv`.
- `PlatformAuthFilter` is available for portal JWTs. Wire it with the same `JwtVerifier` that `RulesConfiguration` produces; the `devFromEnv()` helper is explicitly marked DEV-ONLY so production never reads raw env vars inside filters.

### Readiness, metrics & idempotency
- `/health` (liveness) is backed by `HealthCheck` plus optional DB/token-store providers, so it returns `503` if either persistence layer is configured but failing.
- `/ready` (readiness) calls `RulesStore.getAll("health-probe")` and `tokenStore.count()` via `ReadinessHandler`, returning `status: DEGRADED` and HTTP 503 if either dependency is unreachable. Point your container readiness probe here.
- `/metrics` exposes Prometheus-compatible counters/timers (request totals, webhook dedupe hits, rule evaluations) via `MetricsHandler`.
- `WebhookIdempotencyCache` hashes workspace/event/payload IDs and keeps them for `RULES_WEBHOOK_DEDUP_SECONDS` (minimum 60s). Duplicate deliveries increment metrics and log a `deduplicated webhook` message instead of re-running business logic.

### CORS support (optional)

Enable a strict allowlist of origins by setting `ADDON_CORS_ORIGINS` to a comma-separated list. The demo app will enable CORS only for matching `Origin` values and short-circuit valid preflight requests.

Examples:

```
export ADDON_CORS_ORIGINS=https://app.clockify.me,https://example.com
export ADDON_CORS_ALLOW_CREDENTIALS=true
make run-auto-tag-assistant
```

Notes:
- The filter replies 204 for valid preflight and sets `Vary: Origin`.
- Credentials are not allowed by default.
- Wildcards for subdomains are supported, e.g. `https://*.example.com` (matches `https://app.example.com` but not the bare `https://example.com`).

### Coverage

CI generates an aggregate JaCoCo coverage site and uploads it as an artifact. The Pages job also publishes the coverage site and a badge generated from the aggregate report (when available).

- Pages URL: https://apet97.github.io/boileraddon/
- Coverage site: https://apet97.github.io/boileraddon/coverage/
- Badge: embedded at the top of this README (falls back to N/A if no aggregate report exists for the push)
- JSON summary: https://apet97.github.io/boileraddon/coverage/summary.json

Build status: ![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)

### Testing Infrastructure

**Comprehensive Test Suite**: **638+ tests passing** across 5 layers with 100% security hardening validation

```bash
# Run all tests
mvn test
# Expected: BUILD SUCCESS

# Run specific module
mvn -pl addons/rules test          # 126 tests
mvn -pl addons/addon-sdk test      # 496 tests

# Run specific test
mvn -pl addons/rules -Dtest='*JwtVerifier*' test
```

See [docs/TESTING.md](docs/TESTING.md) for:
- **Unit Tests**: Individual component testing with Mockito (no mocks for final classes)
- **Integration Tests**: End-to-end API testing with EmbeddedServer
- **Security Tests**: JWT verification, CSRF protection, input validation, permission checks
- **Smoke Tests**: Health checks and basic functionality validation
- **CRUD Endpoint Tests**: Full lifecycle testing with security hardening and workspace scoping
- Module-scoped test commands (avoiding `-am` flag issues)
- Running single tests and targeted test patterns
- Current coverage gates and how they're scoped
- JSON error body expectations in downstream tests
- Mockito configuration for Java 17+ compatibility

### Routing note (SDK)

The SDK routes only exact endpoint paths (no wildcards). If you need to pass an identifier in a REST-ish
operation (e.g., delete), prefer query/body parameters with the exact registered path:

```
// Register once
addon.registerCustomEndpoint("/api/items", handler);

// Client deletes by id
DELETE /api/items?id=<ID>
// or body {"id":"..."}
```

Alternatively, register additional exact paths if needed. Keep the runtime manifest in sync by using SDK
helpers (`registerLifecycleHandler`, `registerWebhookHandler`) which auto-update manifest entries.


## Auto-Tag Assistant Walkthrough

The sample add-on demonstrates the complete lifecycle:

1. `AutoTagAssistantApp` builds a manifest, registers endpoints, and launches Jetty with the inline SDK.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java†L23-L96】
2. `LifecycleHandlers` captures the workspace token and environment claims from the INSTALLED payload and stores them via `TokenStore`.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java†L23-L93】【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/TokenStore.java†L19-L89】
3. `WebhookHandlers` processes time-entry events, and `ClockifyApiClient` shows how to call back into Clockify using the stored credentials.
4. The shared SDK module (`addons/addon-sdk/src/main/java/com/clockify/addon/sdk/`) handles manifest modeling, request routing, and lifecycle/webhook dispatch without any external dependencies.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java†L20-L135】【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java†L15-L200】

Useful test commands while the server is running:

```bash
curl http://localhost:8080/auto-tag-assistant/health
curl http://localhost:8080/auto-tag-assistant/manifest.json
curl http://localhost:8080/auto-tag-assistant/settings
```

## Using the Template Module

Prefer to start from a blank slate? Use the Java template under `addons/_template-addon/`.

### Configure local environment values

1. Copy the defaults: `cp .env.example .env`
2. Edit `.env` and adjust `ADDON_PORT` / `ADDON_BASE_URL` to match your local port or tunneling URL.
3. Start the template add-on: `make dev`

The lightweight loader in the template reads `.env` first and then falls back to real environment variables, so overriding values per shell still works.

### Scaffold a new add-on

Automate the copy/rename steps with the helper script:

```bash
scripts/new-addon.sh my-addon "My Add-on"
```

The script clones the template module, updates the Maven coordinates, rewrites the Java package/class names (including renaming `TemplateAddonApp` to a PascalCase `<Name>App` entry point), refreshes the scaffolded UI labels with your display name, and registers the module in the parent `pom.xml` so it builds alongside the rest of the project. A lightweight smoke test lives at `scripts/test-new-addon.sh` (also wired into `make test`) to ensure the generated manifest still contains required fields and the component label rewrite succeeds.

```bash
# Build only the template
mvn -pl addons/_template-addon package -DskipTests

# Or via Makefile helper
make build-template

# Run the generated fat JAR
java -jar addons/_template-addon/target/_template-addon-0.1.0-jar-with-dependencies.jar
```

**⚠️ Do _not_ run `ngrok http 80` — the add-on binds to port 8080, so ngrok must forward `8080`.**

**Manifest URL reminder:** Clockify needs `https://<your-ngrok-domain>/auto-tag-assistant/manifest.json` (note the `/auto-tag-assistant/manifest.json` path).

🎉 You now have a working, installable Clockify add-on!

**Install in Clockify:**
1. Copy the ngrok HTTPS URL (e.g., `https://abc123.ngrok-free.app`)
2. Go to Clockify → Admin → Add-ons → Install Custom Add-on
3. Enter: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`

### Safe branch cleanup helper

Need to prune remote branches after a demo? Use `scripts/git-delete-branches-except-main.sh`. It now:

- Defaults to a **dry run** (lists deletions only)
- Requires an explicit `--prefix <team/feature>` filter **and** `--yes` before deleting
- Protects `main`, `develop`, `release/*`, and any branches outside the allowlisted prefix

Example:

```bash
./scripts/git-delete-branches-except-main.sh --remote origin --prefix feature/jane --yes
```

Without both flags it prints what would be removed and exits safely.

## What's Included

- ✅ **Working Examples**:
  - `addons/rules/` - Declarative automation rules with conditions, actions, persistence, and observability
  - `addons/auto-tag-assistant/` - Auto-tagging demo for smaller experiments
- ✅ **SDK Module**: `addons/addon-sdk` shared by all add-ons - no external dependencies
- ✅ **Maven Central Only**: All dependencies from public Maven Central (Jackson, Jetty, SLF4J)
- ✅ **No Annotation Processing**: Simple Java 17 classes and builders
- ✅ **No Lombok**: No reflection magic, just plain Java
- ✅ **One-Shot Build**: Clone, build, run - that's it!

## Project Structure

```
boileraddon/
├── pom.xml                                    # Multi-module parent POM
├── README.md                                  # This file
│
├── addons/
│   ├── rules/                                 # Canonical production add-on
│   │   ├── README.md                          # Operations + config guide
│   │   └── src/main/java/com/example/rules/
│   │       ├── RulesApp.java                 # Entry point (registers filters/endpoints)
│   │       ├── config/RulesConfiguration.java# Centralized env loader
│   │       ├── config/RuntimeFlags.java      # Dev-only toggles (apply changes, skip sig)
│   │       ├── security/JwtVerifier.java     # JWKS/PEM verification for iframe + platform auth
│   │       ├── middleware/SensitiveHeaderFilter.java
│   │       ├── cache/WebhookIdempotencyCache.java
│   │       ├── health/ReadinessHandler.java  # `/ready`
│   │       ├── metrics/RulesMetrics.java     # Prometheus counters/timers
│   │       └── store/DatabaseRulesStore.java # Persistent storage (PostgreSQL/MySQL)
│   ├── auto-tag-assistant/                    # Demo/sample add-on
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/autotagassistant/
│   │       ├── AutoTagAssistantApp.java
│   │       ├── SettingsController.java
│   │       ├── LifecycleHandlers.java
│   │       └── WebhookHandlers.java
│   ├── addon-sdk/                             # Shared SDK module (routing, filters, token store)
│   │   └── src/main/java/com/clockify/addon/sdk/
│   │       ├── ClockifyAddon.java
│   │       ├── ClockifyManifest.java
│   │       ├── middleware/* (SecurityHeadersFilter, RateLimiter, CorsFilter, WorkspaceContextFilter)
│   │       └── security/* (TokenStore, WebhookSignatureValidator)
│   └── _template-addon/                      # Minimal starter template module
└── tools/
    └── validate-manifest.py                   # Manifest validation helper
```

## Architecture: In-Repo SDK Module

This boilerplate ships a **first-party SDK module** instead of relying on external artifacts:

> **Deployment Model**
> 
> Every add-on packaged from this repository is expected to run inside its **own JVM/process**. Scaling means starting more JVM
> instances (or containers) of that add-on behind a load balancer—**not** co-locating multiple add-ons inside a shared servlet.
> The inline SDK keeps manifests, lifecycle handlers, and token storage as in-memory state tied to a single process. Dropping
> multiple add-ons into one container blurs those boundaries, forcing you to namespace handler paths, separate manifests, and
> isolate per-workspace credentials manually. To avoid cross-talk and reduce operational risk, run each add-on as its own
> deployable unit.

### Why In-Repo SDK?

**Before (External SDK Problems):**
- Required `com.cake.clockify:addon-sdk` from GitHub Packages
- Complex annotation processing at build time
- Circular dependencies between SDK modules
- Hidden authentication requirements

**Now (In-Repo SDK Benefits):**
- ✅ All SDK code lives in `addons/addon-sdk`
- ✅ No annotation processing complexity
- ✅ Simple, readable, customizable
- ✅ Maven Central dependencies only
- ✅ Works offline after first build

### SDK Components

The SDK module provides everything needed for Clockify add-ons:

```
addons/addon-sdk/src/main/java/com/clockify/addon/sdk/
├── ClockifyAddon.java          # Main addon coordinator
├── ClockifyManifest.java       # Manifest model with builder
├── AddonServlet.java           # HTTP servlet for routing
├── EmbeddedServer.java         # Jetty server wrapper
├── RequestHandler.java         # Request handler interface
└── HttpResponse.java           # Response helper
```

**Dependencies (all from Maven Central):**
- Jackson 2.17.1 (JSON processing)
- Jetty 11.0.20 (HTTP server, including jetty-http, jetty-io, jetty-util, jetty-security)
- Jakarta Servlet 5.0.0 (Servlet API)
- SLF4J 2.0.13 (Logging)

## Building and Running

### Build the Fat JAR

```bash
mvn clean package -DskipTests
```

This produces: `addons/rules/target/rules-0.1.0-jar-with-dependencies.jar` (plus the demo/template fat JARs).

**First build:** Maven downloads dependencies from Maven Central (~5MB)
**Subsequent builds:** Uses cached dependencies (fast)

### Run the Application

```bash
# Default configuration (port 8080, base path /auto-tag-assistant)
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Custom configuration
ADDON_PORT=3000 ADDON_BASE_URL=http://localhost:3000/my-addon \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

You'll see:

```
================================================================================
Auto-Tag Assistant Add-on Starting
================================================================================
Base URL: http://localhost:8080/auto-tag-assistant
Port: 8080
Context Path: /auto-tag-assistant

Endpoints:
  Manifest:  http://localhost:8080/auto-tag-assistant/manifest.json
  Settings:  http://localhost:8080/auto-tag-assistant/settings
  Lifecycle (installed): http://localhost:8080/auto-tag-assistant/lifecycle/installed
  Lifecycle (deleted):   http://localhost:8080/auto-tag-assistant/lifecycle/deleted
  Webhook:   http://localhost:8080/auto-tag-assistant/webhook
  Health:    http://localhost:8080/auto-tag-assistant/health
================================================================================
```

### Test Locally

```bash
# Health check
curl http://localhost:8080/auto-tag-assistant/health

# Manifest (note: no $schema field in runtime manifest)
curl http://localhost:8080/auto-tag-assistant/manifest.json

# Settings UI (returns HTML)
curl http://localhost:8080/auto-tag-assistant/settings
```

### Expose via ngrok

In **another terminal**:

```bash
ngrok http 8080
```

Copy the HTTPS URL (e.g., `https://abc123.ngrok-free.app`).

### Install in Clockify

1. Go to **Clockify** → **Admin** → **Add-ons**
2. Click **"Install Custom Add-on"**
3. Enter manifest URL: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`
4. Click **Install**

### Watch It Work

1. Server logs show the **INSTALLED** lifecycle event with workspace token
2. Open a time entry in Clockify
3. Look for **"Auto-Tag Assistant"** in the sidebar
4. Create/update time entries → server logs show webhook events

## Key Concepts

### Runtime Manifest (No $schema!)

**CRITICAL**: Clockify's `/addons` endpoint **rejects** manifests with `$schema` or unknown fields.

The runtime manifest served at `/manifest.json` is generated programmatically and includes ONLY these fields:

```json
{
  "schemaVersion": "1.3",
  "key": "auto-tag-assistant",
  "name": "Auto-Tag Assistant",
  "description": "Automatically detects and suggests tags for time entries",
  "baseUrl": "http://localhost:8080/auto-tag-assistant",
  "minimalSubscriptionPlan": "FREE",
  "scopes": ["TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"],
  "components": [{
    "type": "sidebar",
    "path": "/settings",
    "label": "Auto-Tag Assistant",
    "accessLevel": "ADMINS"
  }],
  "webhooks": [
    {"event": "NEW_TIMER_STARTED", "path": "/webhook"},
    {"event": "TIMER_STOPPED", "path": "/webhook"},
    {"event": "NEW_TIME_ENTRY", "path": "/webhook"},
    {"event": "TIME_ENTRY_UPDATED", "path": "/webhook"}
  ],
  "lifecycle": [
    {"type": "INSTALLED", "path": "/lifecycle/installed"},
    {"type": "DELETED", "path": "/lifecycle/deleted"}
  ]
}
```

### baseUrl and Context Path

The application automatically extracts the context path from `ADDON_BASE_URL`:

- `ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant` → context path = `/auto-tag-assistant`
- `ADDON_BASE_URL=http://localhost:8080` → context path = `/`

All endpoints are served relative to this context path.

### Store the Auth Token

When Clockify installs your add-on, it sends an **INSTALLED** lifecycle event with a workspace-specific auth token:

```java
addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
    JsonNode payload = parseRequestBody(request);
    String workspaceId = payload.get("workspaceId").asText();
    String authToken = payload.get("authToken").asText();

    // CRITICAL: Store this token!
    // Use it for ALL Clockify API calls for this workspace
    tokenStore.save(workspaceId, authToken);

    return HttpResponse.ok("Installed");
});
```

Use this token for API calls:

```java
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("https://api.clockify.me/api/v1/workspaces/" + workspaceId + "/tags"))
    .header("Authorization", "Bearer " + authToken)
    .GET()
    .build();
```

## Verification Checklist

After cloning and building, verify:

- [ ] `mvn clean package -DskipTests` completes with `BUILD SUCCESS`
- [ ] JAR exists at `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`
- [ ] JAR is ~4-5MB (includes all dependencies)
- [ ] `java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar` starts the server
- [ ] `curl http://localhost:8080/auto-tag-assistant/health` returns `Auto-Tag Assistant is running`
- [ ] `curl http://localhost:8080/auto-tag-assistant/manifest.json` returns valid JSON without `$schema`
- [ ] Manifest includes `"schemaVersion": "1.3"` (NOT `"schema_version"`, NOT `"v1.3"`)
- [ ] Manifest includes `components` with `sidebar` having `accessLevel: "ADMINS"`

## Documentation

* [Architecture Overview](docs/ARCHITECTURE.md) – SDK modules, routing, claim handling, and endpoint registration flow.
* [SDK Overview](docs/SDK_OVERVIEW.md) – Routing core, middleware, and consolidated security utilities.
* [Building Your Own Add-on](docs/BUILDING-YOUR-OWN-ADDON.md) – Copy/rename checklist, manifest customization, token storage, and deployment guidance.
* [Quick Start (Local)](docs/QUICK_START_LOCAL.md) – Minimal steps to run add-ons locally
* [Clockify Parameters](docs/CLOCKIFY_PARAMETERS.md) – Manifest fields, headers, webhooks, and env flags.
* [Overtime Add-on (PM)](docs/ADDON_OVERTIME.md) – Product spec for an overtime policy add-on.

## Why this boilerplate
- Zero external SDK installs — everything lives in the repo.
- Exact‑match routing with a tiny servlet runtime, easy to reason about.
- Consolidated security utilities (TokenStore, signature validation) and middleware.
- Production‑ready Rules add‑on (automation) and Auto‑Tag example.
- Strong docs optimized for AI and humans; SHA‑pinned briefings.

## Coverage & CI
- Coverage site (aggregate JaCoCo): https://apet97.github.io/boileraddon/
- CI runs validate + build + tests on every push/PR.

## Troubleshooting
- If you change ports, ensure `ADDON_PORT` and `ADDON_BASE_URL` align.
- Runtime manifest omits `$schema`; `tools/validate-manifest.py` checks this.
- Webhook header is `clockify-webhook-signature`; verify before processing.

## Troubleshooting

* **Manifest rejected by Clockify** – Ensure you are serving the runtime manifest generated by `ClockifyManifest`; it must omit `$schema` and unknown fields.
* **No webhook traffic** – Verify ngrok is forwarding the same port you used when starting the add-on and reinstall the manifest if the URL changes.
* **Missing tokens** – Check server logs for the `INSTALLED` lifecycle event; confirm `LifecycleHandlers` stored the token for your workspace.
### Production Checklist

Before promoting an add‑on, review:
- Security Checklist: docs/SECURITY_CHECKLIST.md
- Production Deployment: docs/PRODUCTION-DEPLOYMENT.md

### Per‑Addon Zero‑Shot

Each add‑on can declare its own plan, scopes, components, webhooks, and lifecycle paths using the SDK’s manifest builder and registration helpers. Start from the examples in each add‑on README and verify live using the Make targets:

- `make manifest-print` — fetch and pretty‑print runtime manifest
- `make manifest-validate-runtime` — fetch and validate runtime manifest against schema
- `make manifest-validate-all URLS="https://.../rules https://.../auto-tag-assistant"` — validate multiple

Guides:
- Manifest & Lifecycle: docs/MANIFEST_AND_LIFECYCLE.md
- Parameters & scopes: docs/CLOCKIFY_PARAMETERS.md
