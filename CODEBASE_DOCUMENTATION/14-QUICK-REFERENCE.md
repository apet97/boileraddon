# Quick Reference Guide

Fast lookup cheat sheet for common tasks, commands, and patterns.

## Common Commands

### Build & Run

```bash
# Build all modules
mvn clean package -DskipTests

# Build specific addon
mvn -pl addons/rules package -DskipTests

# Run addon
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# Run with environment
ADDON_BASE_URL=http://localhost:8080/rules make run-rules
```

### Database

```bash
# Start PostgreSQL (Docker)
docker compose -f docker-compose.dev.yml up -d

# Run migrations
mvn flyway:migrate -Pflyway

# Connect to database
psql -h localhost -U addons -d addons
```

### Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean verify

# Smoke tests
make smoke
```

### Docker

```bash
# Build image
docker build --build-arg ADDON_DIR=addons/rules -t clockify-rules .

# Run container
docker run -p 8080:8080 --env-file .env.rules clockify-rules
```

---

## Environment Variables

### Core Configuration

```bash
ADDON_PORT=8080
ADDON_BASE_URL=http://localhost:8080/addon
```

### Database

```bash
DB_URL=jdbc:postgresql://localhost:5432/addons
DB_USERNAME=addons
DB_PASSWORD=addons
```

### Security

```bash
ADDON_WEBHOOK_SECRET=your-secret-here
ADDON_SKIP_SIGNATURE_VERIFY=true  # Dev only!
ADDON_ACCEPT_JWT_SIGNATURE=true
```

### Middleware

```bash
ADDON_RATE_LIMIT=10
ADDON_LIMIT_BY=workspace
ADDON_CORS_ORIGINS=https://*.clockify.me
ADDON_REQUEST_LOGGING=true
ADDON_FRAME_ANCESTORS='self' https://*.clockify.me
```

### Rules Addon

```bash
RULES_APPLY_CHANGES=false  # Dry-run mode
CLOCKIFY_WORKSPACE_ID=62e123...
CLOCKIFY_INSTALLATION_TOKEN=eyJhbG...
```

---

## Code Snippets

### Minimal Addon

```java
public class MyAddonApp {
    public static void main(String[] args) throws Exception {
        ClockifyManifest manifest = new ClockifyManifest()
            .name("My Addon")
            .schemaVersion("1.3");

        ClockifyAddon addon = new ClockifyAddon(manifest, "/my-addon", 8080);

        // Lifecycle
        addon.registerLifecycleHandler("INSTALLED", request -> {
            JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
            TokenStore.save(
                payload.get("workspaceId").asText(),
                payload.get("installationToken").asText(),
                payload.get("apiBaseUrl").asText()
            );
            return HttpResponse.ok("{\"status\":\"installed\"}");
        });

        // Enable middleware
        addon.enableSecurityHeaders();

        // Start
        addon.start();
    }
}
```

### Webhook Handler

```java
addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();
    JsonNode timeEntry = payload.get("timeEntry");

    // Process event
    processTimeEntry(workspaceId, timeEntry);

    return HttpResponse.ok("{\"status\":\"ok\"}");
});
```

### API Endpoint

```java
addon.registerCustomEndpoint("/api/data", request -> {
    String workspaceId = request.getParameter("workspaceId");

    // Fetch data
    List<String> data = fetchData(workspaceId);

    // Return JSON
    String json = new ObjectMapper().writeValueAsString(data);
    return HttpResponse.ok(json, "application/json");
});
```

### Clockify API Call

```java
Optional<WorkspaceToken> tokenOpt = TokenStore.get(workspaceId);
if (tokenOpt.isEmpty()) {
    return HttpResponse.unauthorized("Token not found");
}

ClockifyHttpClient client = new ClockifyHttpClient(tokenOpt.get().apiBaseUrl());
HttpResponse<String> resp = client.get(
    "/workspaces/" + workspaceId + "/tags",
    tokenOpt.get().token(),
    Map.of()
);

if (resp.statusCode() == 200) {
    JsonNode tags = new ObjectMapper().readTree(resp.body());
    // Process tags
}
```

---

## File Locations

### SDK Core
- `ClockifyAddon`: `addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java`
- `AddonServlet`: `addon-sdk/src/main/java/com/clockify/addon/sdk/AddonServlet.java`
- `TokenStore`: `addon-sdk/src/main/java/com/clockify/addon/sdk/security/TokenStore.java`

### Middleware
- `SecurityHeadersFilter`: `addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/SecurityHeadersFilter.java`
- `RateLimiter`: `addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/RateLimiter.java`
- `CorsFilter`: `addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/CorsFilter.java`

### Database
- Migrations: `/db/migrations/`
- DatabaseTokenStore: `addon-sdk/src/main/java/com/clockify/addon/sdk/security/DatabaseTokenStore.java`

### Rules Addon
- Main App: `addons/rules/src/main/java/com/example/rules/RulesApp.java`
- Rule Engine: `addons/rules/src/main/java/com/example/rules/engine/`
- Database Store: `addons/rules/src/main/java/com/example/rules/store/DatabaseRulesStore.java`

---

## Manifest Structure

```json
{
  "schemaVersion": "1.3",
  "name": "Addon Name",
  "description": "Addon description",
  "key": "com.example.addon",
  "vendor": {
    "name": "Company Name",
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

---

## Available Scopes

- `TIME_ENTRY_READ` / `TIME_ENTRY_WRITE`
- `TAG_READ` / `TAG_WRITE`
- `PROJECT_READ` / `PROJECT_WRITE`
- `TASK_READ` / `TASK_WRITE`
- `CLIENT_READ` / `CLIENT_WRITE`
- `USER_READ`
- `WORKSPACE_READ`

---

## Webhook Events

- `TIME_ENTRY_CREATED`
- `TIME_ENTRY_UPDATED`
- `TIME_ENTRY_DELETED`
- `NEW_TIMER_STARTED`
- `TIMER_STOPPED`

---

## HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 404 | Not Found |
| 429 | Rate Limited |
| 500 | Server Error |

---

## Makefile Targets

```bash
make build                      # Build all
make build-rules                # Build rules addon
make run-rules                  # Run rules addon
make test                       # Run tests
make smoke                      # Smoke tests
make validate                   # Validate manifests
make docker-run                 # Run in Docker
```

---

## Testing Endpoints

```bash
# Health check
curl http://localhost:8080/addon/health

# Metrics
curl http://localhost:8080/addon/metrics

# Manifest
curl http://localhost:8080/addon/manifest.json | jq .
```

---

## Troubleshooting

### Port in Use
```bash
lsof -i :8080
kill -9 <PID>
```

### Database Connection
```bash
docker compose -f docker-compose.dev.yml ps
psql -h localhost -U addons -d addons
```

### Token Not Found
```bash
# Check token store
SELECT * FROM addon_tokens WHERE workspace_id = '...';
```

### Manifest Validation
```bash
python3 tools/validate-manifest.py path/to/manifest.json
```

---

## Useful SQL Queries

```sql
-- List all tokens
SELECT workspace_id, created_at FROM addon_tokens;

-- List all rules for workspace
SELECT rule_id, rule_json FROM rules WHERE workspace_id = '...';

-- Delete inactive tokens (90+ days)
DELETE FROM addon_tokens
WHERE last_accessed_at < EXTRACT(EPOCH FROM NOW() - INTERVAL '90 days')*1000;
```

---

## Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes, commit
git add .
git commit -m "Add feature"

# Push
git push -u origin feature/my-feature

# Create PR (via GitHub UI)
```

---

## Docker Compose

```bash
# Start services
docker compose -f docker-compose.dev.yml up -d

# Stop services
docker compose -f docker-compose.dev.yml down

# View logs
docker compose -f docker-compose.dev.yml logs -f
```

---

## Common Patterns

### Rate Limiting
```java
addon.enableRateLimiting(10.0, "workspace");
```

### CORS
```java
addon.enableCors("https://*.clockify.me");
```

### Security Headers
```java
addon.enableSecurityHeaders();
```

### Request Logging
```java
addon.enableRequestLogging();
```

---

## Dependencies (Key)

```xml
<!-- Jackson JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>

<!-- Jetty Server -->
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>11.0.24</version>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version>
</dependency>
```

---

## Resources

- **Main README:** [/README.md](/README.md)
- **AI Guide:** [/AI_README.md](/AI_README.md)
- **Docs:** [/docs/](/docs/)
- **Examples:** [/examples/](/examples/)
- **Issues:** https://github.com/apet97/boileraddon/issues

---

**Generated:** 2025-11-09 | **Version:** 1.0.0
