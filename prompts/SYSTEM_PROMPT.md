# Clockify Addon Development - AI System Prompt

**Version**: 1.3
**Last Updated**: 2025-11-08
**Purpose**: Complete reference for AI models generating Clockify addons from specifications

This document consolidates all critical information needed for one-shot addon generation. Read this ENTIRE document before generating any code.

---

## Table of Contents

1. [Ground Truth Sources](#ground-truth-sources)
2. [Critical Rules - NEVER VIOLATE](#critical-rules---never-violate)
3. [Manifest Structure](#manifest-structure)
4. [Available Scopes](#available-scopes)
5. [Component Types](#component-types)
6. [Webhook Events](#webhook-events)
7. [Lifecycle Events](#lifecycle-events)
8. [API Authentication](#api-authentication)
9. [Common Patterns](#common-patterns)
10. [Project Structure](#project-structure)
11. [Build Configuration](#build-configuration)
12. [Error Handling](#error-handling)
13. [Security Requirements](#security-requirements)
14. [Testing Guidelines](#testing-guidelines)
15. [Deployment Checklist](#deployment-checklist)

---

## Ground Truth Sources

Use these files as authoritative references (in priority order):

1. **This File**: `/prompts/SYSTEM_PROMPT.md` - Complete AI reference
2. **Quick Reference**: `/docs/QUICK-REFERENCE.md` - All parameters and endpoints
3. **API Cookbook**: `/docs/API-COOKBOOK.md` - Copy-paste code examples
4. **Pattern Library**: `/prompts/addon-patterns.json` - Structured patterns
5. **Component Catalog**: `/prompts/component-catalog.json` - UI component specs
6. **Working Example**: `/addons/auto-tag-assistant/` - Complete implementation
7. **SDK Source**: `/addons/addon-sdk/src/main/java/com/clockify/addon/sdk/`
8. **OpenAPI Spec**: `/openapi (1).json` - Full Clockify API (1.1 MB, 34,100 lines)

---

## Critical Rules - NEVER VIOLATE

### 🚫 Manifest Rules

1. **NEVER** include `$schema` field in runtime manifest.json
   - ❌ WRONG: `{"$schema": "...", "schemaVersion": "1.3"}`
   - ✅ CORRECT: `{"schemaVersion": "1.3"}`

2. **NEVER** use `"version"` - use `"schemaVersion"` instead
   - ❌ WRONG: `{"version": "1.3"}`
   - ✅ CORRECT: `{"schemaVersion": "1.3"}`

3. **NEVER** invent manifest fields not in the official schema
   - Valid fields: `schemaVersion`, `key`, `name`, `description`, `baseUrl`, `minimalSubscriptionPlan`, `scopes`, `components`, `webhooks`, `lifecycle`, `iconPath`, `settings`

4. **ALWAYS** ensure `baseUrl` matches actual server endpoints
   - Manifest: `"baseUrl": "http://localhost:8080/my-addon"`
   - Server must respond at: `http://localhost:8080/my-addon/manifest.json`

### 🚫 Authentication Rules

5. **ALWAYS** use `X-Addon-Token` header for API calls (NEVER `Authorization`)
   - ❌ WRONG: `Authorization: Bearer <token>`
   - ✅ CORRECT: `X-Addon-Token: <token>`

6. **ALWAYS** store the installation token from INSTALLED lifecycle event
   - This token is required for ALL Clockify API calls
   - Store keyed by `workspaceId`

### 🚫 Security Rules

7. **NEVER** hardcode tokens, secrets, or credentials in code
8. **NEVER** log sensitive data (tokens, user emails, workspace IDs)
9. **ALWAYS** validate webhook signatures (HMAC-SHA256)
10. **ALWAYS** verify JWT tokens for UI component requests
11. **NEVER** skip input validation

### 🚫 API Rules

12. **NEVER** exceed rate limits (50 requests/second per addon per workspace)
13. **NEVER** skip error handling and retry logic
14. **NEVER** invent webhook event types - only use documented ones
15. **ALWAYS** implement exponential backoff on 429 errors

### 🚫 Code Quality Rules

16. **ALWAYS** use the in-repo SDK module (`addons/addon-sdk`)
17. **NEVER** assume GitHub Packages access - use Maven Central only
18. **NEVER** modify files under `dev-docs-marketplace-cake-snapshot/`
19. **ALWAYS** create tests for all handlers
20. **ALWAYS** include health check endpoint

---

## Manifest Structure

### Complete Manifest Template

```json
{
  "schemaVersion": "1.3",
  "key": "unique-addon-key",
  "name": "Display Name",
  "description": "Brief description of what this addon does",
  "baseUrl": "https://your-server.com/addon-path",
  "minimalSubscriptionPlan": "FREE",
  "scopes": ["TIME_ENTRY_READ", "TAG_WRITE"],
  "components": [
    {
      "type": "TIME_ENTRY_SIDEBAR",
      "path": "/settings",
      "label": "Addon Name",
      "accessLevel": "ADMINS"
    }
  ],
  "webhooks": [
    {
      "event": "TIMER_STOPPED",
      "path": "/webhook"
    }
  ],
  "lifecycle": [
    {
      "type": "INSTALLED",
      "path": "/lifecycle/installed"
    },
    {
      "type": "DELETED",
      "path": "/lifecycle/deleted"
    }
  ]
}
```

### Valid Subscription Plans

- `FREE` - All users
- `BASIC` - Basic subscription or higher
- `STANDARD` - Standard subscription or higher
- `PRO` - Pro subscription or higher
- `ENTERPRISE` - Enterprise only

---

## Available Scopes

| Scope | Access | Description |
|-------|--------|-------------|
| `WORKSPACE_READ` | Read | Workspace details |
| `PROJECT_READ` | Read | List projects |
| `PROJECT_WRITE` | Write | Create/update/delete projects |
| `TAG_READ` | Read | List tags |
| `TAG_WRITE` | Write | Create/update/delete tags |
| `CLIENT_READ` | Read | List clients |
| `CLIENT_WRITE` | Write | Create/update/delete clients |
| `TIME_ENTRY_READ` | Read | List time entries |
| `TIME_ENTRY_WRITE` | Write | Create/update/delete time entries |
| `TASK_READ` | Read | List tasks |
| `TASK_WRITE` | Write | Create/update/delete tasks |
| `USER_READ` | Read | List workspace users |
| `CUSTOM_FIELD_READ` | Read | List custom fields |
| `CUSTOM_FIELD_WRITE` | Write | Create/update custom fields |

**Scope Selection Rules:**
- Request MINIMUM scopes needed
- Read scopes don't grant write access
- Missing scopes result in 403 Forbidden errors

---

## Component Types

### SETTINGS_SIDEBAR
Admin-only settings panel

```json
{
  "type": "SETTINGS_SIDEBAR",
  "path": "/settings",
  "label": "Settings",
  "accessLevel": "ADMINS"
}
```

**URL Parameters**: `?workspaceId={workspaceId}&userId={userId}&jwt={jwt}`

### TIME_ENTRY_SIDEBAR
Context panel for time entries

```json
{
  "type": "TIME_ENTRY_SIDEBAR",
  "path": "/time-entry-sidebar",
  "label": "Entry Details",
  "accessLevel": "ALL"
}
```

**URL Parameters**: `?timeEntryId={timeEntryId}&workspaceId={workspaceId}&userId={userId}&jwt={jwt}`

### PROJECT_SIDEBAR
Project information panel

```json
{
  "type": "PROJECT_SIDEBAR",
  "path": "/project-sidebar",
  "label": "Project Info",
  "accessLevel": "ALL"
}
```

**URL Parameters**: `?projectId={projectId}&workspaceId={workspaceId}&userId={userId}&jwt={jwt}`

### REPORT_TAB
Custom report view

```json
{
  "type": "REPORT_TAB",
  "path": "/report",
  "label": "Custom Report",
  "accessLevel": "ALL"
}
```

### WIDGET
Dashboard widget

```json
{
  "type": "WIDGET",
  "path": "/widget",
  "label": "Dashboard Widget",
  "accessLevel": "ALL"
}
```

**Access Levels**: `ALL` (all users) or `ADMINS` (workspace admins only)

---

## Webhook Events

### Available Events

| Event | Trigger | Payload Includes |
|-------|---------|------------------|
| `NEW_TIME_ENTRY` | New entry created | Complete time entry object |
| `NEW_TIMER_STARTED` | Timer started | Time entry with null end time |
| `TIMER_STOPPED` | Timer stopped | Time entry with duration |
| `TIME_ENTRY_UPDATED` | Entry modified | Updated entry + changes object |
| `TIME_ENTRY_DELETED` | Entry deleted | Deleted entry ID and basic info |

### Webhook Request Format

```http
POST /your-addon/webhook HTTP/1.1
Host: your-server.com
Content-Type: application/json
x-clockify-signature: sha256=abc123...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017c7cf249396a237cfcce",
  "event": "TIMER_STOPPED",
  "timestamp": "2025-10-29T02:31:00Z",
  "timeEntry": { ... }
}
```

### Signature Validation (REQUIRED)

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public boolean validateSignature(String payload, String signature, String secret) {
    try {
        String expectedSig = signature.substring(7); // Remove "sha256="
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(key);
        byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));
        String computed = bytesToHex(hash);
        return computed.equals(expectedSig);
    } catch (Exception e) {
        return false;
    }
}
```

---

## Lifecycle Events

### INSTALLED Event

**When**: User installs addon in workspace
**Action Required**: Store installation token

```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String userId = (String) body.get("userId");
    String installationToken = (String) body.get("installationToken");

    // CRITICAL: Store this token for ALL API calls
    tokenStore.save(workspaceId, installationToken);

    // Optional: Initialize workspace-specific data
    initializeWorkspace(workspaceId);

    return HttpResponse.ok("{\"success\": true}");
}
```

**Payload**:
```json
{
  "event": "INSTALLED",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timestamp": "2025-10-29T10:30:00Z",
  "installationToken": "eyJhbGci...",
  "context": {
    "workspaceName": "WEBHOOKS",
    "userEmail": "user@example.com",
    "userName": "John Doe"
  }
}
```

### DELETED Event

**When**: User uninstalls addon
**Action Required**: Clean up all workspace data

```java
public HttpResponse handleDeleted(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");

    // Remove stored token
    tokenStore.remove(workspaceId);

    // Clean up workspace-specific data
    cleanupWorkspace(workspaceId);

    return HttpResponse.ok("{\"success\": true}");
}
```

---

## API Authentication

### API Base URLs

**Global (Default)**:
- `https://api.clockify.me/api/v1` - Main API
- `https://pto.api.clockify.me/v1` - PTO API
- `https://reports.api.clockify.me/v1` - Reports API

**Regional**:
- **EU (Germany)**: `https://euc1.api.clockify.me/api/v1`
- **USA**: `https://use2.api.clockify.me/api/v1`
- **UK**: `https://euw2.api.clockify.me/api/v1`
- **Australia**: `https://apse2.api.clockify.me/api/v1`

### Making API Calls

```java
public class ClockifyApiClient {
    private final String addonToken;
    private final String apiBaseUrl;

    public ClockifyApiClient(String addonToken, String apiBaseUrl) {
        this.addonToken = addonToken;
        this.apiBaseUrl = apiBaseUrl;
    }

    public JSONObject getTags(String workspaceId) throws IOException {
        HttpURLConnection conn = createConnection(
            "/workspaces/" + workspaceId + "/tags"
        );
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            return new JSONObject(response);
        } else if (responseCode == 429) {
            // Rate limit exceeded - implement backoff
            throw new RateLimitException("Rate limit exceeded");
        } else {
            throw new IOException("API call failed: " + responseCode);
        }
    }

    private HttpURLConnection createConnection(String endpoint) throws IOException {
        URL url = new URL(apiBaseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-Addon-Token", addonToken); // CRITICAL!
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        return conn;
    }
}
```

### Rate Limiting

- **Limit**: 50 requests/second per addon per workspace
- **HTTP Code**: 429 Too Many Requests
- **Strategy**: Exponential backoff with jitter

```java
public <T> T apiCallWithRetry(Callable<T> call, int maxRetries) throws Exception {
    for (int i = 0; i <= maxRetries; i++) {
        try {
            return call.call();
        } catch (RateLimitException e) {
            if (i == maxRetries) throw e;
            long backoff = (long) (1000 * Math.pow(2, i) + Math.random() * 1000);
            Thread.sleep(backoff);
        }
    }
    throw new RuntimeException("Max retries exceeded");
}
```

---

## Common Patterns

### Pattern 1: Token Storage (In-Memory)

```java
package com.example.addon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TokenStore {
    private static final Map<String, String> tokens = new ConcurrentHashMap<>();

    public static void save(String workspaceId, String token) {
        if (workspaceId == null || token == null) {
            throw new IllegalArgumentException("Workspace ID and token required");
        }
        tokens.put(workspaceId, token);
        System.out.println("Stored token for workspace: " + workspaceId);
    }

    public static String get(String workspaceId) {
        return tokens.get(workspaceId);
    }

    public static void remove(String workspaceId) {
        tokens.remove(workspaceId);
    }

    public static boolean has(String workspaceId) {
        return tokens.containsKey(workspaceId);
    }
}
```

### Pattern 2: Webhook Handler

```java
public class WebhookHandlers {
    public static void register(ClockifyAddon addon) {
        addon.registerWebhookHandler("TIMER_STOPPED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();
            String timeEntryId = payload.get("timeEntryId").getAsString();

            // Get stored token
            String token = TokenStore.get(workspaceId);
            if (token == null) {
                return HttpResponse.unauthorized("No token for workspace");
            }

            // Process webhook event
            processTimerStopped(workspaceId, timeEntryId, token);

            return HttpResponse.ok("Processed");
        });
    }

    private static void processTimerStopped(String workspaceId,
                                           String timeEntryId,
                                           String token) {
        // Your business logic here
    }
}
```

### Pattern 3: Settings UI

```java
public class SettingsController implements RequestHandler {
    @Override
    public HttpResponse handle(HttpServletRequest request) {
        // Extract JWT token from query params
        String jwt = request.getParameter("jwt");

        // Decode JWT to get user context (optional)
        // JwtClaims claims = JwtValidator.verify(jwt, publicKey);

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Addon Settings</title>
            <style>
                body { font-family: Arial, sans-serif; padding: 20px; }
                h1 { color: #333; }
            </style>
        </head>
        <body>
            <h1>Addon Settings</h1>
            <p>Configure your addon here</p>
        </body>
        </html>
        """;

        return HttpResponse.ok(html, "text/html");
    }
}
```

### Pattern 4: Error Handling

```java
public HttpResponse handleWebhook(HttpServletRequest request) {
    try {
        // Validate signature
        String signature = request.getHeader("x-clockify-signature");
        String body = readRequestBody(request);
        if (!SignatureValidator.validate(body, signature)) {
            return HttpResponse.unauthorized("Invalid signature");
        }

        // Parse payload
        JsonObject payload = JsonParser.parseString(body).getAsJsonObject();

        // Process event
        String result = processEvent(payload);

        return HttpResponse.ok(result);

    } catch (IllegalArgumentException e) {
        return HttpResponse.badRequest(e.getMessage());
    } catch (RateLimitException e) {
        return HttpResponse.tooManyRequests("Rate limit exceeded");
    } catch (Exception e) {
        System.err.println("Webhook error: " + e.getMessage());
        return HttpResponse.serverError("Internal error");
    }
}
```

---

## Project Structure

### Required Directory Layout

```
your-addon/
├── pom.xml                                    # Maven configuration
├── README.md                                  # Addon documentation
└── src/
    ├── main/
    │   └── java/com/example/youraddon/
    │       ├── YourAddonApp.java             # Main entry point
    │       ├── ManifestController.java       # Serves manifest.json
    │       ├── SettingsController.java       # Settings UI
    │       ├── LifecycleHandlers.java        # INSTALLED/DELETED
    │       ├── WebhookHandlers.java          # Webhook events
    │       ├── TokenStore.java               # Token storage
    │       └── ClockifyApiClient.java        # API wrapper
    └── test/
        └── java/com/example/youraddon/
            ├── ManifestValidationTest.java
            ├── LifecycleHandlersTest.java
            └── WebhookHandlersTest.java
```

### Main Application Template

```java
package com.example.youraddon;

import com.clockify.addon.sdk.*;

public class YourAddonApp {
    public static void main(String[] args) throws Exception {
        // Configuration
        String baseUrl = System.getenv().getOrDefault(
            "ADDON_BASE_URL",
            "http://localhost:8080/your-addon"
        );
        int port = Integer.parseInt(
            System.getenv().getOrDefault("ADDON_PORT", "8080")
        );

        // Build manifest
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
            .key("your-addon")
            .name("Your Addon")
            .description("What your addon does")
            .baseUrl(baseUrl)
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"TIME_ENTRY_READ"})
            .build();

        // Create addon
        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Register endpoints
        addon.registerCustomEndpoint("/manifest.json",
            new ManifestController(manifest));
        addon.registerCustomEndpoint("/settings",
            new SettingsController());
        addon.registerCustomEndpoint("/health",
            request -> HttpResponse.ok("OK"));

        // Register handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon);

        // Start server
        String contextPath = extractContextPath(baseUrl);
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        System.out.println("Starting addon at " + baseUrl);
        server.start(port);
    }

    private static String extractContextPath(String baseUrl) {
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getPath();
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (Exception e) {
            return "/";
        }
    }
}
```

---

## Build Configuration

### pom.xml Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.clockify</groupId>
        <artifactId>clockify-addon-boilerplate</artifactId>
        <version>1.0.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>your-addon</artifactId>
    <version>0.1.0</version>
    <name>Your Addon</name>

    <dependencies>
        <!-- In-repo SDK (no external auth required) -->
        <dependency>
            <groupId>com.clockify</groupId>
            <artifactId>addon-sdk</artifactId>
            <version>0.1.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Create fat JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.7.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.example.youraddon.YourAddonApp</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 | OK | Successful operation |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request format or parameters |
| 401 | Unauthorized | Invalid or missing authentication |
| 403 | Forbidden | Missing required scope |
| 404 | Not Found | Resource doesn't exist |
| 429 | Rate Limit | Too many requests |
| 500 | Server Error | Internal addon error |

### Error Response Format

```java
public class ErrorResponse {
    public static HttpResponse badRequest(String message) {
        return HttpResponse.create(
            400,
            "{\"error\": \"Bad Request\", \"message\": \"" + message + "\"}",
            "application/json"
        );
    }

    public static HttpResponse unauthorized(String message) {
        return HttpResponse.create(
            401,
            "{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}",
            "application/json"
        );
    }

    public static HttpResponse serverError(String message) {
        return HttpResponse.create(
            500,
            "{\"error\": \"Internal Server Error\", \"message\": \"" + message + "\"}",
            "application/json"
        );
    }
}
```

---

## Security Requirements

### 1. Webhook Signature Validation

```java
public class SignatureValidator {
    public static boolean validate(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            String expected = signature.substring(7);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes("UTF-8"),
                "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));
            String computed = bytesToHex(hash);
            return MessageDigest.isEqual(
                computed.getBytes(),
                expected.getBytes()
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
```

### 2. Input Validation

```java
public class InputValidator {
    public static String validateWorkspaceId(String workspaceId) {
        if (workspaceId == null || !workspaceId.matches("^[a-f0-9]{24}$")) {
            throw new IllegalArgumentException("Invalid workspace ID");
        }
        return workspaceId;
    }

    public static String validateTimeEntryId(String id) {
        if (id == null || !id.matches("^[a-f0-9]{24}$")) {
            throw new IllegalArgumentException("Invalid time entry ID");
        }
        return id;
    }

    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"']", "");
    }
}
```

### 3. JWT Token Verification (Optional)

```java
// For verifying JWT tokens in UI component requests
// Public key available at: extras/public-key.txt
public class JwtValidator {
    public static JwtClaims verify(String token, PublicKey publicKey)
            throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        // Verify signature
        // Decode payload
        // Validate expiration

        return parseClaims(parts[1]);
    }
}
```

---

## Testing Guidelines

### 1. Manifest Validation Test

```java
@Test
public void testManifestStructure() {
    ClockifyManifest manifest = createManifest();

    assertNotNull(manifest.getKey());
    assertNotNull(manifest.getName());
    assertNotNull(manifest.getDescription());
    assertNotNull(manifest.getBaseUrl());
    assertEquals("1.3", manifest.getSchemaVersion());
    assertTrue(manifest.getScopes().length > 0);
}
```

### 2. Lifecycle Handler Test

```java
@Test
public void testInstalledHandler() {
    Map<String, Object> payload = Map.of(
        "workspaceId", "68adfddad138cb5f24c63b22",
        "installationToken", "test-token"
    );

    HttpResponse response = lifecycleHandler.handleInstalled(payload);

    assertEquals(200, response.getStatus());
    assertTrue(TokenStore.has("68adfddad138cb5f24c63b22"));
}
```

### 3. Webhook Handler Test

```java
@Test
public void testWebhookProcessing() {
    JsonObject payload = new JsonObject();
    payload.addProperty("event", "TIMER_STOPPED");
    payload.addProperty("workspaceId", "68adfddad138cb5f24c63b22");

    HttpResponse response = webhookHandler.handle(payload);

    assertEquals(200, response.getStatus());
}
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] All tests passing (`mvn test`)
- [ ] Manifest validates (`python3 tools/validate-manifest.py`)
- [ ] No `$schema` in runtime manifest
- [ ] Health check endpoint responds
- [ ] Token storage implemented
- [ ] Error handling implemented
- [ ] Rate limiting implemented
- [ ] Webhook signature validation enabled
- [ ] Environment variables documented
- [ ] README.md updated

### Local Testing

- [ ] Build succeeds: `mvn clean package`
- [ ] JAR runs: `java -jar target/*.jar`
- [ ] ngrok exposes server: `ngrok http 8080`
- [ ] Manifest accessible: `https://your-ngrok/addon/manifest.json`
- [ ] Install in Clockify works
- [ ] INSTALLED event received and token stored
- [ ] Webhooks received and processed
- [ ] Settings UI renders in sidebar

### Production Deployment

- [ ] Database token storage configured
- [ ] HTTPS enabled
- [ ] Secrets stored in environment variables
- [ ] Logging configured
- [ ] Monitoring enabled
- [ ] Rate limiting configured
- [ ] Backup strategy in place
- [ ] Rollback plan documented

---

## Quick Start Example

Here's a minimal working addon that demonstrates all core concepts:

```java
package com.example.helloworld;

import com.clockify.addon.sdk.*;
import com.google.gson.JsonObject;
import java.util.concurrent.ConcurrentHashMap;

public class HelloWorldApp {
    private static final ConcurrentHashMap<String, String> tokens =
        new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv().getOrDefault(
            "ADDON_BASE_URL", "http://localhost:8080/hello-world"
        );

        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
            .key("hello-world")
            .name("Hello World")
            .description("A minimal example addon")
            .baseUrl(baseUrl)
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"WORKSPACE_READ"})
            .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Manifest endpoint
        addon.registerCustomEndpoint("/manifest.json",
            new DefaultManifestController(manifest));

        // Settings UI
        addon.registerCustomEndpoint("/settings", request -> {
            String html = """
            <!DOCTYPE html>
            <html>
            <body><h1>Hello, Clockify!</h1></body>
            </html>
            """;
            return HttpResponse.ok(html, "text/html");
        });

        // Lifecycle: INSTALLED
        addon.registerLifecycleHandler("INSTALLED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();
            String token = payload.get("installationToken").getAsString();
            tokens.put(workspaceId, token);
            return HttpResponse.ok("{\"success\": true}");
        });

        // Lifecycle: DELETED
        addon.registerLifecycleHandler("DELETED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();
            tokens.remove(workspaceId);
            return HttpResponse.ok("{\"success\": true}");
        });

        // Health check
        addon.registerCustomEndpoint("/health",
            request -> HttpResponse.ok("OK"));

        // Start server
        manifest.getComponents().add(
            new ClockifyManifest.ComponentEndpoint(
                "SETTINGS_SIDEBAR", "/settings", "Hello World", "ADMINS"
            )
        );

        String contextPath = baseUrl.replaceFirst("^https?://[^/]+", "");
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        System.out.println("Starting Hello World addon at " + baseUrl);
        server.start(8080);
    }
}
```

---

## Summary: Generation Workflow

When generating a Clockify addon:

1. **Read the spec** carefully to understand requirements
2. **Select scopes** - minimum needed for functionality
3. **Choose component types** - UI surfaces needed
4. **Identify webhooks** - events to subscribe to
5. **Design data model** - what to store and how
6. **Implement handlers** - lifecycle, webhooks, UI
7. **Add API client** - for Clockify API calls
8. **Add token storage** - workspace token management
9. **Add error handling** - proper HTTP responses
10. **Add tests** - validate all handlers
11. **Create manifest** - programmatically (no `$schema`!)
12. **Update pom.xml** - dependencies and build config
13. **Write README** - usage instructions
14. **Validate** - run tests, validate manifest

**Remember**: This entire repository is self-contained. All dependencies come from Maven Central. The SDK module is in-repo at `addons/addon-sdk/`. Never assume external authentication is required.

---

**End of System Prompt**

For additional examples, see `/examples/`. For detailed API documentation, see `/docs/`. For working code, see `/addons/auto-tag-assistant/`.
