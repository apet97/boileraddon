# RequestHandler.java - Handler Interface

**Location**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/RequestHandler.java`

**Type**: Functional Interface

**Purpose**: Callback interface for processing HTTP requests in addon handlers

---

## Interface Definition

```java
@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}
```

---

## Method Signature

### handle()

```java
HttpResponse handle(HttpServletRequest request) throws Exception
```

**Parameters**:
- `request`: HttpServletRequest with full request context
  - Method (GET, POST, etc.)
  - Path and query parameters
  - Headers (including custom ones)
  - Request body (if POST/PUT)
  - Attributes set by filters/servlet

**Returns**:
- `HttpResponse`: Response object with status, body, content-type
- Never null (throw exception instead)

**Throws**:
- `Exception`: Any checked/unchecked exception
- **Important**: Exceptions caught by AddonServlet, converted to HTTP 500

**Contract**:
- Must return non-null HttpResponse
- Should not throw exceptions (return error HttpResponse instead)
- Can access request attributes (e.g., cached JSON body)
- Should be thread-safe (called concurrently for different requests)

---

## Usage Patterns

### Pattern 1: Simple Handler

```java
RequestHandler simpleHandler = (request) ->
    HttpResponse.ok("Hello, World!");

// Register:
addon.registerCustomEndpoint("/hello", simpleHandler);

// Usage:
// GET http://localhost:8080/addon/hello → "Hello, World!"
```

### Pattern 2: Request Data Access

```java
RequestHandler echoHandler = (request) -> {
    String name = request.getParameter("name");
    return HttpResponse.ok("Hello, " + (name != null ? name : "stranger"));
};

// Usage:
// GET http://localhost:8080/addon/echo?name=Alice → "Hello, Alice"
```

### Pattern 3: JSON Processing

```java
RequestHandler jsonHandler = (request) -> {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode body = mapper.readTree(request.getInputStream());

    String action = body.get("action").asText();
    return HttpResponse.ok("Action: " + action, "application/json");
};

// Usage:
// POST http://localhost:8080/addon/process
// Body: {"action":"create"}
// Response: "Action: create"
```

### Pattern 4: Conditional Logic

```java
RequestHandler conditionalHandler = (request) -> {
    String method = request.getMethod();

    if ("GET".equals(method)) {
        return HttpResponse.ok("GET request processed");
    } else if ("POST".equals(method)) {
        // Process POST
        return HttpResponse.ok("POST request processed");
    } else {
        return HttpResponse.error(405, "Method not allowed");
    }
};
```

### Pattern 5: Using Cached Request Body

```java
RequestHandler webhookHandler = (request) -> {
    // AddonServlet caches parsed JSON in request attributes
    JsonNode body = (JsonNode) request.getAttribute("_cachedJsonBody");

    if (body == null) {
        return HttpResponse.error(400, "No JSON body");
    }

    String workspaceId = body.get("workspaceId").asText();
    return HttpResponse.ok("Workspace: " + workspaceId);
};

// Register as webhook handler:
addon.registerWebhookHandler("TIME_ENTRY_CREATED", webhookHandler);
```

---

## Common Implementation Patterns

### Pattern A: Stateless Lambda

```java
RequestHandler handler = (request) -> HttpResponse.ok("OK");
```

**Pros**: Simple, concise
**Cons**: Hard to test, limited logic

### Pattern B: Anonymous Inner Class

```java
RequestHandler handler = new RequestHandler() {
    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        // Implementation here
        return HttpResponse.ok("Response");
    }
};
```

**Pros**: Readable, can use multi-line logic
**Cons**: Verbose

### Pattern C: Separate Class Implementation

```java
public class SettingsHandler implements RequestHandler {
    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        // Implementation here
        return HttpResponse.ok("Settings content");
    }
}

// Register:
addon.registerCustomEndpoint("/settings", new SettingsHandler());
```

**Pros**: Testable, reusable, maintainable
**Cons**: More boilerplate

### Pattern D: Method Reference

```java
public static HttpResponse handleSettings(HttpServletRequest request) {
    return HttpResponse.ok("Settings");
}

// Register:
addon.registerCustomEndpoint("/settings", SettingsHandler::handleSettings);
```

**Pros**: Clean, testable
**Cons**: Less common in servlet code

---

## Request Context Access

### Common HttpServletRequest Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `getMethod()` | HTTP method | "GET", "POST", "PUT", "DELETE" |
| `getRequestURI()` | Full URI path | "/addon/settings" |
| `getPathInfo()` | Path after servlet prefix | "/settings" |
| `getQueryString()` | URL query string | "id=123&name=test" |
| `getParameter(name)` | Single query/form parameter | "test" |
| `getParameters()` | All parameters map | {"id": ["123"], ...} |
| `getHeader(name)` | Single header value | "application/json" |
| `getHeaders(name)` | All values for header | Enumeration<String> |
| `getInputStream()` | Request body stream | Raw bytes |
| `getReader()` | Request body reader | Character stream |
| `getAttribute(name)` | Request attribute (set by servlet/filters) | Cached JSON body |
| `getContentType()` | Content-Type header | "application/json" |
| `getContentLength()` | Body size in bytes | 1024 |

### Headers Commonly Used

```java
RequestHandler handler = (request) -> {
    String contentType = request.getHeader("Content-Type");
    String authorization = request.getHeader("Authorization");
    String workspaceId = request.getHeader("X-Workspace-ID");
    String eventType = request.getHeader("Clockify-Webhook-Event-Type");

    // Process based on headers
    return HttpResponse.ok("Processed");
};
```

---

## Error Handling in Handlers

### Throwing Exceptions

```java
RequestHandler handler = (request) -> {
    throw new IllegalArgumentException("Invalid input");
    // ↓ AddonServlet catches
    // ↓ Converts to HTTP 500 with exception message in JSON
};
```

**Result**:
```json
{
  "error": "java.lang.IllegalArgumentException",
  "message": "Invalid input",
  "details": "[stack trace...]"
}
```

### Returning Error Responses

```java
RequestHandler handler = (request) -> {
    String param = request.getParameter("required");

    if (param == null) {
        return HttpResponse.error(400, "Missing required parameter");
    }

    return HttpResponse.ok("Success");
};

// Result on error: HTTP 400, plain text "Missing required parameter"
```

### Best Practice

```java
// ✅ Good: Return HTTP error response for client errors (400, 404, etc.)
if (request.getParameter("id") == null) {
    return HttpResponse.error(400, "Missing id parameter");
}

// ✅ Good: Throw exception for server errors (unexpected state)
try {
    // Database operation
} catch (SQLException e) {
    throw new RuntimeException("Database error", e);  // → HTTP 500
}

// ❌ Bad: Don't hide errors
if (request.getParameter("id") == null) {
    return HttpResponse.ok("No id");  // Misleading success response
}

// ❌ Bad: Don't throw for expected errors
if (request.getParameter("id") == null) {
    throw new IllegalArgumentException("Missing id");  // Should be 400, not 500
}
```

---

## JSON Request Processing

### Reading JSON from Request

```java
RequestHandler jsonHandler = (request) -> {
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = mapper.readTree(request.getInputStream());

        String name = body.get("name").asText();
        int age = body.get("age").asInt();

        return HttpResponse.ok("Processed: " + name + ", " + age);
    } catch (IOException e) {
        return HttpResponse.error(400, "Invalid JSON");
    }
};
```

### Using Cached JSON (AddonServlet Optimization)

```java
RequestHandler handler = (request) -> {
    // AddonServlet caches parsed JSON in request attributes
    JsonNode cachedBody = (JsonNode) request.getAttribute("_cachedJsonBody");

    if (cachedBody == null) {
        return HttpResponse.error(400, "No JSON body");
    }

    // Process cached body (no need to re-parse or re-read)
    return HttpResponse.ok("Processed");
};
```

---

## Webhook Handler Example

```java
RequestHandler webhookHandler = (request) -> {
    // 1. Parse webhook event
    String eventType = request.getHeader("Clockify-Webhook-Event-Type");
    JsonNode body = (JsonNode) request.getAttribute("_cachedJsonBody");

    if (body == null) {
        return HttpResponse.error(400, "No body");
    }

    // 2. Extract payload
    String workspaceId = body.get("workspaceId").asText();
    String userId = body.get("userId").asText();

    // 3. Validate signature
    WebhookSignatureValidator validator = new WebhookSignatureValidator();
    HttpResponse signatureCheck = validator.verify(request, workspaceId);

    if (signatureCheck.getStatusCode() != 200) {
        return signatureCheck;  // 401 or 403
    }

    // 4. Process event
    switch (eventType) {
        case "TIME_ENTRY_CREATED":
            // Handle time entry creation
            break;
        case "TIME_ENTRY_UPDATED":
            // Handle update
            break;
    }

    return HttpResponse.ok("Webhook processed");
};
```

---

## Testing Handlers

### Unit Test Example

```java
@Test
void testSimpleHandler() {
    RequestHandler handler = (request) -> HttpResponse.ok("test");

    // Mock request
    HttpServletRequest request = mock(HttpServletRequest.class);
    request.setMethod("GET");

    // Call handler
    HttpResponse response = handler.handle(request);

    // Assert
    assertEquals(200, response.getStatusCode());
    assertEquals("test", response.getBody());
}

@Test
void testParameterHandler() {
    RequestHandler handler = (request) -> {
        String name = request.getParameter("name");
        return name != null
            ? HttpResponse.ok("Hello " + name)
            : HttpResponse.error(400, "Missing name");
    };

    // Test with parameter
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("name")).thenReturn("Alice");

    HttpResponse response = handler.handle(request);

    assertEquals(200, response.getStatusCode());
    assertEquals("Hello Alice", response.getBody());

    // Test without parameter
    when(request.getParameter("name")).thenReturn(null);
    response = handler.handle(request);

    assertEquals(400, response.getStatusCode());
}

@Test
void testExceptionHandling() {
    RequestHandler handler = (request) -> {
        throw new RuntimeException("Test error");
    };

    HttpServletRequest request = mock(HttpServletRequest.class);

    // Exception should be caught by servlet
    assertThrows(RuntimeException.class, () -> handler.handle(request));
}
```

---

## Thread Safety

### Thread-Safe Pattern

```java
// ✅ Good: No shared state
RequestHandler handler = (request) -> {
    // Process request without accessing shared fields
    return HttpResponse.ok("Response");
};

// Can be called from multiple threads concurrently
```

### Not Thread-Safe Pattern

```java
// ❌ Bad: Shared mutable state
List<String> cache = new ArrayList<>();

RequestHandler handler = (request) -> {
    cache.add(request.getParameter("id"));  // Race condition!
    return HttpResponse.ok("Added");
};

// Multiple concurrent requests may corrupt cache
```

### Thread-Safe with Shared State

```java
// ✅ Good: Use synchronized collection
Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

RequestHandler handler = (request) -> {
    cache.put("id", request.getParameter("id"));  // Thread-safe
    return HttpResponse.ok("Added");
};
```

---

## Handler Registration Examples

### Custom Endpoint

```java
addon.registerCustomEndpoint("/settings", (request) -> {
    return HttpResponse.ok("Settings HTML content", "text/html");
});
```

### Lifecycle Handler

```java
addon.registerLifecycleHandler("INSTALLED", (request) -> {
    JsonNode body = (JsonNode) request.getAttribute("_cachedJsonBody");
    String workspaceId = body.get("workspaceId").asText();
    // Save workspace token
    return HttpResponse.ok("Installed");
});
```

### Webhook Handler

```java
addon.registerWebhookHandler("TIME_ENTRY_CREATED", (request) -> {
    // Process webhook
    return HttpResponse.ok("Processed");
});
```

---

## Related Classes

- **AddonServlet.java**: Calls RequestHandler.handle() and processes exceptions
- **HttpResponse.java**: Return type
- **ClockifyAddon.java**: Stores RequestHandler instances
- **EmbeddedServer.java**: Mounts servlet that uses handlers

---

## Best Practices

1. **Return specific HTTP statuses**: 200 for success, 400 for client error, 500 for server error, 404 for not found
2. **Handle exceptions gracefully**: Return HttpResponse.error() for expected cases, throw for unexpected
3. **Validate input**: Check required parameters, headers, body content early
4. **Be thread-safe**: Handlers called concurrently, avoid shared mutable state
5. **Keep handlers simple**: Extract complex logic to separate classes
6. **Use proper content types**: "application/json" for JSON, "text/html" for HTML
7. **Log important events**: Use SLF4J for debugging and monitoring

---

## Common Mistakes

### Mistake 1: Returning null

```java
// ❌ Bad: Returns null
RequestHandler handler = (request) -> {
    if (someCondition) {
        return HttpResponse.ok("OK");
    }
    return null;  // NPE when servlet tries to use response
};

// ✅ Good: Always return HttpResponse
RequestHandler handler = (request) -> {
    if (someCondition) {
        return HttpResponse.ok("OK");
    }
    return HttpResponse.error(400, "Invalid condition");
};
```

### Mistake 2: Reading request body twice

```java
// ❌ Bad: Can't read input stream twice
RequestHandler handler = (request) -> {
    String body1 = new String(request.getInputStream().readAllBytes());
    String body2 = new String(request.getInputStream().readAllBytes());  // Empty!
};

// ✅ Good: Use cached JSON or read once
RequestHandler handler = (request) -> {
    JsonNode cachedBody = (JsonNode) request.getAttribute("_cachedJsonBody");
    // or read once and cache
};
```

### Mistake 3: Throwing for expected errors

```java
// ❌ Bad: Client error becomes 500
RequestHandler handler = (request) -> {
    String id = request.getParameter("id");
    if (id == null) {
        throw new IllegalArgumentException("Missing id");  // 500!
    }
    return HttpResponse.ok("Found");
};

// ✅ Good: Return appropriate HTTP status
RequestHandler handler = (request) -> {
    String id = request.getParameter("id");
    if (id == null) {
        return HttpResponse.error(400, "Missing id parameter");  // 400
    }
    return HttpResponse.ok("Found");
};
```

---

## Notes for Developers

1. **Functional Interface**: Use lambda syntax for simple handlers
2. **Complex Logic**: Extract to separate class implementing RequestHandler
3. **Exception Handling**: Prefer returning error responses over throwing exceptions
4. **Thread Safety**: Handlers must be thread-safe (called concurrently)
5. **Request Caching**: Use cached JSON in request attributes (set by AddonServlet)
6. **Testing**: Mock HttpServletRequest for unit testing
