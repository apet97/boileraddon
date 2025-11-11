# Production Security Checklist

Use this checklist when promoting an add‑on to production. It captures the comprehensive security posture with enterprise hardening features.

## Application Security
- Enforce Java 17 at build and runtime; pin a Temurin 17 JRE in container images.
- Serve the runtime manifest programmatically (do not ship static files). Omit `$schema`; use `schemaVersion: "1.3"`.
- Route only exact paths. Sanitize/validate any user‑controlled path fragments with `PathSanitizer`.
- Verify `clockify-webhook-signature` before processing webhooks. Reject with 401/403 on gaps/mismatch.
- Use `ClockifyHttpClient` for outbound calls (timeouts; retries on 429/5xx). Always send `x-addon-token`.
- Persist installation tokens per workspace (implement a persistent `TokenStore`).

## Enterprise Security Hardening Features
- **JWT Security**: Algorithm enforcement, strict kid handling, JWKS-based key management
- **CSRF Protection**: Token-based validation with constant-time comparison for state-changing operations
- **RFC-7807 Error Handling**: Standardized problem+json error responses
- **Request ID Propagation**: Distributed tracing for all requests with X-Request-Id headers
- **Security Headers**: Comprehensive HTTP security headers (CSP, HSTS, XSS protection)
- **Input Validation**: Comprehensive parameter and payload validation
- **Path Sanitization**: URL path validation and sanitization
- **Rate Limiting**: IP and workspace-based request throttling

## Headers & CORS
- Security headers enabled; configure CSP frame ancestors via `ADDON_FRAME_ANCESTORS`.
  - Example: `'self' https://*.clockify.me`
- Explicit CORS allowlist via `ADDON_CORS_ORIGINS` (CSV). Supports subdomain wildcard patterns like `https://*.example.com`.
- Add `Vary: Origin` for cache correctness (the SDK's `CorsFilter` already does).

## Input sanitization
- Reject null bytes and encodings (`\u0000`, `%00`, `\\0`) in paths; `PathSanitizer.sanitize()` does pre‑trim checks.
- Reject directory traversal (`..`) in any path.
- Restrict characters to sane URL set: `[/a-zA-Z0-9._~:?#\[\]@!$&'()*+,;=-]`.

## Rate limiting & abuse protection
- Turn on `RateLimiter` via env flags: `ADDON_RATE_LIMIT` and `ADDON_LIMIT_BY` (`ip` or `workspace`).
- Log rejected requests at INFO (avoid PII; redact tokens).

## Secrets & tokens
- Do not log installation tokens or webhook secrets.
- Scope tokens to their workspace; do not share.
- Prefer hashed identifiers over raw IDs in logs.

## CI/CD & build
- CI runs tests/coverage on Temurin 17; Pages deploys only after a successful build.
- Keep dependencies on Maven Central only; avoid unauthenticated/private repos.
- All 307 security tests across 5 layers must pass before deployment

## Monitoring & operations
- Use structured logging; ensure sensitive fields are redacted.
- Track distinct 4xx/5xx and webhook signature mismatches.
- Add a `/health` endpoint and consider readiness checks for container runtimes.
- Monitor security events: CSRF token failures, JWT validation errors, rate limit violations

## Security Testing
- Run comprehensive test suite: `mvn test` (307 tests across 5 layers)
- Validate security hardening features are working
- Test error handling and input validation
- Verify request ID propagation and distributed tracing

