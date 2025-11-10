# Security Configuration Guide

**Version**: 2.0.0  
**Last Updated**: November 10, 2025  
**Status**: Production-Ready

This guide covers all security features, configuration options, and best practices for the Clockify Addon boilerplate.

---

## Table of Contents

1. [Quick Start Security Checklist](#quick-start-security-checklist)
2. [Security Features](#security-features)
3. [Configuration](#configuration)
4. [HTTPS Enforcement](#https-enforcement)
5. [JWT Signature Acceptance](#jwt-signature-acceptance)
6. [Rate Limiting](#rate-limiting)
7. [CSRF Protection](#csrf-protection)
8. [Token Rotation](#token-rotation)
9. [Request Size Limits](#request-size-limits)
10. [Audit Logging](#audit-logging)
11. [Database Token Storage](#database-token-storage)
12. [Monitoring & Alerts](#monitoring--alerts)

---

## Quick Start Security Checklist

**Development Environment**:
```bash
# Development (local HTTP, JWT signatures disabled by default)
export ADDON_PORT=8080
export ADDON_BASE_URL=http://localhost:8080/my-addon
# ADDON_ACCEPT_JWT_SIGNATURE defaults to false ✓
# ENFORCE_HTTPS defaults to true (but can be disabled for local development)
export ENFORCE_HTTPS=false
```

**Staging/Production Environment**:
```bash
# Production (HTTPS enforced, rate limits, audit logging)
export ADDON_PORT=8080
export ADDON_BASE_URL=https://addon.example.com/my-addon
export ENFORCE_HTTPS=true
export DB_URL=postgresql://dbhost:5432/addon_db
export DB_USERNAME=addon_user
export DB_PASSWORD=<strong-password>
export DB_POOL_SIZE=20
export MAX_REQUEST_SIZE_MB=50

# JWT signatures MUST be disabled in production
# (do NOT set ADDON_ACCEPT_JWT_SIGNATURE=true)
```

---

## Security Features

The boilerplate includes **12 critical security fixes** (Sprint 1):

| ID | Feature | Enabled | Scope |
|---|---|---|---|
| P0-1 | Webhook Signature Validation | ✅ | Default (reject JWT signatures) |
| P0-2 | Event Type Validation | ✅ | Prevents injection attacks |
| P0-3 | Enhanced Error Logging | ✅ | With workspace context |
| P0-4 | Rate Limiting | ✅ | /lifecycle, /webhook, default endpoints |
| P0-5 | CSRF Protection | ✅ | Browser-based interactions |
| P0-6 | Secret Management | ✅ | No hardcoded tokens in examples |
| P0-7 | Token Rotation | ✅ | Zero-downtime token updates |
| P0-8 | HTTPS Enforcement | ✅ | Production mode (configurable) |
| P0-9 | Dependency Audit | ✅ | All dependencies current |
| P0-10 | Request Size Limits | ✅ | 10MB default (configurable) |
| P0-11 | Connection Pooling | ✅ | HikariCP with leak detection |
| P0-12 | Audit Logging | ✅ | JSON format for log aggregation |

---

## Configuration

All security features are configured via environment variables. No code changes required.

### Environment Variables

```ini
# Server Configuration
ADDON_PORT=8080                              # Server port (1-65535)
ADDON_BASE_URL=https://addon.example.com    # Public URL for webhooks

# Security - HTTPS Enforcement (P0-8)
ENFORCE_HTTPS=true                           # Default: true (reject non-HTTPS in production)
                                             # Set to false for local HTTP development only

# Security - JWT Signature Acceptance (P0-1)
ADDON_ACCEPT_JWT_SIGNATURE=false             # Default: false (REQUIRED for security)
                                             # Only set to true in development/testing
                                             # MUST be false in production

# Rate Limiting (P0-4)
# No configuration needed - automatically applied with these limits:
# /lifecycle: 0.1 req/sec (1 per 10 seconds)
# /webhook:   1.0 req/sec (1 per second)
# Others:     0.5 req/sec (1 per 2 seconds)

# Request Size Limiting (P0-10)
MAX_REQUEST_SIZE_MB=10                       # Default: 10 MB
                                             # Adjust for large webhook payloads

# Database Token Storage (P0-11)
DB_URL=postgresql://localhost:5432/addon_db  # JDBC URL (PostgreSQL or MySQL)
DB_USERNAME=addon_user                       # Database username
DB_PASSWORD=<strong-password>                # Database password (use secrets manager)
DB_POOL_SIZE=10                              # Connection pool size (default: 10)

# For InMemoryTokenStore (development only):
# Do NOT set DB_* variables, tokens are stored in memory
```

---

## HTTPS Enforcement

**Security Level**: Critical (P0-8)

### How It Works

1. **Default**: HTTPS is **enforced** in production
2. **Proxy-Aware**: Checks multiple headers for HTTPS detection
3. **Fail-Safe**: Rejects non-HTTPS connections with 403 Forbidden

### Configuration

```bash
# Production (HTTPS required)
export ENFORCE_HTTPS=true

# Development (HTTP allowed)
export ENFORCE_HTTPS=false

# Docker/Kubernetes (behind reverse proxy)
# Filter checks these headers (in order):
# 1. X-Forwarded-Proto (AWS ALB, Nginx)
# 2. X-Original-Proto (Cloudflare)
# 3. CloudFront-Forwarded-Proto (AWS CloudFront)
# 4. request.isSecure() (direct HTTPS)

# Example: HTTPS via Nginx reverse proxy
# nginx.conf:
#   proxy_set_header X-Forwarded-Proto $scheme;
#   proxy_set_header X-Forwarded-Host $host;
```

### Testing

```bash
# Should REJECT (403 Forbidden) with ENFORCE_HTTPS=true
curl -X GET http://addon.example.com/health

# Should ACCEPT with proper signature
curl -X POST https://addon.example.com/webhook \
  -H "clockify-webhook-signature: hmac-sha256=..." \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

## JWT Signature Acceptance

**Security Level**: Critical (P0-1)

### Default Behavior

- **Production**: JWT signatures are **REJECTED** by default
- **Development**: Can be enabled for testing with `ADDON_ACCEPT_JWT_SIGNATURE=true`
- **Required**: Webhook signature validation is always required

### Configuration

```bash
# Production (MUST be false)
unset ADDON_ACCEPT_JWT_SIGNATURE
# or explicitly:
export ADDON_ACCEPT_JWT_SIGNATURE=false

# Development only (for testing JWT-signed webhooks)
export ADDON_ACCEPT_JWT_SIGNATURE=true
# Tests can set as system property:
System.setProperty("ADDON_ACCEPT_JWT_SIGNATURE", "true");
```

### Why This Matters

- JWT signatures are for **development workspace testing** only
- Unintended acceptance could allow webhook spoofing
- Always verify with production HMAC signatures (never JWT)

### Testing

```java
// Test with JWT signature acceptance enabled
@Test
void testJwtSignatureAcceptance() {
    // Setup
    System.setProperty("ADDON_ACCEPT_JWT_SIGNATURE", "true");
    TokenStore.setClock(Clock.fixed(...));
    
    try {
        // Test JWT signature validation
        // ...
    } finally {
        System.clearProperty("ADDON_ACCEPT_JWT_SIGNATURE");
        TokenStore.resetClock();
    }
}
```

---

## Rate Limiting

**Security Level**: High (P0-4)

### How It Works

1. **Automatic**: Applied to all critical endpoints
2. **Per-Workspace or IP**: Tracks limits by workspace ID or IP address
3. **Fail-Closed**: Blocks requests if rate limiter fails (security-first)
4. **Returns 429**: "Too Many Requests" response

### Limits

| Endpoint | Limit | Purpose |
|---|---|---|
| `/lifecycle/*` | 0.1 req/sec | Installation/deletion (rare events) |
| `/webhook/*` | 1.0 req/sec | Event processing (normal workload) |
| Other endpoints | 0.5 req/sec | Default for custom endpoints |

### Configuration

Rate limits are **not configurable** by design (security-first). To change:

1. Modify `CriticalEndpointRateLimiter.java`
2. Update the constants (e.g., `LIFECYCLE_PERMITS_PER_SECOND`)
3. Rebuild and redeploy

### Monitoring

Check for rate limit exceeded events in logs:

```
[WARN] CRITICAL: Rate limit exceeded for path: /webhook identifier: workspace-123
```

Audit logs include rate limit errors:
```json
{
  "timestamp": "2025-11-10T10:30:45.123Z",
  "event": "RATE_LIMIT_EXCEEDED",
  "level": "ERROR",
  "clientIp": "192.168.1.1",
  "details": {
    "path": "/webhook",
    "limit_permits_sec": 1.0
  }
}
```

---

## CSRF Protection

**Security Level**: High (P0-5)

### How It Works

1. **Session-Based Tokens**: Unique token per session
2. **Double-Submit Pattern**: Token sent via cookie + validated in header
3. **Constant-Time Comparison**: Prevents timing attacks
4. **Automatic Exemption**: Webhooks and lifecycle endpoints bypass CSRF (use signatures instead)

### Scope

- **CSRF Protected**: Custom browser-based endpoints (POST/PUT/DELETE/PATCH)
- **Webhook Endpoints**: `/webhook` and `/lifecycle` (signature-protected, not CSRF)
- **Safe Methods**: GET, HEAD, OPTIONS, TRACE (exempt)

### Configuration

CSRF protection is **automatic and always enabled**. No configuration needed.

### Browser Integration

```html
<!-- JavaScript: Get CSRF token from cookie -->
<script>
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}

// Send token in header
const token = getCookie('clockify-addon-csrf');
fetch('/api/settings/save', {
  method: 'POST',
  headers: {
    'X-CSRF-Token': token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({...})
});
</script>
```

### Webhook Signature Protection

Webhooks do NOT need CSRF tokens - they use HMAC-SHA256 signatures:

```java
@PostMapping("/webhook")
public HttpResponse handleWebhook(HttpServletRequest request) {
    // 1. Extract signature from header
    String signature = request.getHeader("clockify-webhook-signature");
    
    // 2. Verify signature (built-in, automatic)
    WebhookSignatureValidator.VerificationResult result = 
        validator.verify(request.getInputStream(), signature);
    
    if (!result.isValid()) {
        return HttpResponse.error(result.getStatusCode(), "Invalid signature");
    }
    
    // 3. Process webhook (no CSRF token needed)
    return processWebhook(request);
}
```

---

## Token Rotation

**Security Level**: High (P0-7)

### How It Works

1. **Grace Period**: Old token remains valid for 1 hour during rotation
2. **Zero-Downtime**: All instances can use old or new token during transition
3. **Automatic Expiry**: Old token expires after grace period
4. **Metadata**: Tracks rotation time and status

### Configuration

Token rotation is **enabled by default**. No configuration needed.

### Usage

```java
// Trigger token rotation (e.g., during rekey operation)
TokenStore.rotate("workspace-123", "new-token-value");

// During grace period (1 hour), both tokens are valid:
assertTrue(TokenStore.isValidToken("workspace-123", "old-token"));  // true
assertTrue(TokenStore.isValidToken("workspace-123", "new-token"));  // true

// After grace period expires:
assertFalse(TokenStore.isValidToken("workspace-123", "old-token")); // false
assertTrue(TokenStore.isValidToken("workspace-123", "new-token"));  // true
```

### Deployment Pattern

```bash
# 1. Rotate token in staging environment (grace period: 1 hour)
curl -X POST https://addon-staging.example.com/admin/rotate-token \
  -H "Authorization: Bearer <admin-token>" \
  -d "workspaceId=workspace-123&newToken=new-token-value"

# 2. Wait 30 seconds for all instances to notice new token
sleep 30

# 3. Verify both tokens work (grace period active)
# ... test requests ...

# 4. Rotate token in production (same grace period)
curl -X POST https://addon.example.com/admin/rotate-token \
  -H "Authorization: Bearer <admin-token>" \
  -d "workspaceId=workspace-123&newToken=new-token-value"

# 5. After grace period (1 hour), old token automatically expires
# No additional action needed
```

---

## Request Size Limits

**Security Level**: Medium (P0-10)

### How It Works

1. **Limit**: 10MB per request (default, configurable)
2. **Two-Level Validation**: 
   - Header check (fast): `Content-Length` header
   - Stream check (accurate): Actual bytes read
3. **Returns 413**: "Payload Too Large" for oversized requests

### Configuration

```bash
# Development (larger payloads for testing)
export MAX_REQUEST_SIZE_MB=100

# Production (limit to expected webhook size)
export MAX_REQUEST_SIZE_MB=50

# Very strict (e.g., no file uploads)
export MAX_REQUEST_SIZE_MB=1
```

### Monitoring

Large request rejections appear in logs and audit trail:

```json
{
  "timestamp": "2025-11-10T10:30:45.123Z",
  "event": "INVALID_PAYLOAD_SIZE",
  "level": "WARN",
  "clientIp": "192.168.1.1",
  "details": {
    "maxSize": "10485760",
    "requestSize": "52428800"
  }
}
```

---

## Audit Logging

**Security Level**: High (P0-12)

### How It Works

1. **JSON Format**: Structured output for log aggregation systems
2. **Security Events**: Logs all auth, rate limit, CSRF, and error events
3. **Context**: Includes workspace, IP, path, and error details
4. **Compliance-Ready**: PCI DSS, SOC 2, HIPAA audit requirements

### Configuration

Audit logs are written to the `com.clockify.addon.audit` logger (SLF4J).

```xml
<!-- logback.xml: Route audit logs separately -->
<logger name="com.clockify.addon.audit" level="INFO">
    <appender-ref ref="AUDIT_FILE"/>
</logger>
```

### Event Types

```
TOKEN_VALIDATION_SUCCESS       - Token validation succeeded
TOKEN_VALIDATION_FAILURE       - Token validation failed
TOKEN_SAVED                    - Token saved to storage
TOKEN_ROTATED                  - Token rotated
TOKEN_REMOVED                  - Token removed
TOKEN_LOOKUP_FAILURE           - Token lookup failed

RATE_LIMIT_EXCEEDED            - Rate limit enforcement triggered
RATE_LIMIT_ENFORCED            - Rate limit was applied

CSRF_TOKEN_GENERATED           - CSRF token created for session
CSRF_TOKEN_VALIDATED           - CSRF token validated successfully
CSRF_TOKEN_INVALID             - CSRF token validation failed

INVALID_EVENT_TYPE             - Webhook event type validation failed
INVALID_PAYLOAD_SIZE           - Request payload exceeds size limit
INVALID_JSON                   - Malformed JSON payload

INSECURE_CONNECTION_REJECTED   - Non-HTTPS request rejected
DATABASE_CONNECTION_ERROR      - Database connection failed
DATABASE_QUERY_ERROR           - Database query failed

SUSPICIOUS_REQUEST             - Suspicious request detected
MULTIPLE_AUTH_FAILURES         - Multiple failed authentications from IP
```

### Example Audit Log Entry

```json
{
  "timestamp": "2025-11-10T10:30:45.123Z",
  "event": "RATE_LIMIT_EXCEEDED",
  "level": "ERROR",
  "workspace": "workspace-123",
  "clientIp": "192.168.1.1",
  "userId": "user-456",
  "details": {
    "path": "/webhook",
    "method": "POST",
    "limit_permits_sec": 1.0
  }
}
```

### Integration with Log Aggregation

**ELK Stack**:
```json
# Elasticsearch mapping
{
  "properties": {
    "timestamp": {"type": "date"},
    "event": {"type": "keyword"},
    "level": {"type": "keyword"},
    "workspace": {"type": "keyword"},
    "clientIp": {"type": "ip"},
    "details": {"type": "nested"}
  }
}

# Kibana Dashboard
- Filter by event=RATE_LIMIT_EXCEEDED
- Group by workspace
- Show top IPs
```

**Splunk**:
```
index=addon-audit event=RATE_LIMIT_EXCEEDED | stats count by workspace
```

**Datadog**:
```
@event:"RATE_LIMIT_EXCEEDED" @workspace:workspace-123 | stats count
```

---

## Database Token Storage

**Security Level**: High (P0-11)

### How It Works

1. **Persistent Storage**: Tokens survive application restarts
2. **Connection Pooling**: HikariCP with leak detection
3. **Prepared Statements**: SQL injection prevention
4. **Automatic Schema**: Creates tables on first startup

### Configuration

```bash
# Enable database token storage (production recommended)
export DB_URL=postgresql://localhost:5432/addon_db
export DB_USERNAME=addon_user
export DB_PASSWORD=<strong-password>
export DB_POOL_SIZE=10  # Optional: default is 10

# Without DB_* variables, tokens use InMemoryTokenStore (development only)
```

### Supported Databases

- **PostgreSQL 10+**: Recommended for production
- **MySQL 5.7+**: Also supported
- **H2** (for testing): In-memory or file-based

### Schema

```sql
-- Automatically created by addon on startup
CREATE TABLE addon_workspace_token (
    workspace_id VARCHAR(255) PRIMARY KEY,
    token TEXT NOT NULL,
    api_base_url VARCHAR(1024),
    created_at BIGINT,
    last_accessed_at BIGINT,
    expires_at BIGINT
);
```

### Connection Pool Monitoring

```java
// Get pool statistics
PooledDatabaseTokenStore store = (PooledDatabaseTokenStore) tokenStore;
String stats = store.getPoolStats();
// Output: "Active: 5, Idle: 3, Total: 10, Waiting: 0"

// Monitor in metrics dashboard
store.recordPoolMetrics(micrometer.registry);
```

### Health Checks

```bash
# Verify database connectivity
curl http://addon:8080/health
# Returns: {"status":"UP","database":"connected",...}
```

---

## Monitoring & Alerts

### Key Metrics

```
webhook_errors_total{reason="invalid_event_type"}     # Event validation failures
webhook_errors_total{reason="invalid_json"}           # Malformed payloads
webhook_errors_total{reason="rate_limit_exceeded"}    # Rate limit hits (429s)
http_413_payload_too_large_total                      # Request size limit hits
csrf_validation_failures_total                        # CSRF token failures
https_enforcement_failures_total                      # Non-HTTPS rejections (if enforced)
token_rotation_total                                  # Token rotations
database_pool_active_connections                      # Active DB connections
```

### Alert Thresholds

```
webhook_errors_total{reason="rate_limit_exceeded"} > 10/minute
  → Investigate: Traffic spike or misconfiguration

csrf_validation_failures_total > 5/minute
  → Check: Custom endpoint security, JavaScript errors

webhook_errors_total{reason="invalid_event_type"} > 5/minute
  → Review: Webhook sender configuration

database_pool_active_connections > pool_size * 0.8
  → Scale: Increase pool size or optimize queries
```

### Health Endpoint

```bash
# Check addon health
curl http://addon:8080/health

# Response:
{
  "status": "UP",
  "database": "connected",
  "memory": {
    "heapUsedMB": 256,
    "heapMaxMB": 2048
  },
  "uptime_seconds": 3600,
  "version": "1.0.0"
}
```

### Log Aggregation Setup

**Docker Compose**:
```yaml
version: '3'
services:
  addon:
    image: my-addon:latest
    environment:
      ENFORCE_HTTPS: 'true'
      DB_URL: 'postgresql://postgres:5432/addon'
    logging:
      driver: 'json-file'
      options:
        max-size: '10m'
        max-file: '3'
    
  logstash:
    image: docker.elastic.co/logstash/logstash:7.16.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5000:5000/udp"

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.16.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"

  kibana:
    image: docker.elastic.co/kibana/kibana:7.16.0
    ports:
      - "5601:5601"
```

---

## Summary

| Feature | Status | Config | Impact |
|---|---|---|---|
| HTTPS Enforcement | ✅ Active | `ENFORCE_HTTPS` | Encryption guaranteed |
| JWT Signature Bypass | ✅ Blocked | `ADDON_ACCEPT_JWT_SIGNATURE=false` | Webhook spoofing prevented |
| Rate Limiting | ✅ Active | Auto | DoS protection |
| CSRF Protection | ✅ Active | Auto | Browser attack prevention |
| Token Rotation | ✅ Active | Auto | Zero-downtime updates |
| Request Size Limits | ✅ Active | `MAX_REQUEST_SIZE_MB` | Memory exhaustion prevented |
| Audit Logging | ✅ Active | SLF4J | Compliance ready |
| DB Connection Pooling | ✅ Active | `DB_*` | Performance at scale |

---

## Support

For security issues or questions:
1. Review logs and audit trail
2. Check health endpoint: `GET /health`
3. Consult deployment documentation
4. Contact support with audit logs (sanitized)

---

**Remember**: Security is a shared responsibility. Always:
- Keep dependencies updated
- Monitor audit logs regularly
- Test before production deployment
- Follow security checklist for your environment
- Rotate tokens periodically (at least quarterly)

