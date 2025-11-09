# File-by-File Documentation Index

Complete file-level analysis and documentation of every major file in the codebase.

## Navigation

### SDK Core Files
- [ClockifyAddon.java](./files/ClockifyAddon.md) - Central coordinator class
- [AddonServlet.java](./files/AddonServlet.md) - HTTP request router
- [ClockifyManifest.java](./files/ClockifyManifest.md) - Manifest builder
- [EmbeddedServer.java](./files/EmbeddedServer.md) - Jetty server wrapper
- [RequestHandler.java](./files/RequestHandler.md) - Handler interface
- [HttpResponse.java](./files/HttpResponse.md) - Response helper

### Security Files
- [WebhookSignatureValidator.java](./files/WebhookSignatureValidator.md) - HMAC/JWT validation
- [TokenStore.java](./files/TokenStore.md) - In-memory token storage
- [DatabaseTokenStore.java](./files/DatabaseTokenStore.md) - Persistent token storage

### Middleware Files
- [SecurityHeadersFilter.java](./files/SecurityHeadersFilter.md) - Security headers
- [RateLimiter.java](./files/RateLimiter.md) - Rate limiting
- [CorsFilter.java](./files/CorsFilter.md) - CORS handling
- [RequestLoggingFilter.java](./files/RequestLoggingFilter.md) - Request logging

### HTTP Client
- [ClockifyHttpClient.java](./files/ClockifyHttpClient.md) - Clockify API client

### Health & Metrics
- [HealthCheck.java](./files/HealthCheck.md) - Health check endpoint
- [MetricsHandler.java](./files/MetricsHandler.md) - Prometheus metrics

### Utilities
- [PathSanitizer.java](./files/PathSanitizer.md) - URL path normalization
- [ConfigValidator.java](./files/ConfigValidator.md) - Configuration validation
- [BaseUrlDetector.java](./files/BaseUrlDetector.md) - URL parsing

### Rules Addon Files
- [RulesApp.java](./files/RulesApp.md) - Main entry point
- [Rule.java](./files/Rule.md) - Rule data model
- [Evaluator.java](./files/Evaluator.md) - Rule evaluation engine
- [RulesStore.java](./files/RulesStore.md) - In-memory rules storage
- [DatabaseRulesStore.java](./files/DatabaseRulesStore.md) - Persistent rules storage
- [WorkspaceCache.java](./files/WorkspaceCache.md) - Workspace data caching

### Auto-Tag Assistant Files
- [AutoTagAssistantApp.java](./files/AutoTagAssistantApp.md) - Main entry point
- [WebhookHandlers.java](./files/AutoTagWebhookHandlers.md) - Webhook processing

### Template Addon Files
- [TemplateAddonApp.java](./files/TemplateAddonApp.md) - Template entry point

---

## File Categories

### By Component Type

**Core Framework (SDK)**
- 15 core files
- 8 security files
- 4 middleware files
- 5 utility files

**Rules Addon**
- 25+ files
- Engine: 7 files
- Store: 3 files
- Cache: 1 file
- Controllers: 5 files

**Auto-Tag Assistant**
- 5 files
- Simple webhook processing

**Template Addon**
- 6 files
- Minimal starter

### By Functionality

**HTTP Layer**
- AddonServlet.java - Request routing
- EmbeddedServer.java - Server management
- HttpResponse.java - Response wrapper
- RequestHandler.java - Handler interface

**Security Layer**
- WebhookSignatureValidator.java - HMAC/JWT validation
- TokenStore.java - Token management
- SecurityHeadersFilter.java - Security headers

**Business Logic**
- RulesApp.java - Rules addon
- AutoTagAssistantApp.java - Auto-tag addon
- Evaluator.java - Rule evaluation

**Data Persistence**
- DatabaseTokenStore.java - Token persistence
- DatabaseRulesStore.java - Rules persistence
- Flyway migrations - Schema management

---

## File Size Distribution

| Size Range | Count | Examples |
|------------|-------|----------|
| < 100 lines | 15 | HttpResponse.java, RequestHandler.java |
| 100-300 lines | 25 | TokenStore.java, ClockifyAddon.java |
| 300-500 lines | 10 | AddonServlet.java, RulesApp.java |
| 500+ lines | 5 | DatabaseRulesStore.java, Evaluator.java |

---

## Critical Files

Files that are essential to understanding the system:

1. **ClockifyAddon.java** - Central coordinator, start here
2. **AddonServlet.java** - Request routing logic
3. **WebhookSignatureValidator.java** - Security implementation
4. **RulesApp.java** - Complete addon example
5. **DatabaseTokenStore.java** - Database integration pattern

---

## File Dependencies Graph

```
ClockifyAddon
  ├── ClockifyManifest
  ├── RequestHandler (interface)
  └── PathSanitizer

AddonServlet
  ├── ClockifyAddon
  ├── RequestHandler
  ├── HttpResponse
  └── MetricsHandler

EmbeddedServer
  ├── AddonServlet
  └── Filter (middleware)

RulesApp
  ├── ClockifyAddon
  ├── ClockifyManifest
  ├── RulesStore / DatabaseRulesStore
  ├── All handlers (Lifecycle, Webhook)
  └── All middleware

WebhookSignatureValidator
  ├── TokenStore
  └── HttpResponse
```

---

## Usage Patterns

### Pattern 1: Create Simple Addon

```
1. Read: ClockifyAddon.java
2. Read: ClockifyManifest.java
3. Read: TemplateAddonApp.java
4. Implement: Your addon class
```

### Pattern 2: Add Database Support

```
1. Read: DatabaseTokenStore.java
2. Read: DatabaseRulesStore.java
3. Setup: Flyway migrations
4. Implement: Your database store
```

### Pattern 3: Add Security

```
1. Read: WebhookSignatureValidator.java
2. Read: SecurityHeadersFilter.java
3. Read: RateLimiter.java
4. Configure: Environment variables
```

---

## Next Steps

1. Browse individual file documentation in `./files/`
2. Study critical files first
3. Follow usage patterns for your use case
4. Reference code examples in each file doc

---

**Generated:** 2025-11-09 | **Version:** 1.0.0
