# Release Checklist

Use this checklist before promoting an add‑on to production.

## Build & Runtime
- Java 17 everywhere (Maven + forked test JVM via Toolchains)
- CI green: validate, smoke, build‑and‑test, Pages coverage badge
- Runtime manifests served programmatically; `$schema` omitted; `schemaVersion: "1.3"`

## Security
- HTTPS with valid TLS; HSTS enabled at the edge
- SecurityHeadersFilter on all routes; CSP `frame-ancestors` set via `ADDON_FRAME_ANCESTORS`
- Rate limiting configured (`ADDON_RATE_LIMIT`, `ADDON_LIMIT_BY`)
- Webhook signatures verified; secrets rotated periodically
- Least‑privilege database credentials; network access restricted; SSL to DB

## Data & Storage
- Database token store enabled (disable in‑memory)
- Migrations versioned, applied in staging first (Flyway)
- Backups enabled with retention/rotation; restore tested
- Data retention policy documented (e.g., purge on uninstall)

## Observability
- `/health` wired with DB probe if DB configured; external monitor added
- `/metrics` exposed and scraped by Prometheus/agent
- JSON logging in production; logs shipped to aggregator with retention/PII policy
- Alerts configured: downtime, error rate, slow queries, high latency, low free space

## Functional
- Manifest validated live (make manifest-validate-runtime)
- Smoke tests pass (make smoke)
- Full test suite passes; coverage at/above gates
- Scope & plan verified against the feature set (least privilege)

## Ops & Docs
- Runbook for on‑call, playbooks for common incidents
- Disaster recovery procedure documented and rehearsed
- Incident contact channels and escalation tree
- In‑repo docs updated (AI START HERE, Recipes, PostgreSQL, Metrics, Production)

## Deployment
- Rollout plan with rollback strategy
- Environment secrets (ADDON_WEBHOOK_SECRET, DB creds) stored in secret manager
- Config validated on startup (fail fast)

## Final Pre‑Flight
- Manual end‑to‑end test in staging (install, webhook, UI, DB write)
- Verify logs, metrics, and alerts during the flow
- Re‑check manifest URLs and context paths

