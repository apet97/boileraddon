# Clockify Add-on Boilerplate

A clean, **truly self-contained** boilerplate for building Clockify add-ons with **ZERO external authentication required**. Uses a minimal inline SDK with **Maven Central dependencies ONLY** - no GitHub Packages, no private artifacts, no hidden prerequisites.

## Quick Start (ONE Command)

```bash
# Build everything
make build

# Run the working example
make run-auto-tag-assistant
```

Then in another terminal:

```bash
# Expose to Clockify via ngrok
ngrok http 8080
```

🎉 You now have a working, installable Clockify add-on!

## What's Included

- ✅ **Working Example**: `addons/auto-tag-assistant/` - A complete auto-tagging add-on
- ✅ **Clean Template**: `templates/java-basic-addon/` - Minimal starter template
- ✅ **Inline SDK**: Minimal SDK directly in auto-tag-assistant - no external dependencies
- ✅ **Maven Central Only**: All dependencies from public Maven Central (Jackson, Jetty, SLF4J)
- ✅ **Complete Docs**: Full Clockify Marketplace developer documentation snapshot
- ✅ **Build Automation**: Makefile with all common tasks
- ✅ **Scaffolding Script**: Create new add-ons with one command
- ✅ **Validation Tools**: Manifest validation against official schema
- ✅ **Build Verification**: Complete dependency verification guide ([BUILD_VERIFICATION.md](BUILD_VERIFICATION.md))

## Project Structure

```
boileraddon/
├── pom.xml                                    # Multi-module parent POM
├── Makefile                                   # Build automation
├── README.md                                  # This file
│
├── addons/
│   └── auto-tag-assistant/                    # Working example add-on
│       ├── manifest.json                      # Runtime manifest (no $schema!)
│       ├── pom.xml                            # Maven Central dependencies only
│       ├── README.md                          # Detailed implementation guide
│       └── src/main/java/
│           └── com/example/autotagassistant/
│               ├── sdk/                       # Inline minimal SDK
│               │   ├── ClockifyAddon.java
│               │   ├── ClockifyManifest.java
│               │   ├── AddonServlet.java
│               │   ├── EmbeddedServer.java
│               │   ├── RequestHandler.java
│               │   └── HttpResponse.java
│               └── ...                        # App code
│
├── templates/
│   └── java-basic-addon/                      # Starter template
│       ├── manifest.json
│       ├── pom.xml
│       └── src/main/java/...
│
├── dev-docs-marketplace-cake-snapshot/        # Complete documentation snapshot
│   ├── cake_marketplace_dev_docs.md           # Combined docs
│   ├── html/                                  # 48 HTML pages
│   └── extras/
│       ├── addon-java-sdk/                    # Reference SDK (not used in build)
│       ├── manifest-schema-latest.json        # Schema for authoring
│       ├── clockify-openapi.json              # API spec
│       └── webhook-schemas.json               # Event schemas
│
├── scripts/
│   └── new-addon.sh                           # Scaffold new add-ons
│
├── tools/
│   └── validate-manifest.py                   # Manifest validator
│
└── prompts/
    └── zero-shot-addon.md                     # AI generation guidelines
```

## Prerequisites

**Required:**
- **Java 17+** - Verify: `java -version`
- **Maven 3.6+** - Verify: `mvn -version`
- **Internet connection** (first build only, to download from Maven Central)

**Optional:**
- **ngrok** (for local testing with Clockify) - Install: https://ngrok.com/download
- **Python 3** (for manifest validation)

**NOT Required:**
- ❌ GitHub Packages authentication
- ❌ Private artifact repositories
- ❌ External SDK installation

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
- Jetty 11.0.20 (HTTP server)
- Jakarta Servlet 5.0.0 (Servlet API)
- SLF4J 2.0.13 (Logging)

See [BUILD_VERIFICATION.md](BUILD_VERIFICATION.md) for complete dependency list and verification steps.

## Building

### Build Everything

```bash
make build
```

This builds all modules using **Maven Central dependencies only**. No SDK installation needed!

**First build:** Maven downloads dependencies from Maven Central (~50MB)
**Subsequent builds:** Uses cached dependencies (fast)

### Build Individual Modules

```bash
# Just the template
make build-template

# Just auto-tag-assistant
make build-auto-tag-assistant
```

## Running the Auto-Tag Assistant Example

### 1. Build

```bash
make build-auto-tag-assistant
```

### 2. Run

```bash
make run-auto-tag-assistant
```

You'll see:

```
================================================================================
Auto-Tag Assistant Add-on Starting
================================================================================
Base URL: http://localhost:8080/auto-tag-assistant
Port: 8080

Endpoints:
  Manifest:  http://localhost:8080/auto-tag-assistant/manifest.json
  Settings:  http://localhost:8080/auto-tag-assistant/settings
  Lifecycle: http://localhost:8080/auto-tag-assistant/lifecycle
  Webhook:   http://localhost:8080/auto-tag-assistant/webhook
  Health:    http://localhost:8080/auto-tag-assistant/health
================================================================================
```

### 3. Expose via ngrok

In **another terminal**:

```bash
ngrok http 8080
```

Copy the HTTPS URL (e.g., `https://abc123.ngrok-free.app`).

### 4. Update Manifest baseUrl

Edit `addons/auto-tag-assistant/manifest.json`:

```json
{
  "baseUrl": "https://abc123.ngrok-free.app/auto-tag-assistant",
  ...
}
```

**Note**: The manifest served by the running app is programmatic, but you may want to update this for reference.

### 5. Install in Clockify

1. Go to **Clockify** → **Admin** → **Add-ons**
2. Click **"Install Custom Add-on"**
3. Enter manifest URL: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`
4. Click **Install**

### 6. Test It

1. Watch server logs for the **INSTALLED** lifecycle event
2. Open a time entry in Clockify
3. Look for **"Auto-Tag Settings"** in the sidebar
4. Create/update time entries → check logs for webhook events

## Creating a New Add-on

### Option 1: Scaffolding Script (Recommended)

```bash
scripts/new-addon.sh my-awesome-addon "My Awesome Add-on"
```

This creates `addons/my-awesome-addon/` with:
- ✅ Correct package structure (`com.example.myawesomeaddon`)
- ✅ Updated manifest.json (key, name, baseUrl)
- ✅ Maven configuration pointing to local SDK
- ✅ Module added to parent pom.xml

Then:

```bash
# Build it
mvn -f addons/my-awesome-addon/pom.xml clean package

# Run it
java -jar addons/my-awesome-addon/target/my-awesome-addon-0.1.0-jar-with-dependencies.jar
```

### Option 2: Manual Copy

```bash
# Copy template
cp -r templates/java-basic-addon addons/my-addon

# Update manifest.json (key, name, baseUrl)
# Update pom.xml (artifactId, name, mainClass package)
# Update package names in all Java files
# Add <module>addons/my-addon</module> to root pom.xml

# Build
mvn -f addons/my-addon/pom.xml clean package
```

## Key Concepts

### Runtime Manifest (No $schema!)

**CRITICAL**: Clockify's `/addons` endpoint **rejects** manifests with `$schema` or unknown fields.

❌ **WRONG** (will be rejected):

```json
{
  "$schema": "../dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json",
  "version": "1.3",
  ...
}
```

✅ **CORRECT**:

```json
{
  "schemaVersion": "1.3",
  "key": "my-addon",
  "name": "My Add-on",
  "description": "What it does",
  "baseUrl": "http://localhost:8080/my-addon",
  "minimalSubscriptionPlan": "FREE",
  "scopes": ["TIME_ENTRY_READ"],
  "components": [...],
  "webhooks": [...],
  "lifecycle": [...]
}
```

### baseUrl Must Match Server Endpoints

The `baseUrl` in your manifest tells Clockify where to reach your add-on:

```json
{
  "baseUrl": "http://localhost:8080/my-addon",
  "components": [{"type": "TIME_ENTRY_SIDEBAR", "path": "/settings"}],
  "webhooks": [{"path": "/webhook"}],
  "lifecycle": [{"path": "/lifecycle", "type": "INSTALLED"}]
}
```

Your server **MUST** respond to:
- `GET http://localhost:8080/my-addon/manifest.json` → runtime manifest
- `GET http://localhost:8080/my-addon/settings` → sidebar HTML
- `POST http://localhost:8080/my-addon/webhook` → webhook events
- `POST http://localhost:8080/my-addon/lifecycle` → INSTALLED/DELETED

### Store the Addon Token

When Clockify installs your add-on, it sends an **INSTALLED** lifecycle event with a workspace-specific token:

```java
addon.onLifecycleInstalled(request -> {
    String workspaceId = request.getResourceId();
    String addonToken = request.getPayload().get("addonToken").getAsString();

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

## Makefile Targets

```bash
make help                      # Show all targets
make setup                     # Verify Java/Maven installed
make validate                  # Validate all manifests
make build                     # Build everything
make build-sdk                 # Build SDK only
make build-template            # Build template only
make build-auto-tag-assistant  # Build example only
make run-auto-tag-assistant    # Run example
make clean                     # Clean all artifacts
make test                      # Run tests
```

## Validation

Validate manifests before deploying:

```bash
make validate

# Or manually
python3 tools/validate-manifest.py addons/my-addon/manifest.json
```

## Troubleshooting

### "cannot find symbol: package addonsdk"

**Cause**: SDK not built yet.

**Solution**:

```bash
make build-sdk
# or
mvn clean install
```

### Clockify rejects manifest with "unknown field"

**Cause**: Manifest contains `$schema` or invalid fields.

**Solution**:
- Ensure `/manifest.json` endpoint serves programmatically (via `ManifestController`)
- Never copy-paste the manifest.json file directly
- Use only valid fields: `schemaVersion`, `key`, `name`, `description`, `baseUrl`, `minimalSubscriptionPlan`, `scopes`, `components`, `webhooks`, `lifecycle`

### Webhooks not received

**Causes**:
1. ngrok URL changed → restart ngrok, update manifest, reinstall add-on
2. baseUrl mismatch in manifest
3. Events not configured in manifest

**Solution**: Check server logs, verify manifest URL matches running server.

### "401 Unauthorized" when calling Clockify API

**Cause**: Missing or invalid addon token.

**Solution**:
1. Store token from INSTALLED lifecycle event
2. Use correct token for the workspace
3. Use header: `Authorization: Bearer {token}`

## SDK Provenance

The vendored SDK is from https://github.com/clockify/addon-java-sdk (commit at time of snapshot). Check upstream for license and latest changes. This boilerplate vendors it locally for zero-friction development.

## Documentation

All documentation is included:

- **Marketplace Docs**: `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md`
- **Manifest Schema**: `dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json`
- **Clockify API**: `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json`
- **Auto-Tag README**: `addons/auto-tag-assistant/README.md`
- **AI Prompt**: `prompts/zero-shot-addon.md`

## AI Usage Guidelines

If you're an AI generating add-ons in this repo:

1. **Read** `prompts/zero-shot-addon.md` for strict guidelines
2. **Reference** `addons/auto-tag-assistant/` as the canonical working example
3. **Use** `templates/java-basic-addon/` as starting point
4. **Never** emit `$schema` in runtime manifests
5. **Always** use `schemaVersion` (not `version`)
6. **Always** align `baseUrl` + manifest paths + server endpoints
7. **Never** invent manifest fields or webhook events

## Contributing

When adding new examples:

1. Place in `addons/your-addon/`
2. Add `<module>addons/your-addon</module>` to root pom.xml
3. Include README.md with clear "how to run" instructions
4. Validate manifest: `make validate`
5. Test build: `make build`

## License

This boilerplate: [Your license]
Vendored SDK: See `dev-docs-marketplace-cake-snapshot/extras/addon-java-sdk/LICENSE-PLACEHOLDER`
Clockify Docs: © CAKE.com (for reference only)

---

**Have a working add-on in minutes, not hours.**

For questions or issues, see:
- Auto-Tag Assistant README: `addons/auto-tag-assistant/README.md`
- Zero-shot prompt: `prompts/zero-shot-addon.md`
- Developer docs: `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md`
