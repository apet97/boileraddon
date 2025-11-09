# AddonServlet.java

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java`

**Package:** `com.clockify.addon.sdk`

**Lines:** 319

---

## Overview

`AddonServlet` is the primary HTTP servlet that routes incoming requests to registered `RequestHandler` implementations. It extends Jakarta `HttpServlet` and serves as the entry point for all HTTP traffic to the addon.

## Purpose

- Route HTTP requests to appropriate handlers
- Detect webhook vs lifecycle vs custom endpoint requests
- Extract event types from headers and payloads
- Handle errors and send responses
- Record metrics for webhook processing

---

## Class Structure

```java
public class AddonServlet extends HttpServlet {
    private static final Logger logger;
    private final ClockifyAddon addon;
    private final ObjectMapper objectMapper;
}
```

---

## Constructor

### `AddonServlet(ClockifyAddon addon)`

**Parameters:**
- `addon` - Configured ClockifyAddon instance with registered handlers

**Example:**

```java
ClockifyAddon addon = new ClockifyAddon(manifest);
// ... register handlers ...
AddonServlet servlet = new AddonServlet(addon);
```

---

## Core Methods

### service (HTTP Entry Point)

**Signature:** `protected void service(HttpServletRequest req, HttpServletResponse resp)`

**Purpose:** Main HTTP request handler (overrides HttpServlet.service)

**Request Flow:**

```
1. Extract path and method
2. Log request
3. Call handleRequest()
4. Send response
5. Catch errors -> 500 Internal Server Error
```

**Implementation:**

```java
@Override
protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String path = req.getPathInfo() != null ? req.getPathInfo() : "/";
    String method = req.getMethod();

    logger.info("{} {}", method, path);

    try {
        HttpResponse response = handleRequest(req, path);
        sendResponse(resp, response);
    } catch (Exception e) {
        logger.error("Error handling request: {} {}", method, path, e);
        String errorBody = objectMapper.createObjectNode()
                .put("message", "Internal server error")
                .put("details", e.getMessage())
                .toString();
        sendResponse(resp, HttpResponse.error(500, errorBody, "application/json"));
    }
}
```

**Error Handling:**
- Catches all exceptions
- Returns 500 with error details
- Logs full stack trace

---

### handleRequest (Routing Logic)

**Signature:** `private HttpResponse handleRequest(HttpServletRequest req, String path)`

**Purpose:** Route request to appropriate handler

**Routing Priority:**

```
1. Custom endpoints (exact path match)
   └─ addon.getEndpoints().get(path)

2. Webhooks (POST only)
   └─ tryHandleWebhook(req, path)

3. Lifecycle (POST only)
   └─ tryHandleLifecycle(req, path)

4. 404 Not Found
```

**Implementation:**

```java
private HttpResponse handleRequest(HttpServletRequest req, String path) throws Exception {
    // 1. Try custom endpoints
    RequestHandler customHandler = addon.getEndpoints().get(path);
    if (customHandler != null) {
        return customHandler.handle(req);
    }

    // 2. Try webhooks (POST only)
    if ("POST".equalsIgnoreCase(req.getMethod())) {
        HttpResponse webhookResponse = tryHandleWebhook(req, path);
        if (webhookResponse != null) {
            return webhookResponse;
        }

        // 3. Try lifecycle (POST only)
        HttpResponse lifecycleResponse = tryHandleLifecycle(req, path);
        if (lifecycleResponse != null) {
            return lifecycleResponse;
        }
    }

    // 4. 404
    return HttpResponse.error(404, "Endpoint not found: " + path);
}
```

**Key Points:**
- Custom endpoints take precedence
- Webhooks and lifecycle only for POST requests
- Exact path matching (no wildcards)

---

### tryHandleWebhook (Webhook Detection)

**Signature:** `private HttpResponse tryHandleWebhook(HttpServletRequest req, String path)`

**Purpose:** Detect and handle webhook requests

**Detection Strategy:**

```
1. Skip if path starts with /lifecycle (never treat lifecycle as webhook)
2. Look up handlers for this path
3. If not found, try default path (/webhook)
4. If found, delegate to handleWebhook()
```

**Implementation Highlights:**

```java
private HttpResponse tryHandleWebhook(HttpServletRequest req, String path) throws Exception {
    // Never treat lifecycle endpoints as webhooks
    if (path != null && path.startsWith("/lifecycle")) {
        return null;
    }

    Map<String, RequestHandler> handlersForPath = addon.getWebhookHandlersByPath().get(path);
    boolean usingDefaultPath = ClockifyAddon.DEFAULT_WEBHOOK_PATH.equals(path);

    // Fallback to default path if current path has no handlers
    if (handlersForPath == null && !usingDefaultPath) {
        handlersForPath = addon.getWebhookHandlersByPath().get(ClockifyAddon.DEFAULT_WEBHOOK_PATH);
        usingDefaultPath = true;
    }

    if (handlersForPath == null && usingDefaultPath) {
        handlersForPath = addon.getWebhookHandlers();
    }

    if (handlersForPath == null) {
        return null;
    }

    if (handlersForPath.isEmpty() && !usingDefaultPath) {
        return null;
    }

    return handleWebhook(req, handlersForPath);
}
```

---

### handleWebhook (Webhook Processing)

**Signature:** `private HttpResponse handleWebhook(HttpServletRequest req, Map<String, RequestHandler> handlers)`

**Purpose:** Process webhook request and dispatch to event handler

**Event Detection (Priority Order):**

```
1. Header: clockify-webhook-event-type
2. JSON body: { "event": "..." }
3. Error if neither found
```

**Implementation:**

```java
private HttpResponse handleWebhook(HttpServletRequest req, Map<String, RequestHandler> handlers) throws Exception {
    // 1. Parse JSON body
    JsonNode json;
    try {
        json = readAndCacheJsonBody(req);
    } catch (IOException e) {
        Counter.builder("webhook_errors_total")
                .tag("reason", "invalid_json")
                .register(MetricsHandler.registry())
                .increment();
        String errorBody = objectMapper.createObjectNode()
                .put("message", "Invalid JSON payload")
                .put("details", e.getMessage())
                .toString();
        return HttpResponse.error(400, errorBody, "application/json");
    }

    // 2. Determine event type (header first, then body)
    String event = null;
    String headerEventType = req.getHeader("clockify-webhook-event-type");
    if (headerEventType != null && !headerEventType.trim().isEmpty()) {
        event = headerEventType.trim();
    }

    if (event == null && json != null && json.has("event")) {
        event = json.get("event").asText(null);
        if (event != null) {
            event = event.trim();
        }
    }

    if (event == null) {
        Counter.builder("webhook_errors_total")
                .tag("reason", "missing_event")
                .register(MetricsHandler.registry())
                .increment();
        return HttpResponse.error(400, "Missing webhook event type");
    }

    // 3. Find and execute handler
    RequestHandler handler = handlers.get(event);
    if (handler != null) {
        String path = req.getPathInfo() != null ? req.getPathInfo() : "/";

        // Metrics: count + duration
        Timer.Sample sample = Timer.start(MetricsHandler.registry());
        Counter.builder("webhook_requests_total")
                .tag("event", event)
                .tag("path", path)
                .register(MetricsHandler.registry())
                .increment();

        HttpResponse response;
        try {
            response = handler.handle(req);
        } finally {
            Timer timer = Timer.builder("webhook_request_seconds")
                    .tag("event", event)
                    .tag("path", path)
                    .register(MetricsHandler.registry());
            sample.stop(timer);
        }
        return response;
    }

    // 4. No handler found
    logger.warn("No handler registered for webhook event: {}", event);
    Counter.builder("webhook_not_handled_total")
            .tag("event", event)
            .register(MetricsHandler.registry())
            .increment();
    return HttpResponse.ok("Webhook event received but not handled: " + event);
}
```

**Metrics Recorded:**
- `webhook_errors_total{reason}` - Error counter
- `webhook_requests_total{event,path}` - Request counter
- `webhook_request_seconds{event,path}` - Duration timer
- `webhook_not_handled_total{event}` - Unhandled event counter

---

### tryHandleLifecycle (Lifecycle Detection)

**Signature:** `private HttpResponse tryHandleLifecycle(HttpServletRequest req, String path)`

**Purpose:** Detect and handle lifecycle requests

**Detection Strategy:**

```
1. Look up handler by exact path
2. Fallback: parse /lifecycle/{type} pattern
3. Fallback: parse JSON body for lifecycle type
4. If found, dispatch to handler
```

**Lifecycle Type Extraction (from JSON):**

```java
private String extractLifecycleType(JsonNode json) {
    if (json == null) return null;

    // Try "lifecycle" field
    if (json.hasNonNull("lifecycle")) {
        return json.get("lifecycle").asText();
    }

    // Try "type" field
    if (json.hasNonNull("type")) {
        return json.get("type").asText();
    }

    return null;
}
```

**Supported JSON Formats:**

```json
// Format 1
{
  "lifecycle": "INSTALLED",
  "workspaceId": "..."
}

// Format 2
{
  "type": "INSTALLED",
  "workspaceId": "..."
}
```

---

### readAndCacheJsonBody (Body Caching)

**Signature:** `private JsonNode readAndCacheJsonBody(HttpServletRequest req)`

**Purpose:** Read and cache request body to avoid re-reading

**Caching Strategy:**

```
1. Check for cached JsonNode (attribute: clockify.jsonBody)
2. Check for cached raw body (attribute: clockify.rawBody)
3. Read from request stream and cache both raw + parsed
```

**Implementation:**

```java
private JsonNode readAndCacheJsonBody(HttpServletRequest req) throws IOException {
    // 1. Check JsonNode cache
    Object cachedJson = req.getAttribute("clockify.jsonBody");
    if (cachedJson instanceof JsonNode) {
        return (JsonNode) cachedJson;
    }

    // 2. Check raw body cache
    Object cachedBody = req.getAttribute("clockify.rawBody");
    if (cachedBody instanceof String) {
        String bodyString = (String) cachedBody;
        if (bodyString.isBlank()) {
            return null;
        }
        JsonNode jsonNode = objectMapper.readTree(bodyString);
        req.setAttribute("clockify.jsonBody", jsonNode);
        return jsonNode;
    }

    // 3. Read from stream and cache
    String body = req.getReader().lines().collect(Collectors.joining());
    req.setAttribute("clockify.rawBody", body);

    if (body.isBlank()) {
        return null;
    }

    JsonNode json = objectMapper.readTree(body);
    req.setAttribute("clockify.jsonBody", json);
    return json;
}
```

**Why Caching?**
- Request stream can only be read once
- Multiple handlers may need the body (e.g., signature validation + processing)
- Avoids expensive re-parsing

---

### sendResponse (Response Writing)

**Signature:** `private void sendResponse(HttpServletResponse resp, HttpResponse response)`

**Purpose:** Write HttpResponse to servlet response

**Implementation:**

```java
private void sendResponse(HttpServletResponse resp, HttpResponse response) throws IOException {
    resp.setStatus(response.getStatusCode());
    resp.setContentType(response.getContentType());
    resp.getWriter().write(response.getBody());
}
```

---

## Request Attributes

The servlet uses request attributes for caching:

| Attribute | Type | Purpose |
|-----------|------|---------|
| `clockify.rawBody` | String | Cached raw request body |
| `clockify.jsonBody` | JsonNode | Cached parsed JSON |

**Usage Example:**

```java
// In middleware filter
String rawBody = readBody(request);
request.setAttribute("clockify.rawBody", rawBody);

// In servlet
String cachedBody = (String) request.getAttribute("clockify.rawBody");
```

---

## Routing Examples

### Example 1: Custom Endpoint

```
Request: GET /addon/health

Flow:
1. service() called
2. handleRequest() checks custom endpoints
3. Finds handler for "/health"
4. Executes handler.handle(request)
5. Returns response
```

### Example 2: Webhook

```
Request: POST /addon/webhook
Header: clockify-webhook-event-type: TIME_ENTRY_CREATED
Body: { "event": "TIME_ENTRY_CREATED", "timeEntry": {...} }

Flow:
1. service() called
2. handleRequest() checks custom endpoints (not found)
3. tryHandleWebhook() called
4. Finds handlers for path "/webhook"
5. handleWebhook() extracts event from header
6. Finds handler for "TIME_ENTRY_CREATED"
7. Records metrics
8. Executes handler
9. Returns response
```

### Example 3: Lifecycle

```
Request: POST /addon/lifecycle/installed
Body: { "lifecycle": "INSTALLED", "workspaceId": "..." }

Flow:
1. service() called
2. handleRequest() checks custom endpoints (not found)
3. tryHandleWebhook() skips (path starts with /lifecycle)
4. tryHandleLifecycle() called
5. Finds handler by path "/lifecycle/installed"
6. Executes handler
7. Returns response
```

---

## Error Responses

### Invalid JSON Payload

```json
{
  "message": "Invalid JSON payload",
  "details": "Unexpected character..."
}
```

**Status:** 400

### Missing Event Type

```json
{
  "message": "Missing webhook event type"
}
```

**Status:** 400

### Endpoint Not Found

```
Endpoint not found: /unknown/path
```

**Status:** 404

### Internal Server Error

```json
{
  "message": "Internal server error",
  "details": "NullPointerException..."
}
```

**Status:** 500

---

## Metrics Integration

The servlet automatically records Prometheus metrics for webhook processing:

```
# Webhook requests
webhook_requests_total{event="TIME_ENTRY_CREATED",path="/webhook"} 42

# Request duration
webhook_request_seconds{event="TIME_ENTRY_CREATED",path="/webhook",quantile="0.95"} 0.15

# Errors
webhook_errors_total{reason="invalid_json"} 3
webhook_errors_total{reason="missing_event"} 1

# Not handled
webhook_not_handled_total{event="UNKNOWN_EVENT"} 2
```

---

## Logging

All requests are logged at INFO level:

```
[INFO] GET /health
[INFO] POST /webhook
[WARN] No handler registered for webhook event: UNKNOWN_EVENT
[ERROR] Error handling request: POST /webhook
```

---

## Thread Safety

Servlet instances are thread-safe:
- `addon` reference is immutable after construction
- `objectMapper` is thread-safe
- Request/response handling is isolated per thread

---

## Related Classes

- **ClockifyAddon** - Provides handler mappings
- **RequestHandler** - Handler interface
- **HttpResponse** - Response wrapper
- **MetricsHandler** - Metrics registry
- **EmbeddedServer** - Embeds this servlet in Jetty

---

## See Also

- [ClockifyAddon.md](./ClockifyAddon.md) - Handler registration
- [RequestHandler.md](./RequestHandler.md) - Handler interface
- [EmbeddedServer.md](./EmbeddedServer.md) - Server wrapper

---

**File Location:** `/home/user/boileraddon/addons/addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java`

**Last Updated:** 2025-11-09
