# ClockifyManifest.java - Addon Metadata Configuration

**Location**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyManifest.java`

**Type**: Model/Configuration Class

**Purpose**: Represents the addon manifest that describes capabilities, endpoints, and metadata to the Clockify platform

---

## Class Overview

```java
public class ClockifyManifest
```

The manifest is a structured representation of addon configuration that gets serialized to JSON and served at `/manifest.json` endpoint. Clockify platform reads this manifest to understand:
- What events (webhooks) the addon handles
- What UI components the addon provides
- What permissions (scopes) the addon requires
- Where addon endpoints are located

---

## Core Fields

### Identity Fields
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `schemaVersion` | String | "1.3" | Clockify manifest schema version |
| `key` | String | required | Unique addon identifier (e.g., "auto-tag-assistant") |
| `name` | String | required | Display name shown in Clockify UI |
| `description` | String | optional | Addon description for Clockify marketplace |

### Runtime Configuration
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `baseUrl` | String | required | Public URL prefix for all addon endpoints |
| `minimalSubscriptionPlan` | String | "FREE" | Minimum Clockify plan required |
| `scopes` | String[] | [] | OAuth scopes required (e.g., TIME_ENTRY_READ) |

### Component Definitions
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `lifecycle` | List<LifecycleEndpoint> | [] | INSTALLED/DELETED event handlers |
| `webhooks` | List<WebhookEndpoint> | [] | Webhook event handlers |
| `components` | List<ComponentEndpoint> | [] | UI components (sidebars, buttons, etc.) |
| `settings` | Object | null | Custom settings object (addon-specific) |

---

## Nested Classes

### LifecycleEndpoint

```java
public static class LifecycleEndpoint {
    String type;      // "INSTALLED", "DELETED", "UNINSTALLED"
    String path;      // e.g., "/lifecycle/installed"
}
```

**Purpose**: Describes where Clockify should POST when addon is installed/deleted

**Example**:
```json
{
  "type": "INSTALLED",
  "path": "/lifecycle/installed"
}
```

---

### WebhookEndpoint

```java
public static class WebhookEndpoint {
    String event;     // e.g., "TIME_ENTRY_CREATED"
    String path;      // e.g., "/webhook"
}
```

**Purpose**: Describes webhook event handlers

**Example**:
```json
{
  "event": "TIME_ENTRY_CREATED",
  "path": "/webhook"
}
```

**Supported Events** (Clockify events):
- `TIME_ENTRY_CREATED`
- `TIME_ENTRY_UPDATED`
- `TIME_ENTRY_DELETED`
- `NEW_TIMER_STARTED`
- `TIMER_STOPPED`
- `PROJECT_CREATED`
- `TAG_CREATED`
- `CLIENT_CREATED`
- `TASK_CREATED`
- ... and many more

---

### ComponentEndpoint

```java
public static class ComponentEndpoint {
    String type;          // "SIDEBAR", "MODAL", "BUTTON", "EXTENSION"
    String path;          // e.g., "/settings"
    String label;         // Display label shown to user
    String accessLevel;   // "PUBLIC", "PRIVATE", etc.
}
```

**Purpose**: Describes UI components (iframes, modals, buttons) that addon provides

**Example**:
```json
{
  "type": "SIDEBAR",
  "path": "/settings",
  "label": "Auto Tag Settings",
  "accessLevel": "PRIVATE"
}
```

---

## Jackson Annotations

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClockifyManifest
```

**Effect**: Fields with null values are **omitted from JSON** output

**Reason**: Keeps manifest JSON compact; Clockify interprets missing fields as defaults

---

## Builder Pattern

### Creating a Manifest

**Using Builder**:
```java
ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
    .key("auto-tag-assistant")
    .name("Auto Tag Assistant")
    .description("Automatically suggest and apply tags...")
    .baseUrl("http://localhost:8080/auto-tag-assistant")
    .minimalSubscriptionPlan("FREE")
    .scopes(new String[]{"TIME_ENTRY_READ", "TAG_READ", "TAG_WRITE"})
    .build();
```

### Builder Methods

| Method | Parameter | Returns | Effect |
|--------|-----------|---------|--------|
| `key()` | String | Builder | Sets addon key |
| `name()` | String | Builder | Sets display name |
| `description()` | String | Builder | Sets description |
| `baseUrl()` | String | Builder | Sets public URL base |
| `minimalSubscriptionPlan()` | String | Builder | Sets minimum plan |
| `scopes()` | String[] | Builder | Sets required OAuth scopes |
| `build()` | none | ClockifyManifest | Creates manifest with initialized empty collections |

**Key Detail**: `build()` initializes empty lists for lifecycle, webhooks, components:
```java
public ClockifyManifest build() {
    // ...
    this.lifecycle = new ArrayList<>();
    this.webhooks = new ArrayList<>();
    this.components = new ArrayList<>();
    return new ClockifyManifest(this);
}
```

---

## Manifest Registration

### Automatic vs. Manual Registration

**Automatic** (by ClockifyAddon):
```java
addon.registerLifecycleHandler("INSTALLED", handler);
// → Automatically adds to manifest.lifecycle
```

**Manual** (direct manifest updates):
```java
manifest.getLifecycle().add(
    new LifecycleEndpoint("INSTALLED", "/lifecycle/installed")
);
manifest.getWebhooks().add(
    new WebhookEndpoint("TIME_ENTRY_CREATED", "/webhook")
);
```

---

## Serialization & Deserialization

### Manifest to JSON

```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(manifest);

// Result:
// {
//   "schemaVersion": "1.3",
//   "key": "auto-tag-assistant",
//   "name": "Auto Tag Assistant",
//   "baseUrl": "http://localhost:8080/auto-tag-assistant",
//   "scopes": ["TIME_ENTRY_READ", "TAG_READ"],
//   "lifecycle": [
//     {"type": "INSTALLED", "path": "/lifecycle/installed"},
//     {"type": "DELETED", "path": "/lifecycle/deleted"}
//   ],
//   "webhooks": [
//     {"event": "TIME_ENTRY_CREATED", "path": "/webhook"}
//   ]
// }
```

**Note**: Fields with null values omitted (due to @JsonInclude)

### JSON to Manifest

```java
String json = "...";
ObjectMapper mapper = new ObjectMapper();
ClockifyManifest manifest = mapper.readValue(json, ClockifyManifest.class);

// Deserialization uses @JsonCreator constructors in nested classes
```

---

## Common Manifest Patterns

### Pattern 1: Simple Webhook Handler

```java
ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
    .key("simple-logger")
    .name("Simple Logger")
    .baseUrl("http://addon.example.com")
    .scopes(new String[]{"TIME_ENTRY_READ"})
    .build();

ClockifyAddon addon = new ClockifyAddon(manifest);
addon.registerLifecycleHandler("INSTALLED", (req) -> {...});
addon.registerWebhookHandler("TIME_ENTRY_CREATED", (req) -> {...});
```

**Manifest Result**:
```json
{
  "schemaVersion": "1.3",
  "key": "simple-logger",
  "name": "Simple Logger",
  "baseUrl": "http://addon.example.com",
  "scopes": ["TIME_ENTRY_READ"],
  "lifecycle": [
    {"type": "INSTALLED", "path": "/lifecycle/installed"}
  ],
  "webhooks": [
    {"event": "TIME_ENTRY_CREATED", "path": "/webhook"}
  ]
}
```

---

### Pattern 2: With UI Component

```java
manifest.getComponents().add(
    new ComponentEndpoint(
        "SIDEBAR",
        "/settings",
        "Settings",
        "PRIVATE"
    )
);
```

---

### Pattern 3: Multiple Webhook Paths

```java
addon.registerWebhookHandler("TIME_ENTRY_CREATED", "/webhook", handler1);
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", "/webhook", handler2);
addon.registerWebhookHandler("TAG_CREATED", "/tag-events", handler3);
```

**Manifest Result**:
```json
{
  "webhooks": [
    {"event": "TIME_ENTRY_CREATED", "path": "/webhook"},
    {"event": "TIME_ENTRY_UPDATED", "path": "/webhook"},
    {"event": "TAG_CREATED", "path": "/tag-events"}
  ]
}
```

---

## Default Manifest Values

| Field | Default | Behavior |
|-------|---------|----------|
| schemaVersion | "1.3" | Latest schema supported |
| key | required | Must be set, uniquely identifies addon |
| name | required | Must be set, display name |
| baseUrl | required | Runtime value, detected by DefaultManifestController |
| minimalSubscriptionPlan | "FREE" | All plans can use unless restricted |
| scopes | empty array | Addon can work without any scopes (read-only) |
| lifecycle | empty list | Addon doesn't handle install/delete events |
| webhooks | empty list | Addon doesn't handle webhooks |
| components | empty list | Addon doesn't provide UI components |
| settings | null | Omitted from JSON if null |

---

## Scope Types (Common)

| Scope | Purpose | Use Case |
|-------|---------|----------|
| TIME_ENTRY_READ | Read time entries | Any addon that processes entries |
| TIME_ENTRY_WRITE | Modify entries | Automation, tag application |
| TAG_READ | Read workspace tags | Tag suggestion, filtering |
| TAG_WRITE | Create/modify tags | Auto-tag, tag management |
| PROJECT_READ | Read projects | Project-based rules |
| CLIENT_READ | Read clients | Client-based filtering |
| TASK_READ | Read tasks | Task-based rules |
| TASK_WRITE | Create/modify tasks | Task automation |
| REPORT_READ | Access reports | Analytics, aggregation |
| WORKSPACE_READ | Read workspace metadata | General addon functionality |

---

## Validation Rules

### Manifest Validation (Clockify Platform)

Clockify validates manifest during registration:

1. **schemaVersion**: Must be supported (e.g., "1.3")
2. **key**: Must match ^[a-z0-9_-]+$ (lowercase, alphanumeric, -, _)
3. **name**: Must be non-empty string
4. **baseUrl**: Must be valid HTTPS URL (in production)
5. **scopes**: All scopes must be valid Clockify scope names
6. **webhook event names**: Must match Clockify event names
7. **component types**: Must be valid type values

---

## Manifest Endpoint Implementation

Typically served by `DefaultManifestController`:

```java
@Override
public HttpResponse handle(HttpServletRequest request) throws Exception {
    String json = new ObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(manifest);

    return HttpResponse.ok(json, "application/json");
}
```

**Endpoint**: GET `/manifest.json`

**Response**: JSON-formatted manifest

**Headers**: Content-Type: application/json

---

## Dynamic Base URL

The base URL in manifest should reflect the actual deployment URL. `DefaultManifestController` handles this:

```java
// Original manifest baseUrl: http://localhost:8080/auto-tag-assistant
// Request headers show X-Forwarded-Proto: https
// Response manifest baseUrl: https://addon.example.com/auto-tag-assistant
```

This allows addons to be deployed behind proxies without code changes.

---

## Integration with ClockifyAddon

### Lifecycle

1. **Creation**:
   ```java
   ClockifyManifest manifest = ClockifyManifest.v1_3Builder()...build();
   ```

2. **Registration**:
   ```java
   ClockifyAddon addon = new ClockifyAddon(manifest);
   ```

3. **Handler Registration** (updates manifest):
   ```java
   addon.registerLifecycleHandler("INSTALLED", handler);
   // → manifest.lifecycle updated automatically
   ```

4. **Serving**:
   ```java
   addon.registerCustomEndpoint("/manifest.json",
       new DefaultManifestController());
   // → GET /manifest.json serves current manifest
   ```

---

## Testing Strategy

### Unit Tests

```java
@Test
void testManifestBuilder() {
    ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
        .key("test-addon")
        .name("Test")
        .baseUrl("http://localhost:8080")
        .scopes(new String[]{"TIME_ENTRY_READ"})
        .build();

    assertEquals("test-addon", manifest.getKey());
    assertEquals("1.3", manifest.getSchemaVersion());
    assertNotNull(manifest.getLifecycle());
    assertTrue(manifest.getLifecycle().isEmpty());
}

@Test
void testManifestSerialization() {
    ClockifyManifest manifest = ...;
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(manifest);

    assertFalse(json.contains("\"schemaVersion\":null"));  // null fields omitted
    assertTrue(json.contains("\"key\":\"test-addon\""));
}

@Test
void testLifecycleEndpointCreation() {
    LifecycleEndpoint endpoint = new LifecycleEndpoint("INSTALLED", "/webhook");
    assertEquals("INSTALLED", endpoint.getType());
    assertEquals("/webhook", endpoint.getPath());
}
```

---

## Common Mistakes

### Mistake 1: Forgetting to set baseUrl
```java
// ❌ Bad: baseUrl never set
manifest = ClockifyManifest.v1_3Builder()
    .key("addon").name("Addon").build();

// ✅ Good: baseUrl set before serving
manifest = ClockifyManifest.v1_3Builder()
    .key("addon").name("Addon")
    .baseUrl("http://localhost:8080")
    .build();
```

### Mistake 2: Manually updating manifest instead of registering handlers
```java
// ❌ Bad: Manual addition gets out of sync
manifest.getWebhooks().add(new WebhookEndpoint(...));

// ✅ Good: Handler registration auto-updates manifest
addon.registerWebhookHandler("TIME_ENTRY_CREATED", handler);
```

### Mistake 3: Nulling out collections
```java
// ❌ Bad: Returns null, causes NPE
addon.getManifest().getWebhooks().clear();
addon.getManifest().setWebhooks(null);
addon.registerWebhookHandler(...); // NPE!

// ✅ Good: Use clear() to empty collections
addon.getManifest().getWebhooks().clear();
```

---

## Related Files

- **ClockifyAddon.java**: Handler registry
- **DefaultManifestController.java**: Serves manifest with base URL detection
- **AddonServlet.java**: Routes GET /manifest.json requests
- **EmbeddedServer.java**: Hosts servlet
- **TemplateAddonApp.java**: Example manifest creation

---

## Version History

- **1.3**: Current; supports lifecycle, webhooks, components
- **1.2**: Previous version
- **1.1**: Earlier version

---

## Notes for Developers

1. **Schema Version**: Don't change unless upgrading Clockify platform integration
2. **Key Format**: Use kebab-case (lowercase, hyphens): "auto-tag-assistant"
3. **Scope Declaration**: Only declare scopes actually used (principle of least privilege)
4. **Base URL**: Should end without trailing slash (/auto-tag-assistant, not /auto-tag-assistant/)
5. **Component Registration**: Done via manifest.getComponents().add(), not handler registration
6. **Webhook Path**: Can be shared across multiple events (/webhook) or separate (/tag-events, /time-events)
