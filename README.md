# Clockify Add-on Boilerplate

A clean, **truly self-contained** boilerplate for building Clockify add-ons with **ZERO external authentication required**. Uses an in-repo SDK module with **Maven Central dependencies ONLY** - no GitHub Packages, no private artifacts, no hidden prerequisites.

## Requirements

| Tool | Version | Notes |
| --- | --- | --- |
| Java | 17 or newer | Verify with `java -version`. |
| Maven | 3.6 or newer | Verify with `mvn -version`. |
| ngrok | Latest | Optional; required only when exposing localhost to Clockify. |

## Quickstart

1. **Clone and enter the repo**
   ```bash
   git clone https://github.com/apet97/boileraddon.git
   cd boileraddon
   ```
2. **Build every module (downloads Maven Central dependencies on first run)**
   ```bash
   mvn clean package -DskipTests
   ```
3. **Expose the server to Clockify (second terminal)**
   ```bash
   ngrok http 8080
   ```
   Copy the HTTPS forwarding domain that ngrok prints (for example `https://abc123.ngrok-free.app`).
4. **Run the Auto-Tag Assistant example with the ngrok URL**
   ```bash
   ADDON_PORT=8080 ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant \
   java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
   ```
   If you already started the JAR before launching ngrok, stop it and restart with the HTTPS domain so the generated manifest
   points to the public URL.
5. **Install in Clockify** – Provide `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json` when installing a custom add-on in **Admin → Add-ons**.

The runtime manifest served at `/auto-tag-assistant/manifest.json` is already schema-compliant and omits `$schema`, so Clockify accepts it without modification.

## Auto-Tag Assistant Walkthrough

The sample add-on demonstrates the complete lifecycle:

1. `AutoTagAssistantApp` builds a manifest, registers endpoints, and launches Jetty with the inline SDK.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java†L23-L96】
2. `LifecycleHandlers` captures the workspace token and environment claims from the INSTALLED payload and stores them via `TokenStore`.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java†L23-L93】【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/TokenStore.java†L19-L89】
3. `WebhookHandlers` processes time-entry events, and `ClockifyApiClient` shows how to call back into Clockify using the stored credentials.
4. The shared SDK module (`addons/addon-sdk/src/main/java/com/clockify/addon/sdk/`) handles manifest modeling, request routing, and lifecycle/webhook dispatch without any external dependencies.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java†L20-L135】【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java†L15-L200】

Useful test commands while the server is running:

```bash
curl http://localhost:8080/auto-tag-assistant/health
curl http://localhost:8080/auto-tag-assistant/manifest.json
curl http://localhost:8080/auto-tag-assistant/settings
```

## Using the Template Module

Prefer to start from a blank slate? Use the Java template under `templates/java-basic-addon/`.

```bash
# Build only the template
mvn -f templates/java-basic-addon/pom.xml clean package -DskipTests

# Or via Makefile helper
make build-template

# Run the generated fat JAR
java -jar templates/java-basic-addon/target/java-basic-addon-0.1.0-jar-with-dependencies.jar
```

**⚠️ Do _not_ run `ngrok http 80` — the add-on binds to port 8080, so ngrok must forward `8080`.**

**Manifest URL reminder:** Clockify needs `https://<your-ngrok-domain>/auto-tag-assistant/manifest.json` (note the `/auto-tag-assistant/manifest.json` path).

🎉 You now have a working, installable Clockify add-on!

**Install in Clockify:**
1. Copy the ngrok HTTPS URL (e.g., `https://abc123.ngrok-free.app`)
2. Go to Clockify → Admin → Add-ons → Install Custom Add-on
3. Enter: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`

## What's Included

- ✅ **Working Example**: `addons/auto-tag-assistant/` - A complete auto-tagging add-on
- ✅ **SDK Module**: `addons/addon-sdk` shared by all add-ons - no external dependencies
- ✅ **Maven Central Only**: All dependencies from public Maven Central (Jackson, Jetty, SLF4J)
- ✅ **No Annotation Processing**: Simple Java 17 classes and builders
- ✅ **No Lombok**: No reflection magic, just plain Java
- ✅ **One-Shot Build**: Clone, build, run - that's it!

## Project Structure

```
boileraddon/
├── pom.xml                                    # Multi-module parent POM
├── README.md                                  # This file
│
├── addons/
│   ├── addon-sdk/                             # Shared SDK module
│   │   └── src/main/java/com/clockify/addon/sdk/
│   │       ├── ClockifyAddon.java
│   │       ├── ClockifyManifest.java
│   │       ├── AddonServlet.java
│   │       ├── EmbeddedServer.java
│   │       ├── RequestHandler.java
│   │       └── HttpResponse.java
│   └── auto-tag-assistant/                    # Working example add-on
│       ├── pom.xml                            # Maven Central dependencies only
│       ├── README.md                          # Detailed implementation guide
│       └── src/main/java/
│           └── com/example/autotagassistant/
│               ├── AutoTagAssistantApp.java  # Main application
│               ├── ManifestController.java   # Manifest endpoint
│               ├── SettingsController.java   # Settings UI
│               ├── LifecycleHandlers.java    # INSTALLED/DELETED
│               └── WebhookHandlers.java      # Time entry webhooks
├── templates/
│   └── java-basic-addon/                      # Minimal starter template
└── tools/
    └── validate-manifest.py                   # Manifest validation helper
```

## Architecture: In-Repo SDK Module

This boilerplate ships a **first-party SDK module** instead of relying on external artifacts:

### Why In-Repo SDK?

**Before (External SDK Problems):**
- Required `com.cake.clockify:addon-sdk` from GitHub Packages
- Complex annotation processing at build time
- Circular dependencies between SDK modules
- Hidden authentication requirements

**Now (In-Repo SDK Benefits):**
- ✅ All SDK code lives in `addons/addon-sdk`
- ✅ No annotation processing complexity
- ✅ Simple, readable, customizable
- ✅ Maven Central dependencies only
- ✅ Works offline after first build

### SDK Components

The SDK module provides everything needed for Clockify add-ons:

```
addons/addon-sdk/src/main/java/com/clockify/addon/sdk/
├── ClockifyAddon.java          # Main addon coordinator
├── ClockifyManifest.java       # Manifest model with builder
├── AddonServlet.java           # HTTP servlet for routing
├── EmbeddedServer.java         # Jetty server wrapper
├── RequestHandler.java         # Request handler interface
└── HttpResponse.java           # Response helper
```

**Dependencies (all from Maven Central):**
- Jackson 2.17.1 (JSON processing)
- Jetty 11.0.20 (HTTP server, including jetty-http, jetty-io, jetty-util, jetty-security)
- Jakarta Servlet 5.0.0 (Servlet API)
- SLF4J 2.0.13 (Logging)

## Building and Running

### Build the Fat JAR

```bash
mvn clean package -DskipTests
```

This produces: `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`

**First build:** Maven downloads dependencies from Maven Central (~5MB)
**Subsequent builds:** Uses cached dependencies (fast)

### Run the Application

```bash
# Default configuration (port 8080, base path /auto-tag-assistant)
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Custom configuration
ADDON_PORT=3000 ADDON_BASE_URL=http://localhost:3000/my-addon \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

You'll see:

```
================================================================================
Auto-Tag Assistant Add-on Starting
================================================================================
Base URL: http://localhost:8080/auto-tag-assistant
Port: 8080
Context Path: /auto-tag-assistant

Endpoints:
  Manifest:  http://localhost:8080/auto-tag-assistant/manifest.json
  Settings:  http://localhost:8080/auto-tag-assistant/settings
  Lifecycle (installed): http://localhost:8080/auto-tag-assistant/lifecycle/installed
  Lifecycle (deleted):   http://localhost:8080/auto-tag-assistant/lifecycle/deleted
  Webhook:   http://localhost:8080/auto-tag-assistant/webhook
  Health:    http://localhost:8080/auto-tag-assistant/health
================================================================================
```

### Test Locally

```bash
# Health check
curl http://localhost:8080/auto-tag-assistant/health

# Manifest (note: no $schema field in runtime manifest)
curl http://localhost:8080/auto-tag-assistant/manifest.json

# Settings UI (returns HTML)
curl http://localhost:8080/auto-tag-assistant/settings
```

### Expose via ngrok

In **another terminal**:

```bash
ngrok http 8080
```

Copy the HTTPS URL (e.g., `https://abc123.ngrok-free.app`).

### Install in Clockify

1. Go to **Clockify** → **Admin** → **Add-ons**
2. Click **"Install Custom Add-on"**
3. Enter manifest URL: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`
4. Click **Install**

### Watch It Work

1. Server logs show the **INSTALLED** lifecycle event with workspace token
2. Open a time entry in Clockify
3. Look for **"Auto-Tag Assistant"** in the sidebar
4. Create/update time entries → server logs show webhook events

## Key Concepts

### Runtime Manifest (No $schema!)

**CRITICAL**: Clockify's `/addons` endpoint **rejects** manifests with `$schema` or unknown fields.

The runtime manifest served at `/manifest.json` is generated programmatically and includes ONLY these fields:

```json
{
  "schemaVersion": "1.3",
  "key": "auto-tag-assistant",
  "name": "Auto-Tag Assistant",
  "description": "Automatically detects and suggests tags for time entries",
  "baseUrl": "http://localhost:8080/auto-tag-assistant",
  "minimalSubscriptionPlan": "FREE",
  "scopes": ["TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"],
  "components": [{
    "type": "sidebar",
    "path": "/settings",
    "label": "Auto-Tag Assistant",
    "accessLevel": "ADMINS"
  }],
  "webhooks": [
    {"event": "NEW_TIMER_STARTED", "path": "/webhook"},
    {"event": "TIMER_STOPPED", "path": "/webhook"},
    {"event": "NEW_TIME_ENTRY", "path": "/webhook"},
    {"event": "TIME_ENTRY_UPDATED", "path": "/webhook"}
  ],
  "lifecycle": [
    {"type": "INSTALLED", "path": "/lifecycle/installed"},
    {"type": "DELETED", "path": "/lifecycle/deleted"}
  ]
}
```

### baseUrl and Context Path

The application automatically extracts the context path from `ADDON_BASE_URL`:

- `ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant` → context path = `/auto-tag-assistant`
- `ADDON_BASE_URL=http://localhost:8080` → context path = `/`

All endpoints are served relative to this context path.

### Store the Auth Token

When Clockify installs your add-on, it sends an **INSTALLED** lifecycle event with a workspace-specific auth token:

```java
addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
    JsonNode payload = parseRequestBody(request);
    String workspaceId = payload.get("workspaceId").asText();
    String authToken = payload.get("authToken").asText();

    // CRITICAL: Store this token!
    // Use it for ALL Clockify API calls for this workspace
    tokenStore.save(workspaceId, authToken);

    return HttpResponse.ok("Installed");
});
```

Use this token for API calls:

```java
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("https://api.clockify.me/api/v1/workspaces/" + workspaceId + "/tags"))
    .header("Authorization", "Bearer " + authToken)
    .GET()
    .build();
```

## Verification Checklist

After cloning and building, verify:

- [ ] `mvn clean package -DskipTests` completes with `BUILD SUCCESS`
- [ ] JAR exists at `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`
- [ ] JAR is ~4-5MB (includes all dependencies)
- [ ] `java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar` starts the server
- [ ] `curl http://localhost:8080/auto-tag-assistant/health` returns `Auto-Tag Assistant is running`
- [ ] `curl http://localhost:8080/auto-tag-assistant/manifest.json` returns valid JSON without `$schema`
- [ ] Manifest includes `"schemaVersion": "1.3"` (NOT `"schema_version"`, NOT `"v1.3"`)
- [ ] Manifest includes `components` with `sidebar` having `accessLevel: "ADMINS"`

## Documentation

* [Architecture Overview](docs/ARCHITECTURE.md) – SDK modules, routing, claim handling, and endpoint registration flow.
* [Building Your Own Add-on](docs/BUILDING-YOUR-OWN-ADDON.md) – Copy/rename checklist, manifest customization, token storage, and deployment guidance.

## Troubleshooting

* **Manifest rejected by Clockify** – Ensure you are serving the runtime manifest generated by `ClockifyManifest`; it must omit `$schema` and unknown fields.
* **No webhook traffic** – Verify ngrok is forwarding the same port you used when starting the add-on and reinstall the manifest if the URL changes.
* **Missing tokens** – Check server logs for the `INSTALLED` lifecycle event; confirm `LifecycleHandlers` stored the token for your workspace.
