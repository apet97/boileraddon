# Clockify Add-on Boilerplate

A clean, **truly self-contained** boilerplate for building Clockify add-ons with **ZERO external authentication required**. Uses a minimal inline SDK with **Maven Central dependencies ONLY** - no GitHub Packages, no private artifacts, no hidden prerequisites.

## Quick Start (ONE Command)

```bash
# Clone the repository
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# Build (downloads Maven Central dependencies on first run)
mvn clean package -DskipTests

# Run the working example
ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

Then in another terminal:

```bash
# Expose to Clockify via ngrok
ngrok http 8080
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
- ✅ **Inline SDK**: Minimal SDK directly in auto-tag-assistant - no external dependencies
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
└── addons/
    └── auto-tag-assistant/                    # Working example add-on
        ├── pom.xml                            # Maven Central dependencies only
        ├── README.md                          # Detailed implementation guide
        └── src/main/java/
            └── com/example/autotagassistant/
                ├── sdk/                       # Inline minimal SDK
                │   ├── ClockifyAddon.java
                │   ├── ClockifyManifest.java
                │   ├── AddonServlet.java
                │   ├── EmbeddedServer.java
                │   ├── RequestHandler.java
                │   └── HttpResponse.java
                ├── AutoTagAssistantApp.java  # Main application
                ├── ManifestController.java   # Manifest endpoint
                ├── SettingsController.java   # Settings UI
                ├── LifecycleHandlers.java    # INSTALLED/DELETED
                └── WebhookHandlers.java      # Time entry webhooks
```

## Prerequisites

**Required:**
- **Java 17+** - Verify: `java -version`
- **Maven 3.6+** - Verify: `mvn -version`
- **Internet connection** (first build only, to download from Maven Central)

**Optional:**
- **ngrok** (for local testing with Clockify) - Install: https://ngrok.com/download

**NOT Required:**
- ❌ GitHub Packages authentication
- ❌ Private artifact repositories
- ❌ External SDK installation
- ❌ Manual ~/.m2/settings.xml configuration

## Architecture: Inline SDK

This boilerplate uses a **minimal inline SDK** approach instead of external dependencies:

### Why Inline SDK?

**Before (External SDK Problems):**
- Required `com.cake.clockify:addon-sdk` from GitHub Packages
- Complex annotation processing at build time
- Circular dependencies between SDK modules
- Hidden authentication requirements

**Now (Inline SDK Benefits):**
- ✅ All SDK code directly in `src/main/java/.../sdk/`
- ✅ No annotation processing complexity
- ✅ Simple, readable, customizable
- ✅ Maven Central dependencies only
- ✅ Works offline after first build

### SDK Components

The inline SDK provides everything needed for Clockify add-ons:

```
src/main/java/com/example/autotagassistant/sdk/
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

### Store the Addon Token

When Clockify installs your add-on, it sends an **INSTALLED** lifecycle event with a workspace-specific token:

```java
addon.registerLifecycleHandler("INSTALLED", "/lifecycle/installed", request -> {
    JsonNode payload = parseRequestBody(request);
    String workspaceId = payload.get("workspaceId").asText();
    String addonToken = payload.get("addonToken").asText();

    // CRITICAL: Store this token!
    // Use it for ALL Clockify API calls for this workspace
    tokenStore.save(workspaceId, addonToken);

    return HttpResponse.ok("Installed");
});
```

Use this token for API calls:

```java
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("https://api.clockify.me/api/v1/workspaces/" + workspaceId + "/tags"))
    .header("Authorization", "Bearer " + addonToken)
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

## Troubleshooting

### "cannot find symbol" during compilation

**Cause**: Maven dependency resolution failed.

**Solution**:
```bash
mvn clean package -DskipTests -U
```

### Clockify rejects manifest with "unknown field"

**Cause**: Manifest contains `$schema` or invalid fields.

**Solution**:
- The `/manifest.json` endpoint serves programmatically generated JSON
- Never manually edit the runtime manifest
- Use `ManifestController` to ensure only valid fields are included

### Webhooks not received

**Causes**:
1. ngrok URL changed → restart ngrok, reinstall add-on
2. baseUrl mismatch in manifest
3. Events not configured in manifest webhooks array

**Solution**: Check server logs, verify manifest URL matches running server.

### "401 Unauthorized" when calling Clockify API

**Cause**: Missing or invalid addon token.

**Solution**:
1. Store token from INSTALLED lifecycle event
2. Use correct token for the workspace
3. Use header: `Authorization: Bearer {token}`

## Creating Your Own Add-on

Use the Auto-Tag Assistant as a template:

1. **Copy the structure:**
   ```bash
   cp -r addons/auto-tag-assistant addons/my-addon
   ```

2. **Update package names:**
   - Rename `com.example.autotagassistant` → `com.example.myaddon`
   - Update imports in all Java files

3. **Update configuration:**
   - `pom.xml`: Change `<artifactId>`, `<name>`, `<mainClass>`
   - `AutoTagAssistantApp.java`: Update addon key, name, description
   - Add to root `pom.xml`: `<module>addons/my-addon</module>`

4. **Build and run:**
   ```bash
   mvn clean package -DskipTests
   java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar
   ```

## Contributing

When adding new examples:

1. Place in `addons/your-addon/`
2. Add `<module>addons/your-addon</module>` to root pom.xml
3. Include README.md with clear "how to run" instructions
4. Ensure one-shot build works: `mvn clean package -DskipTests`

## License

This boilerplate is provided as-is for educational and development purposes.

---

**Have a working add-on in minutes, not hours.**

For more details, see `addons/auto-tag-assistant/README.md`.
