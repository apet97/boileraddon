# Request/Response Examples

**Complete HTTP exchange examples for Clockify addon development**

This document provides full request and response examples for all Clockify addon interactions, including lifecycle events, webhooks, settings UI, and API calls.

## Table of Contents

- [Lifecycle Callbacks](#lifecycle-callbacks)
- [Webhook Events](#webhook-events)
- [Settings UI Requests](#settings-ui-requests)
- [Clockify API Calls](#clockify-api-calls)

---

## Lifecycle Callbacks

### INSTALLED Event

When a user installs your addon, Clockify sends a POST request to your lifecycle endpoint.

**Request from Clockify**:
```http
POST /your-addon/lifecycle/installed HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=abc123...

{
  "event": "INSTALLED",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timestamp": "2025-10-29T10:30:00Z",
  "installationToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NGMxNjQ3Y...",
  "context": {
    "workspaceName": "WEBHOOKS",
    "userEmail": "user@example.com",
    "userName": "John Doe"
  }
}
```

**Your Addon Response** (should be 200 OK):
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "message": "Addon installed successfully"
}
```

**What to do with this event**:
```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String userId = (String) body.get("userId");
    String installationToken = (String) body.get("installationToken");

    // CRITICAL: Store the installationToken - you'll need it for API calls
    tokenStore.saveToken(workspaceId, installationToken);

    // Optional: Initialize workspace-specific data
    initializeWorkspaceData(workspaceId);

    return HttpResponse.ok("{\"success\": true}");
}
```

---

### DELETED Event

When a user uninstalls your addon.

**Request from Clockify**:
```http
POST /your-addon/lifecycle/deleted HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=def456...

{
  "event": "DELETED",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timestamp": "2025-10-29T11:30:00Z",
  "context": {
    "workspaceName": "WEBHOOKS",
    "userEmail": "user@example.com"
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "message": "Addon uninstalled successfully"
}
```

**What to do with this event**:
```java
public HttpResponse handleDeleted(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");

    // Clean up stored data
    tokenStore.removeToken(workspaceId);
    cleanupWorkspaceData(workspaceId);

    return HttpResponse.ok("{\"success\": true}");
}
```

---

## Webhook Events

All webhook requests include a signature header for verification.

### NEW_TIME_ENTRY

Triggered when a new time entry is created.

**Request from Clockify**:
```http
POST /your-addon/webhooks/new-time-entry HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=ghi789...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017c7cf249396a237cfcce",
  "event": "NEW_TIME_ENTRY",
  "timestamp": "2025-10-29T02:31:00Z",
  "timeEntry": {
    "id": "69017c7cf249396a237cfcce",
    "description": "Working on feature implementation",
    "projectId": "68ffbce07bde82688ecb38fd",
    "projectName": "Mobile App",
    "taskId": null,
    "taskName": null,
    "billable": true,
    "tagIds": [],
    "timeInterval": {
      "start": "2025-10-29T02:31:00Z",
      "end": "2025-10-29T04:31:00Z",
      "duration": "PT2H"
    },
    "userId": "64621faec4d2cc53b91fce6c",
    "userName": "John Doe",
    "userEmail": "user@example.com"
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "processed": true
}
```

---

### NEW_TIMER_STARTED

Triggered when a user starts a new timer.

**Request from Clockify**:
```http
POST /your-addon/webhooks/timer-started HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=jkl012...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017d8af249396a237d0123",
  "event": "NEW_TIMER_STARTED",
  "timestamp": "2025-10-29T08:00:00Z",
  "timeEntry": {
    "id": "69017d8af249396a237d0123",
    "description": "Morning standup",
    "projectId": "68d1e16ef43fa22cf82c1724",
    "projectName": "API Discovery Project",
    "taskId": null,
    "billable": false,
    "tagIds": [],
    "timeInterval": {
      "start": "2025-10-29T08:00:00Z",
      "end": null,
      "duration": null
    },
    "userId": "64621faec4d2cc53b91fce6c",
    "userName": "John Doe",
    "userEmail": "user@example.com"
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "processed": true
}
```

---

### TIMER_STOPPED

Triggered when a running timer is stopped.

**Request from Clockify**:
```http
POST /your-addon/webhooks/timer-stopped HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=mno345...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017d8af249396a237d0123",
  "event": "TIMER_STOPPED",
  "timestamp": "2025-10-29T09:15:00Z",
  "timeEntry": {
    "id": "69017d8af249396a237d0123",
    "description": "Morning standup",
    "projectId": "68d1e16ef43fa22cf82c1724",
    "projectName": "API Discovery Project",
    "taskId": null,
    "billable": false,
    "tagIds": ["68d02fdf93acc646ebc1c6db"],
    "timeInterval": {
      "start": "2025-10-29T08:00:00Z",
      "end": "2025-10-29T09:15:00Z",
      "duration": "PT1H15M"
    },
    "userId": "64621faec4d2cc53b91fce6c",
    "userName": "John Doe",
    "userEmail": "user@example.com"
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "processed": true,
  "action": "tagged_automatically"
}
```

---

### TIME_ENTRY_UPDATED

Triggered when a time entry is modified.

**Request from Clockify**:
```http
POST /your-addon/webhooks/time-entry-updated HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=pqr678...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017c7cf249396a237cfcce",
  "event": "TIME_ENTRY_UPDATED",
  "timestamp": "2025-10-29T10:00:00Z",
  "timeEntry": {
    "id": "69017c7cf249396a237cfcce",
    "description": "Working on feature implementation - UPDATED",
    "projectId": "68ffbce07bde82688ecb38fd",
    "projectName": "Mobile App",
    "taskId": "68ffbd107bde82688ecb3a21",
    "taskName": "Authentication Module",
    "billable": true,
    "tagIds": ["68d02fdf93acc646ebc1c6db", "68e407ea42bbf21caa8388ff"],
    "timeInterval": {
      "start": "2025-10-29T02:31:00Z",
      "end": "2025-10-29T05:31:00Z",
      "duration": "PT3H"
    },
    "userId": "64621faec4d2cc53b91fce6c",
    "userName": "John Doe",
    "userEmail": "user@example.com"
  },
  "changes": {
    "description": {
      "oldValue": "Working on feature implementation",
      "newValue": "Working on feature implementation - UPDATED"
    },
    "duration": {
      "oldValue": "PT2H",
      "newValue": "PT3H"
    },
    "taskId": {
      "oldValue": null,
      "newValue": "68ffbd107bde82688ecb3a21"
    }
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "processed": true
}
```

---

### TIME_ENTRY_DELETED

Triggered when a time entry is deleted.

**Request from Clockify**:
```http
POST /your-addon/webhooks/time-entry-deleted HTTP/1.1
Host: your-server.com
Content-Type: application/json
clockify-webhook-signature: sha256=stu901...
x-clockify-workspace-id: 68adfddad138cb5f24c63b22

{
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "timeEntryId": "69017c7cf249396a237cfcce",
  "event": "TIME_ENTRY_DELETED",
  "timestamp": "2025-10-29T11:00:00Z",
  "timeEntry": {
    "id": "69017c7cf249396a237cfcce",
    "description": "Working on feature implementation - UPDATED",
    "projectId": "68ffbce07bde82688ecb38fd",
    "timeInterval": {
      "start": "2025-10-29T02:31:00Z",
      "end": "2025-10-29T05:31:00Z",
      "duration": "PT3H"
    }
  }
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "processed": true
}
```

---

### Webhook Signature Verification

**All webhook requests include a signature header that MUST be verified**:

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class WebhookSignatureValidator {
    private final String signingSecret;

    public WebhookSignatureValidator(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public boolean validateSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            String expectedSignature = signature.substring(7); // Remove "sha256=" prefix
            String computedSignature = computeSignature(payload);
            return computedSignature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String computeSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
```

---

## Settings UI Requests

### GET Settings Page

When a user opens your addon's settings page, Clockify makes a GET request with JWT token.

**Request from Clockify** (iframe loads your page):
```http
GET /your-addon/settings?jwt=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9... HTTP/1.1
Host: your-server.com
Accept: text/html
User-Agent: Mozilla/5.0...
```

**Decoded JWT Payload**:
```json
{
  "sub": "64621faec4d2cc53b91fce6c",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "userEmail": "user@example.com",
  "userName": "John Doe",
  "iat": 1730188800,
  "exp": 1730189400,
  "iss": "clockify.me"
}
```

**Your Addon Response** (HTML page):
```http
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
<head>
    <title>Auto Tag Assistant Settings</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { color: #333; margin-top: 0; }
        .setting-group { margin-bottom: 20px; }
        label { display: block; margin-bottom: 5px; font-weight: 500; }
        input[type="text"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        button {
            background: #03A9F4;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        button:hover { background: #0288D1; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Auto Tag Assistant Settings</h1>
        <form id="settingsForm">
            <div class="setting-group">
                <label for="keywords">Keywords (comma-separated):</label>
                <input type="text" id="keywords" name="keywords"
                       value="meeting,bug,feature,review" />
            </div>
            <div class="setting-group">
                <label for="autoTag">Auto-tag on timer stop:</label>
                <input type="checkbox" id="autoTag" name="autoTag" checked />
            </div>
            <button type="submit">Save Settings</button>
        </form>
    </div>

    <script>
        // Settings form submission
        document.getElementById('settingsForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(e.target);
            const settings = Object.fromEntries(formData);

            try {
                const response = await fetch('/your-addon/settings', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(settings)
                });

                if (response.ok) {
                    alert('Settings saved successfully!');
                } else {
                    alert('Failed to save settings');
                }
            } catch (error) {
                alert('Error: ' + error.message);
            }
        });
    </script>
</body>
</html>
```

---

### POST Settings (Save Configuration)

When the user saves settings in your UI.

**Request from Browser**:
```http
POST /your-addon/settings HTTP/1.1
Host: your-server.com
Content-Type: application/json

{
  "keywords": "meeting,bug,feature,review",
  "autoTag": true
}
```

**Your Addon Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "message": "Settings saved successfully"
}
```

---

## Clockify API Calls

### Get Workspace Details

**Request to Clockify**:
```http
GET /api/v1/workspaces/68adfddad138cb5f24c63b22 HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response from Clockify**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "68adfddad138cb5f24c63b22",
  "name": "WEBHOOKS",
  "hourlyRate": {
    "amount": 2000,
    "currency": "GPB"
  },
  "costRate": {
    "amount": 20000,
    "currency": "GPB"
  },
  "workspaceSettings": {
    "timeRoundingInReports": false,
    "onlyAdminsSeeBillableRates": false,
    "forceProjects": true,
    "forceTasks": false,
    "forceTags": false,
    "forceDescription": false,
    "trackTimeDownToSecond": true,
    "defaultBillableProjects": true
  },
  "features": [
    "TIME_TRACKING",
    "APPROVAL",
    "CUSTOM_FIELDS",
    "SCHEDULING"
  ],
  "featureSubscriptionType": "BUNDLE_YEAR_2024"
}
```

---

### Create a Tag

**Request to Clockify**:
```http
POST /api/v1/workspaces/68adfddad138cb5f24c63b22/tags HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "auto-generated"
}
```

**Response from Clockify** (201 Created):
```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/workspaces/68adfddad138cb5f24c63b22/tags/69017e9af249396a237d0456

{
  "id": "69017e9af249396a237d0456",
  "name": "auto-generated",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "archived": false
}
```

---

### Update Time Entry (Add Tags)

**Request to Clockify**:
```http
PUT /api/v1/workspaces/68adfddad138cb5f24c63b22/time-entries/69017c7cf249396a237cfcce HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "tagIds": [
    "68d02fdf93acc646ebc1c6db",
    "69017e9af249396a237d0456"
  ]
}
```

**Response from Clockify**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "69017c7cf249396a237cfcce",
  "description": "Working on feature implementation",
  "userId": "64621faec4d2cc53b91fce6c",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "projectId": "68ffbce07bde82688ecb38fd",
  "billable": true,
  "tagIds": [
    "68d02fdf93acc646ebc1c6db",
    "69017e9af249396a237d0456"
  ],
  "timeInterval": {
    "start": "2025-10-29T02:31:00Z",
    "end": "2025-10-29T04:31:00Z",
    "duration": "PT2H"
  },
  "hourlyRate": {
    "amount": 20000,
    "currency": "GPB"
  },
  "isLocked": false
}
```

---

### Error Responses

#### 401 Unauthorized (Invalid Token)

**Request to Clockify**:
```http
GET /api/v1/workspaces/68adfddad138cb5f24c63b22/projects HTTP/1.1
Host: api.clockify.me
X-Addon-Token: invalid-token-here
Content-Type: application/json
```

**Response from Clockify**:
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "message": "Full authentication is required to access this resource",
  "code": 401
}
```

---

#### 403 Forbidden (Missing Scope)

**Request to Clockify**:
```http
POST /api/v1/workspaces/68adfddad138cb5f24c63b22/tags HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "test-tag"
}
```

**Response from Clockify** (if TAG_WRITE scope not granted):
```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "message": "Access is denied. Required scope: TAG_WRITE",
  "code": 403
}
```

---

#### 404 Not Found

**Request to Clockify**:
```http
GET /api/v1/workspaces/68adfddad138cb5f24c63b22/projects/invalid-project-id HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response from Clockify**:
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "message": "Project not found",
  "code": 404
}
```

---

#### 429 Rate Limit Exceeded

**Request to Clockify** (51st request in 1 second):
```http
GET /api/v1/workspaces/68adfddad138cb5f24c63b22/tags HTTP/1.1
Host: api.clockify.me
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response from Clockify**:
```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 1

{
  "message": "Too many requests",
  "code": 429
}
```

---

## Complete Flow Example: Auto-Tagging

### 1. User Installs Addon

**Clockify → Your Server**:
```http
POST /auto-tag-assistant/lifecycle/installed HTTP/1.1
{
  "event": "INSTALLED",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "installationToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Your Server → Clockify**:
```http
HTTP/1.1 200 OK
{ "success": true }
```

### 2. User Creates Time Entry

**Clockify → Your Server**:
```http
POST /auto-tag-assistant/webhooks/new-time-entry HTTP/1.1
{
  "event": "NEW_TIME_ENTRY",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "timeEntryId": "69017c7cf249396a237cfcce",
  "timeEntry": {
    "description": "Fixed bug in authentication",
    "tagIds": []
  }
}
```

### 3. Your Addon Creates Tag

**Your Server → Clockify API**:
```http
POST /api/v1/workspaces/68adfddad138cb5f24c63b22/tags HTTP/1.1
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
{
  "name": "bugfix"
}
```

**Clockify API → Your Server**:
```http
HTTP/1.1 201 Created
{
  "id": "69017e9af249396a237d0456",
  "name": "bugfix"
}
```

### 4. Your Addon Updates Time Entry

**Your Server → Clockify API**:
```http
PUT /api/v1/workspaces/68adfddad138cb5f24c63b22/time-entries/69017c7cf249396a237cfcce HTTP/1.1
X-Addon-Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
{
  "tagIds": ["69017e9af249396a237d0456"]
}
```

**Clockify API → Your Server**:
```http
HTTP/1.1 200 OK
{
  "id": "69017c7cf249396a237cfcce",
  "tagIds": ["69017e9af249396a237d0456"]
}
```

### 5. Your Addon Responds to Webhook

**Your Server → Clockify**:
```http
HTTP/1.1 200 OK
{
  "processed": true,
  "tagsAdded": ["bugfix"]
}
```

---

## Additional Resources

- [API Cookbook](API-COOKBOOK.md) - Copy-paste code examples
- [Data Models Reference](DATA-MODELS.md) - Entity schemas
- [Common Patterns](PATTERNS.md) - Reusable code patterns
- [Quick Reference](QUICK-REFERENCE.md) - One-page cheat sheet
