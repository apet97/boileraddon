# RulesApp.java

**Location:** `addons/rules/src/main/java/com/example/rules/RulesApp.java`

**Package:** `com.example.rules`

**Lines:** 388

---

## Overview

`RulesApp` is the main entry point for the **Rules Addon** - a comprehensive automation engine that provides IFTTT-style rules for Clockify time entries. It demonstrates a complete, production-ready addon with database storage, caching, and extensive API.

## Purpose

- Main entry point for Rules addon
- Configure and start embedded server
- Register all endpoints (lifecycle, webhooks, API, UI)
- Initialize database stores
- Configure middleware (security, CORS, rate limiting)
- Provide health checks and metrics

---

## Features Demonstrated

- ✅ Database-backed storage (PostgreSQL)
- ✅ In-memory caching (workspace data)
- ✅ CRUD API for rules
- ✅ Visual rule builder (IFTTT-style UI)
- ✅ Rule evaluation engine
- ✅ Multiple webhook handlers
- ✅ Security middleware
- ✅ Health checks with DB probe
- ✅ Prometheus metrics
- ✅ Development mode (local token preload)

---

## Main Method

### main()

**Signature:** `public static void main(String[] args)`

**Purpose:** Application entry point

**Flow:**

```
1. Read configuration from environment
2. Build manifest programmatically
3. Create ClockifyAddon instance
4. Initialize stores (rules, tokens)
5. Register all endpoints:
   - Manifest
   - Settings UI
   - IFTTT builder
   - Rules CRUD API
   - Cache API
   - Catalog API
   - Lifecycle handlers
   - Webhook handlers
   - Health & metrics
6. Configure middleware
7. Start embedded Jetty server
8. Add shutdown hook
```

---

## Configuration

### Environment Variables

```java
// Server
String baseUrl = ConfigValidator.validateUrl(
    System.getenv("ADDON_BASE_URL"),
    "http://localhost:8080/rules",
    "ADDON_BASE_URL"
);
int port = ConfigValidator.validatePort(
    System.getenv("ADDON_PORT"),
    8080,
    "ADDON_PORT"
);

// Database
String dbUrl = System.getenv("DB_URL");
String dbUser = System.getenv().getOrDefault("DB_USER",
    System.getenv("DB_USERNAME"));
String dbPassword = System.getenv("DB_PASSWORD");

// Features
boolean applyChanges = "true".equalsIgnoreCase(
    System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false")
);

// Middleware
String rateLimit = System.getenv("ADDON_RATE_LIMIT");
String cors = System.getenv("ADDON_CORS_ORIGINS");
boolean requestLogging = "true".equalsIgnoreCase(
    System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false")
);

// Development
String workspaceId = System.getenv("CLOCKIFY_WORKSPACE_ID");
String installationToken = System.getenv("CLOCKIFY_INSTALLATION_TOKEN");
```

---

## Manifest Configuration

```java
ClockifyManifest manifest = ClockifyManifest
    .v1_3Builder()
    .key("rules")
    .name("Rules")
    .description("Declarative automations for Clockify: if conditions then actions")
    .baseUrl(baseUrl)
    .minimalSubscriptionPlan("FREE")
    .scopes(new String[]{
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "PROJECT_READ"
    })
    .build();

// Add sidebar component
manifest.getComponents().add(
    new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Rules", "ADMINS")
);
```

**Scopes Required:**
- `TIME_ENTRY_READ` - Read time entries
- `TIME_ENTRY_WRITE` - Modify time entries
- `TAG_READ` - Read tags
- `TAG_WRITE` - Create/modify tags
- `PROJECT_READ` - Read projects

---

## Endpoint Registration

### Core Endpoints

```java
// GET /rules/manifest.json - Runtime manifest (no $schema)
addon.registerCustomEndpoint("/manifest.json",
    new ManifestController(manifest));

// GET /rules/ - Settings (convenience)
// GET /rules/settings - Settings sidebar
addon.registerCustomEndpoint("/", new SettingsController());
addon.registerCustomEndpoint("/settings", new SettingsController());
addon.registerCustomEndpoint("/settings/", new SettingsController());

// GET /rules/ifttt - IFTTT builder UI
addon.registerCustomEndpoint("/ifttt", new IftttController());
addon.registerCustomEndpoint("/ifttt/", new IftttController());
```

**URL Aliases:** Multiple paths registered to avoid 404 on trailing slashes.

---

### Rules CRUD API

```java
// GET/POST/DELETE /rules/api/rules
addon.registerCustomEndpoint("/api/rules", request -> {
    String method = request.getMethod();
    if ("GET".equals(method)) {
        return rulesController.listRules().handle(request);
    } else if ("POST".equals(method)) {
        return rulesController.saveRule().handle(request);
    } else if ("DELETE".equals(method)) {
        return rulesController.deleteRule().handle(request);
    } else {
        return HttpResponse.error(405,
            "{\"error\":\"Method not allowed\"}", "application/json");
    }
});

// POST /rules/api/test - Dry-run evaluation
addon.registerCustomEndpoint("/api/test",
    rulesController.testRules());
```

**Operations:**
- `GET /api/rules?workspaceId=...` - List all rules
- `POST /api/rules?workspaceId=...` - Save rule
- `DELETE /api/rules?workspaceId=...&ruleId=...` - Delete rule
- `POST /api/test?workspaceId=...` - Test rules (dry-run)

---

### Cache API

```java
// GET /rules/api/cache?workspaceId=... - Cache summary
addon.registerCustomEndpoint("/api/cache", request -> {
    String ws = request.getParameter("workspaceId");
    var snap = WorkspaceCache.get(ws);

    String json = new ObjectMapper().createObjectNode()
        .put("workspaceId", ws)
        .put("tags", snap.tagsById.size())
        .put("projects", snap.projectsById.size())
        .put("clients", snap.clientsById.size())
        .put("users", snap.usersById.size())
        .toString();

    return HttpResponse.ok(json, "application/json");
});

// POST /rules/api/cache/refresh?workspaceId=... - Force refresh
addon.registerCustomEndpoint("/api/cache/refresh", request -> {
    String ws = request.getParameter("workspaceId");
    var wk = TokenStore.get(ws).orElseThrow();
    WorkspaceCache.refresh(ws, wk.apiBaseUrl(), wk.token());
    return HttpResponse.ok("{\"status\":\"refreshed\"}", "application/json");
});

// GET /rules/api/cache/data?workspaceId=... - Full cache data
addon.registerCustomEndpoint("/api/cache/data", request -> {
    // Returns tags, projects, clients, users, tasks as JSON arrays
    // for autocomplete and dropdown population
});
```

**Cache Features:**
- Stores workspace tags, projects, clients, users, tasks
- Reduces API calls to Clockify
- Refreshable on demand
- Used by rule builder UI for autocomplete

---

### Catalog API

```java
// GET /rules/api/catalog/triggers - Available webhook triggers
addon.registerCustomEndpoint("/api/catalog/triggers", request -> {
    JsonNode json = TriggersCatalog.triggersToJson();
    return HttpResponse.ok(json.toString(), "application/json");
});

// GET /rules/api/catalog/actions - Available API endpoints
addon.registerCustomEndpoint("/api/catalog/actions", request -> {
    JsonNode json = OpenAPISpecLoader.endpointsToJson();
    return HttpResponse.ok(json.toString(), "application/json");
});
```

**Purpose:** Provide metadata for rule builder UI.

**Triggers Example:**

```json
[
  {
    "id": "TIME_ENTRY_CREATED",
    "displayName": "When time entry is created",
    "description": "Triggered when a new time entry is created"
  },
  ...
]
```

**Actions Example:**

```json
[
  {
    "id": "updateTimeEntry",
    "displayName": "Update time entry",
    "endpoint": "PUT /workspaces/{workspaceId}/time-entries/{timeEntryId}",
    "parameters": [...]
  },
  ...
]
```

---

### Status Endpoint

```java
// GET /rules/status?workspaceId=... - Runtime status
addon.registerCustomEndpoint("/status", request -> {
    String ws = request.getParameter("workspaceId");
    boolean tokenPresent = TokenStore.get(ws).isPresent();
    boolean applyChanges = "true".equalsIgnoreCase(
        System.getenv().getOrDefault("RULES_APPLY_CHANGES", "false")
    );

    String json = new ObjectMapper().createObjectNode()
        .put("workspaceId", ws)
        .put("tokenPresent", tokenPresent)
        .put("applyChanges", applyChanges)
        .put("baseUrl", baseUrl)
        .toString();

    return HttpResponse.ok(json, "application/json");
});
```

**Purpose:** Debug endpoint to check addon state.

---

### Lifecycle & Webhooks

```java
// Lifecycle handlers (INSTALLED, DELETED)
LifecycleHandlers.register(addon, rulesStore);

// Webhook handlers (static + legacy)
WebhookHandlers.register(addon, rulesStore);

// Dynamic webhook handlers (IFTTT-style rules)
DynamicWebhookHandlers.registerDynamicEvents(addon, rulesStore);
```

---

### Health & Metrics

```java
// Health check with DB probe
HealthCheck health = new HealthCheck("rules", "0.1.0");
if (dbUrl != null && dbUser != null) {
    health.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
        @Override
        public String getName() { return "database"; }

        @Override
        public HealthCheckResult check() {
            try {
                DatabaseRulesStore dbStore = new DatabaseRulesStore(dbUrl, dbUser, dbPassword);
                int n = dbStore.getAll("health-probe").size();
                return new HealthCheckResult("database", true, "Connected", n);
            } catch (Exception e) {
                return new HealthCheckResult("database", false, e.getMessage());
            }
        }
    });
}
addon.registerCustomEndpoint("/health", health);

// Prometheus metrics
addon.registerCustomEndpoint("/metrics", new MetricsHandler());
```

---

## Store Initialization

### selectRulesStore()

**Signature:** `private static RulesStoreSPI selectRulesStore()`

**Purpose:** Choose between database or in-memory storage

**Logic:**

```java
private static RulesStoreSPI selectRulesStore() {
    String rulesDbUrl = System.getenv("RULES_DB_URL");
    String dbUrl = System.getenv("DB_URL");

    if ((rulesDbUrl != null && !rulesDbUrl.isBlank()) ||
        (dbUrl != null && !dbUrl.isBlank())) {
        try {
            return DatabaseRulesStore.fromEnvironment();
        } catch (Exception e) {
            System.err.println("Failed to init DatabaseRulesStore: " +
                e.getMessage() + "; falling back to in-memory");
        }
    }

    return new RulesStore(); // In-memory
}
```

**Environment Priority:**
1. `RULES_DB_URL` (if set)
2. `DB_URL` (fallback)
3. In-memory (default)

---

## Development Helpers

### preloadLocalSecrets()

**Signature:** `private static void preloadLocalSecrets()`

**Purpose:** Pre-populate TokenStore for local development

**Environment Variables:**
- `CLOCKIFY_WORKSPACE_ID` - Workspace to preload
- `CLOCKIFY_INSTALLATION_TOKEN` - Installation token
- `CLOCKIFY_API_BASE_URL` - API base URL (optional, defaults to production)

**Usage:**

```bash
# .env.rules
CLOCKIFY_WORKSPACE_ID=62e123abc456def789012345
CLOCKIFY_INSTALLATION_TOKEN=eyJhbGc...
CLOCKIFY_API_BASE_URL=https://api.clockify.me/api
```

**Implementation:**

```java
private static void preloadLocalSecrets() {
    String workspaceId = System.getenv("CLOCKIFY_WORKSPACE_ID");
    String installationToken = System.getenv("CLOCKIFY_INSTALLATION_TOKEN");

    if (workspaceId == null || workspaceId.isBlank() ||
        installationToken == null || installationToken.isBlank()) {
        return;
    }

    String apiBaseUrl = System.getenv().getOrDefault(
        "CLOCKIFY_API_BASE_URL",
        "https://api.clockify.me/api"
    );

    TokenStore.save(workspaceId, installationToken, apiBaseUrl);
    System.out.println("Preloaded installation token for workspace " + workspaceId);
}
```

**Benefit:** Skip INSTALLED lifecycle event during local testing.

---

### sanitizeContextPath()

**Signature:** `static String sanitizeContextPath(String baseUrl)`

**Purpose:** Extract context path from base URL

**Examples:**

```
Input: "http://localhost:8080/rules"  → Output: "/rules"
Input: "https://example.com/"         → Output: "/"
Input: "https://example.com"          → Output: "/"
```

---

## Middleware Configuration

### Security Headers (Always Enabled)

```java
server.addFilter(new SecurityHeadersFilter());
```

**Headers Added:**
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer`
- `Strict-Transport-Security` (HTTPS only)
- `Content-Security-Policy` (if `ADDON_FRAME_ANCESTORS` set)

---

### Rate Limiter (Optional)

```java
String rateLimit = System.getenv("ADDON_RATE_LIMIT");
if (rateLimit != null && !rateLimit.isBlank()) {
    double permits = Double.parseDouble(rateLimit.trim());
    String limitBy = System.getenv().getOrDefault("ADDON_LIMIT_BY", "ip");

    server.addFilter(new RateLimiter(permits, limitBy));
    System.out.println("RateLimiter enabled: " + permits + "/sec by " + limitBy);
}
```

**Environment:**
```bash
ADDON_RATE_LIMIT=10      # 10 requests/second
ADDON_LIMIT_BY=workspace # or "ip"
```

---

### CORS (Optional)

```java
String cors = System.getenv("ADDON_CORS_ORIGINS");
if (cors != null && !cors.isBlank()) {
    server.addFilter(new CorsFilter(cors));
    System.out.println("CORS enabled for origins: " + cors);
}
```

**Environment:**
```bash
ADDON_CORS_ORIGINS=https://*.clockify.me,https://app.example.com
```

---

### Request Logging (Optional)

```java
if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))) {
    server.addFilter(new RequestLoggingFilter());
    System.out.println("Request logging enabled (sensitive headers redacted)");
}
```

---

## Server Startup

```java
// Create servlet and server
AddonServlet servlet = new AddonServlet(addon);
EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

// Add middleware
server.addFilter(new SecurityHeadersFilter());
// ... optional filters ...

// Print startup info
System.out.println("=".repeat(80));
System.out.println("Rules Add-on Starting");
System.out.println("=".repeat(80));
System.out.println("Base URL: " + baseUrl);
System.out.println("Port: " + port);
System.out.println();
System.out.println("Endpoints:");
System.out.println("  Manifest:  " + baseUrl + "/manifest.json");
System.out.println("  Settings:  " + baseUrl + "/settings");
System.out.println("  Lifecycle: " + baseUrl + "/lifecycle/installed");
System.out.println("  Webhook:   " + baseUrl + "/webhook");
System.out.println("  Health:    " + baseUrl + "/health");
System.out.println("=".repeat(80));

// Add shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        server.stop();
    } catch (Exception ignored) {}
}));

// Start server (blocks)
server.start(port);
```

**Output Example:**

```
================================================================================
Rules Add-on Starting
================================================================================
Base URL: http://localhost:8080/rules
Port: 8080
Context Path: /rules

Endpoints:
  Manifest:  http://localhost:8080/rules/manifest.json
  Settings:  http://localhost:8080/rules/settings
  Lifecycle: http://localhost:8080/rules/lifecycle/installed
             http://localhost:8080/rules/lifecycle/deleted
  Webhook:   http://localhost:8080/rules/webhook
  Health:    http://localhost:8080/rules/health
  Rules API: http://localhost:8080/rules/api/rules
================================================================================
```

---

## Running the Addon

### Build

```bash
mvn -pl addons/rules package -DskipTests
```

### Run Locally

```bash
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
```

### Run with Environment

```bash
# Set environment variables
export ADDON_BASE_URL=http://localhost:8080/rules
export ADDON_PORT=8080
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons

# Run
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
```

### Run with ngrok

```bash
# Terminal 1: Start addon
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# Terminal 2: Start ngrok
ngrok http 8080

# Terminal 3: Restart with ngrok URL
export ADDON_BASE_URL=https://abc123.ngrok-free.app/rules
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
```

---

## Deployment

### Docker

```bash
docker build --build-arg ADDON_DIR=addons/rules -t clockify-rules .
docker run -p 8080:8080 --env-file .env.rules clockify-rules
```

### systemd

```ini
[Service]
WorkingDirectory=/opt/clockify
Environment="ADDON_BASE_URL=https://addons.example.com/rules"
Environment="DB_URL=jdbc:postgresql://localhost:5432/addons"
Environment="DB_USERNAME=addons"
Environment="DB_PASSWORD=secret"
ExecStart=/usr/bin/java -jar /opt/clockify/rules-0.1.0-jar-with-dependencies.jar
```

---

## Related Files

- **RulesController.java** - CRUD API implementation
- **LifecycleHandlers.java** - INSTALLED/DELETED handlers
- **WebhookHandlers.java** - Static webhook handlers
- **DynamicWebhookHandlers.java** - IFTTT-style webhook handlers
- **RulesStore.java** - In-memory storage
- **DatabaseRulesStore.java** - PostgreSQL storage
- **WorkspaceCache.java** - Workspace data caching

---

## See Also

- [ClockifyAddon.md](./ClockifyAddon.md) - Central coordinator
- [AddonServlet.md](./AddonServlet.md) - Request routing
- [EmbeddedServer.md](./EmbeddedServer.md) - Server wrapper

---

**File Location:** `/home/user/boileraddon/addons/rules/src/main/java/com/example/rules/RulesApp.java`

**Last Updated:** 2025-11-09
