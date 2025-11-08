# SDK Overview

This document explains the in-repo Java SDK used by the add-on modules. It focuses on routing, middleware, security utilities, and operational flags so contributors can add endpoints safely and predictably.

## Routing Core

- ClockifyAddon — helper for registering endpoints and keeping the runtime manifest in sync.
- AddonServlet — lightweight servlet that dispatches to your registered RequestHandler instances.
- EmbeddedServer — boots Jetty with your servlet and any middleware filters.
- ClockifyManifest — builds the runtime manifest JSON served at `/{addon}/manifest.json` (no `$schema`).

Routing note:
- The SDK matches endpoint paths exactly (no wildcards). Pass identifiers via query/body, or register additional exact paths.
  Example: register once with `"/api/items"`, then issue `DELETE /api/items?id=...` (or a JSON body including an id).

## Middleware

- SecurityHeadersFilter — adds security headers and optional CSP frame-ancestors from `ADDON_FRAME_ANCESTORS`.
- CorsFilter — strict allowlist via `ADDON_CORS_ORIGINS` (CSV). Optional `ADDON_CORS_ALLOW_CREDENTIALS=true`.
  - Supports exact origins (e.g., `https://app.clockify.me`) and subdomain wildcards (e.g., `https://*.example.com`).
  - Wildcards only match subdomains, not the bare domain.
- RateLimiter — opt-in throttling by `ip` or `workspace`, configured via `ADDON_RATE_LIMIT` and `ADDON_LIMIT_BY`.
- RequestLoggingFilter — request/response logging for debugging (disable in production unless needed).

Attach filters to the `EmbeddedServer` before start so they protect all routes (manifest, lifecycle, custom endpoints, webhooks).

## Security Utilities

Consolidated in the SDK (preferred for all modules):

- TokenStore — workspace-scoped installation token storage. The demo uses an in-memory store; production should implement a persistent store (see docs/DATABASE_TOKEN_STORE.md). All Clockify API calls reuse this token via the `x-addon-token` header.
- WebhookSignatureValidator — validates `clockify-webhook-signature` using a shared secret derived from the installation token. Reject mismatches with 401/403.

Usage patterns:
- Lifecycle `INSTALLED`: persist `workspaceId`, `authToken` (installation token), and any environment hints.
- Webhooks: load token from TokenStore, validate signature, then process the request.

## Path Safety

- PathSanitizer — normalizes and validates path segments before registering routes. Avoids duplicate slashes and unsafe characters.

## Validation & Briefings

- tools/validate-manifest.py — schema checks for `addons/**/manifest.json`.
- tools/manifest.schema.json — strong JSON Schema for local validation (optional).
- Briefings — `_briefings/*.md` are SHA-pinned role briefings; verify with `make briefings-verify`.

## Examples

- Auto-Tag Assistant — reference wiring for manifest, lifecycle, settings UI, and webhooks.
- Rules Add-on — shows evaluator-driven automation and a dry-run endpoint at `/rules/api/test`.

## HTTP Client

- ClockifyHttpClient — minimal wrapper over Java 17 HttpClient with sane timeouts and retries for 429/5xx. Always sends `x-addon-token` header. Use for reads/writes against `{apiBaseUrl}` stored per workspace via TokenStore.

## Environment Flags (summary)

- ADDON_BASE_URL — external base URL for this module (e.g., ngrok HTTPS URL).
- ADDON_PORT — local port to listen on (default 8080).
- ADDON_FRAME_ANCESTORS — CSP `frame-ancestors` value (e.g., `'self' https://*.clockify.me`).
- ADDON_RATE_LIMIT — numeric rate (requests/sec) for RateLimiter.
- ADDON_LIMIT_BY — `ip` or `workspace` (how to bucket limits).
- ADDON_CORS_ORIGINS — CSV allowlist for CORS; enables preflight handling.
- ADDON_CORS_ALLOW_CREDENTIALS — `true|false`; credentials support for CORS (off by default).

## Pointers

- Architecture overview: docs/ARCHITECTURE.md
- Production deployment: docs/PRODUCTION-DEPLOYMENT.md
- Token storage: docs/DATABASE_TOKEN_STORE.md
- Quick reference: docs/QUICK-REFERENCE.md
