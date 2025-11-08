# Common Mistakes When Building Clockify Addons

**Purpose**: Help developers (including AI) avoid frequent pitfalls and errors

This document catalogs the most common mistakes developers make when building Clockify addons, how to detect them, and how to fix them.

---

## Table of Contents

1. [Manifest Errors](#manifest-errors)
2. [Authentication Mistakes](#authentication-mistakes)
3. [API Integration Errors](#api-integration-errors)
4. [Webhook Issues](#webhook-issues)
5. [Lifecycle Handler Problems](#lifecycle-handler-problems)
6. [UI Component Errors](#ui-component-errors)
7. [Build and Deployment Issues](#build-and-deployment-issues)
8. [Security Vulnerabilities](#security-vulnerabilities)
9. [Performance Problems](#performance-problems)
10. [Testing Oversights](#testing-oversights)

---

## Manifest Errors

### Mistake 1: Including `$schema` in Runtime Manifest

**Problem**: Clockify's `/addons` endpoint **rejects** manifests containing `$schema` field

**❌ Wrong**:
```json
{
  "$schema": "../dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json",
  "schemaVersion": "1.3",
  "key": "my-addon"
}
```

**✅ Correct**:
```json
{
  "schemaVersion": "1.3",
  "key": "my-addon"
}
```

**Detection**:
```bash
grep -r "\$schema" addons/my-addon/manifest.json
# If this returns results, you have a problem
```

**Fix**: Remove `$schema` field from runtime manifest. Only use `$schema` in authoring-time files for IDE validation.

---

### Mistake 2: Using `version` Instead of `schemaVersion`

**Problem**: Incorrect field name causes manifest rejection

**❌ Wrong**:
```json
{
  "version": "1.3"
}
```

**✅ Correct**:
```json
{
  "schemaVersion": "1.3"
}
```

**Detection**:
```bash
python3 tools/validate-manifest.py addons/my-addon/manifest.json
```

**Fix**: Always use `schemaVersion` (not `version`, not `schema_version`)

---

### Mistake 3: BaseURL Mismatch

**Problem**: `baseUrl` in manifest doesn't match actual server endpoints

**❌ Wrong**:
```json
{
  "baseUrl": "http://localhost:8080/my-addon",
  "components": [{"path": "/settings"}]
}
```

But server is running at:
```
http://localhost:8080/different-addon/settings
```

**✅ Correct**: Ensure `baseUrl` matches server configuration:

```java
String baseUrl = "http://localhost:8080/my-addon";
String contextPath = "/my-addon"; // Extracted from baseUrl

// Server serves at:
// http://localhost:8080/my-addon/settings ✓
// http://localhost:8080/my-addon/manifest.json ✓
```

**Detection**:
```bash
# Test manifest accessibility
curl http://localhost:8080/my-addon/manifest.json

# Should return valid JSON, not 404
```

**Fix**: Extract context path correctly from `baseUrl` in your main application:

```java
static String sanitizeContextPath(String baseUrl) {
    try {
        java.net.URI uri = new java.net.URI(baseUrl);
        String path = uri.getPath();
        if (path != null && !path.isEmpty()) {
            return path.replaceAll("/+$", "");
        }
    } catch (java.net.URISyntaxException e) {
        System.err.println("Invalid base URL: " + baseUrl);
    }
    return "/";
}
```

---

### Mistake 4: Inventing Manifest Fields

**Problem**: Adding custom fields not in the official schema

**❌ Wrong**:
```json
{
  "schemaVersion": "1.3",
  "customField": "my-value",  // ← Not in schema!
  "author": "John Doe"         // ← Not in schema!
}
```

**✅ Correct**: Only use official fields:
- `schemaVersion`, `key`, `name`, `description`, `baseUrl`
- `minimalSubscriptionPlan`, `scopes`
- `components`, `webhooks`, `lifecycle`
- `iconPath`, `settings`

**Detection**: Validation script will catch unknown fields

**Fix**: Remove any custom fields. Store addon metadata elsewhere if needed.

---

## Authentication Mistakes

### Mistake 5: Using Wrong Auth Header

**Problem**: Using `Authorization` header instead of `X-Addon-Token`

**❌ Wrong**:
```java
conn.setRequestProperty("Authorization", "Bearer " + token);
```

**✅ Correct**:
```java
conn.setRequestProperty("X-Addon-Token", token);
```

**Detection**:
```bash
grep -r "Authorization.*Bearer" addons/my-addon/src/
# If this returns results, you're using wrong header
```

**Symptoms**:
- API calls return 401 Unauthorized
- Clockify logs show "Missing authentication"

**Fix**: Always use `X-Addon-Token` header exclusively for addon API calls.

---

### Mistake 6: Not Storing Installation Token

**Problem**: Forgetting to save token from INSTALLED lifecycle event

**❌ Wrong**:
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    // Missing: Store installationToken!
    return HttpResponse.ok("Installed");
}
```

**✅ Correct**:
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String token = (String) body.get("installationToken");

    // CRITICAL: Store the token!
    tokenStore.save(workspaceId, token);

    return HttpResponse.ok("Installed");
}
```

**Detection**: Check if INSTALLED handler has token storage logic

**Symptoms**:
- Subsequent API calls fail with 401
- No token available when processing webhooks

**Fix**: Always persist the installation token keyed by `workspaceId`

---

### Mistake 7: Token Storage Without Workspace Key

**Problem**: Storing token globally instead of per-workspace

**❌ Wrong**:
```java
private static String globalToken; // One token for all workspaces!

public void saveToken(String token) {
    globalToken = token; // ← Wrong! Overwrites previous workspace's token
}
```

**✅ Correct**:
```java
private static Map<String, String> tokens = new ConcurrentHashMap<>();

public void saveToken(String workspaceId, String token) {
    tokens.put(workspaceId, token); // ✓ One token per workspace
}
```

**Detection**: Check if token storage uses workspace ID as key

**Symptoms**:
- Addon works for one workspace but not others
- API calls use wrong workspace's token

**Fix**: Always key tokens by `workspaceId`

---

## API Integration Errors

### Mistake 8: Missing Error Handling

**Problem**: Not handling API errors (network failures, rate limits, etc.)

**❌ Wrong**:
```java
public JSONObject getTags(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags");
    String response = readResponse(conn.getInputStream());
    return new JSONObject(response); // No error handling!
}
```

**✅ Correct**:
```java
public JSONObject getTags(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags");
    int responseCode = conn.getResponseCode();

    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else if (responseCode == 429) {
        throw new RateLimitException("Rate limit exceeded");
    } else if (responseCode == 401) {
        throw new AuthenticationException("Invalid token");
    } else {
        throw new IOException("API call failed: " + responseCode);
    }
}
```

**Detection**: Code review - check if HTTP status codes are validated

**Symptoms**:
- Addon crashes on API failures
- Poor user experience

**Fix**: Always check HTTP status codes and handle errors appropriately

---

### Mistake 9: Ignoring Rate Limits

**Problem**: Making too many API requests without backoff strategy

**❌ Wrong**:
```java
// Make 100 API calls in a loop
for (int i = 0; i < 100; i++) {
    getTags(workspaceId); // ← Will hit rate limit!
}
```

**✅ Correct**:
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

**Detection**: Monitor API call rates and 429 responses

**Symptoms**:
- HTTP 429 Too Many Requests errors
- API calls fail intermittently

**Fix**: Implement exponential backoff with jitter. Rate limit: 50 req/s per addon per workspace.

---

### Mistake 10: Missing Request Timeouts

**Problem**: No timeout configured for HTTP connections

**❌ Wrong**:
```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
// No timeouts set - can hang forever!
```

**✅ Correct**:
```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setConnectTimeout(5000);  // 5 seconds to connect
conn.setReadTimeout(10000);    // 10 seconds to read response
```

**Detection**: Check if connection timeout is set

**Symptoms**:
- Addon hangs when Clockify API is slow
- Poor user experience

**Fix**: Always set connect and read timeouts

---

## Webhook Issues

### Mistake 11: Not Validating Webhook Signatures

**Problem**: Processing webhooks without signature verification

**❌ Wrong**:
```java
public HttpResponse handleWebhook(HttpServletRequest request) {
    String body = readRequestBody(request);
    JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
    // Process without validating signature!
    return HttpResponse.ok("OK");
}
```

**✅ Correct**:
```java
public HttpResponse handleWebhook(HttpServletRequest request) {
    String signature = request.getHeader("clockify-webhook-signature");
    String body = readRequestBody(request);

    // Validate signature
    if (!SignatureValidator.validate(body, signature, signingSecret)) {
        return HttpResponse.unauthorized("Invalid signature");
    }

    // Now safe to process
    JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
    return HttpResponse.ok("OK");
}
```

**Detection**: Check if webhook handler validates `clockify-webhook-signature` header

**Symptoms**:
- Security vulnerability
- Potential for malicious webhook injection

**Fix**: Always validate HMAC-SHA256 signature before processing

---

### Mistake 12: Webhook Handler Timeouts

**Problem**: Webhook handler takes too long to respond

**❌ Wrong**:
```java
public HttpResponse handleWebhook(HttpServletRequest request) {
    // Do expensive processing synchronously
    processAllTimeEntries(); // Takes 30 seconds!
    return HttpResponse.ok("OK");
}
```

**✅ Correct**:
```java
public HttpResponse handleWebhook(HttpServletRequest request) {
    JsonObject payload = parsePayload(request);

    // Queue for async processing
    eventQueue.add(payload);

    // Respond quickly (< 3 seconds)
    return HttpResponse.ok("Queued");
}
```

**Detection**: Monitor webhook response times

**Symptoms**:
- Clockify stops sending webhooks
- Webhook delivery failures

**Fix**: Respond to webhooks within 3 seconds. Use async processing for heavy work.

---

## Lifecycle Handler Problems

### Mistake 13: Throwing Exceptions in INSTALLED Handler

**Problem**: Uncaught exceptions cause installation failure

**❌ Wrong**:
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String token = (String) body.get("installationToken");
    tokenStore.save(token); // Might throw exception!
    return HttpResponse.ok("Installed");
}
```

**✅ Correct**:
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    try {
        String workspaceId = (String) body.get("workspaceId");
        String token = (String) body.get("installationToken");
        tokenStore.save(workspaceId, token);
        return HttpResponse.ok("Installed");
    } catch (Exception e) {
        System.err.println("Installation failed: " + e.getMessage());
        return HttpResponse.serverError("Installation failed");
    }
}
```

**Detection**: Test installation with error conditions

**Symptoms**:
- Addon installation fails in Clockify
- No error message shown to user

**Fix**: Wrap in try-catch and return appropriate HTTP response

---

### Mistake 14: Not Cleaning Up in DELETED Handler

**Problem**: Leaving data behind after uninstallation

**❌ Wrong**:
```java
public HttpResponse handleDeleted(Map<String, Object> body) {
    // Just return OK without cleanup!
    return HttpResponse.ok("Deleted");
}
```

**✅ Correct**:
```java
public HttpResponse handleDeleted(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");

    // Clean up all workspace data
    tokenStore.remove(workspaceId);
    configStore.remove(workspaceId);
    cacheStore.clear(workspaceId);

    return HttpResponse.ok("Deleted");
}
```

**Detection**: Check if DELETED handler cleans up workspace data

**Symptoms**:
- Memory leaks
- Stale data for uninstalled workspaces

**Fix**: Always clean up tokens, config, and cached data for workspace

---

## UI Component Errors

### Mistake 15: Not Extracting Query Parameters

**Problem**: Not reading context from URL parameters

**❌ Wrong**:
```html
<script>
  // Hardcoded IDs!
  const timeEntryId = "69017c7cf249396a237cfcce";
</script>
```

**✅ Correct**:
```html
<script>
  const urlParams = new URLSearchParams(window.location.search);
  const timeEntryId = urlParams.get('timeEntryId');
  const workspaceId = urlParams.get('workspaceId');
  const userId = urlParams.get('userId');
</script>
```

**Detection**: Check if UI components read query parameters

**Symptoms**:
- UI shows wrong data
- Components don't work for different users/workspaces

**Fix**: Always extract context from URL parameters

---

### Mistake 16: Missing CORS Headers

**Problem**: API endpoints don't set CORS headers for iframe requests

**Symptoms**:
- Browser console shows CORS errors
- UI components fail to load data

**✅ Fix**:
```java
public HttpResponse handleApiRequest(HttpServletRequest request) {
    // Process request
    String jsonResponse = getDataAsJson();

    // Set CORS headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    headers.put("Access-Control-Allow-Headers", "Content-Type");

    return HttpResponse.create(200, jsonResponse, "application/json", headers);
}
```

---

## Build and Deployment Issues

### Mistake 17: Wrong Main Class in pom.xml

**Problem**: JAR manifest points to wrong main class

**❌ Wrong**:
```xml
<manifest>
  <mainClass>com.example.WrongClass</mainClass>
</manifest>
```

**Symptoms**:
- `java -jar addon.jar` fails with "no main manifest attribute"
- ClassNotFoundException

**✅ Fix**:
```xml
<manifest>
  <mainClass>com.example.youraddon.YourAddonApp</mainClass>
</manifest>
```

**Detection**:
```bash
jar tf target/my-addon-0.1.0-jar-with-dependencies.jar | grep -i manifest
unzip -p target/my-addon-0.1.0-jar-with-dependencies.jar META-INF/MANIFEST.MF
```

---

### Mistake 18: Not Using Fat JAR

**Problem**: Dependencies not included in JAR

**❌ Wrong**:
```bash
# Just packaging classes, not dependencies
mvn package
# Produces: my-addon-0.1.0.jar (100 KB) ← Missing dependencies!
```

**✅ Correct**:
```xml
<!-- Use maven-assembly-plugin -->
<plugin>
  <artifactId>maven-assembly-plugin</artifactId>
  <configuration>
    <descriptorRefs>
      <descriptorRef>jar-with-dependencies</descriptorRef>
    </descriptorRefs>
  </configuration>
</plugin>
```

```bash
mvn clean package
# Produces: my-addon-0.1.0-jar-with-dependencies.jar (4-5 MB) ✓
```

**Detection**: Check JAR size (should be 4-5 MB, not <500 KB)

---

## Security Vulnerabilities

### Mistake 19: Logging Sensitive Data

**Problem**: Logging tokens, API keys, or user emails

**❌ Wrong**:
```java
System.out.println("Installing for workspace: " + workspaceId +
                   " with token: " + installationToken); // ← Exposes token!
```

**✅ Correct**:
```java
System.out.println("Installing for workspace: " + workspaceId);
// Never log the token!
```

**Detection**: Search for token logging:
```bash
grep -r "token.*System.out" addons/my-addon/src/
```

**Fix**: Never log sensitive data (tokens, emails, passwords, API keys)

---

### Mistake 20: Hardcoding Secrets

**Problem**: API keys or secrets in source code

**❌ Wrong**:
```java
private static final String API_KEY = "sk_live_abc123"; // ← Hardcoded!
```

**✅ Correct**:
```java
private static final String API_KEY = System.getenv("EXTERNAL_API_KEY");

if (API_KEY == null) {
    throw new IllegalStateException("EXTERNAL_API_KEY not set");
}
```

**Detection**:
```bash
grep -r "sk_live\|api_key.*=.*\"" addons/my-addon/src/
```

**Fix**: Use environment variables for all secrets

---

## Performance Problems

### Mistake 21: Not Caching API Responses

**Problem**: Fetching same data repeatedly

**❌ Wrong**:
```java
// Called 100 times, makes 100 API calls
public List<Tag> getTags(String workspaceId) {
    return clockifyApi.getTags(workspaceId);
}
```

**✅ Correct**:
```java
private final Map<String, CachedData<List<Tag>>> tagCache = new ConcurrentHashMap<>();

public List<Tag> getTags(String workspaceId) {
    CachedData<List<Tag>> cached = tagCache.get(workspaceId);
    if (cached != null && !cached.isExpired()) {
        return cached.getData();
    }

    List<Tag> tags = clockifyApi.getTags(workspaceId);
    tagCache.put(workspaceId, new CachedData<>(tags, 5 * 60 * 1000)); // 5 min TTL
    return tags;
}
```

**Fix**: Cache frequently-accessed data with appropriate TTL

---

### Mistake 22: Blocking Webhook Handlers

**Problem**: Doing synchronous heavy work in webhook handlers

**Symptoms**:
- Slow webhook processing
- Webhook timeouts
- Poor addon performance

**Fix**: Use async processing (see Mistake #12)

---

## Testing Oversights

### Mistake 23: No Manifest Validation Tests

**Problem**: Manifest changes break without tests catching it

**✅ Fix**:
```java
@Test
public void testManifestStructure() {
    ClockifyManifest manifest = createManifest();

    assertNotNull(manifest.getKey());
    assertNotNull(manifest.getName());
    assertNotNull(manifest.getDescription());
    assertEquals("1.3", manifest.getSchemaVersion());
    assertTrue(manifest.getScopes().length > 0);

    // Ensure no $schema field in JSON
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(manifest);
    assertFalse(json.contains("\"$schema\""));
}
```

---

### Mistake 24: Missing Integration Tests

**Problem**: No end-to-end tests for critical flows

**✅ Fix**: Test at least:
- INSTALLED handler stores token
- DELETED handler cleans up
- Webhook handler processes events
- API client makes authenticated calls
- Manifest endpoint returns valid JSON

---

## Quick Checklist

Before deploying, verify:

- [ ] ❌ No `$schema` in runtime manifest
- [ ] ✅ Using `schemaVersion` (not `version`)
- [ ] ✅ BaseURL matches server endpoints
- [ ] ✅ Using `X-Addon-Token` header (not `Authorization`)
- [ ] ✅ Installation token is stored
- [ ] ✅ Tokens keyed by workspaceId
- [ ] ✅ Error handling for all API calls
- [ ] ✅ Rate limiting with backoff
- [ ] ✅ Webhook signature validation
- [ ] ✅ Webhook handlers respond quickly (<3s)
- [ ] ✅ DELETED handler cleans up data
- [ ] ✅ UI components extract query params
- [ ] ✅ No sensitive data in logs
- [ ] ✅ Secrets from environment variables
- [ ] ✅ Fat JAR includes all dependencies
- [ ] ✅ Tests pass (`mvn test`)
- [ ] ✅ Manifest validates (`python3 tools/validate-manifest.py`)
- [ ] ✅ Health endpoint responds (`curl /health`)

---

## Debugging Tips

### Enable Debug Logging

```java
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
```

### Test Webhooks Locally

```bash
curl -X POST http://localhost:8080/my-addon/webhook \
  -H "Content-Type: application/json" \
  -H "clockify-webhook-signature: sha256=test" \
  -d '{"event": "TIMER_STOPPED", "workspaceId": "test", "timeEntryId": "test"}'
```

### Validate Manifest

```bash
python3 tools/validate-manifest.py addons/my-addon/manifest.json
```

### Check Token Storage

Add debug endpoint:
```java
addon.registerCustomEndpoint("/debug/tokens", request -> {
    return HttpResponse.ok("Tokens stored: " + tokenStore.count());
});
```

---

**Summary**: Most addon bugs fall into these categories. Use this guide to avoid them and debug quickly when they occur!
