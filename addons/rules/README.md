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
| `/metrics` | Prometheus metrics scrape | Not listed in manifest |
| Custom (e.g. `/webhooks/entries`) | Alternative webhook mount | One `webhooks[]` item per event with `path: "/webhooks/entries"` |
| `/health` | Health endpoint (includes DB probe when DB_URL/DB_USER set) | Not listed in manifest |

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
