# Auto-Tag Assistant

A Clockify add-on that automatically manages tags on time entries, ensuring every entry is categorized correctly. For a deep dive into the inline SDK and routing flow that power this example, see [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md).

## Overview

**Auto-Tag Assistant** listens to time entry lifecycle events (start, stop, create, update) and inspects the `tagIds` array. When required tags are missing, the add-on can propose defaults, call the Clockify API to apply tags, or nudge the user through the settings sidebar.

### How It Works

1. **Webhook Event Received** – Clockify sends `NEW_TIMER_STARTED`, `TIMER_STOPPED`, `TIME_ENTRY_UPDATED`, or `NEW_TIME_ENTRY` payloads.
2. **Tag Detection** – `WebhookHandlers.java` parses the payload and evaluates the `tagIds` list.
3. **Signature Verification** – `com.clockify.addon.sdk.security.WebhookSignatureValidator` checks the `clockify-webhook-signature` header using the stored installation token.
4. **Auto-Tag Logic** – Extend `WebhookHandlers.java` to fetch rules, pick defaults, and call the API.
5. **Clockify Update** – Use `ClockifyApiClient.java` to update the time entry or create missing tags.

```text
Clockify Event → Webhook → Tag Detection → (Optional) Auto-Tag → API Update
```

## Features

- 🏷️ **Automatic Tag Detection** – Monitors all supported time entry events.
- ⚙️ **Configurable Rules** – `SettingsController.java` renders a sidebar UI stub for future configuration.
- 🔄 **Real-time Processing** – Responds immediately to webhook payloads.
- 🎯 **Multiple Event Support** – Works with timer start/stop and manual edits.
- 🔐 **Workspace Scoped** – Tokens are isolated per workspace via `com.clockify.addon.sdk.security.TokenStore` and reused for webhook signature validation.

## Architecture

### Key Components

- **`AutoTagAssistantApp.java`** – Bootstraps the embedded Jetty server and registers request handlers.
- **`ManifestController.java`** – Serves `manifest.json` for Clockify discovery.
- **`LifecycleHandlers.java`** – Handles `INSTALLED` and `DELETED` events, persisting tokens in the SDK `TokenStore`.
- **`WebhookHandlers.java`** – Central webhook processor for time entry events.
- **SDK WebhookSignatureValidator** – Verifies webhook requests using the installation token‑derived shared secret.
- **`security/JwtTokenDecoder.java`** – Lightweight helper for decoding Clockify JWTs and extracting environment claims.
- **`ClockifyApiClient.java`** – Minimal HTTP client for Clockify REST calls (GET/PUT/POST).
- **`SettingsController.java`** – Returns the sidebar HTML stub.
- **SDK TokenStore** – In‑memory demo storage for workspace credentials. For production, implement a persistent store.
- **`sdk/` package** – Inline, dependency-free request routing utilities (no external SDK needed).

## Prerequisites

Install the following tools:

- **Java 17+** – Verify with `java -version`.
- **Maven 3.6+** – Verify with `mvn -version`.
- **ngrok** – Required only when exposing localhost to Clockify (https://ngrok.com/download).
- **(Optional) Make** – Provides shortcuts defined in the repository `Makefile`.

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# 2. Build the fat JAR (downloads Maven Central dependencies on first run)
mvn clean package -DskipTests

# 3. Run the Auto-Tag Assistant locally
export CLOCKIFY_WORKSPACE_ID=YOUR_WORKSPACE_ID              # optional, enables webhook validation without re-installing
export CLOCKIFY_INSTALLATION_TOKEN=RAW_INSTALLATION_JWT     # optional, matches the payload from the INSTALLED lifecycle
export CLOCKIFY_API_BASE_URL=https://api.clockify.me/api    # optional override (defaults to production API)

ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

In a **second terminal**:

```bash
# 4. Expose port 8080 to Clockify
ngrok http 8080
```

Finally, **install the manifest** in Clockify:

1. Copy the HTTPS URL from ngrok (for example `https://abc123.ngrok-free.app`).
2. In Clockify, navigate to **Admin → Add-ons → Install Custom Add-on**.
3. Enter `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json` as the manifest URL.

You now have a fully working reference add-on running on your machine.

## Verify Locally

```bash
# Health check
curl http://localhost:8080/auto-tag-assistant/health

# Manifest (runtime manifest has no $schema field)
curl http://localhost:8080/auto-tag-assistant/manifest.json

# Settings HTML (returns inline HTML stub)
curl http://localhost:8080/auto-tag-assistant/settings
```

## Clockify API Usage

- Store the `x-addon-token` and `apiBaseUrl` from the `INSTALLED` lifecycle payload using
  `com.clockify.addon.sdk.security.TokenStore.save(...)`.
- The same installation token is used to derive the shared secret for webhook signatures.
  The SDK `WebhookSignatureValidator` expects it to be saved in TokenStore (Lifecycle handlers do this automatically).
- Every Clockify REST request **must** include the workspace token in the `x-addon-token` header. See `ClockifyApiClient.java` for a production-ready pattern that demonstrates `GET`, `PUT`, and `POST` calls with the correct headers.
- The `apiBaseUrl` can vary per installation (`https://api.clockify.me/api/v1`, staging, etc.). Use the value provided during installation instead of hard-coding endpoints.
- Respect Clockify rate limits (50 requests/second per workspace per add-on) and handle non-200 responses gracefully.

## Configuration & Extensibility

- Extend `WebhookHandlers.java` to implement real tagging logic (load settings, detect missing tags, call the API client). The handler now validates `clockify-webhook-signature` before any processing.
- Replace the HTML stub in `SettingsController.java` with a real React/Vue/vanilla UI and serve static assets.
- Swap the SDK TokenStore for a persistent database in production so tokens survive restarts.
- Use `JwtTokenDecoder` when you need to introspect installation or user tokens (e.g., to discover `backendUrl`, `apiUrl`, or other environment-specific endpoints).

## Production Considerations

1. **Secure Token Storage** – Persist workspace tokens securely (KMS, encrypted DB) instead of the in-memory `TokenStore`.
2. **Webhook Signature Verification** – Validate `clockify-webhook-signature` headers using `WebhookSignatureValidator` (shared secret derived from the installation token). Fail closed (HTTP 401/403) when verification is missing or incorrect.
3. **Error Handling & Retries** – Implement exponential backoff for 429/5xx responses and add structured logging around API calls.
4. **Observability** – Ship logs/metrics to your monitoring system and correlate by workspace ID.

## File Structure

```text
addons/auto-tag-assistant/
├── manifest.json
├── pom.xml
├── README.md
└── src/main/java/com/example/autotagassistant/
    ├── AutoTagAssistantApp.java
    ├── ClockifyApiClient.java
    ├── LifecycleHandlers.java
    ├── ManifestController.java
    ├── SettingsController.java
    ├── WebhookHandlers.java
    └── security/
        └── JwtTokenDecoder.java
```

Note: The SDK runtime, middleware, and security utilities live under `addons/addon-sdk` and are imported via `com.clockify.addon.sdk.*`. This module depends on that SDK for routing, filters, token storage, and webhook signature validation.

## Troubleshooting

- **Webhook not firing?** Confirm ngrok is running on port 8080 and the manifest URL points to `.../auto-tag-assistant/manifest.json`.
- **Auth token missing?** Check the logs for the `INSTALLED` event. The handler stores workspace tokens via `TokenStore.save(...)`.
- **Tag not applied?** Ensure your webhook logic calls `ClockifyApiClient.updateTimeEntryTags(...)` with the correct workspace ID and tag IDs.
- **Build failed?** Clear `~/.m2/repository` entries for Clockify if necessary and rerun `mvn clean package -DskipTests`.

## Manifest Validation

Validate schema compliance before publishing:

```bash
python3 ../../tools/validate-manifest.py manifest.json
```

## Resources

- [Clockify API Documentation](https://docs.clockify.me/)
- [Clockify Marketplace Developer Docs](https://dev-docs.marketplace.cake.com/)
- [Build your own add-on guide](../../docs/BUILDING-YOUR-OWN-ADDON.md)

## License

See the parent repository for licensing details.

## Support

Questions? Open an issue in the root repository.
