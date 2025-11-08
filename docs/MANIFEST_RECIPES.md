# Manifest Recipes (Zero‑Shot)

Opinionated, copy‑pasteable manifest patterns for common add‑on types. All recipes use the runtime (programmatic) manifest via `ClockifyManifest` and the SDK registration helpers so routes and manifest stay in sync. Runtime manifests MUST omit `$schema` and use `schemaVersion: "1.3"`.

See also: docs/MANIFEST_AND_LIFECYCLE.md, docs/CLOCKIFY_PARAMETERS.md, docs/REQUEST-RESPONSE-EXAMPLES.md

## Minimal Add‑on (install/uninstall + settings)

Use when you only need install/uninstall and a settings UI; no webhooks.

```java
ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
  .key("my-addon")
  .name("My Add-on")
  .baseUrl(env("ADDON_BASE_URL"))
  .minimalSubscriptionPlan("FREE")
  .scopes(new String[]{})
  .build();

ClockifyAddon addon = new ClockifyAddon(manifest);
addon.registerCustomEndpoint("/manifest.json", new DefaultManifestController(manifest));

// UI
addon.registerCustomEndpoint("/settings", new SettingsController());
manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Settings", "ADMINS"));

// Lifecycle
addon.registerLifecycleHandler("INSTALLED", handlerInstalled);
addon.registerLifecycleHandler("DELETED",   handlerDeleted);
```

## Sidebar Utility (read‑only)

Settings UI + optional data fetch; no writes to Clockify.

```java
.scopes(new String[]{
  "TIME_ENTRY_READ", "PROJECT_READ", "TAG_READ"
})
```

## Automation/Rules (read + write)

Listens to time entry events and may write tags/updates.

```java
.scopes(new String[]{
  "TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"
})
// Webhooks (default path "/webhook")
addon.registerWebhookHandler("TIME_ENTRY_CREATED", rulesHandler);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", rulesHandler);
```

## Webhook‑Heavy (custom path)

Route all events to a dedicated path.

```java
String path = "/webhooks/entries";
addon.registerWebhookHandler("NEW_TIME_ENTRY", path, handler);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", path, handler);
```

## Premium/Gated Add‑on

Require a higher plan for install.

```java
.minimalSubscriptionPlan("PRO")
```

## Multiple Components

Expose both a sidebar UI and a top‑level nav link.

```java
manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Settings", "ADMINS"));
manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("page", "/dashboard", "Dashboard", "USERS"));
addon.registerCustomEndpoint("/dashboard", new DashboardController());
```

## Lifecycle – custom paths

If you need custom lifecycle paths, pass them explicitly. The SDK sanitizes and updates the manifest accordingly.

```java
addon.registerLifecycleHandler("INSTALLED", "/lifecycle/install", handler);
addon.registerLifecycleHandler("DELETED",   "/lifecycle/remove",  handler);
```

## Observability

Expose health and metrics; not listed in manifest.

```java
addon.registerCustomEndpoint("/health", new HealthCheck("my-addon", "1.0.0"));
addon.registerCustomEndpoint("/metrics", new MetricsHandler());
```

## Security & CORS (optional)

```java
EmbeddedServer server = new EmbeddedServer(new AddonServlet(addon), contextPath);
server.addFilter(new SecurityHeadersFilter());
// Add CORS allowlist via env: ADDON_CORS_ORIGINS="https://app.clockify.me,https://*.example.com"
server.addFilter(new CorsFilter(System.getenv("ADDON_CORS_ORIGINS")));
```

## Checklist Before Install

- minimalSubscriptionPlan set appropriately
- scopes[] = least privilege for your features
- lifecycle[] present with paths you registered
- webhooks[] present with events you need, pointing to your path
- UI components[] added with correct URL endpoints
- baseUrl points to public HTTPS (ngrok/stage/prod)
- Validate live: `make manifest-print` and `make manifest-validate-runtime`

