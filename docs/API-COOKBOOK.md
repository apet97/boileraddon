# Clockify API Cookbook

**Copy-paste code examples for common Clockify API operations**

This cookbook provides ready-to-use Java code snippets for the most common Clockify API operations. All examples use the addon SDK's HTTP utilities and real response data from the Clockify API.

## Table of Contents

- [Authentication](#authentication)
- [Workspace Operations](#workspace-operations)
- [Project Operations](#project-operations)
- [Tag Operations](#tag-operations)
- [Client Operations](#client-operations)
- [Time Entry Operations](#time-entry-operations)
- [User Operations](#user-operations)
- [Error Handling](#error-handling)
- [Scope Requirements](#scope-requirements)

---

## Authentication

All API requests require authentication using either:
- **X-Api-Key**: User's personal API key
- **X-Addon-Token**: Installation token (for addons, received during INSTALLED lifecycle event)

### Using Addon Token (Recommended for Addons)

```java
public class ClockifyApiClient {
    private final String addonToken;
    private final String apiBaseUrl;

    public ClockifyApiClient(String addonToken, String apiBaseUrl) {
        this.addonToken = addonToken;
        this.apiBaseUrl = apiBaseUrl; // e.g., https://api.clockify.me/api/v1
    }

    private HttpURLConnection createConnection(String endpoint) throws IOException {
        URL url = new URL(apiBaseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-Addon-Token", addonToken);
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }
}
```

---

## Workspace Operations

### Get Workspace Details

**Required Scope**: `WORKSPACE_READ`

```java
public JSONObject getWorkspace(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId);
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        throw new IOException("Failed to get workspace: " + responseCode);
    }
}
```

**Example Response**:
```json
{
  "id": "68adfddad138cb5f24c63b22",
  "name": "WEBHOOKS",
  "hourlyRate": {
    "amount": 2000,
    "currency": "GPB"
  },
  "workspaceSettings": {
    "timeRoundingInReports": false,
    "onlyAdminsSeeBillableRates": false,
    "forceProjects": true,
    "forceTasks": false,
    "forceTags": false,
    "forceDescription": false,
    "trackTimeDownToSecond": true
  },
  "features": ["TIME_TRACKING", "APPROVAL", "CUSTOM_FIELDS", "SCHEDULING"],
  "featureSubscriptionType": "BUNDLE_YEAR_2024"
}
```

---

## Project Operations

### List All Projects

**Required Scope**: `PROJECT_READ`

```java
public List<JSONObject> getAllProjects(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/projects");
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        JSONArray projects = new JSONArray(response);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < projects.length(); i++) {
            result.add(projects.getJSONObject(i));
        }
        return result;
    } else {
        throw new IOException("Failed to get projects: " + responseCode);
    }
}
```

**Example Response Item**:
```json
{
  "id": "68d1e16ef43fa22cf82c1724",
  "name": "API Discovery Project",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "billable": false,
  "color": "#2196F3",
  "archived": false,
  "public": true,
  "clientId": "68d1e16e8ac1033711a69680",
  "clientName": "API Discovery Client",
  "hourlyRate": {
    "amount": 22200,
    "currency": "GPB"
  },
  "estimate": {
    "estimate": "PT3H",
    "type": "MANUAL"
  },
  "memberships": [
    {
      "userId": "64621faec4d2cc53b91fce6c",
      "membershipType": "PROJECT",
      "membershipStatus": "ACTIVE"
    }
  ]
}
```

### Create a New Project

**Required Scope**: `PROJECT_WRITE`

```java
public JSONObject createProject(String workspaceId, String name, String clientId, boolean billable)
        throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("name", name);
    payload.put("clientId", clientId);
    payload.put("isPublic", true);
    payload.put("billable", billable);
    payload.put("color", "#03A9F4");

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/projects");
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 201) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to create project: " + responseCode + " - " + error);
    }
}
```

### Update a Project

**Required Scope**: `PROJECT_WRITE`

```java
public JSONObject updateProject(String workspaceId, String projectId, String newName)
        throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("name", newName);

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/projects/" + projectId);
    conn.setRequestMethod("PUT");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to update project: " + responseCode + " - " + error);
    }
}
```

### Delete/Archive a Project

**Required Scope**: `PROJECT_WRITE`

```java
public void archiveProject(String workspaceId, String projectId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/projects/" + projectId);
    conn.setRequestMethod("DELETE");

    int responseCode = conn.getResponseCode();
    if (responseCode != 200 && responseCode != 204) {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to archive project: " + responseCode + " - " + error);
    }
}
```

---

## Tag Operations

### List All Tags

**Required Scope**: `TAG_READ`

```java
public List<JSONObject> getAllTags(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags");
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        JSONArray tags = new JSONArray(response);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < tags.length(); i++) {
            result.add(tags.getJSONObject(i));
        }
        return result;
    } else {
        throw new IOException("Failed to get tags: " + responseCode);
    }
}
```

**Example Response Item**:
```json
{
  "id": "68d02fdf93acc646ebc1c6db",
  "name": "Sprint1",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "archived": false
}
```

### Create a New Tag

**Required Scope**: `TAG_WRITE`

```java
public JSONObject createTag(String workspaceId, String tagName) throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("name", tagName);

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags");
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 201) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to create tag: " + responseCode + " - " + error);
    }
}
```

### Update a Tag

**Required Scope**: `TAG_WRITE`

```java
public JSONObject updateTag(String workspaceId, String tagId, String newName) throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("name", newName);

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags/" + tagId);
    conn.setRequestMethod("PUT");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to update tag: " + responseCode + " - " + error);
    }
}
```

### Delete a Tag

**Required Scope**: `TAG_WRITE`

```java
public void deleteTag(String workspaceId, String tagId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/tags/" + tagId);
    conn.setRequestMethod("DELETE");

    int responseCode = conn.getResponseCode();
    if (responseCode != 200 && responseCode != 204) {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to delete tag: " + responseCode + " - " + error);
    }
}
```

---

## Client Operations

### List All Clients

**Required Scope**: `CLIENT_READ`

```java
public List<JSONObject> getAllClients(String workspaceId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/clients");
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        JSONArray clients = new JSONArray(response);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < clients.length(); i++) {
            result.add(clients.getJSONObject(i));
        }
        return result;
    } else {
        throw new IOException("Failed to get clients: " + responseCode);
    }
}
```

**Example Response Item**:
```json
{
  "id": "68d1e16e8ac1033711a69680",
  "name": "API Discovery Client",
  "email": null,
  "workspaceId": "68adfddad138cb5f24c63b22",
  "archived": false,
  "currencyCode": "GPB"
}
```

### Create a New Client

**Required Scope**: `CLIENT_WRITE`

```java
public JSONObject createClient(String workspaceId, String name, String email) throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("name", name);
    if (email != null && !email.isEmpty()) {
        payload.put("email", email);
    }

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/clients");
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 201) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to create client: " + responseCode + " - " + error);
    }
}
```

---

## Time Entry Operations

### Get User's Time Entries

**Required Scope**: `TIME_ENTRY_READ`

```java
public List<JSONObject> getUserTimeEntries(String workspaceId, String userId, int pageSize)
        throws IOException {
    String endpoint = "/workspaces/" + workspaceId + "/user/" + userId + "/time-entries";
    if (pageSize > 0) {
        endpoint += "?page-size=" + pageSize;
    }

    HttpURLConnection conn = createConnection(endpoint);
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        JSONArray entries = new JSONArray(response);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            result.add(entries.getJSONObject(i));
        }
        return result;
    } else {
        throw new IOException("Failed to get time entries: " + responseCode);
    }
}
```

**Example Response Item**:
```json
{
  "id": "69017c7cf249396a237cfcce",
  "description": "Working on feature",
  "userId": "64621faec4d2cc53b91fce6c",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "projectId": "68ffbce07bde82688ecb38fd",
  "taskId": null,
  "billable": true,
  "tagIds": null,
  "timeInterval": {
    "start": "2025-10-29T02:31:00Z",
    "end": "2025-10-29T04:31:00Z",
    "duration": "PT2H"
  },
  "customFieldValues": [],
  "type": "REGULAR",
  "hourlyRate": {
    "amount": 20000,
    "currency": "GPB"
  },
  "isLocked": false
}
```

### Create a Time Entry

**Required Scope**: `TIME_ENTRY_WRITE`

```java
public JSONObject createTimeEntry(String workspaceId, String userId, String start, String end,
        String description, String projectId) throws IOException {
    JSONObject payload = new JSONObject();
    payload.put("start", start); // ISO 8601: "2025-10-29T02:31:00Z"
    payload.put("end", end);
    payload.put("description", description);
    payload.put("projectId", projectId);
    payload.put("billable", true);

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/time-entries");
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 201) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to create time entry: " + responseCode + " - " + error);
    }
}
```

### Update a Time Entry

**Required Scope**: `TIME_ENTRY_WRITE`

```java
public JSONObject updateTimeEntry(String workspaceId, String timeEntryId, String[] tagIds)
        throws IOException {
    JSONObject payload = new JSONObject();
    if (tagIds != null) {
        JSONArray tagsArray = new JSONArray();
        for (String tagId : tagIds) {
            tagsArray.put(tagId);
        }
        payload.put("tagIds", tagsArray);
    }

    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId);
    conn.setRequestMethod("PUT");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.toString().getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to update time entry: " + responseCode + " - " + error);
    }
}
```

### Delete a Time Entry

**Required Scope**: `TIME_ENTRY_WRITE`

```java
public void deleteTimeEntry(String workspaceId, String timeEntryId) throws IOException {
    HttpURLConnection conn = createConnection("/workspaces/" + workspaceId + "/time-entries/" + timeEntryId);
    conn.setRequestMethod("DELETE");

    int responseCode = conn.getResponseCode();
    if (responseCode != 200 && responseCode != 204) {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("Failed to delete time entry: " + responseCode + " - " + error);
    }
}
```

---

## User Operations

### Get Current User

**Required Scope**: None (works with any valid token)

```java
public JSONObject getCurrentUser() throws IOException {
    HttpURLConnection conn = createConnection("/user");
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else {
        throw new IOException("Failed to get user: " + responseCode);
    }
}
```

**Example Response**:
```json
{
  "id": "64621faec4d2cc53b91fce6c",
  "email": "user@example.com",
  "name": "John Doe",
  "activeWorkspace": "68adfddad138cb5f24c63b22",
  "defaultWorkspace": "68adfddad138cb5f24c63b22",
  "settings": {
    "weekStart": "MONDAY",
    "timeZone": "Europe/Belgrade",
    "timeFormat": "HOUR12",
    "dateFormat": "MM/DD/YYYY"
  },
  "status": "ACTIVE"
}
```

---

## Error Handling

### Standard Error Response Format

```java
private String readResponse(InputStream stream) throws IOException {
    if (stream == null) return "";

    StringBuilder response = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
    }
    return response.toString();
}

public JSONObject makeApiRequest(String endpoint, String method, JSONObject payload)
        throws IOException {
    HttpURLConnection conn = createConnection(endpoint);
    conn.setRequestMethod(method);

    if (payload != null) {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("UTF-8"));
        }
    }

    int responseCode = conn.getResponseCode();

    if (responseCode >= 200 && responseCode < 300) {
        String response = readResponse(conn.getInputStream());
        return new JSONObject(response);
    } else if (responseCode == 401) {
        throw new IOException("Unauthorized: Invalid or expired token");
    } else if (responseCode == 403) {
        throw new IOException("Forbidden: Insufficient scopes or permissions");
    } else if (responseCode == 404) {
        throw new IOException("Not found: Resource does not exist");
    } else if (responseCode == 429) {
        throw new IOException("Rate limit exceeded: Too many requests");
    } else {
        String error = readResponse(conn.getErrorStream());
        throw new IOException("API error (" + responseCode + "): " + error);
    }
}
```

### Common HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Request successful, no response body |
| 400 | Bad Request | Check request payload format |
| 401 | Unauthorized | Invalid or expired token |
| 403 | Forbidden | Missing required scope or permission |
| 404 | Not Found | Resource does not exist |
| 429 | Too Many Requests | Rate limit exceeded, implement backoff |
| 500 | Internal Server Error | Server error, retry with exponential backoff |

---

## Scope Requirements

### Scope-to-Endpoint Mapping

| Scope | Allowed Operations |
|-------|-------------------|
| `WORKSPACE_READ` | GET /workspaces/{workspaceId} |
| `PROJECT_READ` | GET /workspaces/{workspaceId}/projects |
| `PROJECT_WRITE` | POST, PUT, DELETE /workspaces/{workspaceId}/projects |
| `TAG_READ` | GET /workspaces/{workspaceId}/tags |
| `TAG_WRITE` | POST, PUT, DELETE /workspaces/{workspaceId}/tags |
| `CLIENT_READ` | GET /workspaces/{workspaceId}/clients |
| `CLIENT_WRITE` | POST, PUT, DELETE /workspaces/{workspaceId}/clients |
| `TIME_ENTRY_READ` | GET /workspaces/{workspaceId}/time-entries, GET /workspaces/{workspaceId}/user/{userId}/time-entries |
| `TIME_ENTRY_WRITE` | POST, PUT, DELETE /workspaces/{workspaceId}/time-entries |
| `TASK_READ` | GET /workspaces/{workspaceId}/projects/{projectId}/tasks |
| `TASK_WRITE` | POST, PUT, DELETE /workspaces/{workspaceId}/projects/{projectId}/tasks |
| `USER_READ` | GET /user, GET /workspaces/{workspaceId}/users |

### Example Manifest with Scopes

```json
{
  "schemaVersion": "1.3",
  "key": "my-addon",
  "name": "My Addon",
  "scopes": [
    "WORKSPACE_READ",
    "PROJECT_READ",
    "TAG_READ",
    "TAG_WRITE",
    "TIME_ENTRY_READ",
    "TIME_ENTRY_WRITE"
  ]
}
```

---

## Pagination

Many endpoints support pagination:

```java
public List<JSONObject> getAllProjectsPaginated(String workspaceId) throws IOException {
    List<JSONObject> allProjects = new ArrayList<>();
    int page = 1;
    int pageSize = 50;

    while (true) {
        String endpoint = "/workspaces/" + workspaceId + "/projects?page=" + page + "&page-size=" + pageSize;
        HttpURLConnection conn = createConnection(endpoint);
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            JSONArray projects = new JSONArray(response);

            if (projects.length() == 0) {
                break; // No more results
            }

            for (int i = 0; i < projects.length(); i++) {
                allProjects.add(projects.getJSONObject(i));
            }

            page++;
        } else {
            throw new IOException("Failed to get projects: " + responseCode);
        }
    }

    return allProjects;
}
```

---

## Rate Limiting

Clockify API has a rate limit of **50 requests per second** per addon per workspace.

### Rate Limit Handler with Exponential Backoff

```java
public JSONObject makeApiRequestWithRetry(String endpoint, String method, JSONObject payload, int maxRetries)
        throws IOException {
    int retries = 0;
    long backoffMs = 1000; // Start with 1 second

    while (retries <= maxRetries) {
        try {
            return makeApiRequest(endpoint, method, payload);
        } catch (IOException e) {
            if (e.getMessage().contains("429") && retries < maxRetries) {
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff
                    retries++;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for retry", ie);
                }
            } else {
                throw e;
            }
        }
    }

    throw new IOException("Max retries exceeded");
}
```

---

## Complete Example: Auto-Tagging Addon

```java
public class AutoTaggerAddon {
    private final ClockifyApiClient apiClient;

    public void handleNewTimeEntry(String workspaceId, String timeEntryId, String description) {
        try {
            // 1. Analyze description to determine tags
            List<String> tagNames = analyzeDescription(description);

            // 2. Get all existing tags
            List<JSONObject> existingTags = apiClient.getAllTags(workspaceId);
            Map<String, String> tagNameToId = new HashMap<>();
            for (JSONObject tag : existingTags) {
                tagNameToId.put(tag.getString("name"), tag.getString("id"));
            }

            // 3. Create missing tags
            List<String> tagIds = new ArrayList<>();
            for (String tagName : tagNames) {
                if (tagNameToId.containsKey(tagName)) {
                    tagIds.add(tagNameToId.get(tagName));
                } else {
                    JSONObject newTag = apiClient.createTag(workspaceId, tagName);
                    tagIds.add(newTag.getString("id"));
                }
            }

            // 4. Update time entry with tags
            String[] tagIdsArray = tagIds.toArray(new String[0]);
            apiClient.updateTimeEntry(workspaceId, timeEntryId, tagIdsArray);

            System.out.println("Successfully tagged time entry with: " + String.join(", ", tagNames));
        } catch (IOException e) {
            System.err.println("Failed to auto-tag time entry: " + e.getMessage());
        }
    }

    private List<String> analyzeDescription(String description) {
        // Simple keyword-based tagging logic
        List<String> tags = new ArrayList<>();
        String lower = description.toLowerCase();

        if (lower.contains("meeting")) tags.add("meeting");
        if (lower.contains("bug") || lower.contains("fix")) tags.add("bugfix");
        if (lower.contains("feature") || lower.contains("implement")) tags.add("development");
        if (lower.contains("review")) tags.add("code-review");

        return tags;
    }
}
```

---

## Additional Resources

- [Clockify OpenAPI Specification](../dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json)
- [Full Marketplace Documentation](../dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md)
- [Request/Response Examples](REQUEST-RESPONSE-EXAMPLES.md)
- [Data Models Reference](DATA-MODELS.md)
- [Common Patterns](PATTERNS.md)
