# Rules Add-on

Automation add-on that applies rule-driven actions to time entries (e.g., tagging entries that match conditions). Includes lifecycle handlers, a settings page, and webhook processing.

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
addon.registerWebhookHandler("TIME_ENTRY_CREATED", handler);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", handler);
// Or supply a custom path, e.g.: addon.registerWebhookHandler("TIME_ENTRY_CREATED", "/webhooks/entries", handler);
```

See docs/MANIFEST_AND_LIFECYCLE.md for manifest/lifecycle patterns and docs/REQUEST-RESPONSE-EXAMPLES.md for full HTTP exchanges.

## Route → Manifest Mapping

| Route | Purpose | Manifest Entry |
|------|---------|----------------|
| `/manifest.json` | Serve runtime manifest | n/a (content of manifest itself) |
| `/settings` | Settings UI | `components[]` item with `type: SETTINGS_SIDEBAR`, `url: /settings` |
| `/lifecycle/installed` | Lifecycle install callback | `lifecycle[]` item `{ type: "INSTALLED", path: "/lifecycle/installed" }` |
| `/lifecycle/deleted` | Lifecycle uninstall callback | `lifecycle[]` item `{ type: "DELETED", path: "/lifecycle/deleted" }` |
| `/webhook` (default) | Time entry webhooks (TIME_ENTRY_CREATED, TIME_ENTRY_UPDATED) | One `webhooks[]` item per event with `path: "/webhook"` |
| Custom (e.g. `/webhooks/entries`) | Alternative webhook mount | One `webhooks[]` item per event with `path: "/webhooks/entries"` |
