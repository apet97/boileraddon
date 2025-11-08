# Overtime Add-on

Policy add-on that enforces or audits overtime rules on time entries. Demonstrates lifecycle handlers, webhooks, and a minimal settings UI.

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

