# Clockify Parameters Reference

Canonical reference for the parameters used by this boilerplate and add-ons: manifest fields, headers, webhook signals, JWT claims, and runtime environment flags.

## Manifest (schemaVersion 1.3)

- key: unique string for the add-on (e.g., "auto-tag-assistant").
- name: human-readable name.
- description: short description shown to admins.
- schemaVersion: "1.3" (do not include `$schema`).
- baseUrl: external base URL for your add-on (e.g., ngrok HTTPS URL + context path).
- minimalSubscriptionPlan: one of `FREE`, `BASIC`, `STANDARD`, `PRO`, `ENTERPRISE` — the minimum Clockify plan required to install your add‑on.
- components:
  - sidebar: `{ path: "/settings", accessLevel: "ADMINS" }` (example)
- webhooks: `{ event: "...", path: "/webhook" }` entries.
- lifecycle: `{ type: "INSTALLED"|"DELETED", path: "..." }` entries.

## Headers

- x-addon-token: workspace-scoped installation token for Clockify API calls (canonical in this boilerplate).
- clockify-webhook-signature: HMAC-SHA256 signature header for webhook bodies (validate with SDK WebhookSignatureValidator).
- Clockify-Signature: Developer JWT header for webhooks. Accepted by default when `ADDON_ACCEPT_JWT_SIGNATURE=true` (default). The validator inspects the JWT payload and matches `workspaceId`.
- x-clockify-workspace: workspace id (when provided by Clockify; not guaranteed on all routes).

Notes:
- Older examples may show `Authorization: Bearer <token>` but the boilerplate patterns prefer `x-addon-token`.

## Webhooks

Common events used by the demo and rules add-ons:
- NEW_TIMER_STARTED, TIMER_STOPPED
- NEW_TIME_ENTRY, TIME_ENTRY_UPDATED

Signature validation: Use WebhookSignatureValidator with the stored installation token-derived secret.

Webhook catalog: See docs/REQUEST-RESPONSE-EXAMPLES.md for examples and the original developer docs snapshot under `dev-docs-marketplace-cake-snapshot/` for the full event list (e.g., `NEW_TIME_ENTRY`, `TIME_ENTRY_UPDATED`, `NEW_PROJECT`, `PROJECT_UPDATED`, `NEW_CLIENT`, `CLIENT_UPDATED`, `TIME_OFF_REQUESTED`, etc.).

## JWT (environment claims)

Tokens and UI endpoints may include environment claims (e.g., backend/api base URLs). Decode as needed in UI flows.

## Runtime Environment (SDK / server)

- ADDON_BASE_URL — External URL exposed to Clockify (e.g., https://abc123.ngrok-free.app/auto-tag-assistant)
- ADDON_PORT — Local port (default 8080)
- ADDON_FRAME_ANCESTORS — CSP frame-ancestors value for SecurityHeadersFilter
- ADDON_RATE_LIMIT — Numeric requests/sec limit (RateLimiter)
- ADDON_LIMIT_BY — ip|workspace (RateLimiter bucketing)
- ADDON_CORS_ORIGINS — CSV list of allowed origins (CorsFilter)
- ADDON_CORS_ALLOW_CREDENTIALS — true|false for CORS credentials
- ADDON_ACCEPT_JWT_SIGNATURE — true|false to accept Developer JWT signature header `Clockify-Signature` (default true)

## Persistence (DatabaseTokenStore)

- DB_URL — JDBC URL (e.g., jdbc:postgresql://host/db)
- DB_USERNAME — database username
- DB_PASSWORD — database password
