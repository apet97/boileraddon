# Rules Add-on Production Checklist

Track the hardening items required to ship the Rules add-on safely. Update each line with context (file paths, notes) when work is completed.

- [x] Single source of truth for config (env vars + config module). (`RulesConfiguration`, `.env.rules.example`, `addons/rules/README.md`)
- [x] Proper Clockify JWT/signature validation for webhooks and UI. (`WebhookSignatureValidator`, `JwtVerifier`, `WorkspaceContextFilter`)
- [x] Webhook idempotency/deduplication with storage. (`WebhookIdempotencyCache`, handlers, `RulesMetrics.recordDeduplicatedEvent`)
- [x] Reasonable timeouts and error handling on all HTTP calls to Clockify. (`ClockifyHttpClient`)
- [x] Backoff / rate limiting respecting Clockify’s 50 RPS per add-on per workspace. (`RulesApp` + `RateLimiter`)
- [x] Security headers & CSP in all HTTP responses. (`SecurityHeadersFilter` wired for every request)
- [x] No secrets/API tokens leaked to logs or UI. (`SensitiveHeaderFilter`, lifecycle redaction, structured logging)
- [x] Health (`/health`) and readiness (`/ready`) endpoints for Docker/Kubernetes. (`RulesApp`, `ReadinessHandler`)
- [x] Metrics endpoint (e.g., Prometheus style) with basic counters/timers. (`MetricsHandler`, `RulesMetrics`)
- [x] Integration/unit tests for lifecycle, webhooks, and key business logic. (Existing suites + new cache/readiness specs)
- [x] Minimal Dockerfile with non-root user and env-driven config. (`Dockerfile`)
- [x] README/OPERATIONS.md explaining config, deployment, and failure modes. (`addons/rules/README.md` config section)
