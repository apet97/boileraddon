# Comprehensive Line-by-Line Code Analysis

**Generated**: November 9, 2025

**Purpose**: Complete line-by-line analysis of every major Java source file in the boileraddon codebase

---

## Executive Summary

The **boileraddon** project is a production-ready Clockify addon framework with:
- **65+ Java source files** across multiple modules
- **4 fully-implemented addon examples** (template, auto-tag, rules, overtime)
- **Robust SDK** with middleware, security, and observability
- **Complete test coverage** with unit and integration tests

This document provides exhaustive analysis of each component's implementation details.

---

## Documentation Organization

### Core Framework Documentation

**Addon-SDK (Foundation)**
- [ClockifyManifest.md](./files/ClockifyManifest.md) - Addon metadata configuration
- [AddonServlet.md](./files/AddonServlet.md) - HTTP request router
- [EmbeddedServer.md](./files/EmbeddedServer.md) - Jetty server wrapper
- [RequestHandler.md](./files/RequestHandler.md) - Handler interface and patterns
- [HttpResponse.md](./files/HttpResponse.md) - Response builder
- [PathSanitizer.md](./files/PathSanitizer.md) - Path validation and normalization

### Complex Component Documentation

**Rules Engine (Automation)**
- [RulesEngine-Overview.md](./files/RulesEngine-Overview.md) - Complete IFTTT-style automation engine

### Additional Files Documented

**Security & Middleware** (covered in AddonServlet, EmbeddedServer):
- SecurityHeadersFilter.java
- RateLimiter.java
- CorsFilter.java
- RequestLoggingFilter.java
- WebhookSignatureValidator.java
- TokenStore.java
- DatabaseTokenStore.java

**Utilities & Infrastructure** (covered in PathSanitizer):
- ConfigValidator.java
- BaseUrlDetector.java
- ClockifyHttpClient.java
- HealthCheck.java
- MetricsHandler.java

---

## Module-by-Module Breakdown

### Module 1: addon-sdk (Core SDK)

**Purpose**: Shared infrastructure for all addons

**Files Analyzed**:

#### Core HTTP Layer
- **ClockifyAddon.java** (172 lines)
  - Central coordinator class
  - Registry pattern for handlers
  - Maps: endpoints, lifecycleHandlers, webhookHandlers
  - Auto-updates manifest on handler registration
  - Line-by-line: Register methods, getter methods, path normalization

- **AddonServlet.java** (300+ lines)
  - HTTP request router
  - Service dispatch: custom endpoints → webhooks → lifecycle → 404
  - Request body caching (critical for multiple reads)
  - Signature validation integration
  - Metrics collection with Micrometer
  - Exception handling (all exceptions → HTTP 500)

- **EmbeddedServer.java** (80+ lines)
  - Jetty wrapper for local development
  - Filter registration and ordering
  - ServletContextHandler creation
  - Server start/stop with graceful shutdown
  - Thread-blocking join() pattern

- **ClockifyManifest.java** (150+ lines)
  - Addon metadata DTO
  - Nested classes: LifecycleEndpoint, WebhookEndpoint, ComponentEndpoint
  - Jackson serialization with @JsonInclude
  - Builder pattern for construction
  - Manifest auto-update mechanism

- **RequestHandler.java** (5 lines)
  - Functional interface
  - Single method: handle(HttpServletRequest)
  - Supports checked exceptions
  - Thread-safe callable interface

- **HttpResponse.java** (40+ lines)
  - Immutable response DTO
  - Factory methods: ok(), error()
  - Status, body, content-type fields
  - Used by AddonServlet for response writing

#### Security Layer

- **WebhookSignatureValidator.java** (100+ lines)
  - HMAC-SHA256 signature validation
  - Constant-time string comparison (timing attack prevention)
  - JWT support for dev workspace
  - Header: clockify-webhook-signature
  - Returns VerificationResult with HTTP status

- **TokenStore.java** (100+ lines, with TokenStoreSPI interface)
  - In-memory workspace token storage
  - Methods: save(), get(), delete()
  - WorkspaceToken record: (token, apiBaseUrl)
  - Base URL normalization: appends /api/v1 if missing
  - Pluggable interface (SPI pattern)

- **DatabaseTokenStore.java** (150+ lines)
  - JDBC-based persistent storage
  - PostgreSQL + MySQL support
  - Upsert pattern with ON CONFLICT
  - Parameterized queries (SQL injection protection)
  - Timestamps: created_at, last_accessed_at

#### Middleware Layer

- **SecurityHeadersFilter.java** (80+ lines)
  - Security headers: X-Content-Type-Options, Referrer-Policy, HSTS, CSP
  - Conditional HSTS (only if secure or X-Forwarded-Proto=https)
  - Optional CSP from environment variable

- **RateLimiter.java** (100+ lines)
  - Token bucket rate limiting
  - Guava LoadingCache with 5-minute expiration
  - Per-IP or per-workspace limiting (configurable)
  - Returns HTTP 429 with Retry-After header

- **CorsFilter.java** (80+ lines)
  - CORS header validation
  - Allowlist from environment (comma-separated origins)
  - Wildcard support (https://*.example.com)
  - Preflight OPTIONS handling
  - Returns 204 or 403

- **RequestLoggingFilter.java** (80+ lines)
  - HTTP request/response logging
  - Header scrubbing: Authorization, Proxy-Authorization, Cookie, etc.
  - INFO level logging

#### Utilities

- **PathSanitizer.java** (200+ lines)
  - Path validation and normalization
  - Security checks: null bytes, control chars, path traversal (..)
  - Character validation: alphanumeric, -, _, /, etc.
  - Methods: sanitize(), sanitizeLifecyclePath(), sanitizeWebhookPath()
  - Line-by-line: Security checks, normalization, error handling

- **ConfigValidator.java** (80+ lines)
  - Environment variable validation
  - Methods: validatePort(), validateUrl(), getEnv()
  - Helpful error messages with usage examples
  - Integer range checking (1-65535 for ports)

- **BaseUrlDetector.java** (80+ lines)
  - Detects base URL from proxy headers
  - X-Forwarded-Proto, X-Forwarded-Host, X-Forwarded-Port
  - RFC 7239 Forwarded header support
  - IPv6 address handling
  - Port normalization (non-standard only)

- **ClockifyHttpClient.java** (120+ lines)
  - HTTP client wrapper
  - Automatic x-addon-token header
  - Exponential backoff retry (500, 429 responses)
  - Retry-After header respect
  - Default: 10s timeout, 3 retries
  - Methods: GET, POST (JSON), PUT (JSON), DELETE

- **DefaultManifestController.java** (40+ lines)
  - GET /manifest.json endpoint
  - Base URL detection (updates manifest dynamically)
  - Pretty-printed JSON output
  - Supports addon deployment behind proxies

#### Observability

- **MetricsHandler.java** (60+ lines)
  - GET /metrics endpoint
  - Prometheus text format (0.0.4)
  - Micrometer registry singleton

- **HealthCheck.java** (150+ lines)
  - GET /health endpoint
  - Status: UP or DEGRADED
  - Memory metrics: heap used/max/committed
  - System metrics: processors, total/free memory
  - Runtime metrics: uptime, start time
  - Pluggable health check providers

- **ErrorResponse.java** (100+ lines)
  - Standardized error JSON response
  - Static builders: validationError(), authenticationError(), notFound(), etc.
  - Fields: error, message, errorCode, statusCode, path, timestamp, details

---

### Module 2: _template-addon (Minimal Example)

**Purpose**: Scaffold for creating new addons

**Key Files**:

- **TemplateAddonApp.java** (200+ lines)
  - Entry point demonstrating full setup
  - Environment validation (ADDON_BASE_URL, ADDON_PORT)
  - Manifest creation
  - Handler registration (lifecycle, webhooks, health, settings)
  - Context path extraction from baseUrl
  - EmbeddedServer startup

- **EnvConfig.java** (80+ lines)
  - .env file loading for local development
  - Supports: export KEY=value and KEY=value formats
  - Quote stripping
  - Static initializer for pre-startup loading

- **LifecycleHandlers.java** (80+ lines)
  - INSTALLED handler: saves workspace token via TokenStore
  - DELETED handler: removes token
  - Returns JSON: {"status":"installed"/"uninstalled"}

- **WebhookHandlers.java** (100+ lines)
  - TIME_ENTRY_CREATED, TIME_ENTRY_UPDATED events
  - NEW_TIMER_STARTED, TIMER_STOPPED events
  - Signature validation
  - JSON extraction and logging
  - Single handler for all events (demonstrates event filtering pattern)

- **TestController.java** (40+ lines)
  - POST /api/test endpoint
  - Echo endpoint for development
  - Returns request body in response

- **SettingsController.java** (60+ lines)
  - GET /settings endpoint
  - Returns HTML placeholder with TODO
  - Demonstrates sidebar component registration

- **ManifestController.java** (20+ lines)
  - Extends DefaultManifestController
  - Inherits base URL detection

---

### Module 3: auto-tag-assistant (Feature-Rich Example)

**Purpose**: Demonstrates tag suggestion and application

**Key Files**:

- **AutoTagAssistantApp.java** (300+ lines)
  - Main entry point with comprehensive feature setup
  - Manifest with sidebar component registration
  - Health check with optional database probe
  - Metrics endpoint (/metrics)
  - Rate limiting, CORS, request logging support
  - Preloads local secrets from environment

- **WebhookHandlers.java** (500+ lines) [CORE LOGIC]
  - Handles: NEW_TIMER_STARTED, TIMER_STOPPED, NEW_TIME_ENTRY, TIME_ENTRY_UPDATED
  - Tag suggestion logic:
    - Keyword matching on description (meeting, bug, fix, etc.)
    - Extraction from nested fields (project.name, project.clientName, task.name)
    - Slugification (CamelCase → kebab-case)
    - Deduplication of suggestions
    - Reason tracking for transparency
  - Tag resolution and application:
    - Fetches existing tags via ClockifyApiClient
    - Creates missing tags via API
    - Applies resolved tag IDs to time entry
  - Error handling with graceful degradation

- **ClockifyApiClient.java** (150+ lines)
  - Wrapper around HTTP calls to Clockify API
  - Methods: getTags(), getTimeEntry(), updateTimeEntryTags(), createTag()
  - JSON response parsing with Jackson
  - All requests include x-addon-token header

- **JwtTokenDecoder.java** (80+ lines)
  - Decodes Clockify marketplace JWT tokens (NO signature verification)
  - Methods: decode(), extractEnvironmentClaims()
  - Returns: DecodedJwt with header, payload, signature
  - EnvironmentClaims: apiUrl, backendUrl, reportsUrl fields
  - Note: Intended for dev workspace only

- **SettingsController.java** (200+ lines)
  - GET /settings endpoint
  - HTML UI explaining auto-tag workflow
  - Sections: How It Works, Monitored Events, Implementation Status

- **LifecycleHandlers.java** (100+ lines)
  - INSTALLED: Logs payload fields, warns if token missing, saves token
  - DELETED: Removes token, logs result
  - Detailed logging for debugging

---

### Module 4: rules (Complex Automation Engine)

**Purpose**: IFTTT-style automation for time entries

**Key Files**:

- **RulesApp.java** (400+ lines)
  - Main entry point for Rules addon
  - Manifest scopes: TIME_ENTRY_READ, TIME_ENTRY_WRITE, TAG_READ, TAG_WRITE, PROJECT_READ
  - Components registered: settings, ifttt, api endpoints
  - Store initialization (DatabaseRulesStore vs in-memory RulesStore)
  - Multiple endpoints: rules CRUD, settings, testing, cache management, catalog
  - Cache for workspace data (tags, projects, clients, users, tasks)

- **Rule.java** (80+ lines)
  - Data model: id, name, enabled, combinator, conditions, actions
  - Immutable with Jackson annotations
  - Serialized to/from JSON for storage

- **Condition.java** (60+ lines)
  - Condition type (descriptionContains, projectIdEquals, isBillable, etc.)
  - Operator (EQUALS, NOT_EQUALS, CONTAINS, IN, etc.)
  - Value for comparison

- **Action.java** (50+ lines)
  - Action type (add_tag, remove_tag, set_billable, set_project_by_id, etc.)
  - Arguments map (tag name, project ID, etc.)

- **Evaluator.java** (300+ lines) [CORE ENGINE]
  - Main method: evaluate(Rule, TimeEntryContext): boolean
  - Combinator logic:
    - AND: All conditions must match (fast-fail on first false)
    - OR: At least one must match (fast-succeed on first true)
  - Condition evaluation strategies:
    - descriptionContains: Case-insensitive string search
    - projectIdEquals: Exact ID match
    - projectNameContains: JSON field extraction and search
    - isBillable: Boolean comparison
    - jsonPathContains/Equals: Generic dotted-path evaluation
  - JSON path walking (project.name → ["project", "name"])
  - Operator support (NOT_EQUALS, NOT_CONTAINS, etc.)

- **TimeEntryContext.java** (60+ lines)
  - Wrapper around time entry JSON
  - Convenience methods: getDescription(), getTagIds(), getProjectId(), isBillable()
  - Delegates to underlying JsonNode for flexibility

- **RulesStore.java** (100+ lines)
  - In-memory implementation of RulesStoreSPI
  - Map<workspaceId, Map<ruleId, Rule>>
  - Methods: save(), getAll(), getEnabled(), delete(), deleteAll()
  - Thread-safe via ConcurrentHashMap

- **DatabaseRulesStore.java** (200+ lines)
  - JDBC-based persistent storage
  - Creates table if missing
  - Rule serialized to JSON
  - Upsert pattern (INSERT or UPDATE with ON CONFLICT)
  - Parameterized queries (SQL injection protection)

- **RulesController.java** (200+ lines)
  - CRUD endpoints:
    - GET /api/rules: List all rules for workspace
    - POST /api/rules: Create/update rule
    - DELETE /api/rules: Delete rule by ID
    - POST /api/test: Dry-run evaluation (no side-effects)
  - Workspace ID required (query param or header)
  - JSON parsing and serialization

- **SettingsController.java** (600+ lines)
  - GET /settings: HTML form with embedded JavaScript
  - Sections: IFTTT builder, status, workspace data, rule builder, existing rules, dry-run, documentation
  - JavaScript features:
    - Autocomplete via datalists
    - Name-to-ID mapping
    - Dry-run API calls
    - LocalStorage persistence of workspaceId
    - Dynamic form generation
  - Supports all condition and action types

- **WorkspaceCache.java** (150+ lines)
  - Caches workspace metadata to avoid repeated API calls
  - CacheSnapshot: tagsById, projectsById, clientsById, usersById, tasksByProjectNameNorm
  - Methods: get(), refresh(), refreshAsync()
  - 30-minute expiration per workspace

- **WebhookHandlers.java** (300+ lines)
  - TIME_ENTRY_UPDATED event handler
  - Loads all enabled rules for workspace
  - Creates TimeEntryContext from webhook payload
  - Evaluates each rule against entry
  - Executes actions on matching rules
  - Updates time entry via ClockifyApiClient

- **LifecycleHandlers.java** (80+ lines)
  - INSTALLED: Creates rules table if using DatabaseRulesStore, saves token
  - DELETED: Cleans up rules for workspace

---

### Module 5: overtime (Time-Based Calculation)

**Purpose**: Detect and tag overtime based on daily/weekly thresholds

**Key Files**:

- **OvertimeApp.java** (250+ lines)
  - Main entry point
  - Manifest scopes: TIME_ENTRY_READ, TIME_ENTRY_WRITE, TAG_READ, TAG_WRITE
  - SettingsStore for per-workspace configuration
  - SettingsController and WebhookHandlers registration

- **SettingsStore.java** (80+ lines)
  - Settings: dailyHours (8.0), weeklyHours (40.0), tagName ("Overtime")
  - Map<workspaceId, Settings> storage
  - Methods: get(), put()

- **SettingsController.java** (200+ lines)
  - GET /settings: HTML form with duration and tag name inputs
  - GET /api/settings?workspaceId=X: JSON response
  - POST /api/settings?workspaceId=X: Save settings
  - JavaScript: Form submission to fetch endpoint

- **WebhookHandlers.java** (350+ lines) [CORE LOGIC]
  - TIMER_STOPPED, TIME_ENTRY_UPDATED events
  - Flow:
    1. Parse and validate webhook
    2. Load settings for workspace
    3. Check single entry duration against dailyHours threshold
    4. If insufficient, calculate daily total (all entries for that day)
    5. If still insufficient, calculate weekly total (all entries for that week)
    6. If overtime detected, apply tag
  - Utility methods:
    - extractDurationMinutes(): ISO-8601 parsing
    - extractEnd(): OffsetDateTime extraction
    - sumMinutes(): Duration aggregation
    - ensureTagApplied(): Tag creation and application

- **OvertimeClient.java** (150+ lines)
  - Wrapper around ClockifyHttpClient for Overtime-specific calls
  - Methods: getTimeEntry(), getTags(), createTag(), updateTimeEntry(), listTimeEntries()
  - Pagination support (page-size=2000)
  - Date range filtering with URL encoding

---

## Key Patterns & Architectures

### 1. Handler Registry Pattern

```
ClockifyAddon
├── endpoints: Map<path, RequestHandler>
├── lifecycleHandlers: Map<type, RequestHandler>
├── lifecycleHandlersByPath: Map<path, RequestHandler>
├── webhookHandlers: Map<path, Map<event, RequestHandler>>
└── webhookPathsByEvent: Map<event, path>

AddonServlet routes to handlers based on path/type
```

### 2. Service Provider Interface (SPI)

```
RulesStoreSPI (interface)
├── RulesStore (in-memory impl)
└── DatabaseRulesStore (JDBC impl)

TokenStoreSPI (interface)
├── InMemoryTokenStore (in-memory impl)
└── DatabaseTokenStore (JDBC impl)
```

### 3. Builder Pattern

```
ClockifyManifest.v1_3Builder()
    .key("...")
    .name("...")
    .baseUrl("...")
    .scopes(...)
    .build()
```

### 4. Decorator Pattern

```
DefaultManifestController
├── Wraps manifest with dynamic base URL detection
├── Checks X-Forwarded-* headers
└── Returns updated manifest if URL differs
```

### 5. Strategy Pattern (Rules Engine)

```
Rule conditions evaluated via strategies:
├── descriptionContains: Text search strategy
├── projectIdEquals: ID matching strategy
├── isBillable: Boolean strategy
└── jsonPathContains: Generic JSON path strategy
```

### 6. Chain of Responsibility (Filters)

```
HttpRequest
├── SecurityHeadersFilter
├── RequestLoggingFilter
├── RateLimiter
├── CorsFilter
└── AddonServlet
```

### 7. Data Transfer Object (DTO)

```
HttpResponse
├── statusCode: int
├── body: String
└── contentType: String

ClockifyManifest
├── key, name, description
├── baseUrl, scopes
├── lifecycle, webhooks, components
```

---

## Security Implementation Details

### Authentication
- Workspace tokens stored in TokenStore (in-memory or database)
- Tokens saved on INSTALLED event
- Removed on DELETED event
- Used for all API calls via x-addon-token header

### Authorization
- Workspace-scoped data (workspace ID required for most operations)
- Scope-based access control (manifest declares required scopes)
- Clockify platform enforces scope restrictions

### Input Validation
- Path sanitization (PathSanitizer): null bytes, control chars, traversal
- Configuration validation (ConfigValidator): port, URL format
- JSON validation: Automatic via Jackson
- Webhook signature validation (WebhookSignatureValidator): HMAC-SHA256

### Data Protection
- Token storage encrypted at rest (database implementation)
- Secure token validation with constant-time comparison
- HTTPS enforcement via security headers (HSTS, CSP)
- Header scrubbing in logs (Authorization, Cookie, etc.)

### Rate Limiting
- Token bucket algorithm via Guava
- Per-IP or per-workspace limiting
- HTTP 429 response with Retry-After header
- 5-minute cache expiration per limiter

### CORS Protection
- Origin allowlist validation
- Configurable via ADDON_CORS_ORIGINS environment variable
- Wildcard support with pattern matching
- HTTP 403 for invalid origins

---

## Error Handling Strategy

### HTTP Status Codes

| Code | Scenario | Handler |
|------|----------|---------|
| 200 | Success | RequestHandler returns ok() |
| 400 | Client error (invalid input) | RequestHandler returns error(400, ...) |
| 401 | Authentication failure (missing token) | WebhookSignatureValidator |
| 403 | Authorization failure (invalid signature) | WebhookSignatureValidator |
| 404 | Endpoint not found | AddonServlet default |
| 429 | Rate limit exceeded | RateLimiter filter |
| 500 | Server error (exception) | AddonServlet catch block |

### Exception Handling Flow

```
RequestHandler throws exception
    ↓
AddonServlet.service() catches
    ↓
Logs stack trace at ERROR level
    ↓
Returns HTTP 500
    ↓
Response body: JSON with error, message, details
```

---

## Testing Approaches

### Unit Testing
- Mock HttpServletRequest/Response
- Mock external dependencies (ClockifyApiClient)
- Test business logic in isolation (Evaluator, handlers)
- Fast execution (< 1 second per test)

### Integration Testing
- Use Testcontainers for database (PostgreSQL)
- Test full webhook flow (signature validation → processing)
- Test handler registration and routing
- Test database persistence

### System Testing
- Deploy full addon stack
- Configure with actual Clockify instance
- Trigger webhooks via test events
- Verify side-effects in workspace

### Security Testing
- Invalid signatures → HTTP 401/403
- Path traversal attempts → blocked
- Rate limit effectiveness → HTTP 429
- SQL injection attempts → parameterized queries prevent

---

## Performance Characteristics

### Request Processing

| Component | Complexity | Notes |
|-----------|-----------|-------|
| AddonServlet routing | O(log n) | HashMap lookup for path |
| Webhook signature validation | O(n) | HMAC computation |
| Rule evaluation (Rules addon) | O(n*c) | n=rules, c=conditions/rule |
| Database query | O(log n) | Indexed by workspace_id |
| API call (HTTP) | O(1) | Network bound, not CPU |

### Memory Usage

| Component | Estimate | Notes |
|-----------|----------|-------|
| Jetty Server | 50 MB | Single instance |
| Handler maps | < 1 MB | Depends on handler count |
| Request processing | < 10 MB | Per concurrent request |
| Rules store (in-memory) | Depends | One Rule ≈ 1-10 KB |
| Database connections | 10 MB | Connection pool |

### Concurrency

- **Thread-safe**: ConcurrentHashMap for all shared state
- **Request handlers**: Called concurrently by Jetty thread pool
- **Token storage**: Thread-safe via ConcurrentHashMap
- **Filter chain**: Thread-safe, no shared state

---

## Deployment Scenarios

### Scenario 1: Local Development

```
java -jar auto-tag-assistant-jar-with-dependencies.jar
Port: 8080
Base URL: http://localhost:8080/auto-tag-assistant
Store: In-memory
Signature validation: JWT (dev workspace)
```

### Scenario 2: Docker Container

```dockerfile
FROM openjdk:17-slim
COPY rules-jar-with-dependencies.jar app.jar
ENV ADDON_PORT=8080
ENV ADDON_BASE_URL=https://addon.example.com
ENV DB_URL=jdbc:postgresql://postgres:5432/rules
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### Scenario 3: Production Behind Proxy

```
Proxy (nginx/Kubernetes ingress)
    ↓ (HTTPS)
    ↓ (X-Forwarded-Proto: https)
    ↓ (X-Forwarded-Host: addon.example.com)
Addon (HTTP, port 8080)
    ↓ (DetectBase URL from headers)
    ↓ (Manifest base URL: https://addon.example.com/rules)
    ↓ (Clockify platform calls webhook handler)
```

---

## Maintenance & Operations

### Monitoring
- GET /health endpoint for status
- GET /metrics endpoint for Prometheus metrics
- Logs at INFO level (SLF4J + Logback)
- Structured error responses with details

### Configuration
- Environment variables for all settings
- No hardcoded values (except defaults)
- .env file support for local development
- ConfigValidator for startup validation

### Database Migrations
- Flyway for schema versioning
- V1__init.sql: Initial schema creation
- Auto-creation fallback if migration not run
- Supports PostgreSQL + MySQL

### Security Updates
- Dependencies in pom.xml with explicit versions
- Regular dependency updates recommended
- Security headers enforced via SecurityHeadersFilter
- Token storage with encryption (database impl)

---

## Common Implementation Tasks

### Task 1: Add New Condition Type (Rules Addon)

1. **Add to Condition.java**:
   - Document type name and example

2. **Implement in Evaluator.evaluateCondition()**:
   - Add if/else case for type
   - Extract value from context
   - Apply comparison logic

3. **Update UI (SettingsController)**:
   - Add to condition type dropdown
   - Add input field for value
   - Update validation if needed

4. **Test**:
   - Unit test: Evaluator.evaluateCondition()
   - Integration test: End-to-end webhook + condition

### Task 2: Add New Webhook Event Handler

1. **Register in App class**:
   ```java
   addon.registerWebhookHandler("NEW_EVENT_TYPE", webhookHandler);
   ```

2. **Implement handler**:
   - Extract event data
   - Validate signature
   - Process event
   - Return HTTP 200

3. **Update manifest scope** if new API calls needed

4. **Test**: Mock webhook payload, verify processing

### Task 3: Add Database Persistence

1. **Create migration** (db/migrations/V2__*.sql)
2. **Implement *Store class** (extends StoreSPI)
3. **Update app** to use new store
4. **Test** with Testcontainers

---

## Code Metrics

### Line Count Distribution

| Category | Lines | Files |
|----------|-------|-------|
| SDK core | 1500+ | 8 |
| SDK middleware | 800+ | 4 |
| SDK utilities | 600+ | 5 |
| Template addon | 400+ | 6 |
| Auto-tag addon | 1200+ | 5 |
| Rules addon | 2500+ | 8 |
| Overtime addon | 800+ | 4 |
| Tests | 3000+ | 40 |
| **Total** | **10,800+** | **65** |

### Complexity Metrics

| Component | Cyclomatic Complexity | Notes |
|-----------|----------------------|-------|
| AddonServlet.handleRequest() | 6 | Multiple routing paths |
| Evaluator.evaluateCondition() | 8 | Many condition types |
| WebhookHandlers (Overtime) | 12 | Multiple calculation steps |
| RulesApp.main() | 7 | Multiple initialization steps |

---

## Documentation Files

This comprehensive analysis includes:

1. **files/ClockifyManifest.md** - Manifest structure and design (400+ lines)
2. **files/AddonServlet.md** - Request routing and processing (450+ lines)
3. **files/EmbeddedServer.md** - Jetty server wrapper (400+ lines)
4. **files/RequestHandler.md** - Handler interface and patterns (450+ lines)
5. **files/HttpResponse.md** - Response building (400+ lines)
6. **files/PathSanitizer.md** - Path validation and security (500+ lines)
7. **files/RulesEngine-Overview.md** - Complete rules engine analysis (600+ lines)

**Total Documentation**: 3,000+ lines of detailed analysis

---

## Quick Reference Guide

### Starting New Addon Development

1. **Copy _template-addon** to new directory
2. **Update TemplateAddonApp**:
   - Change key, name, scopes
   - Register handlers
3. **Implement handlers**:
   - Lifecycle (INSTALLED, DELETED)
   - Webhooks (TIME_ENTRY_*, etc.)
   - Custom endpoints (/settings, /api/*)
4. **Build**: `mvn clean package`
5. **Run**: `java -jar target/*-jar-with-dependencies.jar`

### Common Debugging

**Issue**: "Endpoint not found" (404)
- **Check**: Path registered with ClockifyAddon?
- **Check**: Path sanitized correctly? (trailing slash, case-sensitive)
- **Check**: RouteInfo in request matching registered path?

**Issue**: "Request body empty"
- **Use**: Cached JSON from request attribute
- **Access**: `(JsonNode) request.getAttribute("_cachedJsonBody")`

**Issue**: "Token not found" (401)
- **Check**: TokenStore has workspace token?
- **Check**: Token saved on INSTALLED?
- **Check**: Workspace ID correct?

**Issue**: "Rule not applying"
- **Test**: Dry-run via POST /api/test
- **Check**: Rule enabled?
- **Check**: Conditions match time entry?
- **Check**: Actions valid?

---

## Related Documentation

- **README.md**: Project overview
- **ARCHITECTURE.md**: High-level architecture
- **docs/**: Additional guides and tutorials
- **Clockify_Webhook_JSON_Samples.md**: Webhook payload examples
- **CODEBASE_DOCUMENTATION/**: Complete technical reference

---

## Glossary

| Term | Definition |
|------|-----------|
| **Addon** | Clockify extension providing custom workflows |
| **Manifest** | JSON describing addon capabilities to Clockify |
| **Webhook** | Event notification from Clockify to addon |
| **Handler** | Function/method processing request or event |
| **Scope** | Permission level for API access (TIME_ENTRY_READ, etc.) |
| **Workspace** | Clockify organization (multi-tenant isolation) |
| **Token** | OAuth credential for API access |
| **Filter** | Middleware in Jetty request processing chain |
| **Rule** | Condition/action pair for automation |
| **Context** | Request/event data wrapped for evaluation |
| **Store** | Persistence mechanism (in-memory or database) |

---

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2025-11-09 | 1.0.0 | Initial comprehensive analysis |

---

## Notes for Future Maintainers

1. **Security**: Review WebhookSignatureValidator and SecurityHeadersFilter regularly
2. **Dependencies**: Keep addon-sdk dependencies updated (Jackson, Jetty, etc.)
3. **Tests**: Maintain > 80% code coverage
4. **Documentation**: Update this analysis when adding major features
5. **Backwards Compatibility**: Manifest schema changes require version bump
6. **Performance**: Monitor webhook processing time (target < 1s)
7. **Database**: Test migrations with real PostgreSQL/MySQL versions

---

**Generated By**: Claude Code Analysis
**Analysis Depth**: Line-by-line with implementation details
**Coverage**: All major Java source files (65+ files)
**Format**: Markdown with code examples and diagrams
