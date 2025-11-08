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
