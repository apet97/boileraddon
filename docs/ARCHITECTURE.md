# Architecture Overview

This project ships a fully runnable Clockify add-on plus a minimal inline SDK so you can start building without pulling in external artifacts. The sections below explain how the modules fit together, how requests are routed, how workspace/environment claims are consumed, and how endpoints become available to Clockify.

## Deployment Model

Each add-on built from this boilerplate is intended to run inside its **own JVM/process**. Horizontal scaling is achieved by launching additional identical processes (or containers) of that add-on behind a load balancer. The inline SDK purposely keeps manifests, lifecycle handlers, webhook registries, and workspace token stores as in-memory state that assumes a single add-on per process. Hosting multiple add-ons inside one servlet container means those registries now share memory—you must isolate context paths, manifest builders, handler namespaces, and credential storage by hand to prevent cross-talk. Unless you are prepared to write that extra isolation layer, prefer separate deployable units so every add-on keeps its state boundaries intact.

## Modules at a Glance

The Auto-Tag Assistant example is implemented inside `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/` and is split into two layers:

| Area | Key Classes | Responsibility |
| --- | --- | --- |
| Application | `AutoTagAssistantApp`, `ManifestController`, `SettingsController`, `LifecycleHandlers`, `WebhookHandlers`, `ClockifyApiClient`, `TokenStore` | Bootstraps the add-on, exposes business endpoints, and coordinates persistence of workspace data. |
| Inline SDK | `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon`, `ClockifyManifest`, `AddonServlet`, `EmbeddedServer`, `RequestHandler`, `HttpResponse` | Provides a tiny runtime that replaces the external SDK—responsible for manifest modeling, endpoint registration, HTTP routing, and Jetty startup. |

### Application Layer

* **`AutoTagAssistantApp`** wires everything together. It derives the context path from `ADDON_BASE_URL`, constructs a `ClockifyManifest`, registers endpoints, and launches the embedded Jetty server.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java†L23-L96】
* **Controllers and handlers** (`ManifestController`, `SettingsController`, `LifecycleHandlers`, `WebhookHandlers`) are registered with the inline SDK and encapsulate the behavior for each Clockify integration point.
* **`ClockifyApiClient`** demonstrates how to call back into Clockify’s REST API using the workspace token and base URL.
* **`TokenStore`** keeps workspace-scoped credentials in memory for the sample. Production add-ons should swap this for encrypted, persistent storage.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/TokenStore.java†L1-L107】

### Inline SDK Layer

* **`ClockifyManifest`** mirrors the runtime manifest and exposes builder helpers for schema 1.3.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyManifest.java†L10-L131】
* **`ClockifyAddon`** is the in-memory registry for custom endpoints, lifecycle handlers, and webhook handlers. It also updates the manifest when routes are registered so Clockify sees accurate paths.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java†L20-L135】
* **`AddonServlet`** receives every HTTP request, selects the appropriate registered handler, caches JSON payloads for reuse, and normalizes lifecycle/webhook routing.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java†L15-L200】
* **`EmbeddedServer`** wraps Jetty configuration and deploys the servlet under the detected context path.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/EmbeddedServer.java†L10-L55】

## Request Routing Flow

1. **Jetty startup** – `EmbeddedServer` mounts a single `AddonServlet` instance on the context path extracted from `ADDON_BASE_URL`.
2. **Incoming request** – `AddonServlet.service` logs the method/path and delegates into `handleRequest`.
3. **Custom endpoints** – Any registered path (such as `/manifest.json`, `/settings`, `/health`) is mapped through `ClockifyAddon.getEndpoints()` to the provided handler.
4. **Lifecycle events** – POST requests to `/lifecycle`, `/lifecycle/installed`, or `/lifecycle/deleted` are routed via `ClockifyAddon.getLifecycleHandlers()` or `getLifecycleHandlersByPath()`. JSON payloads are parsed once and cached on the request to avoid repeated stream reads.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java†L41-L190】
5. **Webhooks** – POST `/webhook` resolves the event type using the `clockify-webhook-event-type` header (fallback to body) before dispatching to `ClockifyAddon.getWebhookHandlers()`.
6. **Fallbacks** – Unregistered routes respond with a 404, and unexpected exceptions return a 500 with structured JSON.

## Environment & Claim Handling

Clockify sends workspace-scoped tokens during installation and inside webhook headers. Those tokens carry claims that indicate which environment (production, staging, EU, etc.) should be targeted.

* The `LifecycleHandlers.register` method inspects the INSTALLED payload, extracting `workspaceId`, `authToken`, and `apiUrl`. When present, the token plus API base URL are persisted via `TokenStore.save` for later use.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java†L23-L93】
* `TokenStore` normalizes workspace IDs, trims tokens, and expands partial API URLs (for example, appending `/api/v1` when the claim only includes `/api`). This prevents accidental calls to the wrong host or API version.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/TokenStore.java†L19-L89】
* Downstream components such as `ClockifyApiClient` read from `TokenStore` to pick both the correct token and the correct environment-specific API root before issuing requests.
* When handling JWTs directly (e.g., validating webhook signatures), the example tools under `tools/verify-jwt-example.py` illustrate how to inspect claims.

Always validate that the workspace referenced in a payload matches the workspace claim from the token to avoid cross-tenant leakage. If Clockify rotates the token or changes the API base URL, update the stored record with the latest values from the next lifecycle event.

## Endpoint Registration Lifecycle

The add-on follows the sequence below when starting up:

1. **Manifest construction** – `ClockifyManifest.v1_3Builder()` assembles the base manifest including scopes and the `baseUrl` derived from `ADDON_BASE_URL`.
2. **ClockifyAddon instantiation** – `ClockifyAddon` receives the manifest and initializes empty registries for endpoints, lifecycle hooks, and webhook handlers.
3. **Endpoint registration** – The application calls `registerCustomEndpoint` for `/manifest.json`, `/settings`, and `/health`. Each call adds the handler to the routing map.
4. **Lifecycle registration** – `LifecycleHandlers.register` uses `registerLifecycleHandler` to bind INSTALLED/DELETED to explicit paths. The helper keeps `ClockifyManifest.getLifecycle()` synchronized so Clockify knows which URLs to invoke.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java†L37-L90】
5. **Webhook registration** – `WebhookHandlers.register` calls `registerWebhookHandler` for each event; the SDK auto-adds missing entries into the manifest’s `webhooks` list.
6. **Server start** – `EmbeddedServer.start` opens the listening port, and the add-on is ready for Clockify to fetch the manifest and deliver events.

Use this flow as a reference when creating new add-ons: build your manifest, instantiate `ClockifyAddon`, register every endpoint, then launch the servlet.
