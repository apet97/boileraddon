# ClockifyAddon.java

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java`

**Package:** `com.clockify.addon.sdk`

**Lines:** 173

---

## Overview

`ClockifyAddon` is the central coordinator class that manages all handlers and manifest metadata for a Clockify add-on. It serves as the main configuration point where developers register endpoints, lifecycle handlers, and webhook handlers.

## Purpose

- Coordinate addon configuration and handler registration
- Manage mapping between paths and handlers
- Auto-update manifest when handlers are registered
- Provide unified API for addon configuration

---

## Class Structure

```java
public class ClockifyAddon {
    // Constants
    public static final String DEFAULT_WEBHOOK_PATH = "/webhook";

    // State
    private final ClockifyManifest manifest;
    private final Map<String, RequestHandler> endpoints;
    private final Map<String, RequestHandler> lifecycleHandlers;
    private final Map<String, RequestHandler> lifecycleHandlersByPath;
    private final Map<String, String> lifecyclePathsByType;
    private final Map<String, Map<String, RequestHandler>> webhookHandlersByPath;
    private final Map<String, String> webhookPathsByEvent;
}
```

---

## Constructor

### `ClockifyAddon(ClockifyManifest manifest)`

**Parameters:**
- `manifest` - Manifest configuration for this addon

**Usage:**

```java
ClockifyManifest manifest = new ClockifyManifest()
    .name("My Addon")
    .schemaVersion("1.3");

ClockifyAddon addon = new ClockifyAddon(manifest);
```

---

## Core Methods

### registerCustomEndpoint

**Signature:** `public void registerCustomEndpoint(String path, RequestHandler handler)`

**Purpose:** Register a custom HTTP endpoint

**Parameters:**
- `path` - Path relative to addon base URL (e.g., `/manifest.json`, `/settings`)
- `handler` - RequestHandler implementation to process requests

**Example:**

```java
// Register manifest endpoint
addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

// Register settings page
addon.registerCustomEndpoint("/settings", new SettingsController());

// Register API endpoint with lambda
addon.registerCustomEndpoint("/api/data", request -> {
    String json = fetchData();
    return HttpResponse.ok(json, "application/json");
});
```

**Implementation Details:**
- Path is normalized using `PathSanitizer.sanitize()`
- Stored in `endpoints` map for routing
- No automatic manifest update (custom endpoints not in manifest)

---

### registerLifecycleHandler

**Signature:** `public void registerLifecycleHandler(String lifecycleType, RequestHandler handler)`

**Purpose:** Register lifecycle event handler (INSTALLED, DELETED)

**Parameters:**
- `lifecycleType` - Lifecycle event type (`"INSTALLED"` or `"DELETED"`)
- `handler` - RequestHandler to process lifecycle event

**Example:**

```java
// INSTALLED handler
addon.registerLifecycleHandler("INSTALLED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();
    String token = payload.get("installationToken").asText();
    String apiBaseUrl = payload.get("apiBaseUrl").asText();

    // Store token
    TokenStore.save(workspaceId, token, apiBaseUrl);

    return HttpResponse.ok("{\"status\":\"installed\"}");
});

// DELETED handler
addon.registerLifecycleHandler("DELETED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();

    // Cleanup
    TokenStore.delete(workspaceId);

    return HttpResponse.ok("{\"status\":\"deleted\"}");
});
```

**Implementation Details:**
- Default path is `/lifecycle/{type_lowercase}` (e.g., `/lifecycle/installed`)
- Handler stored in `lifecycleHandlers` map by type
- Handler stored in `lifecycleHandlersByPath` map by path
- Path mapping stored in `lifecyclePathsByType`
- **Automatically adds/updates manifest lifecycle endpoints**

**Overload:**

```java
public void registerLifecycleHandler(String lifecycleType, String path, RequestHandler handler)
```

Allows custom path instead of default.

---

### registerWebhookHandler

**Signature:** `public void registerWebhookHandler(String event, RequestHandler handler)`

**Purpose:** Register webhook event handler

**Parameters:**
- `event` - Webhook event type (e.g., `"TIME_ENTRY_CREATED"`)
- `handler` - RequestHandler to process webhook event

**Example:**

```java
addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    JsonNode timeEntry = payload.get("timeEntry");
    String workspaceId = payload.get("workspaceId").asText();

    // Process time entry
    processTimeEntry(workspaceId, timeEntry);

    return HttpResponse.ok("{\"status\":\"ok\"}");
});

addon.registerWebhookHandler("TIME_ENTRY_UPDATED", request -> {
    // Handle update
    return HttpResponse.ok("{\"status\":\"ok\"}");
});
```

**Implementation Details:**
- Default webhook path is `/webhook` (see `DEFAULT_WEBHOOK_PATH`)
- Handlers stored in `webhookHandlersByPath` map: `path -> event -> handler`
- Path mapping stored in `webhookPathsByEvent`: `event -> path`
- **Automatically adds/updates manifest webhook subscriptions**
- Multiple events can share the same path

**Overload:**

```java
public void registerWebhookHandler(String event, String path, RequestHandler handler)
```

Allows custom path instead of default `/webhook`.

**Advanced Example (multiple paths):**

```java
// Group time entry events on one path
addon.registerWebhookHandler("TIME_ENTRY_CREATED", "/webhook/time-entries", handler1);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", "/webhook/time-entries", handler2);

// Group project events on another path
addon.registerWebhookHandler("PROJECT_CREATED", "/webhook/projects", handler3);
```

---

## Getters

### getManifest

**Signature:** `public ClockifyManifest getManifest()`

**Returns:** The manifest associated with this addon

---

### getEndpoints

**Signature:** `public Map<String, RequestHandler> getEndpoints()`

**Returns:** Map of all registered custom endpoints (path -> handler)

**Usage:**

```java
Map<String, RequestHandler> endpoints = addon.getEndpoints();
RequestHandler handler = endpoints.get("/settings");
```

---

### getLifecycleHandlers

**Signature:** `public Map<String, RequestHandler> getLifecycleHandlers()`

**Returns:** Map of lifecycle handlers by type (`INSTALLED` -> handler)

---

### getLifecycleHandlersByPath

**Signature:** `public Map<String, RequestHandler> getLifecycleHandlersByPath()`

**Returns:** Map of lifecycle handlers by path (`/lifecycle/installed` -> handler)

---

### getWebhookHandlers

**Signature:** `public Map<String, RequestHandler> getWebhookHandlers()`

**Returns:** Map of webhook handlers for default path (`/webhook`)

**Note:** This is a convenience method equivalent to:
```java
getWebhookHandlersByPath().get(DEFAULT_WEBHOOK_PATH)
```

---

### getWebhookHandlersByPath

**Signature:** `public Map<String, Map<String, RequestHandler>> getWebhookHandlersByPath()`

**Returns:** Nested map: `path -> event -> handler`

**Usage:**

```java
Map<String, Map<String, RequestHandler>> allWebhooks = addon.getWebhookHandlersByPath();
Map<String, RequestHandler> webhooksAtDefaultPath = allWebhooks.get("/webhook");
RequestHandler handler = webhooksAtDefaultPath.get("TIME_ENTRY_CREATED");
```

---

### getWebhookPathsByEvent

**Signature:** `public Map<String, String> getWebhookPathsByEvent()`

**Returns:** Map of event types to their registered paths

**Usage:**

```java
Map<String, String> paths = addon.getWebhookPathsByEvent();
String path = paths.get("TIME_ENTRY_CREATED"); // "/webhook"
```

---

## Private Helper Methods

### normalizeLifecyclePath

**Signature:** `private String normalizeLifecyclePath(String lifecycleType, String path)`

**Purpose:** Normalize lifecycle path using PathSanitizer

**Logic:**
- If custom path provided, use it
- Otherwise generate default: `/lifecycle/{type_lowercase}`

---

### normalizeWebhookPath

**Signature:** `private String normalizeWebhookPath(String path)`

**Purpose:** Normalize webhook path using PathSanitizer

---

## Data Structure Details

### Handler Storage Strategy

**Lifecycle Handlers (3 maps):**
1. `lifecycleHandlers` - By type (`INSTALLED` -> handler)
2. `lifecycleHandlersByPath` - By path (`/lifecycle/installed` -> handler)
3. `lifecyclePathsByType` - Type to path mapping

**Why 3 maps?** Allows efficient lookup by either type or path during routing.

**Webhook Handlers (2 maps):**
1. `webhookHandlersByPath` - Nested map: `path -> event -> handler`
2. `webhookPathsByEvent` - Event to path mapping

**Why nested map?** Multiple events can share the same path (e.g., all at `/webhook`).

---

## Usage Examples

### Complete Addon Setup

```java
// 1. Build manifest
ClockifyManifest manifest = new ClockifyManifest()
    .schemaVersion("1.3")
    .name("My Automation Addon")
    .description("Automate your workflows")
    .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE"});

// 2. Create addon
ClockifyAddon addon = new ClockifyAddon(manifest);

// 3. Register manifest endpoint
addon.registerCustomEndpoint("/manifest.json",
    new ManifestController(manifest));

// 4. Register lifecycle handlers
addon.registerLifecycleHandler("INSTALLED", request -> {
    // Save installation token
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    TokenStore.save(
        payload.get("workspaceId").asText(),
        payload.get("installationToken").asText(),
        payload.get("apiBaseUrl").asText()
    );
    return HttpResponse.ok("{\"status\":\"installed\"}");
});

addon.registerLifecycleHandler("DELETED", request -> {
    // Cleanup
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    TokenStore.delete(payload.get("workspaceId").asText());
    return HttpResponse.ok("{\"status\":\"deleted\"}");
});

// 5. Register webhook handlers
addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
    // Process event
    return HttpResponse.ok("ok");
});

// 6. Register custom endpoints
addon.registerCustomEndpoint("/settings", new SettingsController());
addon.registerCustomEndpoint("/health", new HealthCheck("my-addon", "1.0"));

// 7. Start server
AddonServlet servlet = new AddonServlet(addon);
EmbeddedServer server = new EmbeddedServer(servlet, "/my-addon");
server.start(8080);
```

---

## Manifest Auto-Update

When you register lifecycle or webhook handlers, the manifest is **automatically updated**:

```java
// Before registration
manifest.getLifecycle() // empty list
manifest.getWebhooks()  // empty list

// Register handlers
addon.registerLifecycleHandler("INSTALLED", handler);
addon.registerWebhookHandler("TIME_ENTRY_CREATED", handler);

// After registration
manifest.getLifecycle() // [{type: "INSTALLED", path: "/lifecycle/installed"}]
manifest.getWebhooks()  // [{event: "TIME_ENTRY_CREATED", path: "/webhook"}]
```

This ensures the manifest always reflects registered handlers.

---

## Thread Safety

**Not thread-safe.** All handler registration should be done during application startup before starting the server.

Typical pattern:
```java
// Setup (single-threaded)
ClockifyAddon addon = new ClockifyAddon(manifest);
addon.registerLifecycleHandler(...);
addon.registerWebhookHandler(...);

// Start server (handlers frozen)
server.start(8080);
```

---

## Related Classes

- **ClockifyManifest** - Manifest builder and container
- **AddonServlet** - Uses ClockifyAddon for routing
- **RequestHandler** - Handler interface
- **PathSanitizer** - Path normalization utility

---

## See Also

- [AddonServlet.md](./AddonServlet.md) - Request routing
- [ClockifyManifest.md](./ClockifyManifest.md) - Manifest structure
- [RequestHandler.md](./RequestHandler.md) - Handler interface

---

**File Location:** `/home/user/boileraddon/addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java`

**Last Updated:** 2025-11-09
