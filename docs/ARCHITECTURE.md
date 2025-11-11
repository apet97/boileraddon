# Architecture Overview

Components
- ManifestController — serves runtime manifest at `/{addon}/manifest.json`.
- LifecycleHandlers — processes INSTALLED/DELETED and stores workspace tokens via TokenStore.
- WebhookHandlers — handles time entry events using stored tokens and Clockify API.
- TokenStore — InMemory (demo) or DatabaseTokenStore (prod) selected by env.
- AutoTagAssistantApp — wires manifest, endpoints, and embedded server.

## Security Architecture

This boilerplate includes comprehensive enterprise security hardening features:

### Security Components
- **JWT Security**: Algorithm enforcement, strict kid handling, JWKS-based key management
- **CSRF Protection**: Token-based validation with constant-time comparison for state-changing operations
- **RFC-7807 Error Handling**: Standardized problem+json error responses
- **Request ID Propagation**: Distributed tracing for all requests with X-Request-Id headers
- **Security Headers**: Comprehensive HTTP security headers (CSP, HSTS, XSS protection)
- **Input Validation**: Comprehensive parameter and payload validation
- **Path Sanitization**: URL path validation and sanitization
- **Rate Limiting**: IP and workspace-based request throttling

### Security Middleware
- SecurityHeadersFilter — adds security headers and CSP frame-ancestors
- CsrfProtectionFilter — CSRF protection for state-changing operations
- RequestIdPropagationFilter — distributed tracing with unique request IDs
- DiagnosticContextFilter — structured logging with request context
- RequestSizeLimitFilter — request size limits to prevent DoS attacks
- RateLimiter — request throttling by IP or workspace

Key flows
1. Discovery: Clockify fetches `{baseUrl}/manifest.json`.
2. Install: Clockify posts `INSTALLED` with workspace token; token persisted via TokenStore.
3. Webhooks: Clockify posts events; signature verified using stored installation token.
4. UI: Sidebar loads `{baseUrl}/settings` in an iframe with JWT security.

Configuration
- ADDON_BASE_URL and ADDON_PORT define runtime URLs.
- DB_URL/DB_USERNAME/DB_PASSWORD select DatabaseTokenStore.
- Security features automatically enabled and validated through 307 tests across 5 layers.

See also
- README.md (Quickstart)
- docs/DATABASE_TOKEN_STORE.md
- SECURITY.md, THREAT_MODEL.md
- docs/SDK_OVERVIEW.md (Security Utilities)
