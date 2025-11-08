# Zero-Shot Add-on Prompt (Clockify / CAKE Marketplace)

This prompt provides strict guidelines for AI models generating Clockify add-ons in this repository.

## Ground Truth Sources

Use these files as the authoritative reference:

1. **Documentation**: `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md`
   - Complete developer documentation for Clockify Marketplace
   - Covers authentication, webhooks, lifecycle events, API calls

2. **Manifest Schema**: `dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json`
   - JSON Schema for add-on manifests (for IDE validation during authoring)
   - Use for understanding structure, but DO NOT include `$schema` in runtime manifests

3. **Clockify API**: `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json`
   - OpenAPI specification for Clockify API
   - Reference for all API endpoints, parameters, and models
   - Rate limit: 50 requests/second per add-on per workspace

4. **Working Example**: `addons/auto-tag-assistant/`
   - Complete, working add-on implementation
   - Shows proper manifest structure, lifecycle handling, webhook processing
   - Demonstrates Clockify API client usage
   - Uses the shared SDK module (`addons/addon-sdk`)

5. **Template**: `templates/java-basic-addon/`
   - Minimal starter template
   - Use as starting point for new add-ons

## Critical Manifest Rules

### DO NOT Include `$schema` in Runtime Manifests

**CRITICAL**: Clockify's `/addons` endpoint REJECTS manifests with `$schema` or any unknown fields.

- ❌ **WRONG** (authoring-time schema reference - for IDE only):
  ```json
  {
    "$schema": "../dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json",
    "schemaVersion": "1.3",
    ...
  }
  ```

- ✅ **CORRECT** (runtime manifest served to Clockify):
  ```json
  {
    "schemaVersion": "1.3",
    "key": "my-addon",
    "name": "My Add-on",
    "description": "What this add-on does",
    "baseUrl": "http://localhost:8080/my-addon",
    "minimalSubscriptionPlan": "FREE",
    "scopes": ["TIME_ENTRY_READ"],
    "components": [...],
    "webhooks": [...],
    "lifecycle": [...]
  }
  ```

### Valid Manifest Fields

Only use these fields (per AddonManifestDto):

- `schemaVersion` (string, e.g., "1.3") - NOT "version"
- `key` (string, unique identifier, DNS-like)
- `name` (string, display name)
- `description` (string)
- `baseUrl` (string, must be reachable by Clockify)
- `minimalSubscriptionPlan` (string: "FREE", "BASIC", "STANDARD", "PRO", "ENTERPRISE")
- `scopes` (array of strings, e.g., ["TIME_ENTRY_READ", "TIME_ENTRY_WRITE"])
- `components` (array of component objects)
- `webhooks` (array of webhook objects)
- `lifecycle` (array of lifecycle objects)
- `iconPath` (optional string)
- `settings` (optional object)

### BaseURL Must Match Server Endpoints

The `baseUrl` in the manifest MUST match where your server actually serves:

```json
{
  "baseUrl": "http://localhost:8080/my-addon",
  "components": [
    {
      "type": "TIME_ENTRY_SIDEBAR",
      "path": "/settings"  // Clockify will GET {baseUrl}/settings
    }
  ],
  "webhooks": [
    {
      "path": "/webhook"  // Clockify will POST to {baseUrl}/webhook
    }
  ],
  "lifecycle": [
    {
      "type": "INSTALLED",
      "path": "/lifecycle"  // Clockify will POST to {baseUrl}/lifecycle
    }
  ]
}
```

Your server must respond to:
- `GET http://localhost:8080/my-addon/manifest.json` - Returns the manifest
- `GET http://localhost:8080/my-addon/settings` - Returns sidebar HTML
- `POST http://localhost:8080/my-addon/webhook` - Handles webhook events
- `POST http://localhost:8080/my-addon/lifecycle` - Handles INSTALLED/DELETED events

## Project Structure

This repository uses a multi-module Maven structure:

```
boileraddon/
├── pom.xml                                    # Parent POM
├── Makefile                                   # Build automation
├── dev-docs-marketplace-cake-snapshot/
│   └── extras/
│       └── addon-java-sdk/                    # Archived documentation snapshot
│           ├── annotation-processor/
│           └── addon-sdk/
├── templates/
│   └── java-basic-addon/                      # Starter template
├── addons/
│   ├── addon-sdk/                             # Shared SDK module (servlet, manifest, Jetty helpers)
│   └── auto-tag-assistant/                    # Working example that consumes the SDK module
└── scripts/
    └── new-addon.sh                           # Scaffolding script
```

## Building Add-ons

The SDK ships as a dedicated Maven module in this repository - **no GitHub Packages authentication needed**:

```bash
# Build everything (SDK + all add-ons)
make build

# Build just auto-tag-assistant
make build-auto-tag-assistant

# Run auto-tag-assistant
make run-auto-tag-assistant
```

## Creating a New Add-on

### Option 1: Use Scaffolding Script (Recommended)

```bash
scripts/new-addon.sh my-awesome-addon "My Awesome Add-on"
```

This creates `addons/my-awesome-addon/` with:
- Correct package structure
- Updated manifest.json
- Proper Maven configuration
- Module added to parent pom.xml

### Option 2: Manual Creation

1. Copy `templates/java-basic-addon/` to `addons/your-addon/`
2. Update `pom.xml`: change `<artifactId>` and `<name>`
3. Update package names in all Java files
4. Update `manifest.json`: change `key`, `name`, `baseUrl`
5. Add `<module>addons/your-addon</module>` to root `pom.xml`

## Implementation Guidelines

### 1. Lifecycle Events

**INSTALLED Event** (most important):
```java
addon.onLifecycleInstalled(request -> {
    String workspaceId = request.getResourceId();
    JsonObject payload = request.getPayload();

    // CRITICAL: Extract and store auth token
    String authToken = payload.get("authToken").getAsString();

    // TODO: Store token in database keyed by workspaceId
    // You MUST use this token for ALL Clockify API calls

    return HttpResponse.ok("Installed");
});
```

**DELETED Event**:
```java
addon.onLifecycleDeleted(request -> {
    String workspaceId = request.getResourceId();

    // TODO: Clean up stored token and data for this workspace

    return HttpResponse.ok("Deleted");
});
```

### 2. Webhook Events

```java
addon.onWebhook(request -> {
    String eventType = request.getWebhookEvent();
    String workspaceId = request.getResourceId();
    JsonObject payload = request.getPayload();

    // TODO: Process event based on eventType
    // Use stored auth token to make Clockify API calls

    return HttpResponse.ok("Processed");
});
```

### 3. Clockify API Calls

```java
// Retrieve stored token for workspace
String authToken = tokenStore.get(workspaceId);
String baseUrl = "https://api.clockify.me/api/v1";

// Create HTTP client
HttpClient client = HttpClient.newHttpClient();

// Make API call (ALWAYS send the token via the x-addon-token header)
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create(baseUrl + "/workspaces/" + workspaceId + "/tags"))
    .header("x-addon-token", authToken)
    .GET()
    .build();

HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
```

Clockify expects the add-on token exclusively via the `x-addon-token` header—never send it using `Authorization` or any other header.

See `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/ClockifyApiClient.java` for a complete example.

### 4. Sidebar Components

Return valid HTML that will be rendered in an iframe:

```java
addon.registerCustomEndpoint("/settings", request -> {
    String html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Settings</title>
    </head>
    <body>
        <h1>Add-on Settings</h1>
        <p>Configure your add-on here</p>
    </body>
    </html>
    """;

    return HttpResponse.ok(html, "text/html");
});
```

## Running Locally with ngrok

1. **Build**:
   ```bash
   make build-auto-tag-assistant
   ```

2. **Run**:
   ```bash
   make run-auto-tag-assistant
   ```

3. **Expose**:
   ```bash
   ngrok http 8080
   ```

4. **Update manifest** in `addons/auto-tag-assistant/manifest.json`:
   ```json
   {
     "baseUrl": "https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant"
   }
   ```

5. **Install in Clockify**:
   - Go to Admin > Add-ons
   - Click "Install Custom Add-on"
   - Enter: `https://YOUR-SUBDOMAIN.ngrok-free.app/auto-tag-assistant/manifest.json`

## Strict Rules (NEVER Do This)

1. ❌ **NEVER** emit `$schema` in runtime manifest.json served to Clockify
2. ❌ **NEVER** use `"version"` for schema - use `"schemaVersion"`
3. ❌ **NEVER** invent manifest fields not in the schema
4. ❌ **NEVER** invent webhook event types - only use documented ones
5. ❌ **NEVER** hardcode access tokens or secrets
6. ❌ **NEVER** modify files under `dev-docs-marketplace-cake-snapshot/`
7. ❌ **NEVER** assume GitHub Packages access - use the in-repo SDK under `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/`
8. ❌ **NEVER** skip storing the auth token from INSTALLED event
9. ❌ **NEVER** use any auth header other than `x-addon-token: {token}`
10. ❌ **NEVER** exceed rate limits (50 req/s per addon per workspace)

## Environment Variables

Add-ons should read configuration from environment:

- `ADDON_PORT` (default: 8080) - Port to listen on
- `ADDON_BASE_URL` (default: http://localhost:8080/addon-name) - Base URL for endpoints
- `DATABASE_URL` (optional) - Database connection for token storage
- `LOG_LEVEL` (optional) - Logging verbosity

## Testing Manifests

Validate before deploying:

```bash
make validate
# or
python3 tools/validate-manifest.py addons/my-addon/manifest.json
```

## Normalized Specification Format

Before implementing, create a spec in `examples/` using `spec-template.md` format:

```markdown
# Add-on Name

## Purpose
Brief description

## Events
- NEW_TIME_ENTRY
- TIMER_STOPPED

## Scopes
- TIME_ENTRY_READ
- TIME_ENTRY_WRITE

## Components
- TIME_ENTRY_SIDEBAR: /settings

## Logic
1. When timer stops, check if tags exist
2. If missing, analyze description
3. Suggest/apply appropriate tags
```

## References

- **Full example**: `addons/auto-tag-assistant/`
- **Template**: `templates/java-basic-addon/`
- **Docs**: `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md`
- **API Spec**: `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json`
- **Schema**: `dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json` (authoring only)
- **SDK Source**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/`

## Version Pinning

Use versions from `extras/versions.md` if it exists. Otherwise:

- Java: 17
- Maven: 3.6+
- SDK Module: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/` (kept in-repo instead of an external dependency)
- Jackson: 2.17.1
- Jetty: 11.0.20
