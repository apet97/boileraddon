# Architecture Overview

This document provides a comprehensive overview of the Clockify Add-on Boilerplate architecture.

## Table of Contents
- [High-Level Architecture](#high-level-architecture)
- [System Components](#system-components)
- [Request Flow](#request-flow)
- [Data Flow](#data-flow)
- [Module Structure](#module-structure)
- [Design Patterns](#design-patterns)

---

## High-Level Architecture

The boilerplate follows a **multi-module microservices architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      Clockify Platform                       │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    │ HTTP (Lifecycle, Webhooks, API calls)
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                      Addon Application                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Embedded Jetty Server                      │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │           Middleware Layer                        │  │ │
│  │  │  • Security Headers   • Rate Limiter              │  │ │
│  │  │  • CORS Filter        • Request Logging           │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │           AddonServlet (Router)                   │  │ │
│  │  │  • Path matching      • Handler dispatch          │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              ClockifyAddon (Coordinator)               │ │
│  │  • Manifest                • Custom Endpoints          │ │
│  │  • Lifecycle Handlers      • Middleware Config         │ │
│  │  • Webhook Handlers        • Token Store               │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Business Logic Layer                      │ │
│  │  • Controllers            • Services                   │ │
│  │  • Data Access            • Domain Models              │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    │ JDBC
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                    PostgreSQL Database                       │
│  • addon_tokens (workspace tokens)                          │
│  • rules (automation rules)                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## System Components

### 1. SDK Module (`addon-sdk`)

**Location:** `addons/addon-sdk/`

**Purpose:** Reusable library providing core addon functionality.

**Key Classes:**
- `ClockifyAddon` - Central coordinator class
- `ClockifyManifest` - Manifest builder (v1.3)
- `AddonServlet` - HTTP request router
- `EmbeddedServer` - Jetty server wrapper
- `RequestHandler` - Handler interface

**Responsibilities:**
- HTTP server management
- Request routing and dispatching
- Middleware orchestration
- Security validation
- Token management
- HTTP client for Clockify API

### 2. Addon Modules

Each addon is a separate Maven module that depends on the SDK:

#### a. Template Addon (`_template-addon`)
- Minimal starter template
- All required endpoints (manifest, lifecycle, webhooks)
- No business logic, ready for customization

#### b. Auto-Tag Assistant (`auto-tag-assistant`)
- Simple production example
- Demonstrates tag suggestions
- Uses in-memory storage

#### c. Rules Addon (`rules`)
- Most complex example
- IFTTT-style automation engine
- Database-backed (PostgreSQL)
- Visual rule builder UI
- Workspace data caching

#### d. Overtime Addon (`overtime`)
- Basic structure
- Demonstrates overtime policy tracking

### 3. Middleware Layer

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/`

**Components:**
1. **SecurityHeadersFilter** - Adds security headers (CSP, HSTS, etc.)
2. **RateLimiter** - Token bucket rate limiting
3. **CorsFilter** - CORS handling with wildcard support
4. **RequestLoggingFilter** - HTTP request/response logging

**Execution Order:**
```
Request → Security Headers → CORS → Rate Limiter → Logging → AddonServlet
```

### 4. Security Layer

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/`

**Components:**
1. **TokenStore** - In-memory token storage
2. **DatabaseTokenStore** - Persistent token storage
3. **WebhookSignatureValidator** - HMAC/JWT validation

### 5. HTTP Client

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/http/`

**Features:**
- Automatic retries (exponential backoff)
- Rate limit handling (429 responses)
- `x-addon-token` header injection
- Timeout configuration

---

## Request Flow

### Lifecycle Request Flow (INSTALLED)

```
1. Clockify → POST /addon/lifecycle/installed
   {
     "workspaceId": "...",
     "installationToken": "...",
     "apiBaseUrl": "..."
   }

2. AddonServlet.service()
   ↓
3. SecurityHeadersFilter → add headers
   ↓
4. CorsFilter → validate origin
   ↓
5. RateLimiter → check rate limit
   ↓
6. RequestLoggingFilter → log request
   ↓
7. AddonServlet.tryHandleLifecycle()
   ↓
8. Extract lifecycle type ("INSTALLED")
   ↓
9. Find registered handler
   ↓
10. Execute handler (save token to TokenStore)
   ↓
11. Return HttpResponse(200, "{\"status\":\"installed\"}")
```

### Webhook Request Flow

```
1. Clockify → POST /addon/webhook
   Headers:
     clockify-webhook-event-type: TIME_ENTRY_CREATED
     clockify-webhook-signature: sha256=abc123...
   Body:
     { "event": "TIME_ENTRY_CREATED", ... }

2. AddonServlet.service()
   ↓
3. Middleware filters (same as above)
   ↓
4. AddonServlet.tryHandleWebhook()
   ↓
5. Validate webhook signature (HMAC-SHA256)
   ↓
6. Extract event type from header or body
   ↓
7. Find registered webhook handler
   ↓
8. Execute handler (process event)
   ↓
9. Return HttpResponse(200, "OK")
```

### Custom Endpoint Request Flow

```
1. Client → GET /addon/api/rules?workspaceId=...
   ↓
2. AddonServlet.service()
   ↓
3. Middleware filters
   ↓
4. AddonServlet.handleRequest()
   ↓
5. Lookup custom endpoint ("/api/rules")
   ↓
6. Execute RequestHandler.handle()
   ↓
7. Business logic (e.g., fetch rules from database)
   ↓
8. Return HttpResponse(200, JSON)
```

---

## Data Flow

### Installation Flow

```
1. User installs addon in Clockify
   ↓
2. Clockify sends INSTALLED lifecycle event
   ↓
3. Addon receives installationToken and apiBaseUrl
   ↓
4. Addon stores token in TokenStore/Database
   ↓
5. Addon subscribes to webhooks (via manifest)
   ↓
6. Installation complete
```

### Webhook Processing Flow

```
1. Event occurs in Clockify (e.g., time entry created)
   ↓
2. Clockify sends webhook to addon
   ↓
3. Addon validates signature
   ↓
4. Addon processes event (e.g., apply automation rules)
   ↓
5. Addon may call Clockify API (using stored token)
   ↓
6. Addon returns 200 OK to Clockify
   ↓
7. Event processing complete
```

### API Call Flow (Addon → Clockify)

```
1. Addon needs to fetch/update Clockify data
   ↓
2. Retrieve installationToken from TokenStore
   ↓
3. Create ClockifyHttpClient with apiBaseUrl
   ↓
4. Make HTTP request with x-addon-token header
   ↓
5. Handle response (retry on 429/5xx)
   ↓
6. Return data to business logic
```

---

## Module Structure

### Parent POM Structure

```xml
<project>
  <groupId>com.clockify.boilerplate</groupId>
  <artifactId>clockify-addon-boilerplate</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>addons/addon-sdk</module>
    <module>addons/_template-addon</module>
    <module>addons/auto-tag-assistant</module>
    <module>addons/rules</module>
    <module>addons/overtime</module>
  </modules>
</project>
```

### Addon Module Structure

Each addon module follows this structure:

```
addon-name/
├── pom.xml                     # Module POM (depends on addon-sdk)
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/addonname/
    │   │       ├── AddonNameApp.java          # Main entry point
    │   │       ├── ManifestController.java    # Manifest endpoint
    │   │       ├── SettingsController.java    # Settings UI
    │   │       ├── LifecycleHandlers.java     # INSTALLED/DELETED
    │   │       └── WebhookHandlers.java       # Event handlers
    │   └── resources/
    │       ├── logback.xml                    # Logging config
    │       └── application.properties         # Optional config
    └── test/
        └── java/
            └── com/example/addonname/
                └── AddonNameAppTest.java
```

### Dependency Graph

```
Auto-Tag Assistant Addon
    ↓ depends on
Addon SDK
    ↓ depends on
Jackson, Jetty, SLF4J, Micrometer, etc.
```

---

## Design Patterns

### 1. Builder Pattern

**Used in:** `ClockifyManifest`

```java
ClockifyManifest manifest = new ClockifyManifest()
    .schemaVersion("1.3")
    .name("Rules Automation")
    .description("IFTTT-style automation")
    .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE"})
    .components(List.of(
        new ComponentEndpoint("sidebar", "/settings", "Settings", "ADMINS")
    ))
    .webhooks(List.of(
        new WebhookSubscription("TIME_ENTRY_CREATED", "/webhook")
    ));
```

### 2. Strategy Pattern

**Used in:** Token storage (TokenStoreSPI)

```java
public interface TokenStoreSPI {
    void save(String workspaceId, String token, String apiBaseUrl);
    Optional<WorkspaceToken> get(String workspaceId);
    boolean delete(String workspaceId);
}

// Implementations:
// - TokenStore (in-memory)
// - DatabaseTokenStore (PostgreSQL)
```

### 3. Chain of Responsibility

**Used in:** Middleware filters

```java
Request → SecurityHeadersFilter
       → CorsFilter
       → RateLimiter
       → RequestLoggingFilter
       → AddonServlet
```

### 4. Front Controller

**Used in:** `AddonServlet`

Single entry point for all HTTP requests, routes to appropriate handlers.

### 5. Facade Pattern

**Used in:** `ClockifyAddon`

Simplified interface to complex subsystems (server, routing, middleware).

### 6. Service Layer Pattern

**Used in:** Rules addon business logic

```java
RulesController (HTTP) → RulesService → RulesStore (persistence)
```

### 7. Repository Pattern

**Used in:** `RulesStore`, `DatabaseRulesStore`

Abstraction over data access layer.

---

## Component Interaction Diagram

```
┌─────────────────┐
│   Clockify      │
│   Platform      │
└────────┬────────┘
         │
         │ HTTP
         │
         ▼
┌─────────────────────────────────┐
│   EmbeddedServer (Jetty)        │
│   ┌─────────────────────────┐   │
│   │  Filter Chain           │   │
│   │  (Middleware)           │   │
│   └──────────┬──────────────┘   │
│              │                   │
│              ▼                   │
│   ┌─────────────────────────┐   │
│   │  AddonServlet           │   │
│   │  (Router)               │   │
│   └──────────┬──────────────┘   │
└──────────────┼──────────────────┘
               │
               ├─ Lifecycle? ──► LifecycleHandler ──► TokenStore
               │
               ├─ Webhook? ────► WebhookHandler ──┬─► Business Logic
               │                                   │
               └─ Custom? ─────► RequestHandler ──┴─► Database
```

---

## Scalability Considerations

### Horizontal Scaling

The architecture supports horizontal scaling:

1. **Stateless Design** - No session state (except token cache)
2. **Database-Backed Storage** - Shared state in PostgreSQL
3. **Load Balancer Ready** - Health checks at `/health`

```
                    ┌─── Addon Instance 1 ───┐
Load Balancer ──────┼─── Addon Instance 2 ───┼──► PostgreSQL
                    └─── Addon Instance 3 ───┘
```

### Vertical Scaling

Adjustable via JVM options:

```bash
JAVA_OPTS="-Xmx2g -Xms512m" java -jar addon.jar
```

### Caching Strategy

**Rules Addon Example:**

- **Workspace Cache** - In-memory cache of tags, projects, users
- **TTL** - Configurable cache expiration
- **Refresh API** - Manual cache invalidation endpoint

---

## Security Architecture

### Defense in Depth

1. **Network Layer** - HTTPS only in production
2. **Application Layer** - HMAC signature validation
3. **Transport Layer** - Security headers (HSTS, CSP)
4. **Data Layer** - Encrypted database connections

### Authentication Flow

```
Clockify
  │
  ├─ INSTALLED event ──► installationToken ──► TokenStore
  │
  └─ Webhooks ─────────► HMAC signature ─────► Validate

Addon → Clockify API
  │
  └─ HTTP request ─────► x-addon-token: <installationToken>
```

---

## Technology Choices Rationale

### Why Jetty?

- **Embedded** - No external server required
- **Lightweight** - Small memory footprint
- **Servlet API** - Standard Java web API
- **Production-Ready** - Battle-tested in production

### Why No Spring?

- **Simplicity** - Reduced complexity
- **Startup Time** - Faster startup (< 2s vs. 10s+)
- **Memory** - Lower memory footprint
- **Transparency** - Easier to understand for AI/beginners

### Why PostgreSQL?

- **Reliability** - ACID compliance
- **JSON Support** - Native JSON storage for rules
- **Migrations** - Flyway integration
- **Open Source** - No licensing costs

### Why Jackson?

- **Performance** - Fast JSON processing
- **Annotations** - Clean serialization
- **Standard** - De facto standard in Java
- **Compatibility** - Works everywhere

---

## Future Architecture Enhancements

### Planned Improvements

1. **Event Sourcing** - Audit log for all state changes
2. **CQRS** - Separate read/write models for complex addons
3. **Circuit Breaker** - Resilience for Clockify API calls
4. **Message Queue** - Async webhook processing (RabbitMQ/Kafka)
5. **Multi-Tenancy** - Better workspace isolation
6. **Metrics Dashboard** - Grafana integration

---

**Next:** [SDK Components Reference](./03-SDK-COMPONENTS.md)
