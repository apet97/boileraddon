# Security Notes (JWT + Signatures)

- Canonical signature header is `Clockify-Signature`. Alternate headers are temporarily accepted; usage is logged with a WARN and counted under `addon.signature.header.noncanonical`.
- JWT issuer configuration prefers `CLOCKIFY_JWT_EXPECTED_ISS`. Legacy `CLOCKIFY_JWT_EXPECT_ISS` is deprecated; a WARN is logged once per process and `addon.jwt.issuer.env.fallback` increments when used.
- Clock skew for `exp` checks is configurable via `JWT_MAX_CLOCK_SKEW_SECONDS` (default 30).
  - Webhooks: `exp` is optional; if present and expired beyond skew → 401.
  - Lifecycle: `exp` is required and enforced.
- UI/settings JWT verification also enforces `sub == <manifest.key>` (Rules uses `rules`).
