# Threat Model (High-level)

- Token theft
  - Mitigation: store installation tokens in database; scope least privilege; rotate on request.
- Replay of webhooks
  - Mitigation: verify signatures; enforce TTL and nonce if available; idempotency on handlers.
- SSRF / URL tampering
  - Mitigation: never fetch arbitrary URLs; whitelist Clockify domains; validate baseUrl format.
- Path traversal
  - Mitigation: sanitize any file paths; avoid dynamic file access for handlers.
- DoS / abuse
  - Mitigation: rate limit per IP/workspace; set HTTP client timeouts and backoff.
- Secrets leakage
  - Mitigation: never log secrets; scrub structured logs; restrict access to logs.

Tie these to SECURITY.md practices and CI validations.
