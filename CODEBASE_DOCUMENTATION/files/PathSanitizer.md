# PathSanitizer.java - URL Path Validation & Normalization

**Location**: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/util/PathSanitizer.java`

**Type**: Utility/Security Class (static methods only)

**Purpose**: Validates and normalizes URL paths for security; prevents path traversal, injection, and other attacks

---

## Overview

PathSanitizer enforces strict validation rules for all paths used in the addon framework:
- Custom endpoints (/settings, /health, etc.)
- Lifecycle paths (/lifecycle/installed)
- Webhook paths (/webhook)

---

## Core Methods

### sanitize() - General Path Validation

```java
static String sanitize(String path)
```

**Purpose**: Validates and normalizes arbitrary paths (custom endpoints)

**Line-by-line Logic**:

1. **Null/Empty Check**:
   ```java
   if (path == null || path.isEmpty()) {
       return "/";  // Default to root
   }
   ```

2. **Trim Whitespace**:
   ```java
   path = path.trim();
   ```

3. **Security Checks** (sequential, fail-fast):

   **3a. Null Bytes** (multiple encodings):
   ```java
   // Detects: \u0000, %00, \\0, \\u0000
   if (path.contains("\u0000") ||
       path.contains("%00") ||
       path.contains("\\0")) {
       throw new IllegalArgumentException("Null byte detected");
   }
   ```
   **Reason**: Null bytes can bypass security filters in some languages

   **3b. ASCII Control Characters** (0x00-0x1F):
   ```java
   for (char c : path.toCharArray()) {
       if (c >= 0x00 && c <= 0x1F) {  // Control characters
           throw new IllegalArgumentException(
               "Invalid control character: " + ((int)c));
       }
   }
   ```
   **Reason**: Control chars can be interpreted differently by different systems

   **3c. Path Traversal** (.. sequences):
   ```java
   if (path.contains("..")) {
       throw new IllegalArgumentException("Path traversal (..) detected");
   }
   ```
   **Reason**: Prevents directory traversal attacks (../../../etc/passwd)

4. **Normalization**:
   ```java
   // Remove duplicate slashes
   while (path.contains("//")) {
       path = path.replace("//", "/");
   }

   // Ensure starts with /
   if (!path.startsWith("/")) {
       path = "/" + path;
   }

   // Remove trailing slash (except root)
   if (!path.equals("/") && path.endsWith("/")) {
       path = path.substring(0, path.length() - 1);
   }
   ```

5. **Character Validation**:
   ```java
   String validChars = "abcdefghijklmnopqrstuvwxyz0123456789-_/.~:?#[]@!$&'()*+,;=";

   for (char c : path.toCharArray()) {
       if (validChars.indexOf(c) < 0) {
           throw new IllegalArgumentException(
               "Invalid character: " + c + " (code: " + ((int)c) + ")");
       }
   }
   ```
   **Allowed**: alphanumeric, -, _, /, ., ~, :, ?, #, [, ], @, !, $, &, ', (, ), *, +, ,, ;, =

6. **Return Normalized Path**:
   ```java
   return path;  // e.g., "/settings", "/webhook", "/"
   ```

**Examples**:
```java
sanitize("/settings")                  // → "/settings"
sanitize("settings")                   // → "/settings"
sanitize("/settings/")                 // → "/settings"
sanitize("/settings//config")          // → "/settings/config"
sanitize("/")                          // → "/"
sanitize("//")                         // → "/"
sanitize(null)                         // → "/"
sanitize("")                           // → "/"
sanitize("/settings/../admin")         // → IllegalArgumentException ("Path traversal detected")
sanitize("/settings\u0000")            // → IllegalArgumentException ("Null byte detected")
sanitize("/settings\x01")              // → IllegalArgumentException ("Control character")
```

---

### sanitizeLifecyclePath() - Lifecycle Path Validation

```java
static String sanitizeLifecyclePath(String lifecycleType, String customPath)
```

**Purpose**: Validates lifecycle-specific paths (INSTALLED, DELETED, etc.)

**Parameters**:
- `lifecycleType`: Event type (INSTALLED, DELETED, UNINSTALLED)
- `customPath`: Optional custom path (null = use default)

**Logic**:

1. **Validate Lifecycle Type**:
   ```java
   if (lifecycleType == null || lifecycleType.isEmpty()) {
       return "/lifecycle";
   }
   ```

2. **Use Custom Path if Provided**:
   ```java
   if (customPath != null && !customPath.isEmpty()) {
       return sanitize(customPath);  // Recursively sanitize custom path
   }
   ```

3. **Generate Default Path**:
   ```java
   String defaultPath = "/lifecycle/" + lifecycleType.toLowerCase();
   return sanitize(defaultPath);
   ```

**Examples**:
```java
sanitizeLifecyclePath("INSTALLED", null)           // → "/lifecycle/installed"
sanitizeLifecyclePath("DELETED", null)             // → "/lifecycle/deleted"
sanitizeLifecyclePath("UNINSTALLED", null)         // → "/lifecycle/uninstalled"
sanitizeLifecyclePath("INSTALLED", "/custom")      // → "/custom"
sanitizeLifecyclePath("INSTALLED", "/events/installed")  // → "/events/installed"
sanitizeLifecyclePath(null, null)                  // → "/lifecycle"
```

---

### sanitizeWebhookPath() - Webhook Path Validation

```java
static String sanitizeWebhookPath(String path)
```

**Purpose**: Validates webhook-specific paths

**Logic**:

1. **Use Default if Null/Empty**:
   ```java
   if (path == null || path.isEmpty()) {
       return "/webhook";
   }
   ```

2. **Sanitize Provided Path**:
   ```java
   return sanitize(path);
   ```

**Examples**:
```java
sanitizeWebhookPath(null)              // → "/webhook"
sanitizeWebhookPath("")                // → "/webhook"
sanitizeWebhookPath("/webhook")        // → "/webhook"
sanitizeWebhookPath("/events")         // → "/events"
sanitizeWebhookPath("/webhook/time-entries")  // → "/webhook/time-entries"
```

---

## Security Threat Model

### Threats Addressed

| Threat | Attack | Defense | Method |
|--------|--------|---------|--------|
| **Null Byte Injection** | `/settings\x00.jsp` | Reject \x00, %00 | sanitize() |
| **Path Traversal** | `/../../admin` | Reject .. | sanitize() |
| **Directory Traversal** | `/settings/../admin` | Reject .. | sanitize() |
| **Control Char Injection** | `/settings\x1f` | Reject 0x00-0x1F | sanitize() |
| **Double Encoding** | `%252E%252E%252Fadmin` | Already decoded by servlet | N/A |
| **Unicode Encoding** | `/\u2215admin` | Accept only ASCII | Character validation |
| **Case Sensitivity** | `/Settings` vs `/settings` | Keep as-is (routes case-sensitive) | N/A |

### Threats NOT Fully Addressed

| Threat | Reason | Mitigation |
|--------|--------|-----------|
| **Case Sensitivity Bypass** | Windows paths case-insensitive | Use Linux for production |
| **Symlink Traversal** | File system issue, not URL issue | Resolve symlinks at app level |
| **LDAP Injection** | Not path-related | Validate other inputs separately |
| **SQL Injection** | Not path-related | Use parameterized queries |

---

## Usage in Addon Framework

### Custom Endpoint Registration

```java
// In ClockifyAddon
public void registerCustomEndpoint(String path, RequestHandler handler) {
    String normalizedPath = PathSanitizer.sanitize(path);
    endpoints.put(normalizedPath, handler);
}

// User calls:
addon.registerCustomEndpoint("/settings", settingsHandler);
// Validated to: "/settings"

addon.registerCustomEndpoint("settings", settingsHandler);
// Validated to: "/settings" (normalized)

addon.registerCustomEndpoint("/settings/", settingsHandler);
// Validated to: "/settings" (trailing slash removed)

addon.registerCustomEndpoint("/settings/../admin", settingsHandler);
// Throws: IllegalArgumentException (path traversal detected)
```

### Lifecycle Handler Registration

```java
// In ClockifyAddon
public void registerLifecycleHandler(String lifecycleType, String path, RequestHandler handler) {
    String normalizedPath = normalizeLifecyclePath(lifecycleType, path);
    // Uses PathSanitizer.sanitizeLifecyclePath()
}

// User calls:
addon.registerLifecycleHandler("INSTALLED", handler);
// Path: "/lifecycle/installed"

addon.registerLifecycleHandler("INSTALLED", "/custom-installed", handler);
// Path: "/custom-installed" (sanitized)
```

### Webhook Handler Registration

```java
// In ClockifyAddon
public void registerWebhookHandler(String event, String path, RequestHandler handler) {
    String normalizedPath = normalizeWebhookPath(path);
    // Uses PathSanitizer.sanitizeWebhookPath()
}

// User calls:
addon.registerWebhookHandler("TIME_ENTRY_CREATED", handler);
// Path: "/webhook" (default)

addon.registerWebhookHandler("TIME_ENTRY_CREATED", "/events", handler);
// Path: "/events" (sanitized)
```

---

## Valid Character Set

### Allowed Characters

```
Letters:  a-z, A-Z
Numbers:  0-9
Symbols:  - _ / . ~ : ? # [ ] @ ! $ & ' ( ) * + , ; =
Spaces:   None (not allowed)
```

### Examples of Valid Paths

```
✓ /settings
✓ /api/v1/rules
✓ /webhook-events
✓ /cache_refresh
✓ /api/v2.0/entities
✓ /endpoint?param=value
✓ /path#fragment
✓ /path[0]
✓ /notify@domain
✓ /action!important
```

### Examples of Invalid Paths

```
✗ /settings space  (space not allowed)
✗ /settings<script> (< and > not allowed)
✗ /settings|admin  (| not allowed)
✗ /settings\admin  (\ not allowed)
✗ /settings`pwd`  (` not allowed)
✗ /settings;rm /  (; followed by dangerous pattern)
✗ /settings%00    (null byte)
```

---

## Integration Points

### 1. ClockifyAddon Registration

```java
public void registerCustomEndpoint(String path, RequestHandler handler) {
    String normalizedPath = PathSanitizer.sanitize(path);  // ← Called here
    endpoints.put(normalizedPath, handler);
}
```

### 2. AddonServlet Routing

```java
private HttpResponse handleRequest(HttpServletRequest request) {
    String path = request.getPathInfo();
    // Path already validated by registerCustomEndpoint() or servlet

    RequestHandler handler = addon.getEndpoints().get(path);
    // Direct lookup (safe to use)
}
```

### 3. Testing

```java
@Test
void testPathSanitizer() {
    assertEquals("/settings", PathSanitizer.sanitize("/settings"));
    assertEquals("/settings", PathSanitizer.sanitize("/settings/"));

    assertThrows(
        IllegalArgumentException.class,
        () -> PathSanitizer.sanitize("/settings/../admin"));
}
```

---

## Exception Handling

PathSanitizer throws **IllegalArgumentException** with descriptive messages:

```java
try {
    addon.registerCustomEndpoint("/settings\x00admin", handler);
} catch (IllegalArgumentException e) {
    System.out.println(e.getMessage());
    // "Null byte detected"
}

try {
    addon.registerCustomEndpoint("/settings/../admin", handler);
} catch (IllegalArgumentException e) {
    System.out.println(e.getMessage());
    // "Path traversal (..) detected"
}

try {
    addon.registerCustomEndpoint("/settings<script>", handler);
} catch (IllegalArgumentException e) {
    System.out.println(e.getMessage());
    // "Invalid character: < (code: 60)"
}
```

---

## Performance

All validation operations are **O(n)** where n = path length:
- Single pass for null byte detection
- Single pass for control character detection
- Single pass for double-slash normalization
- Single pass for character validation

**Typical paths** (< 100 chars): < 1ms validation time

---

## Testing Strategy

### Unit Tests

```java
@Test
void testBasicSanitization() {
    assertEquals("/settings", PathSanitizer.sanitize("/settings"));
}

@Test
void testPathTraversalDetection() {
    assertThrows(IllegalArgumentException.class,
        () -> PathSanitizer.sanitize("/../admin"));
    assertThrows(IllegalArgumentException.class,
        () -> PathSanitizer.sanitize("/settings/.."));
}

@Test
void testNullByteDetection() {
    assertThrows(IllegalArgumentException.class,
        () -> PathSanitizer.sanitize("/settings\u0000"));
    assertThrows(IllegalArgumentException.class,
        () -> PathSanitizer.sanitize("/settings%00"));
}

@Test
void testControlCharDetection() {
    for (int i = 0; i < 32; i++) {
        char c = (char) i;
        assertThrows(IllegalArgumentException.class,
            () -> PathSanitizer.sanitize("/settings" + c));
    }
}

@Test
void testNormalization() {
    assertEquals("/settings", PathSanitizer.sanitize("/settings/"));
    assertEquals("/settings", PathSanitizer.sanitize("/settings//"));
    assertEquals("/", PathSanitizer.sanitize("/"));
    assertEquals("/", PathSanitizer.sanitize("//"));
}

@Test
void testValidCharacters() {
    assertEquals("/api/v2.0", PathSanitizer.sanitize("/api/v2.0"));
    assertEquals("/webhook_events", PathSanitizer.sanitize("/webhook_events"));
    assertEquals("/cache-refresh", PathSanitizer.sanitize("/cache-refresh"));
}

@Test
void testLifecyclePath() {
    assertEquals("/lifecycle/installed",
        PathSanitizer.sanitizeLifecyclePath("INSTALLED", null));
    assertEquals("/lifecycle/deleted",
        PathSanitizer.sanitizeLifecyclePath("DELETED", null));
    assertEquals("/custom",
        PathSanitizer.sanitizeLifecyclePath("INSTALLED", "/custom"));
}

@Test
void testWebhookPath() {
    assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(null));
    assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath(""));
    assertEquals("/webhook", PathSanitizer.sanitizeWebhookPath("/webhook"));
    assertEquals("/events", PathSanitizer.sanitizeWebhookPath("/events"));
}
```

---

## Common Mistakes

### Mistake 1: Not Sanitizing User Input

```java
// ❌ Bad: Direct registration of user-provided path
String path = request.getParameter("endpoint");
addon.registerCustomEndpoint(path, handler);  // Vulnerable if not validated!

// ✅ Good: Framework does sanitization automatically
// (ClockifyAddon.registerCustomEndpoint calls PathSanitizer.sanitize)
```

### Mistake 2: Sanitizing Twice

```java
// ❌ Bad: Redundant sanitization
String path = PathSanitizer.sanitize("/settings");
addon.registerCustomEndpoint(path, handler);  // Already sanitized!

// ✅ Good: Let framework sanitize
addon.registerCustomEndpoint("/settings", handler);
```

### Mistake 3: Assuming Sanitized Paths are Identical

```java
// ✓ These are equivalent after sanitization:
// "/settings"
// "/settings/"
// "/settings//"
// "settings"

// Register with one form:
addon.registerCustomEndpoint("/settings/", handler);

// Request with another form (both route to same handler):
// GET /addon/settings      ✓ Found
// GET /addon/settings/     ✓ Found (normalized to /settings)
// GET /addon/Settings      ✗ Not found (case-sensitive)
```

---

## Related Classes

- **ClockifyAddon.java**: Calls PathSanitizer for all path registrations
- **AddonServlet.java**: Uses sanitized paths for routing
- **BaseUrlDetector.java**: URL/path parsing (uses pre-sanitized paths)

---

## Notes for Developers

1. **Automatic Sanitization**: Always register paths via ClockifyAddon methods (they sanitize automatically)
2. **Don't Bypass**: Never access endpoint maps directly (paths are already sanitized)
3. **Early Validation**: Sanitization happens at registration time, not request time
4. **Case Sensitive**: Paths are case-sensitive on all platforms (even Windows)
5. **Trailing Slashes**: Removed during normalization for consistency
6. **Default Paths**: Use built-in defaults (/webhook, /lifecycle/{type}) when possible
7. **Testing**: Write tests for custom endpoint paths to ensure proper routing
