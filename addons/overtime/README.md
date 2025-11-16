# Overtime Add-on  
[AI START HERE](../../docs/AI_START_HERE.md)

![CI](https://github.com/apet97/boileraddon/actions/workflows/build-and-test.yml/badge.svg)
[![Validate](https://github.com/apet97/boileraddon/actions/workflows/validate.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/validate.yml)
[![Docs](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml/badge.svg)](https://github.com/apet97/boileraddon/actions/workflows/jekyll-gh-pages.yml)
[![Coverage](https://apet97.github.io/boileraddon/coverage/badge.svg)](https://apet97.github.io/boileraddon/coverage/)
[![Docs Index](https://img.shields.io/badge/Docs-Index-blue)](../../docs/README.md)

Policy add-on that enforces or audits overtime rules on time entries. Demonstrates lifecycle handlers, webhooks, and a minimal settings UI.

See also: [Manifest Recipes](../../docs/MANIFEST_RECIPES.md) and [Permissions Matrix](../../docs/PERMISSIONS_MATRIX.md) to tailor plan/scopes/components for your deployment.

## Quick Start

```
cp .env.overtime.example .env.overtime
mvn -q -pl addons/overtime -am package -DskipTests
ADDON_BASE_URL=http://localhost:8080/overtime java -jar addons/overtime/target/overtime-0.1.0-jar-with-dependencies.jar
# In another terminal:
ngrok http 8080
# Restart with HTTPS base URL
ADDON_BASE_URL=https://YOUR.ngrok-free.app/overtime java -jar addons/overtime/target/overtime-0.1.0-jar-with-dependencies.jar
# Install using: https://YOUR.ngrok-free.app/overtime/manifest.json
```

## Manifest (Scopes and Plan)

Overtime reads and may adjust time entries; it also uses tags. It targets the FREE plan by default; adjust `minimalSubscriptionPlan` and scopes to suit your deployment.

```java
ClockifyManifest manifest = ClockifyManifest
    .v1_3Builder()
    .key("overtime")
    .name("Overtime")
    .baseUrl(baseUrl)
    .minimalSubscriptionPlan("FREE")
    .scopes(new String[]{
        "TIME_ENTRY_READ", "TIME_ENTRY_WRITE",
        "TAG_READ", "TAG_WRITE"
    })
    .build();
```

Register settings, lifecycle, and webhooks; the runtime manifest remains synchronized at `/{addon}/manifest.json`.

```java
addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));
addon.registerCustomEndpoint("/settings", new SettingsController());
addon.registerLifecycleHandler("INSTALLED", handler);
addon.registerLifecycleHandler("DELETED",   handler);
addon.registerWebhookHandler("NEW_TIME_ENTRY", "/webhooks/entries", handler);
```

See docs/MANIFEST_AND_LIFECYCLE.md for full guidance and docs/REQUEST-RESPONSE-EXAMPLES.md for request/response shapes.

## Route → Manifest Mapping

| Route | Purpose | Manifest Entry |
|------|---------|----------------|
| `/manifest.json` | Serve runtime manifest | n/a (content of manifest itself) |
| `/settings` | Settings UI | `components[]` item with `type: SETTINGS_SIDEBAR`, `url: /settings` |
| `/lifecycle/installed` | Lifecycle install callback | `lifecycle[]` item `{ type: "INSTALLED", path: "/lifecycle/installed" }` |
| `/lifecycle/deleted` | Lifecycle uninstall callback | `lifecycle[]` item `{ type: "DELETED", path: "/lifecycle/deleted" }` |
| `/webhook` (default) | Time entry webhooks (e.g., NEW_TIME_ENTRY, TIME_ENTRY_UPDATED) | One `webhooks[]` item per event with `path: "/webhook"` |
| `/health` | Health endpoint | Not listed in manifest |
| `/status` | Runtime status (requires bearer token) | Not listed in manifest |
| `/metrics` | Prometheus metrics scrape | Not listed in manifest |

## Checklist: Plan, Scopes, Events

- Plan (minimalSubscriptionPlan)
  - Start with `FREE`; consider `STANDARD`/`PRO` if policy or UI requires.
- Scopes (least privilege)
  - Core: `TIME_ENTRY_READ`, `TIME_ENTRY_WRITE`
  - Helpful: `TAG_READ`, `TAG_WRITE` (if tagging overtime entries)
- Webhook events
  - Time entry: `NEW_TIME_ENTRY`/`TIME_ENTRY_CREATED`, `TIME_ENTRY_UPDATED`
  - Optional: `TIME_ENTRY_DELETED`, `NEW_TIMER_STARTED`, `TIMER_STOPPED`
- References
  - Event payloads: docs/REQUEST-RESPONSE-EXAMPLES.md
  - Full catalog: dev-docs-marketplace-cake-snapshot/
  - Manifest fields: docs/CLOCKIFY_PARAMETERS.md

## Security & Environment

- `.env.overtime.example` captures the base settings (base URL, port, addon key, ENV) plus the required `CLOCKIFY_JWT_*` inputs. Copy it, set `ENV=prod|staging`, and provide one of JWKS/PEM entries before deploying.
- `/api/settings` and `/status` are protected by `PlatformAuthFilter`; pass `Authorization: Bearer <auth_token>` issued by Clockify.
- The settings iframe now relies on an `auth_token` query parameter (or Authorization header). Requests without a valid token return `401`.
- When `ENV=dev`, `GET /overtime/debug/config` returns a sanitized JSON snapshot (environment label, token store mode, JWT bootstrap status) to verify local wiring. The route is never registered outside development.
