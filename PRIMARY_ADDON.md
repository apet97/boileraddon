# Primary Add-on Selection

- **Selected module:** `addons/rules` (`com.example.rules`)
- **Why:** This repository ships multiple sample add-ons, but the `rules` module is the documented production target with the most complete manifest, lifecycle wiring, webhook coverage, UI, and persistence hooks.

## Key artifacts
- **Entrypoint:** `addons/rules/src/main/java/com/example/rules/RulesApp.java`
- **Runtime manifest endpoint:** `/{baseUrl}/manifest.json` (generated from `ClockifyManifest` inside `RulesApp`)
- **Lifecycle handlers:** `/lifecycle/installed` and `/lifecycle/deleted` via `LifecycleHandlers.register`
- **Webhook handlers:** `/webhook` (time entry automations) plus dynamic handlers wired in `WebhookHandlers` and `DynamicWebhookHandlers`
- **UI endpoints:** `/settings`, `/simple`, `/ifttt`, `/status`
- **Observability:** `/health` and `/metrics` registered from `RulesApp`

All subsequent hardening work in this task focuses on `addons/rules`.
