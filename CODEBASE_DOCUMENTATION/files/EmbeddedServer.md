# EmbeddedServer.java - Jetty Server Wrapper

**Location**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/EmbeddedServer.java`

**Type**: Server Wrapper/Facade

**Purpose**: Wraps Jetty embedded servlet container to simplify local addon development and deployment

---

## Class Overview

```java
public class EmbeddedServer
```

Provides a simple API for starting/stopping Jetty without exposing Jetty complexity. Used by addon apps in their main() methods.

---

## Core Fields

| Field | Type | Purpose |
|-------|------|---------|
| `servlet` | AddonServlet | Main servlet handling all HTTP requests |
| `contextPath` | String | URL prefix (e.g., "/auto-tag-assistant") |
| `filters` | List<Filter> | Middleware filters to apply |
| `server` | Server | Jetty Server instance (initialized on start) |

---

## Constructors

### Constructor 1: With Context Path

```java
public EmbeddedServer(AddonServlet servlet, String contextPath)
```

**Parameters**:
- `servlet`: AddonServlet instance to mount
- `contextPath`: URL path prefix (e.g., "/auto-tag-assistant")

**Example**:
```java
AddonServlet servlet = new AddonServlet(addon);
EmbeddedServer server = new EmbeddedServer(servlet, "/auto-tag-assistant");
```

### Constructor 2: Without Context Path

```java
public EmbeddedServer(AddonServlet servlet)
```

**Default contextPath**: "/" (root)

**Example**:
```java
EmbeddedServer server = new EmbeddedServer(servlet);
// Mounts addon at http://localhost:8080/
```

---

## Methods

### start(int port) - Start Server

```java
public void start(int port) throws Exception
```

**Line-by-line execution**:

1. **Create Jetty Server**:
   ```java
   server = new Server(port);
   ```
   - Binds to specified port
   - Jetty Server object manages lifecycle

2. **Create ServletContextHandler**:
   ```java
   ServletContextHandler handler = new ServletContextHandler(
       ServletContextHandler.SESSIONS
   );
   handler.setContextPath(contextPath);
   ```
   - SESSIONS enabled (supports HttpSession)
   - Context path sets URL prefix (e.g., "/auto-tag-assistant")
   - All requests under this path routed to this context

3. **Register Filters**:
   ```java
   for (Filter filter : filters) {
       handler.addFilter(
           new FilterHolder(filter),
           "/*",
           EnumSet.of(DispatcherType.REQUEST)
       );
   }
   ```
   - Applies all registered filters to all requests ("/*")
   - DispatcherType.REQUEST: Filter applies to HTTP requests (not forwards/includes)
   - Order: Filters executed in registration order

4. **Mount Servlet**:
   ```java
   handler.addServlet(new ServletHolder(servlet), "/*");
   ```
   - Maps servlet to all paths under context
   - Receives requests after filter chain processing

5. **Set Handler**:
   ```java
   server.setHandler(handler);
   ```

6. **Start Jetty**:
   ```java
   server.start();
   ```
   - Initializes server components
   - Binds to port
   - Jetty runs in background thread

7. **Wait for Shutdown**:
   ```java
   server.join();
   ```
   - **Blocks calling thread** until server shutdown
   - Keeps application alive
   - Shutdown triggered by SIGTERM or programmatic stop()

**Example Usage**:
```java
public static void main(String[] args) {
    ClockifyAddon addon = new ClockifyAddon(manifest);
    AddonServlet servlet = new AddonServlet(addon);
    EmbeddedServer server = new EmbeddedServer(servlet, "/auto-tag-assistant");

    try {
        server.start(8080);  // Blocks here, runs until shutdown
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

**Error Handling**:
- Throws `Exception` (Jetty checked exception)
- Common exceptions:
  - `BindException`: Port already in use
  - `SocketException`: Permission denied (port < 1024 on Linux)
  - `IllegalStateException`: Server already running

---

### stop() - Stop Server

```java
public void stop() throws Exception
```

**Logic**:
- Calls `server.stop()` (graceful shutdown)
- Releases port binding
- Stops background threads
- Joins with calling thread

**Exceptions**: Throws if server not running or already stopped

**Usage** (typically from shutdown hook):
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        server.stop();
    } catch (Exception e) {
        e.printStackTrace();
    }
}));
```

---

### addFilter(Filter) - Register Middleware

```java
public void addFilter(Filter filter)
```

**Parameters**:
- `filter`: Servlet Filter implementation (e.g., SecurityHeadersFilter, CorsFilter)

**When to Call**: Before `start(port)` is called

**Important Constraints**:
- Must be called **before start()**
- Filters are lost if called after start()
- Order matters: First registered = first executed

**Example**:
```java
server.addFilter(new SecurityHeadersFilter());
server.addFilter(new CorsFilter());
server.addFilter(new RateLimiter());
server.start(8080);

// Request flow:
// HttpRequest
//   → SecurityHeadersFilter
//   → CorsFilter
//   → RateLimiter
//   → AddonServlet
```

---

## Filter Execution Order

Filters registered in this order in a typical addon app:

```
1. SecurityHeadersFilter     (Security headers: CSP, X-Frame-Options, etc.)
2. RequestLoggingFilter      (Log requests with header scrubbing)
3. RateLimiter               (Limit requests per IP/workspace)
4. CorsFilter                (CORS validation)
5. AddonServlet              (Main request handler)
```

Response travels back through filters in reverse order.

---

## Jetty Configuration

### Implicit Defaults

| Setting | Value | Notes |
|---------|-------|-------|
| Thread Pool | Default (8 threads) | Can handle concurrent requests |
| Connection Timeout | 30 seconds | Jetty default |
| Request Buffer Size | 8KB | Default header/body buffer |
| Session Manager | Servlet default | In-memory sessions |
| SSL/TLS | Disabled | Use proxy for HTTPS |

### Not Exposed

- Connector configuration (port is only exposed parameter)
- Thread pool sizing
- Connection handling
- Graceful shutdown timeout

---

## URL Structure

With `EmbeddedServer`:

```
EmbeddedServer(servlet, "/auto-tag-assistant").start(8080)

Base URL: http://localhost:8080
Context Path: /auto-tag-assistant

Addon Endpoints:
  GET  http://localhost:8080/auto-tag-assistant/manifest.json
  POST http://localhost:8080/auto-tag-assistant/webhook
  POST http://localhost:8080/auto-tag-assistant/lifecycle/installed
  GET  http://localhost:8080/auto-tag-assistant/settings
  GET  http://localhost:8080/auto-tag-assistant/health
```

---

## Request Processing Pipeline

```
HTTP Request to http://localhost:8080/auto-tag-assistant/webhook

1. Jetty accepts connection
2. HTTP request parsed
3. ServletContextHandler routes to context path
4. Filter chain executes:
   a) SecurityHeadersFilter
   b) RequestLoggingFilter
   c) RateLimiter
   d) CorsFilter
5. AddonServlet.service() called
6. Request routed to handler
7. HttpResponse generated
8. Filter chain (reverse):
   d) CorsFilter (adds headers)
   c) RateLimiter (increment counter)
   b) RequestLoggingFilter (log response)
   a) SecurityHeadersFilter (verify headers set)
9. HTTP response sent to client
10. Connection closed
```

---

## Common Patterns

### Pattern 1: Local Development

```java
public static void main(String[] args) {
    int port = ConfigValidator.validatePort(
        System.getenv("ADDON_PORT"), 8080, "ADDON_PORT");
    String baseUrl = ConfigValidator.validateUrl(
        System.getenv("ADDON_BASE_URL"),
        "http://localhost:" + port + "/auto-tag-assistant",
        "ADDON_BASE_URL");

    ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
        .key("auto-tag-assistant")
        .name("Auto Tag Assistant")
        .baseUrl(baseUrl)
        .scopes(new String[]{"TIME_ENTRY_READ", "TAG_READ"})
        .build();

    ClockifyAddon addon = new ClockifyAddon(manifest);
    // Register handlers...

    AddonServlet servlet = new AddonServlet(addon);
    EmbeddedServer server = new EmbeddedServer(servlet, "/auto-tag-assistant");

    server.addFilter(new SecurityHeadersFilter());
    server.addFilter(new RateLimiter());

    try {
        server.start(port);  // Blocks, runs until Ctrl+C
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### Pattern 2: Docker/Production

```java
// Same startup code, but with:
// - ADDON_BASE_URL from environment (e.g., https://addon.example.com/auto-tag-assistant)
// - ADDON_PORT from environment (e.g., 8080)
// - Additional filters (RequestLoggingFilter, CorsFilter)
// - Additional healthcheck endpoint
```

---

## Thread Safety

| Aspect | Thread-Safe? | Notes |
|--------|--------------|-------|
| `addFilter()` | Not thread-safe | Must be called before start() |
| `start(port)` | Not thread-safe | Don't call multiple times |
| `stop()` | Safe | Can be called from any thread (e.g., shutdown hook) |
| Request handling | Thread-safe | Jetty uses thread pool, each request in separate thread |

---

## Lifecycle

```
1. Construction: EmbeddedServer(servlet, contextPath)
   - Initializes fields
   - No server created yet

2. Pre-start: addFilter(...) calls
   - Registers middleware
   - Must be called before start()

3. start(port)
   - Creates Jetty Server
   - Creates ServletContextHandler
   - Registers filters
   - Mounts servlet
   - Starts server
   - **BLOCKS on server.join()**

4. Runtime
   - Jetty handles incoming requests
   - Filter chain executes
   - AddonServlet processes request

5. Shutdown
   - SIGTERM or programmatic stop()
   - server.join() unblocks
   - Application exits

6. stop() (optional)
   - Graceful shutdown
   - Releases resources
   - Frees port for reuse
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void testServerStartsOnPort() throws Exception {
    AddonServlet servlet = mock(AddonServlet.class);
    EmbeddedServer server = new EmbeddedServer(servlet);

    Thread thread = new Thread(() -> {
        try {
            server.start(8081);
        } catch (Exception e) {
            fail(e);
        }
    });
    thread.setDaemon(true);
    thread.start();

    Thread.sleep(500);  // Wait for server start

    // Test connectivity
    HttpURLConnection conn = (HttpURLConnection)
        new URL("http://localhost:8081/").openConnection();
    assertEquals(200, conn.getResponseCode());

    server.stop();
}

@Test
void testFiltersAddedBeforeStart() throws Exception {
    EmbeddedServer server = new EmbeddedServer(servlet);
    Filter filter = mock(Filter.class);

    server.addFilter(filter);
    server.start(8082);
    // Verify filter is in chain

    server.stop();
}

@Test
void testContextPathRouting() throws Exception {
    EmbeddedServer server = new EmbeddedServer(servlet, "/addon");
    server.start(8083);

    // Root path returns 404 or different handler
    // /addon/* routes to servlet

    server.stop();
}
```

### Integration Tests

```java
@Test
void testFullRequestFlow() throws Exception {
    ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
        .key("test").name("Test").baseUrl("http://localhost:8084").build();

    ClockifyAddon addon = new ClockifyAddon(manifest);
    addon.registerCustomEndpoint("/test", (req) ->
        HttpResponse.ok("test response"));

    AddonServlet servlet = new AddonServlet(addon);
    EmbeddedServer server = new EmbeddedServer(servlet, "/test-addon");

    Thread thread = new Thread(() -> {
        try {
            server.start(8084);
        } catch (Exception e) {
            fail(e);
        }
    });
    thread.setDaemon(true);
    thread.start();

    Thread.sleep(500);

    // Make request
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8084/test-addon/test"))
        .GET()
        .build();

    HttpResponse<String> response = client.send(request,
        HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertEquals("test response", response.body());

    server.stop();
}
```

---

## Common Issues & Solutions

### Issue 1: Port Already in Use

```
Error: java.net.BindException: Address already in use
```

**Solution**: Either:
- Use different port: `server.start(9000)`
- Kill process on port 8080: `lsof -i :8080 | kill -9 <PID>`
- Wait for TIME_WAIT timeout (Windows: 4 minutes)

### Issue 2: Filters Not Executing

```java
// ❌ Bad: addFilter called after start
server.start(8080);
server.addFilter(new SecurityHeadersFilter());  // Too late!

// ✅ Good: addFilter before start
server.addFilter(new SecurityHeadersFilter());
server.start(8080);
```

### Issue 3: Application Hangs on Start

```
// ❌ Bad: server.join() blocks indefinitely
EmbeddedServer server = new EmbeddedServer(servlet);
server.start(8080);  // Blocks here!
System.out.println("Never prints");

// ✅ Good: Run in separate thread
new Thread(() -> {
    try {
        server.start(8080);
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
System.out.println("This prints immediately");
```

---

## Performance Characteristics

| Scenario | Impact | Notes |
|----------|--------|-------|
| Large number of filters | Slight overhead | Each filter adds processing time |
| High request volume | Depends on handlers | Jetty handles concurrency well |
| Long-running handlers | Blocks worker threads | Thread pool size determines concurrency |
| Memory | ~50MB per Jetty instance | Lightweight embedded server |
| Startup time | < 1 second | Fast startup, minimal initialization |

---

## Related Files

- **AddonServlet.java**: Main servlet mounted by EmbeddedServer
- **SecurityHeadersFilter.java**: Common middleware example
- **RequestLoggingFilter.java**: Logging middleware example
- **RateLimiter.java**: Rate limiting middleware example
- **CorsFilter.java**: CORS middleware example
- All addon apps (TemplateAddonApp, AutoTagAssistantApp, RulesApp, OvertimeApp)

---

## Jetty Dependency

Requires in pom.xml:
```xml
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>11.0.24</version>
</dependency>
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-servlet</artifactId>
    <version>11.0.24</version>
</dependency>
```

---

## Notes for Developers

1. **Single Instance**: Create one EmbeddedServer per addon process
2. **Port Binding**: Port should be configurable via environment variable (ADDON_PORT)
3. **Context Path**: Should match addon key (e.g., "/auto-tag-assistant")
4. **Filter Order**: Matters! Register filters from security to business logic
5. **Graceful Shutdown**: Consider adding shutdown hooks for cleanup
6. **Proxy Headers**: Jetty respects X-Forwarded-* headers (enabled by default)
