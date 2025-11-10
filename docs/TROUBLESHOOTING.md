# Troubleshooting Guide

**Version**: 2.0.0
**Last Updated**: November 10, 2025

Common issues and solutions for Clockify addon deployment and operation.

---

## Table of Contents

1. [Webhook Issues](#webhook-issues)
2. [Authentication & Token Issues](#authentication--token-issues)
3. [Database Issues](#database-issues)
4. [Performance Issues](#performance-issues)
5. [Configuration Issues](#configuration-issues)
6. [Deployment Issues](#deployment-issues)
7. [Getting Help](#getting-help)

---

## Webhook Issues

### Webhooks Not Being Delivered

**Symptoms**:
- No logs in application
- Webhook handler never called
- 404 responses from addon endpoint

**Debug Steps**:

1. **Verify webhook is registered in addon**:
```bash
# GET /manifest.json should show webhook endpoint
curl http://addon:8080/manifest.json | jq '.webhooks'

# Expected output:
# [
#   {
#     "event": "TIME_ENTRY_CREATED",
#     "path": "/webhook"
#   }
# ]
```

2. **Check Clockify workspace configuration**:
   - Go to Clockify workspace settings → Webhooks
   - Verify addon webhook URL is set correctly
   - Check webhook is enabled (toggle is ON)
   - Verify URL matches your addon base URL

3. **Test webhook delivery manually**:
```bash
# Generate HMAC signature
WEBHOOK_SECRET="your-webhook-secret"
PAYLOAD='{"event":"TIME_ENTRY_CREATED","workspaceId":"test"}'
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -r | awk '{print $1}')

# Send test webhook
curl -X POST http://addon:8080/webhook \
  -H "Content-Type: application/json" \
  -H "clockify-webhook-signature: sha256=$SIGNATURE" \
  -H "clockify-webhook-event-type: TIME_ENTRY_CREATED" \
  -d "$PAYLOAD"
```

4. **Check logs**:
```bash
tail -f logs/application.log | grep -i webhook
# Look for: "POST /webhook", "No handler registered", errors
```

5. **Verify network connectivity**:
```bash
# From Clockify server, test addon reachability
curl -I http://addon-host:8080/manifest.json
# Should return 200 OK
```

---

### Invalid Webhook Signature

**Symptoms**:
- Logs show: `Invalid webhook signature`
- 401 Unauthorized responses
- Webhooks fail intermittently

**Root Causes**:

1. **Webhook secret mismatch**:
```bash
# Verify webhook secret in Clockify workspace settings matches addon secret
# On addon: echo $WEBHOOK_SECRET
# On Clockify: Settings → Webhooks → Secret (may be hidden)
```

2. **Request body modified**:
   - Middleware may transform request body
   - Signature validation must use original request bytes
   - JSON may be reformatted (whitespace changes break signature)

3. **Clock skew** (rare):
   - Server time differences can affect timestamp-based validation
   - Sync server clocks: `ntpdate -s time.nist.gov`

**Solution**:
```java
// Ensure signature validation uses raw request body
InputStream rawBody = request.getInputStream();
String signature = request.getHeader("clockify-webhook-signature");
boolean valid = WebhookSignatureValidator.verify(request, workspaceId).isValid();
```

---

### Webhook Handler Errors

**Symptoms**:
- Handler executed but returns errors
- 500 Internal Server Error from webhook endpoint
- Handler exceptions logged

**Debug Steps**:

1. **Check handler logs**:
```bash
grep -A 5 "Error handling webhook" logs/application.log
```

2. **Verify payload structure**:
```bash
# Enable webhook payload logging (development only)
export ADDON_LOG_WEBHOOK_PAYLOAD=true

# Then check logs for actual payload
grep "webhook_payload" logs/application.log
```

3. **Test handler in isolation**:
```java
@Test
void testWebhookHandler() {
    String json = """
    {
      "event": "TIME_ENTRY_CREATED",
      "workspaceId": "test",
      "timeEntry": {
        "id": "123",
        "description": "Test",
        "duration": 3600
      }
    }
    """;

    HttpResponse response = handler.handle(mockRequest(json));
    assertEquals(200, response.getStatusCode());
}
```

---

## Authentication & Token Issues

### 401 Unauthorized / Token Not Found

**Symptoms**:
- Logs show: `Token not found for workspace`
- API calls return 401 errors
- Token operations fail

**Debug Steps**:

1. **Verify token is stored**:
```bash
# Connect to token store (if using database)
psql postgresql://user:pass@localhost/addon_db
SELECT * FROM addon_workspace_token WHERE workspace_id = 'your-workspace';

# Output should show: workspace_id, token, created_at, expires_at
```

2. **Check token expiry**:
```bash
# If using rotation, verify grace period active
SELECT expires_at, rotated_at FROM addon_workspace_token
WHERE workspace_id = 'your-workspace';

# Current time should be before expires_at
date +%s000  # Current timestamp in milliseconds
```

3. **Verify lifecycle handler is registered**:
```bash
# Check manifest includes lifecycle endpoints
curl http://addon:8080/manifest.json | jq '.lifecycleEndpoints'
# Should show: [{"path": "/lifecycle", "type": "INSTALLED"}, ...]
```

4. **Test lifecycle handler manually**:
```bash
# Simulate INSTALLED event
curl -X POST http://addon:8080/lifecycle/INSTALLED \
  -H "Content-Type: application/json" \
  -d '{
    "workspaceId": "test-workspace",
    "token": "test-token-value",
    "baseUrl": "https://api.clockify.me"
  }'
```

---

### Token Rotation Issues

**Symptoms**:
- Old tokens rejected immediately
- New tokens not accepted during rotation
- "Token validation failed" errors

**Debug Steps**:

1. **Check token rotation status**:
```bash
# Database query
SELECT workspace_id, rotated_at, expires_at FROM addon_workspace_token;

# Verify rotated_at is recent (within last 1 hour for default grace period)
```

2. **Verify both tokens are accepted**:
```bash
# During grace period, both old and new should work
curl -H "x-addon-token: old-token-value" http://clockify.api/workspaces/...
curl -H "x-addon-token: new-token-value" http://clockify.api/workspaces/...

# Both should return 200
```

3. **Check grace period expiry**:
```java
// Grace period default is 1 hour
// If old token was rotated >1 hour ago, it will be rejected
System.setProperty("clockify.token.rotation.grace.ms", "3600000");  // 1 hour
```

---

## Database Issues

### Database Connection Failed

**Symptoms**:
- Logs show: `Connection refused` or `Connection timeout`
- Health check returns DOWN for database
- Token operations timeout

**Verify Connection Settings**:

```bash
# Check environment variables
echo $DB_URL
echo $DB_USERNAME
# echo $DB_PASSWORD  (don't print password!)

# Example correct format:
# postgresql://hostname:5432/database_name
# mysql://hostname:3306/database_name
```

**Test Connection**:

```bash
# PostgreSQL
psql "postgresql://user:pass@localhost:5432/addon_db" -c "SELECT 1"

# MySQL
mysql -h localhost -u user -p -e "SELECT 1"

# If fails: check host, port, credentials
```

**Common Mistakes**:

| Issue | Fix |
|-------|-----|
| Wrong host | Use IP or FQDN, not alias |
| Wrong port | PostgreSQL=5432, MySQL=3306 |
| Missing database name | Add to URL path |
| Invalid driver | PostgreSQL driver not in classpath |
| Firewall blocked | Open port between addon and DB |
| SSL required | Add `?ssl=true` to URL |

---

### Database Pool Exhaustion

**Symptoms**:
- Logs show: `Connection pool size exceeded`
- Slow queries or timeouts
- Health check shows high pool usage %

**Debug**:

```bash
# Check pool stats from health endpoint
curl http://addon:8080/health | jq '.checks.database'

# Output:
# {
#   "status": "UP",
#   "details": {
#     "active_connections": 8,
#     "idle_connections": 2,
#     "total_connections": 10,
#     "pool_usage_percent": 80
#   }
# }
```

**Solutions**:

1. **Increase pool size**:
```bash
export DB_POOL_SIZE=20  # Default is 10
```

2. **Optimize queries**:
   - Add database indexes on workspace_id
   - Use connection pooling at application level
   - Check for slow queries

3. **Monitor query duration**:
```bash
# Enable slow query log
# PostgreSQL:
SET log_min_duration_statement = 1000;  -- Log queries > 1 second

# MySQL:
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
```

---

### Table Not Found / Schema Issues

**Symptoms**:
- Logs show: `Table not found` or `Column not found`
- Database connection OK but operations fail
- Schema creation failed silently

**Verify Schema**:

```bash
# PostgreSQL
psql "postgresql://user:pass@localhost/addon_db" \
  -c "\\dt addon_workspace_token"

# MySQL
mysql -u user -p -e "DESCRIBE addon_db.addon_workspace_token"
```

**Recreate Schema**:

```bash
# The addon auto-creates tables on startup
# To force recreation, delete and restart:

# PostgreSQL
psql postgresql://user:pass@localhost/addon_db \
  -c "DROP TABLE IF EXISTS addon_workspace_token CASCADE"

# Restart addon (will recreate table)
```

---

## Performance Issues

### Slow Webhook Processing

**Symptoms**:
- Webhook handler takes > 1 second
- Timeouts from Clockify
- Metrics show high webhook_request_seconds

**Optimize**:

```java
// WRONG: Synchronous external API call
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    externalAPI.create(data);  // Blocks for 5+ seconds
    return HttpResponse.ok();
}

// CORRECT: Async processing
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    executor.submit(() -> externalAPI.create(data));  // Non-blocking
    return HttpResponse.ok();
}
```

**Monitor**:

```bash
# Check webhook processing times
curl http://addon:8080/metrics | grep webhook_request_seconds_bucket

# Check database query times
curl http://addon:8080/metrics | grep token_store_seconds
```

---

### Memory Leaks

**Symptoms**:
- Heap memory usage continuously increases
- Garbage collection pauses getting longer
- Out of Memory errors after hours/days

**Diagnose**:

```bash
# Check memory over time
curl http://addon:8080/health | jq '.memory'

# If heapUsed keeps growing, profile the application
# Use: jmap, jstat, or YourKit profiler
```

**Common Causes**:

1. **TokenStore memory leaks**:
   - In-memory store unbounded (no eviction)
   - Use database store instead: `export DB_URL=...`

2. **Webhook handler memory leaks**:
   - Large collections not cleared
   - File handles not closed
   - Thread pools not cleaned up

3. **Database connection leaks**:
   - Connections not returned to pool
   - Verify HikariCP leak detection enabled

---

## Configuration Issues

### Environment Variable Not Recognized

**Symptoms**:
- Setting env var has no effect
- Default values used instead of custom
- Configuration seems to be ignored

**Debug**:

```bash
# Check that variables are set in process
curl http://addon:8080/health | jq '.application'

# Or check application logs on startup
grep "configuration" logs/application.log | head -20

# Look for:
# "Port '8080' not specified"
# "ENFORCE_HTTPS: true"
# "MAX_REQUEST_SIZE_MB: 10"
```

**Common Issues**:

| Variable | Wrong | Correct |
|----------|-------|---------|
| Port | PORT=8080 | ADDON_PORT=8080 |
| HTTPS | HTTPS=true | ENFORCE_HTTPS=true |
| Max Size | MAX_SIZE=100 | MAX_REQUEST_SIZE_MB=100 |
| JWT | ACCEPT_JWT=true | ADDON_ACCEPT_JWT_SIGNATURE=true |

---

### HTTPS Enforcement Blocking Local Development

**Symptoms**:
- `403 Forbidden` with message "HTTPS required"
- Webhooks rejected with `ERR_HTTPS_REQUIRED`
- Health checks return 403

**Solution** (development only):

```bash
# Disable HTTPS enforcement for local development
export ENFORCE_HTTPS=false

# Restart addon
docker-compose restart addon

# Verify
curl -i http://localhost:8080/health
# Should return 200 OK
```

**Note**: Never disable HTTPS enforcement in production!

---

## Deployment Issues

### Addon Not Responding After Deployment

**Symptoms**:
- 502 Bad Gateway or connection refused
- Port not listening
- Process exited unexpectedly

**Debug**:

```bash
# Check if addon process is running
ps aux | grep addon

# If not running, check logs
tail -f logs/error.log

# Common startup errors:
# - Port already in use: lsof -i :8080
# - Out of memory: java.lang.OutOfMemoryError
# - JDBC connection failed: Check DB_URL, DB_USERNAME, DB_PASSWORD
```

**Port Already In Use**:

```bash
# Find and kill process using port
lsof -i :8080
kill -9 <PID>

# Or use different port
export ADDON_PORT=9090
```

---

### Graceful Shutdown Not Working

**Symptoms**:
- In-flight webhooks interrupted
- Tokens left in inconsistent state
- Database connections abruptly closed

**Implementation**:

```java
// Add shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    logger.info("Shutdown signal received, starting graceful shutdown");

    // Stop accepting new requests
    server.setStopTimeout(30000);  // 30 second timeout

    // Close database connections
    if (tokenStore instanceof PooledDatabaseTokenStore) {
        ((PooledDatabaseTokenStore) tokenStore).close();
    }

    // Stop server
    try {
        server.stop();
    } catch (Exception e) {
        logger.error("Error stopping server: {}", e.getMessage());
    }

    logger.info("Graceful shutdown complete");
}));
```

---

## Getting Help

### Information to Include in Bug Report

When reporting issues, include:

1. **Configuration**:
   - Environment variables (mask secrets)
   - Addon version and Java version
   - Database type and version
   - Docker/Kubernetes version if applicable

2. **Error Details**:
   - Full error message from logs
   - Stack trace (if available)
   - When did it first occur
   - Is it reproducible

3. **Context**:
   - What were you trying to do
   - Has this worked before
   - Recent changes to configuration/code
   - Frequency (every time / intermittent)

### Enabling Debug Logging

```bash
# Edit logback.xml or set via environment
export LOG_LEVEL=DEBUG

# Or in logback.xml:
<root level="DEBUG">
  <appender-ref ref="CONSOLE"/>
</root>

# Restart addon to see detailed logs
```

### Getting Application Metrics

```bash
# Export metrics for analysis
curl http://addon:8080/metrics > metrics.txt

# Check health endpoint
curl http://addon:8080/health | jq '.'

# View recent logs
docker logs addon-container --tail=100
```

---

**Last Updated**: November 10, 2025
**Version**: 2.0.0
