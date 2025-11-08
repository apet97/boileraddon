# Clockify Add-on Boilerplate

![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)
[![Validate](https://github.com/apet97/boileraddon/actions/workflows/validate.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/validate.yml)
[![Docs](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml)
![Java](https://img.shields.io/badge/Java-17+-informational)
![Maven](https://img.shields.io/badge/Maven-3.6+-informational)
[![Coverage](https://apet97.github.io/boileraddon/coverage/badge.svg)](https://apet97.github.io/boileraddon/coverage/)

A clean, **self-contained** boilerplate for building Clockify add‑ons with **Maven Central dependencies only** — no private repos, no external SDK installs. It ships a lightweight in‑repo SDK (routing, middleware, security), a production‑ready Rules add‑on, and an Auto‑Tag example.

Quick links
- Quick Start (Local): docs/QUICK_START_LOCAL.md
- Zero‑Shot: docs/ZERO_SHOT.md
- Ngrok Testing: docs/NGROK_TESTING.md
- SDK Overview: docs/SDK_OVERVIEW.md
- Repo Structure: docs/REPO_STRUCTURE.md
- Build Environment (Java 17): docs/BUILD_ENVIRONMENT.md
- Make Targets: docs/MAKE_TARGETS.md
- CI Overview: docs/CI_OVERVIEW.md
- Security Checklist: docs/SECURITY_CHECKLIST.md
- Parameters Reference: docs/CLOCKIFY_PARAMETERS.md
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

## 🚀 New: Production-Ready Improvements

This boilerplate now includes **comprehensive production enhancements**:

- ✅ **Security**: Path sanitization, rate limiting, input validation
- ✅ **Persistence**: Database-backed token storage (PostgreSQL/MySQL)
- ✅ **Reliability**: HTTP timeouts, retries, health checks
- ✅ **Observability**: Structured logging, metrics, monitoring
- ✅ **Testing**: Comprehensive test suite with CI/CD automation
- ✅ **Documentation**: Complete production deployment guide

**See**: [Production Deployment Guide](docs/PRODUCTION-DEPLOYMENT.md) | [Improvements Summary](docs/IMPROVEMENTS-SUMMARY.md) | [CHANGELOG](CHANGELOG.md)

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
4. **Run the Auto-Tag Assistant example (choose your runtime)**

   **Native JVM** – keeps everything on your host machine.
   ```bash
   ADDON_PORT=8080 ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant \
   java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
   ```

   **Docker container** – builds the selected add-on and runs it with the same environment variables.
   ```bash
   ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant make docker-run TEMPLATE=auto-tag-assistant
   ```
   `make docker-run` forwards `ADDON_PORT`/`ADDON_BASE_URL`, publishes the selected port (default `8080`), and uses the
   multi-stage `Dockerfile` to produce a lightweight runtime image. Omit the variables to fall back to
   `http://localhost:<port>/<addon-name>`.

   If you already started the add-on before launching ngrok, stop it and restart with the HTTPS domain so the generated manifest
   points to the public URL.
5. **Install in Clockify** – Provide `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json` when installing a custom add-on in **Admin → Add-ons**.

The runtime manifest served at `/auto-tag-assistant/manifest.json` is already schema-compliant and omits `$schema`, so Clockify accepts it without modification.

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

### Use a database-backed token store (recommended)

For production, persist installation tokens. This boilerplate includes docs and a sample schema to implement a `DatabaseTokenStore` in your add-on. The demo module uses an in-memory `TokenStore` by default; wire your own persistent store before going to production.

Examples:

```
# Enable request logging (headers scrubbed)
export ADDON_REQUEST_LOGGING=true

# Start a local Postgres (optional)
docker compose -f docker-compose.dev.yml up -d

# Export DB env and run
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons
make run-auto-tag-assistant-db
```

See also: docs/DATABASE_TOKEN_STORE.md and extras/sql/token_store.sql.

### Optional runtime safeguards

- Security headers: enabled by default; set CSP frame-ancestors via `ADDON_FRAME_ANCESTORS`.
  - Example: `export ADDON_FRAME_ANCESTORS="'self' https://*.clockify.me"`
- Rate limiting: enable via env to throttle by IP or workspace.
  - `export ADDON_RATE_LIMIT=10` (requests/sec)
  - `export ADDON_LIMIT_BY=ip` (or `workspace`)

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

### Testing Guide

See docs/TESTING_GUIDE.md for:
- Running single tests and modules
- Current coverage gates and how they’re scoped
- JSON error body expectations in downstream tests
- How lifecycle endpoints are dispatched (explicit handler-by-path)
- Mockito notes for newer JDKs during local development

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

## What's Included

- ✅ **Working Examples**:
  - `addons/auto-tag-assistant/` - Auto-tagging for time entries
  - `addons/rules/` - Declarative automation rules with conditions and actions
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
│   ├── _template-addon/                      # Minimal starter template module
│   ├── addon-sdk/                             # Shared SDK module
│   │   └── src/main/java/com/clockify/addon/sdk/
│   │       ├── ClockifyAddon.java
│   │       ├── ClockifyManifest.java
│   │       ├── AddonServlet.java
│   │       ├── EmbeddedServer.java
│   │       ├── RequestHandler.java
│   │       └── HttpResponse.java
│   ├── auto-tag-assistant/                    # Working example add-on
│   │   ├── pom.xml                            # Maven Central dependencies only
│   │   ├── README.md                          # Detailed implementation guide
│   │   └── src/main/java/
│   │       └── com/example/autotagassistant/
│   │           ├── AutoTagAssistantApp.java  # Main application
│   │           ├── ManifestController.java   # Manifest endpoint
│   │           ├── SettingsController.java   # Settings UI
│   │           ├── LifecycleHandlers.java    # INSTALLED/DELETED
│   │           └── WebhookHandlers.java      # Time entry webhooks
│   └── rules/                                 # Rules automation add-on
│       ├── pom.xml                            # Maven Central dependencies only
│       └── src/main/java/
│           └── com/example/rules/
│               ├── RulesApp.java             # Main application
│               ├── RulesController.java      # CRUD API for rules
│               ├── engine/                    # Rule evaluation engine
│               │   ├── Rule.java             # Rule model
│               │   ├── Condition.java        # Condition model
│               │   ├── Action.java           # Action model
│               │   └── Evaluator.java        # Rule evaluator (AND/OR)
│               └── store/
│                   └── RulesStore.java       # In-memory rule storage
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

This produces: `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`

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
