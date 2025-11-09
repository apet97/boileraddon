# HttpResponse.java - Response Builder

**Location**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/HttpResponse.java`

**Type**: Immutable DTO

**Purpose**: Standardized response object returned by RequestHandler implementations; encapsulates HTTP status, body, and content-type

---

## Class Overview

```java
public class HttpResponse
```

Immutable value object representing an HTTP response. RequestHandlers return this instead of directly writing to HttpServletResponse.

---

## Core Fields

| Field | Type | Purpose |
|-------|------|---------|
| `statusCode` | int | HTTP status (200, 404, 500, etc.) |
| `body` | String | Response body content |
| `contentType` | String | MIME type (application/json, text/plain, text/html, etc.) |

---

## Static Factory Methods

### ok() - 200 Success

**Method 1: Text/Plain**
```java
static HttpResponse ok(String body)
```
- Status: 200
- Body: Provided string
- Content-Type: text/plain

**Example**:
```java
HttpResponse.ok("Hello, World!")
// → HTTP 200 OK
// → Content-Type: text/plain
// → Body: Hello, World!
```

**Method 2: Custom Content-Type**
```java
static HttpResponse ok(String body, String contentType)
```
- Status: 200
- Body: Provided string
- Content-Type: Custom (application/json, text/html, etc.)

**Example**:
```java
HttpResponse.ok("{\"message\":\"success\"}", "application/json")
// → HTTP 200 OK
// → Content-Type: application/json
// → Body: {"message":"success"}
```

---

### error() - Error Response

**Method 1: Simple Error**
```java
static HttpResponse error(int statusCode, String message)
```
- Status: Provided code (400, 404, 500, etc.)
- Body: Provided message
- Content-Type: text/plain

**Example**:
```java
HttpResponse.error(404, "Endpoint not found")
// → HTTP 404 Not Found
// → Content-Type: text/plain
// → Body: Endpoint not found
```

**Method 2: Error with Content-Type**
```java
static HttpResponse error(int statusCode, String message, String contentType)
```
- Status: Provided code
- Body: Provided message
- Content-Type: Custom

**Example**:
```java
HttpResponse.error(400, "{\"error\":\"Invalid input\"}", "application/json")
// → HTTP 400 Bad Request
// → Content-Type: application/json
// → Body: {"error":"Invalid input"}
```

---

## Getter Methods

### getStatusCode()

```java
int getStatusCode()
```

Returns HTTP status code.

**Example**:
```java
HttpResponse response = HttpResponse.ok("Success");
int status = response.getStatusCode();  // 200
```

---

### getBody()

```java
String getBody()
```

Returns response body content.

**Example**:
```java
HttpResponse response = HttpResponse.ok("Test body");
String body = response.getBody();  // "Test body"
```

---

### getContentType()

```java
String getContentType()
```

Returns MIME type.

**Example**:
```java
HttpResponse response = HttpResponse.ok("{}", "application/json");
String contentType = response.getContentType();  // "application/json"
```

---

## HTTP Status Codes Reference

| Code | Method | Usage |
|------|--------|-------|
| 200 | ok() | Request succeeded |
| 201 | error(201, msg) | Resource created |
| 204 | error(204, msg) | No content (success) |
| 400 | error(400, msg) | Bad request (invalid input) |
| 401 | error(401, msg) | Unauthorized (missing/invalid auth) |
| 403 | error(403, msg) | Forbidden (invalid signature) |
| 404 | error(404, msg) | Not found (endpoint doesn't exist) |
| 405 | error(405, msg) | Method not allowed (GET vs POST) |
| 429 | error(429, msg) | Too many requests (rate limited) |
| 500 | error(500, msg) | Server error (exception in handler) |
| 502 | error(502, msg) | Bad gateway (upstream error) |
| 503 | error(503, msg) | Service unavailable |

---

## Common Content-Types

| Type | Extension | Usage |
|------|-----------|-------|
| application/json | .json | JSON responses |
| text/plain | .txt | Plain text responses |
| text/html | .html | HTML documents |
| text/css | .css | Stylesheets |
| application/xml | .xml | XML documents |
| text/javascript | .js | JavaScript code |
| image/png | .png | PNG images |
| image/jpeg | .jpg | JPEG images |
| application/pdf | .pdf | PDF documents |

---

## Usage Patterns

### Pattern 1: Simple Success Response

```java
RequestHandler handler = (request) ->
    HttpResponse.ok("Operation successful");

// Result: HTTP 200, text/plain, "Operation successful"
```

### Pattern 2: JSON Success Response

```java
RequestHandler handler = (request) -> {
    String json = "{\"status\":\"ok\",\"id\":123}";
    return HttpResponse.ok(json, "application/json");
};

// Result: HTTP 200, application/json, {"status":"ok","id":123}
```

### Pattern 3: Validation Error (400)

```java
RequestHandler handler = (request) -> {
    String name = request.getParameter("name");

    if (name == null || name.isEmpty()) {
        return HttpResponse.error(400, "Name parameter is required");
    }

    return HttpResponse.ok("Name: " + name);
};

// If name missing: HTTP 400, text/plain, "Name parameter is required"
// If name provided: HTTP 200, text/plain, "Name: Alice"
```

### Pattern 4: Signature Verification Failure (401/403)

```java
RequestHandler webhookHandler = (request) -> {
    WebhookSignatureValidator validator = new WebhookSignatureValidator();
    HttpResponse validationResult = validator.verify(request, workspaceId);

    // validator.verify() already returns HttpResponse
    if (validationResult.getStatusCode() != 200) {
        return validationResult;  // 401 or 403
    }

    // Continue processing if signature valid
    return HttpResponse.ok("Webhook processed");
};
```

### Pattern 5: Not Found (404)

```java
RequestHandler handler = (request) -> {
    String id = request.getParameter("id");

    if (id == null) {
        return HttpResponse.error(404, "Resource not found");
    }

    // Load and return resource...
    return HttpResponse.ok(resource);
};
```

### Pattern 6: Server Error (500)

```java
RequestHandler handler = (request) -> {
    try {
        // Database operation
        return HttpResponse.ok("Success");
    } catch (SQLException e) {
        // Unexpected error, return 500
        return HttpResponse.error(500, "Database error: " + e.getMessage());
    }
};
```

### Pattern 7: HTML Response

```java
RequestHandler settingsController = (request) -> {
    String html = """
        <html>
        <head><title>Settings</title></head>
        <body>
            <h1>Addon Settings</h1>
            <p>Configure addon preferences here</p>
        </body>
        </html>
        """;

    return HttpResponse.ok(html, "text/html");
};

// Result: HTTP 200, text/html, [HTML content]
```

### Pattern 8: JSON Error Response

```java
RequestHandler handler = (request) -> {
    if (someError) {
        String errorJson = "{\"error\":\"Invalid request\",\"code\":\"INVALID_INPUT\"}";
        return HttpResponse.error(400, errorJson, "application/json");
    }

    return HttpResponse.ok("Success");
};
```

---

## Immutability

HttpResponse is **immutable** (fields set only in constructor, no setters).

**Benefits**:
- Thread-safe (can be shared across threads)
- Predictable behavior (values don't change after creation)
- Safe for testing (mock responses are consistent)

**Example**:
```java
HttpResponse response = HttpResponse.ok("Test");

// Cannot modify:
response.statusCode = 404;  // Compile error
response.body = "Changed";  // Compile error

// Create new response instead:
HttpResponse modified = HttpResponse.error(404, "Not found");
```

---

## Integration with AddonServlet

### How AddonServlet Uses HttpResponse

```java
// In AddonServlet.service()
HttpResponse response = handler.handle(request);

// Extract fields and apply to servlet response
servletResponse.setStatus(response.getStatusCode());
servletResponse.setContentType(response.getContentType());
servletResponse.getWriter().write(response.getBody());
servletResponse.getWriter().flush();
```

### Request Handler Returns HttpResponse

```java
// RequestHandler returns HttpResponse
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}

// Handler implementation
RequestHandler handler = (request) -> {
    if (isValid(request)) {
        return HttpResponse.ok("Success");  // Return HttpResponse
    } else {
        return HttpResponse.error(400, "Invalid");  // Return HttpResponse
    }
};
```

---

## Testing HttpResponse

### Unit Test Examples

```java
@Test
void testOkResponse() {
    HttpResponse response = HttpResponse.ok("Test message");

    assertEquals(200, response.getStatusCode());
    assertEquals("Test message", response.getBody());
    assertEquals("text/plain", response.getContentType());
}

@Test
void testOkWithContentType() {
    String json = "{\"status\":\"ok\"}";
    HttpResponse response = HttpResponse.ok(json, "application/json");

    assertEquals(200, response.getStatusCode());
    assertEquals(json, response.getBody());
    assertEquals("application/json", response.getContentType());
}

@Test
void testErrorResponse() {
    HttpResponse response = HttpResponse.error(404, "Not found");

    assertEquals(404, response.getStatusCode());
    assertEquals("Not found", response.getBody());
    assertEquals("text/plain", response.getContentType());
}

@Test
void testErrorWithContentType() {
    String errorJson = "{\"error\":\"Invalid\"}";
    HttpResponse response = HttpResponse.error(400, errorJson, "application/json");

    assertEquals(400, response.getStatusCode());
    assertEquals(errorJson, response.getBody());
    assertEquals("application/json", response.getContentType());
}

@Test
void testImmutability() {
    HttpResponse response = HttpResponse.ok("Test");

    // Should be immutable (no way to change values after creation)
    HttpResponse sameResponse = response;
    assertEquals("Test", sameResponse.getBody());

    // Changing reference doesn't affect original
    response = HttpResponse.error(500, "Error");
    assertEquals("Test", sameResponse.getBody());  // Unchanged
}
```

---

## Common Patterns in Addons

### Auto-Tag Assistant

```java
// Successful webhook processing
RequestHandler webhookHandler = (request) -> {
    try {
        // Process webhook and apply tags
        int tagsApplied = applySuggestedTags(...);
        String response = "{\"applied\":" + tagsApplied + "}";
        return HttpResponse.ok(response, "application/json");
    } catch (Exception e) {
        return HttpResponse.error(500, e.getMessage());
    }
};
```

### Rules Engine

```java
// Return list of rules
RequestHandler listRules = (request) -> {
    List<Rule> rules = rulesStore.getAll(workspaceId);
    String json = objectMapper.writeValueAsString(rules);
    return HttpResponse.ok(json, "application/json");
};

// Error on missing workspace
if (workspaceId == null) {
    return HttpResponse.error(400, "workspaceId parameter required");
}
```

### Overtime Addon

```java
// Settings page
RequestHandler settingsController = (request) -> {
    String html = "<html>...</html>";
    return HttpResponse.ok(html, "text/html");
};

// API endpoint
RequestHandler getSettings = (request) -> {
    Settings settings = settingsStore.get(workspaceId);
    String json = objectMapper.writeValueAsString(settings);
    return HttpResponse.ok(json, "application/json");
};
```

---

## Best Practices

1. **Choose Correct Status Code**:
   ```java
   // ✅ Good: 400 for client error
   return HttpResponse.error(400, "Missing parameter");

   // ❌ Bad: 500 for expected error
   return HttpResponse.error(500, "Missing parameter");
   ```

2. **Set Content-Type for JSON**:
   ```java
   // ✅ Good
   return HttpResponse.ok(json, "application/json");

   // ❌ Bad (defaults to text/plain)
   return HttpResponse.ok(json);
   ```

3. **Use Consistent Error Format**:
   ```java
   // Define error response format and use consistently
   String errorJson = "{\"error\":\"...\",\"code\":\"...\"}";
   return HttpResponse.error(400, errorJson, "application/json");
   ```

4. **Never Return Null**:
   ```java
   // ✅ Good: Always return HttpResponse
   if (error) return HttpResponse.error(400, "Error");
   return HttpResponse.ok("Success");

   // ❌ Bad: Returning null causes NPE
   if (error) return null;
   return HttpResponse.ok("Success");
   ```

5. **Meaningful Error Messages**:
   ```java
   // ✅ Good: Specific, actionable message
   return HttpResponse.error(400, "workspaceId parameter is required");

   // ❌ Bad: Vague message
   return HttpResponse.error(400, "Error");
   ```

---

## Response Examples

### Success Response
```
HTTP/1.1 200 OK
Content-Type: text/plain

Operation successful
```

### JSON Success Response
```
HTTP/1.1 200 OK
Content-Type: application/json

{"status":"ok","id":123}
```

### Error Response
```
HTTP/1.1 404 Not Found
Content-Type: text/plain

Endpoint not found
```

### JSON Error Response
```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"Invalid input","message":"Name parameter required"}
```

---

## Related Classes

- **RequestHandler.java**: Returns HttpResponse
- **AddonServlet.java**: Uses HttpResponse fields
- **DefaultManifestController.java**: Returns JSON response
- **WebhookSignatureValidator.java**: Returns error responses

---

## Version History

- **1.0**: Initial implementation with ok() and error() factories
- **1.1**: Added custom content-type support

---

## Notes for Developers

1. **Always Return HttpResponse**: Never return null
2. **Immutable**: Fields cannot be changed after creation
3. **Thread-Safe**: Safe to share responses between threads
4. **Simple DTO**: No business logic, just data container
5. **Factory Methods**: Use static factories (ok, error) for creation
6. **Content-Type**: Set explicitly for JSON and HTML responses
7. **Status Codes**: Choose appropriate codes (400, 404, 500, etc.)
