# Clockify Addon Quick Reference

**One-page cheat sheet for Clockify addon development**

---

## Manifest Structure

```json
{
  "schemaVersion": "1.3",
  "key": "unique-addon-key",
  "name": "Display Name",
  "description": "Brief description",
  "baseUrl": "https://your-server.com/addon-path",
  "minimalSubscriptionPlan": "FREE|BASIC|STANDARD|PRO|ENTERPRISE",
  "scopes": ["SCOPE_1", "SCOPE_2"],
  "components": [...],
  "webhooks": [...],
  "lifecycle": [
    {"event": "INSTALLED", "url": "/lifecycle/installed"},
    {"event": "DELETED", "url": "/lifecycle/deleted"}
  ]
}
```

**Critical Rules**:
- ❌ NEVER include `$schema` in runtime manifest (Clockify rejects it)
- ✅ Use `schemaVersion` not `version`
- ✅ `baseUrl` must match your actual server
- ✅ Send token via `x-addon-token` header ONLY

---

## Available Scopes

| Scope | Read/Write | Description |
|-------|------------|-------------|
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

---

## Component Types

### Settings Sidebar
```json
{
  "type": "SETTINGS_SIDEBAR",
  "name": "Settings",
  "url": "/settings"
}
```

### Time Entry Sidebar
```json
{
  "type": "TIME_ENTRY_SIDEBAR",
  "name": "Details",
  "url": "/time-entry-sidebar?timeEntryId={timeEntryId}"
}
```

### Project Sidebar
```json
{
  "type": "PROJECT_SIDEBAR",
  "name": "Project Info",
  "url": "/project-sidebar?projectId={projectId}"
}
```

### Report Tab
```json
{
  "type": "REPORT_TAB",
  "name": "Custom Report",
  "url": "/report"
}
```

### Widget
```json
{
  "type": "WIDGET",
  "name": "Dashboard Widget",
  "url": "/widget"
}
```

**URL Parameters Available**:
- `{workspaceId}` - Current workspace ID
- `{userId}` - Current user ID
- `{timeEntryId}` - Time entry ID (TIME_ENTRY_SIDEBAR only)
- `{projectId}` - Project ID (PROJECT_SIDEBAR only)

All component URLs receive a `jwt` query parameter with user context.

---

## Webhook Events

| Event | Trigger | Payload Includes |
|-------|---------|------------------|
| `NEW_TIME_ENTRY` | New entry created | Full time entry object |
| `NEW_TIMER_STARTED` | Timer started | Time entry with null end time |
| `TIMER_STOPPED` | Timer stopped | Time entry with duration |
| `TIME_ENTRY_UPDATED` | Entry modified | Updated entry + changes object |
| `TIME_ENTRY_DELETED` | Entry deleted | Deleted entry ID and basic info |

**Webhook Manifest Example**:
```json
{
  "event": "NEW_TIME_ENTRY",
  "url": "/webhooks/new-time-entry"
}
```

**All webhooks include**:
- `x-clockify-signature` header (MUST validate!)
- `x-clockify-workspace-id` header
- JSON body with event data

---

## Lifecycle Events

| Event | Trigger | Action Required |
|-------|---------|-----------------|
| `INSTALLED` | Addon installed | **Store installationToken** |
| `DELETED` | Addon uninstalled | Clean up workspace data |

**INSTALLED Payload**:
```json
{
  "event": "INSTALLED",
  "workspaceId": "...",
  "userId": "...",
  "installationToken": "eyJhbGci..."  // ← STORE THIS!
}
```

---

## API Base URLs

### Global (Default)
```
https://api.clockify.me/api/v1
https://pto.api.clockify.me/v1
https://reports.api.clockify.me/v1
```

### Regional (by prefix)
- **EU (Germany)**: `euc1.clockify.me`
- **USA**: `use2.clockify.me`
- **UK**: `euw2.clockify.me`
- **Australia**: `apse2.clockify.me`

Example: `https://euc1.clockify.me/api/v1`

---

## Common API Endpoints

| Operation | Method | Endpoint |
|-----------|--------|----------|
| Get workspace | GET | `/v1/workspaces/{workspaceId}` |
| List projects | GET | `/v1/workspaces/{workspaceId}/projects` |
| Create project | POST | `/v1/workspaces/{workspaceId}/projects` |
| List tags | GET | `/v1/workspaces/{workspaceId}/tags` |
| Create tag | POST | `/v1/workspaces/{workspaceId}/tags` |
| Update tag | PUT | `/v1/workspaces/{workspaceId}/tags/{tagId}` |
| Delete tag | DELETE | `/v1/workspaces/{workspaceId}/tags/{tagId}` |
| List clients | GET | `/v1/workspaces/{workspaceId}/clients` |
| Get time entries | GET | `/v1/workspaces/{workspaceId}/user/{userId}/time-entries` |
| Create time entry | POST | `/v1/workspaces/{workspaceId}/time-entries` |
| Update time entry | PUT | `/v1/workspaces/{workspaceId}/time-entries/{id}` |
| Delete time entry | DELETE | `/v1/workspaces/{workspaceId}/time-entries/{id}` |
| Get current user | GET | `/v1/user` |

**Authentication Header**: `X-Addon-Token: {installationToken}`

---

## HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | OK | Success |
| 201 | Created | Resource created |
| 204 | No Content | Success, no body |
| 400 | Bad Request | Fix request format |
| 401 | Unauthorized | Check token |
| 403 | Forbidden | Missing scope |
| 404 | Not Found | Resource doesn't exist |
| 429 | Rate Limit | Retry with backoff |
| 500 | Server Error | Retry later |

---

## Rate Limits

- **50 requests/second** per addon per workspace
- Use exponential backoff on 429 errors
- Implement request queuing for high-volume operations

---

## JWT Token Structure

**Received in**: Settings/sidebar URL as `?jwt=...`

**Decoded Payload**:
```json
{
  "sub": "userId",
  "workspaceId": "workspaceId",
  "userId": "userId",
  "userEmail": "user@example.com",
  "userName": "User Name",
  "iat": 1730188800,
  "exp": 1730189400,
  "iss": "clockify.me"
}
```

**Verify using**: Public key from [extras/public-key.txt](../extras/public-key.txt)

---

## Webhook Signature Verification

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public boolean validateSignature(String payload, String signature) {
    String expectedSig = signature.substring(7); // Remove "sha256="
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec key = new SecretKeySpec(signingSecret.getBytes("UTF-8"), "HmacSHA256");
    mac.init(key);
    byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));
    String computed = bytesToHex(hash);
    return computed.equals(expectedSig);
}
```

**Signing Secret**: Provided when you register webhook in Clockify

---

## Minimal Addon Structure

```
your-addon/
├── pom.xml
├── manifest.json
└── src/main/java/com/example/youraddon/
    ├── YourAddonApp.java           # Main entry
    ├── LifecycleHandlers.java      # INSTALLED/DELETED
    ├── WebhookHandlers.java        # Event handlers
    ├── SettingsController.java     # Settings UI
    ├── TokenStore.java             # Store tokens
    └── ClockifyApiClient.java      # API wrapper
```

---

## Build & Run Commands

```bash
# Create new addon from template
./scripts/new-addon.sh --port 8080 my-addon "My Addon"

# Build addon
mvn -pl addons/my-addon clean package -DskipTests

# Run locally
java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar

# Or use Make
make build-my-addon
make run-my-addon

# Validate manifest
python3 tools/validate-manifest.py addons/my-addon/manifest.json

# Build Docker image
docker build -t my-addon --build-arg ADDON_DIR=addons/my-addon .

# Run in Docker
docker run -p 8080:8080 \
  -e ADDON_BASE_URL=https://your-ngrok.ngrok-free.app/my-addon \
  my-addon
```

---

## Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `ADDON_PORT` | No | Server port | `8080` |
| `ADDON_BASE_URL` | Yes | Public URL | `https://example.com/addon` |
| `CLOCKIFY_WORKSPACE_ID` | No | For dev/testing | `68adfdda...` |
| `CLOCKIFY_INSTALLATION_TOKEN` | No | For dev/testing | `eyJhbGci...` |
| `CLOCKIFY_API_BASE_URL` | No | API endpoint | `https://api.clockify.me/api/v1` |

---

## Testing Locally with ngrok

```bash
# 1. Build and run addon
make run-my-addon

# 2. Start ngrok (in another terminal)
ngrok http 8080

# 3. Update manifest.json baseUrl
"baseUrl": "https://abc123.ngrok-free.app/my-addon"

# 4. Install addon in Clockify
# Use manifest URL: https://abc123.ngrok-free.app/my-addon/manifest.json
```

---

## Common Patterns

### Store Installation Token
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String token = (String) body.get("installationToken");
    tokenStore.saveToken(workspaceId, token); // ← CRITICAL!
    return HttpResponse.ok("{\"success\": true}");
}
```

### Make Authenticated API Call
```java
HttpURLConnection conn = new URL(apiBaseUrl + endpoint).openConnection();
conn.setRequestProperty("X-Addon-Token", installationToken);
conn.setRequestProperty("Content-Type", "application/json");
conn.setRequestMethod("GET");
```

### Validate Webhook Signature
```java
String signature = request.getHeader("x-clockify-signature");
String rawBody = readRequestBody(request);
if (!signatureValidator.validate(rawBody, signature)) {
    return HttpResponse.unauthorized();
}
```

### Handle Rate Limiting
```java
public JSONObject apiCallWithRetry(String endpoint, int maxRetries) {
    for (int i = 0; i <= maxRetries; i++) {
        try {
            return makeApiCall(endpoint);
        } catch (RateLimitException e) {
            if (i == maxRetries) throw e;
            Thread.sleep(1000 * (long) Math.pow(2, i)); // Exponential backoff
        }
    }
}
```

---

## Key Data Models

### Time Entry
```json
{
  "id": "string",
  "description": "string",
  "userId": "string",
  "workspaceId": "string",
  "projectId": "string | null",
  "taskId": "string | null",
  "tagIds": ["string"],
  "billable": boolean,
  "timeInterval": {
    "start": "2025-10-29T02:31:00Z",
    "end": "2025-10-29T04:31:00Z",
    "duration": "PT2H"
  },
  "customFieldValues": [],
  "type": "REGULAR|TIME_OFF|BREAK",
  "isLocked": boolean
}
```

### Project
```json
{
  "id": "string",
  "name": "string",
  "workspaceId": "string",
  "clientId": "string | null",
  "color": "#RRGGBB",
  "billable": boolean,
  "archived": boolean,
  "public": boolean,
  "estimate": {
    "estimate": "PT3H",
    "type": "MANUAL|AUTO"
  }
}
```

### Tag
```json
{
  "id": "string",
  "name": "string",
  "workspaceId": "string",
  "archived": boolean
}
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Manifest rejected | Remove `$schema` field, validate with `validate-manifest.py` |
| 401 Unauthorized | Check token is stored from INSTALLED event |
| 403 Forbidden | Add required scope to manifest |
| Webhook not received | Verify signature validation, check ngrok URL |
| Settings page blank | Check JWT token validation, verify CORS headers |
| Rate limit errors | Implement exponential backoff, reduce request frequency |

---

## Useful Commands

```bash
# Check if server is running
curl http://localhost:8080/my-addon/health

# Get manifest
curl http://localhost:8080/my-addon/manifest.json

# Test lifecycle (manual)
curl -X POST http://localhost:8080/my-addon/lifecycle/installed \
  -H "Content-Type: application/json" \
  -d '{"workspaceId":"123","installationToken":"test"}'

# Check logs
tail -f logs/addon.log

# Validate all manifests
make validate
```

---

## Security Checklist

- ✅ Validate webhook signatures (HMAC-SHA256)
- ✅ Verify JWT tokens for settings/sidebar
- ✅ Store tokens securely (never in code/logs)
- ✅ Use HTTPS in production
- ✅ Sanitize user inputs
- ✅ Implement rate limiting
- ✅ Don't log sensitive data
- ✅ Use environment variables for secrets

---

## Resources

- **API Cookbook**: [docs/API-COOKBOOK.md](API-COOKBOOK.md)
- **Request/Response Examples**: [docs/REQUEST-RESPONSE-EXAMPLES.md](REQUEST-RESPONSE-EXAMPLES.md)
- **Data Models**: [docs/DATA-MODELS.md](DATA-MODELS.md)
- **Common Patterns**: [docs/PATTERNS.md](PATTERNS.md)
- **Architecture**: [docs/ARCHITECTURE.md](ARCHITECTURE.md)
- **Building Your Own**: [docs/BUILDING-YOUR-OWN-ADDON.md](BUILDING-YOUR-OWN-ADDON.md)
- **OpenAPI Spec**: [dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json](../dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json)
- **Full Marketplace Docs**: [dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md](../dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md)

---

**Quick Start**: Run `./scripts/new-addon.sh my-addon "My Addon"` to create a new addon from the template!
