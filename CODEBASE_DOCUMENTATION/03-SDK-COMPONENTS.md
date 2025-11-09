# SDK Components Reference

Complete reference for all components in the `addon-sdk` module.

## Table of Contents
- [Core Classes](#core-classes)
- [Security Components](#security-components)
- [Middleware Components](#middleware-components)
- [HTTP Client](#http-client)
- [Utilities](#utilities)
- [Health & Metrics](#health--metrics)

---

## Core Classes

### ClockifyAddon

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java`

**Purpose:** Central coordinator class that manages addon lifecycle, routing, and server.

**Key Methods:**

```java
// Lifecycle handler registration
ClockifyAddon registerLifecycleHandler(String type, RequestHandler handler)

// Webhook handler registration
ClockifyAddon registerWebhookHandler(String eventType, RequestHandler handler)

// Custom endpoint registration
ClockifyAddon registerCustomEndpoint(String path, RequestHandler handler)

// Middleware configuration
ClockifyAddon enableRateLimiting(double requestsPerSecond, String limitBy)
ClockifyAddon enableCors(String allowedOrigins)
ClockifyAddon enableSecurityHeaders()
ClockifyAddon enableRequestLogging()

// Server lifecycle
void start() throws Exception
void stop() throws Exception
```

**Usage Example:**

```java
ClockifyManifest manifest = new ClockifyManifest()
    .name("My Addon")
    .schemaVersion("1.3");

ClockifyAddon addon = new ClockifyAddon(manifest, "/my-addon", 8080);

// Register lifecycle handlers
addon.registerLifecycleHandler("INSTALLED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();
    String token = payload.get("installationToken").asText();
    TokenStore.save(workspaceId, token, payload.get("apiBaseUrl").asText());
    return HttpResponse.ok("{\"status\":\"installed\"}");
});

// Enable middleware
addon.enableSecurityHeaders()
     .enableRateLimiting(10.0, "workspace")
     .enableCors("https://*.clockify.me");

// Start server
addon.start();
```

---

### ClockifyManifest

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyManifest.java`

**Purpose:** Builder for addon manifest (v1.3 schema).

**Key Methods:**

```java
ClockifyManifest schemaVersion(String version)
ClockifyManifest name(String name)
ClockifyManifest description(String description)
ClockifyManifest key(String key)
ClockifyManifest vendor(String name, String url)
ClockifyManifest scopes(String[] scopes)
ClockifyManifest components(List<ComponentEndpoint> components)
ClockifyManifest webhooks(List<WebhookSubscription> webhooks)
ClockifyManifest lifecycle(Lifecycle lifecycle)
```

**Example:**

```java
ClockifyManifest manifest = new ClockifyManifest()
    .schemaVersion("1.3")
    .name("Rules Automation")
    .description("IFTTT-style automation for Clockify")
    .key("com.example.rules")
    .vendor("Example Corp", "https://example.com")
    .scopes(new String[]{
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "TAG_READ",
        "TAG_WRITE"
    })
    .components(List.of(
        new ComponentEndpoint("sidebar", "/settings", "Settings", "ADMINS")
    ))
    .webhooks(List.of(
        new WebhookSubscription("TIME_ENTRY_CREATED", "/webhook"),
        new WebhookSubscription("TIME_ENTRY_UPDATED", "/webhook")
    ))
    .lifecycle(new Lifecycle(
        new LifecycleEndpoint("/lifecycle/installed"),
        new LifecycleEndpoint("/lifecycle/deleted")
    ));
```

---

### AddonServlet

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java`

**Purpose:** HTTP servlet that routes requests to appropriate handlers.

**Routing Logic:**

```java
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    String path = req.getPathInfo();

    // 1. Try custom endpoints (exact match)
    RequestHandler customHandler = addon.getEndpoints().get(path);
    if (customHandler != null) {
        handleRequest(req, resp, customHandler);
        return;
    }

    // 2. Try webhooks (POST only)
    if ("POST".equals(req.getMethod()) && tryHandleWebhook(req, resp)) {
        return;
    }

    // 3. Try lifecycle (POST only)
    if ("POST".equals(req.getMethod()) && tryHandleLifecycle(req, resp)) {
        return;
    }

    // 4. 404 Not Found
    resp.sendError(404, "Endpoint not found");
}
```

**Features:**
- Exact path matching (no wildcards)
- Automatic webhook event detection
- Lifecycle type detection
- Error handling and logging

---

### EmbeddedServer

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/EmbeddedServer.java`

**Purpose:** Wrapper around Jetty server with configuration.

**Configuration Options:**

```java
public class EmbeddedServer {
    private final int port;
    private final String contextPath;
    private final List<Filter> filters;
    private final Servlet servlet;

    public void start() throws Exception {
        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(contextPath);

        // Add filters
        for (Filter filter : filters) {
            context.addFilter(new FilterHolder(filter), "/*",
                EnumSet.of(DispatcherType.REQUEST));
        }

        // Add servlet
        context.addServlet(new ServletHolder(servlet), "/*");

        server.setHandler(context);
        server.start();
    }
}
```

---

### RequestHandler

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/RequestHandler.java`

**Purpose:** Functional interface for request handling.

```java
@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}
```

**Usage:**

```java
// Lambda style
addon.registerCustomEndpoint("/api/hello", request -> {
    return HttpResponse.ok("Hello, World!");
});

// Method reference
addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

// Class implementation
public class MyController implements RequestHandler {
    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        // Handle request
        return HttpResponse.ok("...");
    }
}
```

---

### HttpResponse

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/HttpResponse.java`

**Purpose:** Simple response wrapper with status, body, and content type.

```java
public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final String contentType;

    // Factory methods
    public static HttpResponse ok(String body);
    public static HttpResponse ok(String body, String contentType);
    public static HttpResponse error(int statusCode, String message);
    public static HttpResponse notFound(String message);
    public static HttpResponse unauthorized(String message);
    public static HttpResponse badRequest(String message);
}
```

**Example:**

```java
return HttpResponse.ok("{\"status\":\"ok\"}", "application/json");
return HttpResponse.error(500, "Internal server error");
return HttpResponse.unauthorized("Invalid token");
```

---

## Security Components

### TokenStore

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/TokenStore.java`

**Purpose:** In-memory storage for installation tokens.

```java
public class TokenStore {
    public record WorkspaceToken(String token, String apiBaseUrl) {}

    private static final Map<String, WorkspaceToken> STORE = new ConcurrentHashMap<>();

    public static void save(String workspaceId, String token, String apiBaseUrl);
    public static Optional<WorkspaceToken> get(String workspaceId);
    public static boolean delete(String workspaceId);
    public static void clear();
}
```

**Usage:**

```java
// Save token during installation
TokenStore.save(workspaceId, installationToken, apiBaseUrl);

// Retrieve token for API calls
Optional<WorkspaceToken> tokenOpt = TokenStore.get(workspaceId);
if (tokenOpt.isPresent()) {
    ClockifyHttpClient client = new ClockifyHttpClient(tokenOpt.get().apiBaseUrl());
    // Make API call
}

// Delete token on uninstallation
TokenStore.delete(workspaceId);
```

---

### DatabaseTokenStore

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/DatabaseTokenStore.java`

**Purpose:** PostgreSQL-backed persistent token storage.

```java
public class DatabaseTokenStore implements TokenStoreSPI {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseTokenStore(String dbUrl, String dbUser, String dbPassword);

    @Override
    public void save(String workspaceId, String token, String apiBaseUrl);

    @Override
    public Optional<WorkspaceToken> get(String workspaceId);

    @Override
    public boolean delete(String workspaceId);

    public long count(); // For health checks
}
```

**Database Schema:**

```sql
CREATE TABLE addon_tokens (
  workspace_id VARCHAR(255) PRIMARY KEY,
  auth_token TEXT NOT NULL,
  api_base_url VARCHAR(512),
  created_at BIGINT NOT NULL,
  last_accessed_at BIGINT NOT NULL
);
```

**Usage:**

```java
DatabaseTokenStore store = new DatabaseTokenStore(
    System.getenv("DB_URL"),
    System.getenv("DB_USERNAME"),
    System.getenv("DB_PASSWORD")
);

store.save(workspaceId, token, apiBaseUrl);
Optional<WorkspaceToken> tokenOpt = store.get(workspaceId);
```

---

### WebhookSignatureValidator

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/WebhookSignatureValidator.java`

**Purpose:** HMAC-SHA256 and JWT signature validation for webhooks.

```java
public class WebhookSignatureValidator {
    public static boolean validate(
        String signatureHeader,
        byte[] body,
        String sharedSecret
    );

    public static boolean isJwtSignature(String signature);

    public static String extractWorkspaceIdFromJwt(String jwt);
}
```

**HMAC Validation:**

```java
String signature = request.getHeader("clockify-webhook-signature");
byte[] body = request.getInputStream().readAllBytes();
String secret = System.getenv("ADDON_WEBHOOK_SECRET");

if (!WebhookSignatureValidator.validate(signature, body, secret)) {
    return HttpResponse.unauthorized("Invalid signature");
}
```

**JWT Support (Dev Workspaces):**

```java
if (WebhookSignatureValidator.isJwtSignature(signature)) {
    String workspaceId = WebhookSignatureValidator.extractWorkspaceIdFromJwt(signature);
    // Process webhook with extracted workspaceId
}
```

**Environment Controls:**

```bash
ADDON_SKIP_SIGNATURE_VERIFY=true      # Skip validation (testing only!)
ADDON_ACCEPT_JWT_SIGNATURE=true       # Allow JWT (dev workspaces)
```

---

## Middleware Components

### SecurityHeadersFilter

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/SecurityHeadersFilter.java`

**Purpose:** Add security headers to all responses.

**Headers Added:**
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` (HTTPS only)
- `Content-Security-Policy: frame-ancestors <origins>` (optional)

**Configuration:**

```bash
ADDON_FRAME_ANCESTORS='self' https://*.clockify.me
```

**Usage:**

```java
addon.enableSecurityHeaders();
```

---

### RateLimiter

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/RateLimiter.java`

**Purpose:** Rate limit requests using token bucket algorithm (Guava).

**Configuration:**

```bash
ADDON_RATE_LIMIT=10           # requests per second
ADDON_LIMIT_BY=workspace      # "ip" or "workspace"
```

**Usage:**

```java
addon.enableRateLimiting(10.0, "workspace");
```

**Behavior:**
- Returns 429 (Too Many Requests) when limit exceeded
- Separate buckets per IP or workspace
- Automatic cleanup of idle buckets

---

### CorsFilter

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/CorsFilter.java`

**Purpose:** Handle CORS preflight and set CORS headers.

**Features:**
- Wildcard subdomain support (`https://*.clockify.me`)
- Preflight OPTIONS handling
- `Vary: Origin` header

**Configuration:**

```bash
ADDON_CORS_ORIGINS=https://*.clockify.me,https://app.example.com
```

**Usage:**

```java
addon.enableCors("https://*.clockify.me");
```

---

### RequestLoggingFilter

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/RequestLoggingFilter.java`

**Purpose:** Log HTTP requests and responses.

**Configuration:**

```bash
ADDON_REQUEST_LOGGING=true
```

**Usage:**

```java
addon.enableRequestLogging();
```

**Log Format:**

```
[INFO] GET /addon/health → 200 OK (12ms)
[INFO] POST /addon/webhook → 200 OK (45ms)
```

---

## HTTP Client

### ClockifyHttpClient

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/http/ClockifyHttpClient.java`

**Purpose:** HTTP client for Clockify API with retries and rate limiting.

**Features:**
- Automatic retries (429, 5xx errors)
- Exponential backoff
- `Retry-After` header support
- `x-addon-token` header injection
- Configurable timeouts

**Constructor:**

```java
public ClockifyHttpClient(String baseUrl);
public ClockifyHttpClient(String baseUrl, Duration timeout, int maxRetries);
```

**Methods:**

```java
public HttpResponse<String> get(String path, String token, Map<String, String> headers);
public HttpResponse<String> post(String path, String token, String body, Map<String, String> headers);
public HttpResponse<String> put(String path, String token, String body, Map<String, String> headers);
public HttpResponse<String> delete(String path, String token, Map<String, String> headers);
```

**Usage:**

```java
ClockifyHttpClient client = new ClockifyHttpClient(
    "https://api.clockify.me/api/v1",
    Duration.ofSeconds(10),
    3  // max retries
);

HttpResponse<String> resp = client.get(
    "/workspaces/" + workspaceId + "/tags",
    installationToken,
    Map.of("Custom-Header", "value")
);

if (resp.statusCode() == 200) {
    String body = resp.body();
    // Parse JSON response
}
```

**Retry Logic:**

```java
// Retries on:
// - 429 (Rate Limited) - waits for Retry-After duration
// - 500-599 (Server Errors) - exponential backoff (1s, 2s, 4s)

// Does NOT retry on:
// - 4xx (Client Errors, except 429)
// - Network errors (throws exception)
```

---

## Utilities

### PathSanitizer

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/util/PathSanitizer.java`

**Purpose:** Normalize and sanitize URL paths.

```java
public class PathSanitizer {
    public static String sanitize(String path) {
        // Normalize slashes
        String normalized = path.replaceAll("/+", "/");

        // Ensure leading slash
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // Remove trailing slashes
        normalized = normalized.replaceAll("/+$", "");

        return normalized.isEmpty() ? "/" : normalized;
    }
}
```

**Examples:**

```java
PathSanitizer.sanitize("/addon//path/")  → "/addon/path"
PathSanitizer.sanitize("addon/path")     → "/addon/path"
PathSanitizer.sanitize("//addon//")      → "/addon"
```

---

### ConfigValidator

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ConfigValidator.java`

**Purpose:** Validate configuration values (URLs, ports, etc.).

```java
public class ConfigValidator {
    public static String validateUrl(String value, String defaultValue, String name);
    public static int validatePort(String value, int defaultPort, String name);
    public static String requireNonBlank(String value, String name);
}
```

**Usage:**

```java
String baseUrl = ConfigValidator.validateUrl(
    System.getenv("ADDON_BASE_URL"),
    "http://localhost:8080/addon",
    "ADDON_BASE_URL"
);

int port = ConfigValidator.validatePort(
    System.getenv("ADDON_PORT"),
    8080,
    "ADDON_PORT"
);
```

---

### BaseUrlDetector

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/BaseUrlDetector.java`

**Purpose:** Extract context path from base URL.

```java
public class BaseUrlDetector {
    public static String extractContextPath(String baseUrl) {
        try {
            URI uri = new URI(baseUrl);
            String path = uri.getPath();
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (URISyntaxException e) {
            return "/";
        }
    }
}
```

**Examples:**

```java
BaseUrlDetector.extractContextPath("http://localhost:8080/addon")  → "/addon"
BaseUrlDetector.extractContextPath("https://example.com")          → "/"
BaseUrlDetector.extractContextPath("https://example.com/")         → "/"
```

---

## Health & Metrics

### HealthCheck

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/health/HealthCheck.java`

**Purpose:** Health check endpoint with dependency probes.

```java
public class HealthCheck implements RequestHandler {
    public HealthCheck(String name, String version);

    public void addHealthCheckProvider(HealthCheckProvider provider);

    public interface HealthCheckProvider {
        String getName();
        HealthCheckResult check();
    }
}
```

**Usage:**

```java
HealthCheck health = new HealthCheck("rules", "0.1.0");

// Add database health check
health.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
    @Override
    public String getName() { return "database"; }

    @Override
    public HealthCheckResult check() {
        try {
            long count = store.count();
            return new HealthCheckResult("database", true, "Connected", count);
        } catch (Exception e) {
            return new HealthCheckResult("database", false, e.getMessage());
        }
    }
});

addon.registerCustomEndpoint("/health", health);
```

**Response:**

```json
{
  "name": "rules",
  "version": "0.1.0",
  "status": "UP",
  "checks": [
    {
      "name": "database",
      "healthy": true,
      "message": "Connected",
      "details": 5
    }
  ]
}
```

---

### MetricsHandler

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/metrics/MetricsHandler.java`

**Purpose:** Prometheus metrics exporter.

```java
public class MetricsHandler implements RequestHandler {
    private static final PrometheusMeterRegistry REGISTRY =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    public static PrometheusMeterRegistry registry() {
        return REGISTRY;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String scrape = REGISTRY.scrape();
        return HttpResponse.ok(scrape, "text/plain; version=0.0.4; charset=utf-8");
    }
}
```

**Usage:**

```java
addon.registerCustomEndpoint("/metrics", new MetricsHandler());

// Record metrics
Counter.builder("webhook_requests_total")
    .tag("event", "TIME_ENTRY_CREATED")
    .register(MetricsHandler.registry())
    .increment();

Timer.Sample sample = Timer.start(MetricsHandler.registry());
// ... process request ...
Timer timer = Timer.builder("request_duration_seconds")
    .tag("endpoint", "/webhook")
    .register(MetricsHandler.registry());
sample.stop(timer);
```

**Metrics Endpoint:**

```bash
curl http://localhost:8080/addon/metrics

# Output:
# HELP webhook_requests_total Total webhook requests
# TYPE webhook_requests_total counter
webhook_requests_total{event="TIME_ENTRY_CREATED"} 42
```

---

**Next:** [API Endpoints Documentation](./04-API-ENDPOINTS.md)
