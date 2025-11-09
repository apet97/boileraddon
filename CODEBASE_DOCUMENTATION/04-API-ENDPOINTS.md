# API Endpoints Documentation

Complete reference for all API endpoints in the Clockify Add-on Boilerplate.

## Table of Contents
- [Core Endpoints](#core-endpoints)
- [Auto-Tag Assistant Endpoints](#auto-tag-assistant-endpoints)
- [Rules Addon Endpoints](#rules-addon-endpoints)
- [Endpoint Patterns](#endpoint-patterns)
- [Error Responses](#error-responses)

---

## Core Endpoints

All addons must implement these core endpoints:

### GET /manifest.json

**Purpose:** Return addon manifest (Clockify Add-on specification v1.3)

**Headers:** None required

**Response:**

```json
{
  "schemaVersion": "1.3",
  "name": "Rules Automation",
  "description": "IFTTT-style automation for Clockify",
  "key": "com.example.rules",
  "vendor": {
    "name": "Example Corp",
    "url": "https://example.com"
  },
  "scopes": [
    "TIME_ENTRY_READ",
    "TIME_ENTRY_WRITE",
    "TAG_READ",
    "TAG_WRITE"
  ],
  "components": [
    {
      "type": "sidebar",
      "path": "/settings",
      "displayName": "Settings",
      "accessLevel": "ADMINS"
    }
  ],
  "webhooks": [
    {
      "event": "TIME_ENTRY_CREATED",
      "path": "/webhook"
    }
  ],
  "lifecycle": {
    "installed": { "path": "/lifecycle/installed" },
    "deleted": { "path": "/lifecycle/deleted" }
  }
}
```

**File:** `ManifestController.java` in each addon

---

### GET /health

**Purpose:** Health check endpoint for monitoring

**Headers:** None required

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

**Status Codes:**
- `200 OK` - Service is healthy
- `503 Service Unavailable` - Service is unhealthy

**File:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/health/HealthCheck.java`

---

### GET /metrics

**Purpose:** Prometheus metrics endpoint for observability

**Headers:** None required

**Response Format:** Prometheus exposition format

```
# HELP webhook_requests_total Total webhook requests
# TYPE webhook_requests_total counter
webhook_requests_total{event="TIME_ENTRY_CREATED",path="/webhook"} 42.0

# HELP webhook_request_seconds Webhook request duration
# TYPE webhook_request_seconds summary
webhook_request_seconds{event="TIME_ENTRY_CREATED",quantile="0.5"} 0.025
webhook_request_seconds{event="TIME_ENTRY_CREATED",quantile="0.95"} 0.15
webhook_request_seconds{event="TIME_ENTRY_CREATED",quantile="0.99"} 0.3
webhook_request_seconds_count{event="TIME_ENTRY_CREATED"} 42.0
webhook_request_seconds_sum{event="TIME_ENTRY_CREATED"} 2.5
```

**File:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/metrics/MetricsHandler.java`

---

### POST /lifecycle/installed

**Purpose:** Handle addon installation lifecycle event

**Headers:**
- `Content-Type: application/json`
- `clockify-webhook-signature: sha256=<signature>` (optional)

**Request Body:**

```json
{
  "workspaceId": "62e123abc456def789012345",
  "installationToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "apiBaseUrl": "https://api.clockify.me/api",
  "environment": "PRODUCTION"
}
```

**Response:**

```json
{
  "status": "installed"
}
```

**Implementation Pattern:**

```java
addon.registerLifecycleHandler("INSTALLED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();
    String token = payload.get("installationToken").asText();
    String apiBaseUrl = payload.get("apiBaseUrl").asText("https://api.clockify.me/api");

    // Store token for future API calls
    TokenStore.save(workspaceId, token, apiBaseUrl);

    return HttpResponse.ok("{\"status\":\"installed\"}");
});
```

**File:** `LifecycleHandlers.java` in each addon

---

### POST /lifecycle/deleted

**Purpose:** Handle addon uninstallation lifecycle event

**Headers:**
- `Content-Type: application/json`

**Request Body:**

```json
{
  "workspaceId": "62e123abc456def789012345"
}
```

**Response:**

```json
{
  "status": "deleted"
}
```

**Implementation Pattern:**

```java
addon.registerLifecycleHandler("DELETED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();

    // Clean up stored data
    TokenStore.delete(workspaceId);

    return HttpResponse.ok("{\"status\":\"deleted\"}");
});
```

---

### POST /webhook

**Purpose:** Receive webhook events from Clockify

**Headers:**
- `Content-Type: application/json`
- `clockify-webhook-event-type: <event_type>`
- `clockify-webhook-signature: sha256=<signature>` (HMAC-SHA256)

**Request Body (TIME_ENTRY_CREATED example):**

```json
{
  "event": "TIME_ENTRY_CREATED",
  "workspaceId": "62e123abc456def789012345",
  "userId": "62e123abc456def789012346",
  "timeEntry": {
    "id": "62e123abc456def789012347",
    "description": "Working on feature X",
    "start": "2025-11-09T10:00:00Z",
    "end": "2025-11-09T11:30:00Z",
    "projectId": "62e123abc456def789012348",
    "tagIds": ["62e123abc456def789012349"],
    "billable": true
  }
}
```

**Response:**

```json
{
  "status": "ok"
}
```

**Implementation Pattern:**

```java
addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();
    JsonNode timeEntry = payload.get("timeEntry");

    // Process time entry
    processTimeEntry(workspaceId, timeEntry);

    return HttpResponse.ok("{\"status\":\"ok\"}");
});
```

**Supported Events:**
- `TIME_ENTRY_CREATED`
- `TIME_ENTRY_UPDATED`
- `TIME_ENTRY_DELETED`
- `NEW_TIMER_STARTED`
- `TIMER_STOPPED`

**File:** `WebhookHandlers.java` in each addon

---

## Auto-Tag Assistant Endpoints

**Base Path:** `/auto-tag-assistant`

### GET /auto-tag-assistant/manifest.json

Returns addon manifest (see Core Endpoints)

---

### GET /auto-tag-assistant/health

Returns health status (see Core Endpoints)

---

### GET /auto-tag-assistant/metrics

Returns Prometheus metrics (see Core Endpoints)

---

### POST /auto-tag-assistant/lifecycle/installed

Handles installation (see Core Endpoints)

---

### POST /auto-tag-assistant/lifecycle/deleted

Handles uninstallation (see Core Endpoints)

---

### POST /auto-tag-assistant/webhook

**Purpose:** Process time entry events and suggest tags

**Events Handled:**
- `NEW_TIMER_STARTED`
- `TIMER_STOPPED`
- `NEW_TIME_ENTRY`
- `TIME_ENTRY_UPDATED`

**Logic:**
1. Extract time entry description and project
2. Analyze text for keywords
3. Suggest relevant tags based on patterns
4. Store suggestions (if applicable)

**File:** `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/WebhookHandlers.java`

---

### GET /auto-tag-assistant/settings

**Purpose:** Settings sidebar UI component

**Query Parameters:**
- `workspaceId` - Current workspace ID
- `userId` - Current user ID

**Response:** HTML page with settings form

**File:** `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/SettingsController.java`

---

## Rules Addon Endpoints

**Base Path:** `/rules`

### Core Endpoints

- `GET /rules/manifest.json`
- `GET /rules/health`
- `GET /rules/metrics`
- `POST /rules/lifecycle/installed`
- `POST /rules/lifecycle/deleted`
- `POST /rules/webhook`

---

### GET /rules/status

**Purpose:** Get addon runtime status and configuration

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Response:**

```json
{
  "addon": "rules",
  "version": "0.1.0",
  "workspace": {
    "id": "62e123abc456def789012345",
    "hasToken": true,
    "tokenSource": "database"
  },
  "modes": {
    "applyChanges": false,
    "dryRun": true
  },
  "cache": {
    "loaded": true,
    "tags": 25,
    "projects": 10,
    "users": 5
  }
}
```

**File:** `addons/rules/src/main/java/com/example/rules/RulesController.java`

---

### Rules CRUD API

#### GET /rules/api/rules

**Purpose:** List all rules for a workspace

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Response:**

```json
[
  {
    "id": "rule-001",
    "name": "Auto-tag dev work",
    "enabled": true,
    "trigger": "TIME_ENTRY_CREATED",
    "logicOperator": "AND",
    "conditions": [
      {
        "field": "project.name",
        "operator": "contains",
        "value": "Development"
      }
    ],
    "actions": [
      {
        "type": "add_tag",
        "params": {
          "tagName": "Development"
        }
      }
    ]
  }
]
```

**File:** `addons/rules/src/main/java/com/example/rules/RulesController.java:123`

---

#### POST /rules/api/rules

**Purpose:** Create or update a rule

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Request Body:**

```json
{
  "id": "rule-001",
  "name": "Auto-tag dev work",
  "enabled": true,
  "trigger": "TIME_ENTRY_CREATED",
  "logicOperator": "AND",
  "conditions": [
    {
      "field": "project.name",
      "operator": "contains",
      "value": "Development"
    }
  ],
  "actions": [
    {
      "type": "add_tag",
      "params": {
        "tagName": "Development"
      }
    }
  ]
}
```

**Response:**

```json
{
  "status": "saved",
  "ruleId": "rule-001"
}
```

**File:** `addons/rules/src/main/java/com/example/rules/RulesController.java:156`

---

#### DELETE /rules/api/rules

**Purpose:** Delete a rule

**Query Parameters:**
- `workspaceId` (required) - Workspace ID
- `ruleId` (required) - Rule ID

**Response:**

```json
{
  "status": "deleted",
  "ruleId": "rule-001"
}
```

**File:** `addons/rules/src/main/java/com/example/rules/RulesController.java:189`

---

#### POST /rules/api/test

**Purpose:** Test rules against time entry (dry-run)

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Request Body:**

```json
{
  "timeEntry": {
    "id": "test-001",
    "description": "Working on feature",
    "projectId": "proj-001",
    "tagIds": []
  }
}
```

**Response:**

```json
{
  "matched": true,
  "rules": [
    {
      "ruleId": "rule-001",
      "ruleName": "Auto-tag dev work",
      "matched": true,
      "actions": [
        {
          "type": "add_tag",
          "params": {
            "tagName": "Development"
          },
          "preview": "Would add tag: Development"
        }
      ]
    }
  ]
}
```

**File:** `addons/rules/src/main/java/com/example/rules/RulesController.java:212`

---

### Catalog Endpoints

#### GET /rules/api/catalog/triggers

**Purpose:** Get available webhook triggers for rule builder

**Response:**

```json
[
  {
    "id": "TIME_ENTRY_CREATED",
    "displayName": "When time entry is created",
    "description": "Triggered when a new time entry is created"
  },
  {
    "id": "TIME_ENTRY_UPDATED",
    "displayName": "When time entry is updated",
    "description": "Triggered when a time entry is modified"
  }
]
```

**File:** `addons/rules/src/main/java/com/example/rules/spec/TriggersCatalog.java`

---

#### GET /rules/api/catalog/actions

**Purpose:** Get available actions from OpenAPI spec

**Response:**

```json
[
  {
    "id": "updateTimeEntry",
    "displayName": "Update time entry",
    "endpoint": "PUT /workspaces/{workspaceId}/time-entries/{timeEntryId}",
    "parameters": [
      {
        "name": "description",
        "type": "string",
        "required": false
      },
      {
        "name": "tagIds",
        "type": "array",
        "required": false
      }
    ]
  }
]
```

**File:** `addons/rules/src/main/java/com/example/rules/spec/OpenAPISpecLoader.java`

---

### Cache Endpoints

#### GET /rules/api/cache

**Purpose:** Get workspace cache summary

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Response:**

```json
{
  "loaded": true,
  "tags": 25,
  "projects": 10,
  "clients": 5,
  "users": 8,
  "tasks": 50
}
```

**File:** `addons/rules/src/main/java/com/example/rules/cache/WorkspaceCache.java`

---

#### POST /rules/api/cache/refresh

**Purpose:** Force refresh workspace cache

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Response:**

```json
{
  "status": "refreshed",
  "tags": 25,
  "projects": 10
}
```

---

#### GET /rules/api/cache/data

**Purpose:** Get full cache data (for autocomplete, etc.)

**Query Parameters:**
- `workspaceId` (required) - Workspace ID

**Response:**

```json
{
  "tags": {
    "tag-001": "Development",
    "tag-002": "Meeting"
  },
  "projects": {
    "proj-001": "Project Alpha",
    "proj-002": "Project Beta"
  },
  "clients": {
    "client-001": "Acme Corp"
  },
  "users": {
    "user-001": "John Doe"
  }
}
```

---

### UI Component Endpoints

#### GET /rules/settings

**Purpose:** Settings sidebar UI

**Query Parameters:**
- `workspaceId` - Workspace ID
- `userId` - User ID

**Response:** HTML page with settings interface

**File:** `addons/rules/src/main/java/com/example/rules/SettingsController.java`

---

#### GET /rules/ifttt

**Purpose:** IFTTT-style rule builder UI

**Query Parameters:**
- `workspaceId` - Workspace ID

**Response:** HTML page with visual rule builder

**File:** `addons/rules/src/main/java/com/example/rules/IftttController.java`

---

## Endpoint Patterns

### Naming Conventions

- **Lifecycle:** `/lifecycle/{event_type}` (e.g., `/lifecycle/installed`)
- **Webhooks:** `/webhook` (single endpoint for all events)
- **APIs:** `/api/{resource}` (e.g., `/api/rules`)
- **UI:** `/{component_name}` (e.g., `/settings`, `/ifttt`)
- **Health:** `/health`
- **Metrics:** `/metrics`

### Query Parameters

Always use `workspaceId` for workspace-scoped operations:

```
GET /api/rules?workspaceId=62e123abc456def789012345
POST /api/rules?workspaceId=62e123abc456def789012345
DELETE /api/rules?workspaceId=62e123abc456def789012345&ruleId=rule-001
```

### Headers

**Request Headers:**
- `Content-Type: application/json` - For JSON payloads
- `clockify-webhook-event-type: <event>` - Webhook event type
- `clockify-webhook-signature: sha256=<sig>` - HMAC signature

**Response Headers:**
- `Content-Type: application/json` - JSON responses
- `Content-Type: text/html` - HTML UI components
- `X-Content-Type-Options: nosniff` - Security header
- `Referrer-Policy: no-referrer` - Security header

---

## Error Responses

### Standard Error Format

```json
{
  "error": "Error message",
  "details": "Detailed error description",
  "code": 400
}
```

### Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful request |
| 201 | Created | Resource created |
| 204 | No Content | Successful delete |
| 400 | Bad Request | Invalid input |
| 401 | Unauthorized | Invalid/missing token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service down |

### Example Error Responses

**400 Bad Request:**
```json
{
  "error": "Missing required parameter",
  "details": "workspaceId is required",
  "code": 400
}
```

**401 Unauthorized:**
```json
{
  "error": "Unauthorized",
  "details": "Installation token not found for workspace",
  "code": 401
}
```

**429 Rate Limit:**
```json
{
  "error": "Rate limit exceeded",
  "details": "Maximum 10 requests per second allowed",
  "code": 429
}
```

**500 Internal Server Error:**
```json
{
  "error": "Internal server error",
  "details": "Database connection failed",
  "code": 500
}
```

---

**Next:** [Database Schema Documentation](./05-DATABASE-SCHEMA.md)
