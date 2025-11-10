# Security Guidelines

This project provides a Clockify add-on boilerplate. Use these practices when building real add-ons.

- Secrets and config
  - Store secrets in environment variables or a secret manager. Never commit secrets.
  - Provide `.env.example` only with placeholders.
- TLS and transport
  - Use HTTPS in all environments. HSTS recommended in production.
- Webhooks and lifecycle
  - Verify signatures for webhook/lifecycle requests before processing.
  - Reject unsigned or malformed requests.
  - Developer environments may send a JWT header (`Clockify-Signature`). The SDK validator accepts this header only when `ADDON_ACCEPT_JWT_SIGNATURE=true` (disabled by default for production). Prefer HMAC (`clockify-webhook-signature`) for production consistency.
- Embedding and origins
  - Frame embedding: set `ADDON_FRAME_ANCESTORS='self' https://*.clockify.me` so the settings UI is only embeddable inside Clockify.
  - CORS: set `ADDON_CORS_ORIGINS=https://app.clockify.me,https://developer.clockify.me` (and `ADDON_CORS_ALLOW_CREDENTIALS=true` if needed). CORS is disabled by default.
- Tokens and storage
  - Persist installation tokens in a database for HA and auditability.
  - Rotate and revoke tokens when requested.
- Least privilege and scopes
  - Request only required scopes in your manifest.
- Rate limiting and abuse prevention
  - Apply IP/workspace throttling and backpressure.
- Timeouts and retries
  - Use HTTP client timeouts; retry idempotent operations with backoff.
- Logging
  - Avoid logging secrets or PII. Use structured logs.

See README and briefings for pinned references.
