# Manifest and Lifecycle — Clockify Add‑on Boilerplate

This document consolidates how manifests are modeled and served at runtime, and how lifecycle events are registered and handled in this boilerplate. It aims to remove ambiguity between schema examples and the in‑repo SDK behavior.

## Manifest Fundamentals

- schemaVersion: "1.3" (mandatory)
- Never include `$schema` in the runtime manifest (Clockify rejects it).
- baseUrl: must match the public URL (e.g., your ngrok HTTPS + addon context path).
- The runtime manifest is served programmatically by `DefaultManifestController` at `/{addon}/manifest.json`.

Top‑level shape (excerpt):

```json
{
  "schemaVersion": "1.3",
  "key": "my-addon",
  "name": "My Add-on",
  "baseUrl": "https://YOUR.ngrok-free.app/my-addon",
  "components": [ ... ],
  "webhooks": [
    { "event": "NEW_TIME_ENTRY", "path": "/webhook" }
  ],
  "lifecycle": [
    { "type": "INSTALLED", "path": "/lifecycle/installed" },
    { "type": "DELETED",   "path": "/lifecycle/deleted" }
  ]
}
```

Notes:
- Components use a `url` property (UI routes). Webhooks and lifecycle use `path`.
- `tools/manifest.schema.json` validates `lifecycle` as an array of objects with `type` and `path`.

## Runtime Manifest (SDK)

You seldom edit `manifest.json` by hand. Instead, you supply a `ClockifyManifest` instance to the SDK and register routes; the SDK keeps the manifest in sync.

```java
ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
    .key("my-addon")
    .name("My Add-on")
    .baseUrl("https://YOUR.ngrok-free.app/my-addon")
    .scopes(new String[]{ "TIME_ENTRY_READ" })
    .build();

ClockifyAddon addon = new ClockifyAddon(manifest);

// Minimal lifecycle + webhook + settings
addon.registerLifecycleHandler("INSTALLED", request -> HttpResponse.ok("{}","application/json"));
addon.registerLifecycleHandler("DELETED",   request -> HttpResponse.ok("{}","application/json"));
addon.registerWebhookHandler("NEW_TIME_ENTRY", request -> HttpResponse.ok("{}","application/json"));
addon.registerCustomEndpoint("/settings", new SettingsController());
```

Serving the manifest:

```java
// DefaultManifestController renders manifest with per-request baseUrl detection
addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(addon.getManifest()));
```

`DefaultManifestController` computes `baseUrl` per request (useful behind proxies) so your manifest always points to the correct public URL.

## Per‑Addon Variations (Zero‑Shot Friendly)

Different add‑ons require different permissions (scopes), minimum subscription plans, UI components, and webhook subscriptions. The SDK’s manifest builder and registration helpers are designed to make this trivial and zero‑shot for each module.

### Scopes and Plans

Add scopes and set the minimum plan for installation:

```java
ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
    .key("rules")
    .name("Rules")
    .baseUrl(env("ADDON_BASE_URL"))
    .minimalSubscriptionPlan("PRO") // options: FREE, BASIC, STANDARD, PRO, ENTERPRISE
    .scopes(new String[]{
        "TIME_ENTRY_READ", "TIME_ENTRY_WRITE",
        "PROJECT_READ", "TAG_READ", "TAG_WRITE"
    })
    .build();
```

Pick the smallest viable set of scopes for your add‑on. For the full list of scopes, see docs/QUICK-REFERENCE.md.

### UI Components

Components are configured separately from lifecycle/webhooks and use a `url` pointing to a UI endpoint you serve.

```json
{
  "type": "SETTINGS_SIDEBAR",
  "name": "Settings",
  "url": "/settings"
}
```

Register a custom endpoint for the settings page in your add‑on code:

```java
addon.registerCustomEndpoint("/settings", new SettingsController());
```

### Webhook Selection

Subscribe only to the events your add‑on needs and route them to a path you own:

```java
addon.registerWebhookHandler("NEW_TIME_ENTRY", "/webhooks/entries", handler);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", "/webhooks/entries", handler);
```

For a broader event catalog and payloads, see docs/REQUEST-RESPONSE-EXAMPLES.md and the original docs snapshot in `dev-docs-marketplace-cake-snapshot/`.

### Lifecycle Customization

If you need non‑default lifecycle paths, pass your own. The SDK sanitizes and updates the manifest accordingly:

```java
addon.registerLifecycleHandler("INSTALLED", "/lifecycle/install", handler);
addon.registerLifecycleHandler("DELETED",   "/lifecycle/remove",  handler);
```

### Zero‑Shot per Add‑on

Every add‑on in this repo (template, auto‑tag‑assistant, rules, overtime) can define its own:
- `minimalSubscriptionPlan`
- `scopes[]`
- `components[]` (UI)
- `webhooks[]`
- `lifecycle[]` paths

Use the module entrypoint (e.g., `AutoTagAssistantApp`, `RulesApp`) to set the manifest builder fields and register handlers appropriate for that add‑on’s purpose. Keep your manifest synchronized by using the SDK’s `register*` helpers.

## Lifecycle Registration

Lifecycle entries are explicit and path‑based. The SDK normalizes and auto‑registers lifecycle paths in the manifest.

```java
// Use default sanitized paths if you omit the path argument
addon.registerLifecycleHandler("INSTALLED", handler);                  // → /lifecycle/installed
addon.registerLifecycleHandler("DELETED",   handler);                  // → /lifecycle/deleted

// Or provide custom paths (sanitized by PathSanitizer)
addon.registerLifecycleHandler("INSTALLED", "/my/install", handler);
```

Path safety:
- `PathSanitizer.sanitizeLifecyclePath(lifecycleType, path)` ensures the path starts with `/`, has no duplicate slashes, rejects `..`, null bytes (real `\u0000`, `%00`, literal `\\0`), and strips control characters.
- If you pass `null` or empty, the SDK uses `/lifecycle/{type.toLowerCase()}`.

Dispatch model:
- The servlet dispatches lifecycle requests by exact path to the registered handler for that path.
- Lifecycle routes are not treated as webhooks; they are first‑class, explicit paths.

## Webhook Registration

Webhooks are mapped by event to a path (default `/webhook`).

```java
// Default path for all events
addon.registerWebhookHandler("NEW_TIME_ENTRY", handler);              // → /webhook

// Custom path per event
addon.registerWebhookHandler("NEW_TIME_ENTRY", "/webhooks/events", handler);
```

The SDK updates the manifest’s `webhooks[]` with `{ event, path }` entries as you register them.

## Lifecycle Payloads (Example)

Installed:
```http
POST /{addon}/lifecycle/installed HTTP/1.1
Content-Type: application/json
clockify-webhook-signature: sha256=...

{
  "event": "INSTALLED",
  "workspaceId": "...",
  "installationToken": "...",
  "userId": "...",
  "timestamp": "..."
}
```

Deleted:
```http
POST /{addon}/lifecycle/deleted HTTP/1.1
Content-Type: application/json
clockify-webhook-signature: sha256=...

{
  "event": "DELETED",
  "workspaceId": "...",
  "userId": "...",
  "timestamp": "..."
}
```

Recommendations:
- Store `installationToken` securely on INSTALLED (see `DatabaseTokenStore`).
- Remove workspace data on DELETED.
- Validate signatures for lifecycle callbacks if present (same header as webhooks in examples).

## Common Pitfalls

- Including `$schema` in runtime manifest → Clockify rejects it. Omit it.
- Using `event/url` for lifecycle → Use `type/path` instead.
- Forgetting to keep manifest and routes in sync → Use SDK registration helpers; they auto‑update manifest entries.
- Not sanitizing custom lifecycle paths → PathSanitizer enforces sane defaults and rejects unsafe inputs.

## See Also (Reference)

- docs/CLOCKIFY_PARAMETERS.md — manifest fields, headers, env flags, and link to webhook catalog
- docs/QUICK-REFERENCE.md — scopes list and component/webhook examples
- dev-docs-marketplace-cake-snapshot/ — upstream developer docs snapshot preserved in this repo
