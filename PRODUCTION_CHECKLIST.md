# Rules Add-on Production Checklist

Track the hardening items required to ship the Rules add-on safely. Update each line with context (file paths, notes) when work is completed.

- [x] Single source of truth for config (env vars + config module). (`RulesConfiguration.fromEnvironment`, `.env.rules.example`, `RuntimeFlags` for live toggles)
- [x] Proper Clockify JWT/signature validation for webhooks and UI. (`JwtBootstrapConfig` → `JwtVerifier`, `WorkspaceContextFilter`, `PlatformAuthFilter`, `WebhookSignatureValidator` with HMAC default and guarded `ADDON_ACCEPT_JWT_SIGNATURE`)
- [x] Webhook idempotency/deduplication with storage + metrics. (`WebhookIdempotencyCache`, TTL via `RULES_WEBHOOK_DEDUP_SECONDS`, `RulesMetrics.recordDeduplicatedEvent`)
- [x] Reasonable timeouts and error handling on all HTTP calls to Clockify. (`ClockifyHttpClient`)
- [x] Backoff / rate limiting respecting Clockify’s 50 RPS per add-on per workspace. (`RulesApp` + `RateLimiter`)
- [x] Security headers & CSP in all HTTP responses. (`SecurityHeadersFilter` wired ahead of controllers; `ADDON_FRAME_ANCESTORS` documented)
- [x] No secrets/API tokens leaked to logs or UI. (`SensitiveHeaderFilter`, lifecycle redaction, scrubbed `RequestLoggingFilter`)
- [x] Dev-only helpers blocked outside `ENV=dev`. (`RuntimeFlags.skipSignatureVerification`, `LocalDevSecrets`, CI enforcement that `.env.rules.example` marks DEV ONLY)
- [x] Health (`/health`) and readiness (`/ready`) endpoints for Docker/Kubernetes. (`RulesApp`, `ReadinessHandler`)
- [x] Metrics endpoint (e.g., Prometheus style) with basic counters/timers. (`MetricsHandler`, `RulesMetrics`)
- [x] Integration/unit tests for lifecycle, webhooks, and key business logic. (Existing suites + new cache/readiness specs)
- [x] Minimal Dockerfile + Make target that passes module-specific build args. (`Dockerfile`, `make docker-build TEMPLATE=rules`)
- [x] README/OPERATIONS.md explaining config, deployment, and failure modes. (`addons/rules/README.md` config section)
