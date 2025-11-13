# Rules Add-on — Production Summary

The Rules add-on evaluates Clockify time-entry events against declarative conditions and applies actions (tagging, description edits, HTTP calls) with built-in lifecycle handlers, settings UI, and webhook processing. It ships with webhook idempotency + metrics, hardened JWT verification for the iframe bootstrap, and CSP/headers for the entire surface. The service exposes `/health`, `/ready`, and `/metrics` for orchestration and Prometheus scraping, and it can persist rules/tokens via PostgreSQL.

## Running locally
1. `cp .env.rules.example .env.rules` then tweak envs (base URL, optional DB, dev helpers).
2. `make dev-rules` or `mvn -q -pl addons/rules -am package && ADDON_BASE_URL=http://localhost:8080/rules java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar`.
3. Use ngrok to expose port 8080 and reinstall via `/rules/manifest.json`. Keep `ADDON_SKIP_SIGNATURE_VERIFY` dev-only.

## Running in production
1. Build the Docker image (`make docker-build TEMPLATE=rules`) or use the provided `Dockerfile` (non-root `addon` user, `/opt/addon/app.jar`).
2. Supply mandatory envs via secrets manager: `ADDON_BASE_URL`, `ADDON_PORT`, Clockify JWT keys (`CLOCKIFY_JWT_PUBLIC_KEY*` or `CLOCKIFY_JWT_JWKS_URI`), DB credentials when persistence is required, and rate limit/CORS/CSP flags.
3. Wire probes: `/ready` for readiness/startup gates, `/health` for liveness, `/metrics` for scraping. Monitor `RulesMetrics` counters for dedupe hits and rule evaluations.
4. Never set `ADDON_SKIP_SIGNATURE_VERIFY`, `CLOCKIFY_WORKSPACE_ID`, or `CLOCKIFY_INSTALLATION_TOKEN` outside dev/test.

For detailed procedures, see `addons/rules/README.md` (config/deployment guide) and `PRODUCTION_CHECKLIST.md` (audited hardening items).
