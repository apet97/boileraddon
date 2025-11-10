# Clockify Webhook Payloads Reference

**Version**: 2.0.0
**Last Updated**: November 10, 2025

Complete reference for all Clockify webhook event types, payload structures, and integration patterns.

---

## Table of Contents

1. [Overview](#overview)
2. [Time Entry Events](#time-entry-events)
3. [Timer Events](#timer-events)
4. [Project Events](#project-events)
5. [Task Events](#task-events)
6. [Client Events](#client-events)
7. [User Events](#user-events)
8. [Integration Patterns](#integration-patterns)
9. [Error Handling](#error-handling)
10. [Testing](#testing)

---

## Overview

### Webhook Authentication

All webhooks from Clockify include an HMAC-SHA256 signature in the `clockify-webhook-signature` header:

```
clockify-webhook-signature: sha256=abc123def456...
```

**Signature Verification**:
```java
// SDK handles this automatically via WebhookSignatureValidator
boolean isValid = WebhookSignatureValidator.verify(request, workspaceId).isValid();
if (!isValid) {
    return HttpResponse.error(401, "Invalid webhook signature");
}
```

### Event Type Header

Webhooks include the event type in both header and body:

```
clockify-webhook-event-type: TIME_ENTRY_CREATED
```

And in the JSON body:
```json
{
  "event": "TIME_ENTRY_CREATED",
  ...
}
```

### Workspace Context

All webhooks include workspace context:

```json
{
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace"
}
```

---

## Time Entry Events

### TIME_ENTRY_CREATED

Fired when a user creates a time entry.

**Payload Structure**:
```json
{
  "event": "TIME_ENTRY_CREATED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "userId": "507f1f77bcf86cd799439011",
  "userName": "John Doe",
  "userEmail": "john@example.com",
  "timeEntry": {
    "id": "507f1f77bcf86cd799439013",
    "description": "Worked on API integration",
    "start": "2025-01-10T09:00:00Z",
    "end": "2025-01-10T12:30:00Z",
    "duration": 12600,
    "projectId": "507f1f77bcf86cd799439014",
    "projectName": "Project Alpha",
    "taskId": "507f1f77bcf86cd799439015",
    "taskName": "API Integration",
    "tags": [
      {
        "id": "507f1f77bcf86cd799439016",
        "name": "billable"
      },
      {
        "id": "507f1f77bcf86cd799439017",
        "name": "development"
      }
    ],
    "billable": true,
    "hourlyRate": 75.00,
    "costRate": null,
    "estimate": null,
    "createdAt": "2025-01-10T09:00:00Z",
    "updatedAt": "2025-01-10T09:00:00Z"
  }
}
```

**Key Fields**:
- `timeEntry.description`: Optional user-entered description
- `timeEntry.duration`: Duration in seconds
- `timeEntry.tags`: Array of assigned tags (can be empty)
- `timeEntry.billable`: Whether entry is billable
- `projectId`: Can be null if no project assigned
- `taskId`: Can be null if no task assigned

**Common Use Cases**:
- Auto-tag based on description keywords
- Send to external time tracking systems
- Update project status or client reports
- Generate notifications for project managers

---

### TIME_ENTRY_UPDATED

Fired when a user modifies a time entry.

**Payload Structure** (same as TIME_ENTRY_CREATED):
```json
{
  "event": "TIME_ENTRY_UPDATED",
  "workspaceId": "...",
  "timeEntry": { ... }
}
```

**Differences from Created**:
- May have different `start`, `end`, `duration` values
- `tags`, `projectId`, `taskId` may have changed
- `updatedAt` reflects the modification time

**Common Use Cases**:
- Re-sync time entries with external systems
- Recalculate billing based on changes
- Update project budgets
- Audit trail for time modifications

---

### TIME_ENTRY_DELETED

Fired when a user deletes a time entry.

**Payload Structure**:
```json
{
  "event": "TIME_ENTRY_DELETED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "userId": "507f1f77bcf86cd799439011",
  "timeEntryId": "507f1f77bcf86cd799439013",
  "projectId": "507f1f77bcf86cd799439014",
  "taskId": "507f1f77bcf86cd799439015"
}
```

**Key Fields**:
- `timeEntryId`: The ID of deleted entry (for cleanup)
- `projectId`: Can be null
- `taskId`: Can be null

**Common Use Cases**:
- Remove corresponding records from external systems
- Adjust project totals and budgets
- Maintain audit trail of deletions
- Clean up related data

---

## Timer Events

### NEW_TIMER_STARTED

Fired when a user starts the timer (begins tracking time).

**Payload Structure**:
```json
{
  "event": "NEW_TIMER_STARTED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "userId": "507f1f77bcf86cd799439011",
  "userName": "John Doe",
  "userEmail": "john@example.com",
  "timeEntry": {
    "id": "507f1f77bcf86cd799439013",
    "description": "Working on dashboard",
    "start": "2025-01-10T14:30:00Z",
    "end": null,
    "projectId": "507f1f77bcf86cd799439014",
    "projectName": "Project Beta",
    "taskId": "507f1f77bcf86cd799439015",
    "taskName": "Dashboard Design",
    "tags": [],
    "billable": true,
    "createdAt": "2025-01-10T14:30:00Z"
  }
}
```

**Key Fields**:
- `timeEntry.end`: Always `null` (timer is running)
- `timeEntry.start`: When timer started

**Common Use Cases**:
- Notify team leads that someone is working
- Update status dashboards
- Trigger notifications
- Auto-start external timers/tools

---

### TIMER_STOPPED

Fired when a user stops the timer.

**Payload Structure**:
```json
{
  "event": "TIMER_STOPPED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "userId": "507f1f77bcf86cd799439011",
  "userName": "John Doe",
  "userEmail": "john@example.com",
  "timeEntry": {
    "id": "507f1f77bcf86cd799439013",
    "description": "Working on dashboard",
    "start": "2025-01-10T14:30:00Z",
    "end": "2025-01-10T16:45:00Z",
    "duration": 8100,
    "projectId": "507f1f77bcf86cd799439014",
    "projectName": "Project Beta",
    "taskId": "507f1f77bcf86cd799439015",
    "taskName": "Dashboard Design",
    "tags": [],
    "billable": true
  }
}
```

**Key Fields**:
- `timeEntry.end`: When timer was stopped
- `timeEntry.duration`: Total tracked time in seconds

**Common Use Cases**:
- Log time to external systems
- Update team dashboards
- Generate notifications
- Trigger billing calculations

---

## Project Events

### PROJECT_CREATED

Fired when a project is created.

**Payload Structure**:
```json
{
  "event": "PROJECT_CREATED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "userId": "507f1f77bcf86cd799439011",
  "project": {
    "id": "507f1f77bcf86cd799439014",
    "name": "Website Redesign",
    "description": "Complete website overhaul",
    "clientId": "507f1f77bcf86cd799439018",
    "clientName": "Acme Corp",
    "public": false,
    "archived": false,
    "budget": 10000.00,
    "budgetType": "FIXED",
    "billable": true,
    "budgetStatus": "UNDER",
    "color": "#FF6B35",
    "createdAt": "2025-01-10T10:00:00Z",
    "updatedAt": "2025-01-10T10:00:00Z"
  }
}
```

---

### PROJECT_UPDATED

Fired when a project is modified (name, budget, etc.).

**Common Use Cases**:
- Sync project info to external systems
- Update project dashboards
- Validate budget changes
- Trigger notifications

---

### PROJECT_DELETED

Fired when a project is deleted.

**Payload**: Contains only `projectId` and metadata (project data not available).

---

## Task Events

### TASK_CREATED

Fired when a task is created.

**Payload Structure**:
```json
{
  "event": "TASK_CREATED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "workspaceName": "My Workspace",
  "projectId": "507f1f77bcf86cd799439014",
  "projectName": "Website Redesign",
  "task": {
    "id": "507f1f77bcf86cd799439015",
    "name": "Design homepage",
    "description": "Create high-fidelity mockups",
    "status": "ACTIVE",
    "estimate": 16,
    "assigneeId": "507f1f77bcf86cd799439011",
    "assigneeName": "John Doe",
    "createdAt": "2025-01-10T10:00:00Z"
  }
}
```

---

## Client Events

### CLIENT_CREATED / CLIENT_UPDATED / CLIENT_DELETED

Similar structure to project events, fired when clients are managed.

---

## User Events

### USER_JOINED

Fired when a new user is invited/joins the workspace.

**Payload**:
```json
{
  "event": "USER_JOINED",
  "workspaceId": "65a4f0c5b8d1e2f3g4h5i6j7",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "john@example.com",
    "name": "John Doe",
    "role": "EMPLOYEE",
    "status": "ACTIVE",
    "joinedAt": "2025-01-10T10:00:00Z"
  }
}
```

---

## Integration Patterns

### Pattern 1: Auto-Tagging Based on Description

```java
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    if (!WebhookSignatureValidator.verify(request, workspaceId).isValid()) {
        return HttpResponse.error(401, "Invalid signature");
    }

    String event = request.getHeader("clockify-webhook-event-type");

    if ("TIME_ENTRY_CREATED".equals(event)) {
        JsonNode payload = parseJson(request);
        JsonNode timeEntry = payload.get("timeEntry");
        String description = timeEntry.get("description").asText();

        // Extract keywords and apply tags
        List<String> tags = extractKeywords(description);
        if (!tags.isEmpty()) {
            applyTags(timeEntry.get("id").asText(), tags);
        }
    }

    return HttpResponse.ok("Processed");
}

private List<String> extractKeywords(String description) {
    Map<String, String> keywordMap = Map.of(
        "meeting", "meeting",
        "bug", "bug-fix",
        "review", "code-review",
        "documentation", "docs"
    );

    List<String> tags = new ArrayList<>();
    for (String keyword : keywordMap.keySet()) {
        if (description.toLowerCase().contains(keyword)) {
            tags.add(keywordMap.get(keyword));
        }
    }
    return tags;
}
```

### Pattern 2: Sync to External System

```java
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    String event = request.getHeader("clockify-webhook-event-type");
    JsonNode payload = parseJson(request);

    try {
        switch (event) {
            case "TIME_ENTRY_CREATED":
                externalAPI.createTimeEntry(payload.get("timeEntry"));
                break;
            case "TIME_ENTRY_UPDATED":
                externalAPI.updateTimeEntry(payload.get("timeEntry"));
                break;
            case "TIME_ENTRY_DELETED":
                externalAPI.deleteTimeEntry(payload.get("timeEntryId").asText());
                break;
            default:
                logger.debug("Unhandled event: {}", event);
        }
        return HttpResponse.ok("Synced");
    } catch (Exception e) {
        logger.error("Sync failed: {}", e.getMessage());
        return HttpResponse.error(500, "Sync failed");
    }
}
```

### Pattern 3: Dashboard Update

```java
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    String event = request.getHeader("clockify-webhook-event-type");
    JsonNode payload = parseJson(request);

    if (event.contains("TIMER")) {
        String userId = payload.get("userId").asText();
        String projectId = payload.get("timeEntry").get("projectId").asText();

        // Update dashboard with current activity
        dashboard.updateUserActivity(userId, projectId, event);
        dashboard.broadcastUpdate();  // Push to WebSocket clients
    }

    return HttpResponse.ok("Updated");
}
```

---

## Error Handling

### Invalid Signature

```java
WebhookSignatureValidator.VerificationResult result =
    WebhookSignatureValidator.verify(request, workspaceId);

if (!result.isValid()) {
    logger.warn("Invalid webhook signature from {}", getClientIp(request));
    return HttpResponse.error(401, "Invalid signature");
}
```

### Missing Event Type

```java
String event = request.getHeader("clockify-webhook-event-type");
if (event == null || event.isBlank()) {
    JsonNode json = parseJson(request);
    event = json.get("event").asText(null);
}

if (event == null) {
    return HttpResponse.error(400, "Missing event type");
}
```

### JSON Parsing Errors

```java
try {
    JsonNode json = objectMapper.readTree(request.getInputStream());
    // Process webhook
} catch (IOException e) {
    logger.warn("Malformed JSON in webhook: {}", e.getMessage());
    return HttpResponse.error(400, "Invalid JSON");
}
```

### Handler Not Found

```java
if (!handlers.containsKey(event)) {
    logger.debug("No handler for event: {}", event);
    return HttpResponse.ok("Event acknowledged but not handled");
}
```

---

## Testing

### Mock Webhook Payload

```java
@Test
void testTimeEntryCreatedWebhook() {
    // Setup
    String json = """
        {
          "event": "TIME_ENTRY_CREATED",
          "workspaceId": "test-workspace",
          "timeEntry": {
            "id": "entry-123",
            "description": "Testing webhook",
            "start": "2025-01-10T09:00:00Z",
            "end": "2025-01-10T12:00:00Z",
            "duration": 10800,
            "projectId": "project-456",
            "tags": [],
            "billable": true
          }
        }
        """;

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType("application/json");
    request.setContent(json.getBytes());
    request.addHeader("clockify-webhook-event-type", "TIME_ENTRY_CREATED");
    request.addHeader("clockify-webhook-signature", generateSignature(json));

    // Execute
    HttpResponse response = handler.handle(request);

    // Assert
    assertEquals(200, response.getStatusCode());
}
```

### Generate Test Signature

```java
private String generateSignature(String payload) throws Exception {
    String secret = "your-webhook-secret";
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    mac.init(keySpec);
    byte[] hmacBytes = mac.doFinal(payload.getBytes());
    return "sha256=" + Base64.getEncoder().encodeToString(hmacBytes);
}
```

---

## Best Practices

1. **Always verify signatures** - Never trust webhook source
2. **Idempotent handlers** - Handle duplicate webhooks gracefully
3. **Log everything** - Include event type, workspace ID, timestamp
4. **Fail fast** - Return error immediately for malformed payloads
5. **Async processing** - Don't block webhook handling for long operations
6. **Retry logic** - External API calls may fail temporarily
7. **Monitor metrics** - Track webhook processing success/failure rates

---

**Last Updated**: November 10, 2025
