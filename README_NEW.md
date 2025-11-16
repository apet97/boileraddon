# Clockify Add-on Boilerplate (2025 update)

This repository contains everything you need to ship production-grade Clockify add-ons without external SDKs or private Maven repositories. It includes:

- `addons/rules` – the hardened “Rules” automation add-on (primary production target).
- `addons/addon-sdk` – the embedded SDK used by all add-ons (routing, middleware, security, metrics).
- `addons/auto-tag-assistant`, `addons/overtime`, `addons/_template-addon` – demo/reference code you can copy or inspect.
- `docs/` – architecture notes, security guidance, zero-shot playbooks, and production checklists.

The repo is wired for Java 17, Maven 3.6+, and standard `make` helpers. CI (GitHub Actions) runs the full build/test/coverage pipelines on every change.

> ⚠️ This README replaces references to the old `run-rules.sh` helper. The workflow below reflects the commands we actively support.

---

## 1. Prerequisites

| Requirement | Notes | Verify |
| --- | --- | --- |
| Java 17 (Temurin recommended) | Surefire forked JVM also runs with Java 17 (toolchains enabled). | `java -version` |
| Maven 3.6+ | Used for builds/tests across all modules. | `mvn -version` |
| ngrok (optional) | Needed only when exposing `localhost` to Clockify during manual testing. | `ngrok version` |

Quick toolchain check:

```bash
make dev-check          # verifies java, maven, ngrok
```

---

## 2. Running the Rules add-on locally

### Option A: Make + `.env.rules`

```bash
cp .env.rules.example .env.rules    # configure ADDON_BASE_URL, JWT bootstrap, DB creds, etc.
make dev-rules                      # starts the Rules fat JAR on port 8080
```

This path loads `RulesConfiguration` from `.env.rules`, applies middleware (security headers, optional rate limiting/CORS), and watches the logs directly.

### Option B: Manual Maven + Java

```bash
mvn -q -pl addons/rules -am package
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
```

Expose via ngrok if you plan to install it inside Clockify:

```bash
ngrok http 8080
ADDON_BASE_URL=https://<your-ngrok>.ngrok-free.app/rules \
  java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar
```

Once running, verify:

```bash
curl http://localhost:8080/rules/health
curl http://localhost:8080/rules/manifest.json
```

Then install the add-on in Clockify (Admin → Add-ons → Install Custom Add-on) using the manifest URL `https://<ngrok-domain>/rules/manifest.json`.

---

## 3. Building, testing, and coverage

Common commands:

```bash
# Validate manifests (runtime JSON)
python3 tools/validate-manifest.py

# Module tests (Rules add-on plus dependencies)
mvn -q -pl addons/rules -am test -DtrimStackTrace=false

# Full repo build + verification
mvn -q clean verify -DtrimStackTrace=false

# Smoke tests only (health/metrics endpoints)
make smoke
```

CI pipelines run `mvn -B -q -Pci,security-scan verify`, aggregate JaCoCo coverage, and publish documentation to GitHub Pages.

---

## 4. Key directories & files

| Path | What lives here | Notes |
| --- | --- | --- |
| `addons/rules` | Production Rules add-on | Controllers, webhook handlers, DB stores, cache, README. |
| `addons/addon-sdk` | Shared SDK | Jetty servlet, middleware, security helpers, metrics, path sanitization. |
| `scripts/` | Automation helpers | `quick-start.sh`, `rules-up.sh`, `rules-webhook-sim.sh`, etc. No deprecated `run-rules.sh` references. |
| `docs/` | Architecture & ops docs | `AI_START_HERE.md`, `RULES_DB_SCHEMA.md`, `RULES_PROD_LAUNCH_CHECKLIST.md`, etc. |
| `tools/` | Linting/validation scripts | Manifest schema, coverage badge generator, briefing tooling. |

Notable configuration files:

- `.env.rules.example` – base template for RulesConfiguration.
- `Makefile` – shortcuts such as `make dev-rules`, `make docker-build TEMPLATE=rules`.
- `_briefings/` – SHA-pinned references for AI contributors.

---

## 5. Observability & security quick facts

- `/rules/health`, `/rules/ready`, `/rules/metrics` are always registered. Use readiness for Kubernetes probes and Prometheus for metrics (`rules_webhook_latency_ms`, `rules_actions_total`, `rules_webhook_dedup_{hits,misses}_total`, etc.).
- Webhook and lifecycle requests must include valid signatures. `WebhookSignatureValidator` rejects unsigned requests unless `ADDON_SKIP_SIGNATURE_VERIFY=true` **and** `ENV=dev`.
- `WebhookIdempotencyCache` now persists dedupe entries in the `webhook_dedup` table whenever `RULES_DB_*` (or shared `DB_*`) config is provided; otherwise it falls back to per-node memory.
- `PlatformAuthFilter` protects `/api/**` + `/status`, so production callers must supply the Clockify-issued `auth_token` bearer JWT.

---

## 6. Helpful docs

- [docs/AI_START_HERE.md](docs/AI_START_HERE.md) – zero-shot instructions.
- [RULES_ADDON_PRODUCTION_SUMMARY.md](RULES_ADDON_PRODUCTION_SUMMARY.md) – hardened deployment summary.
- [RULES_PROD_LAUNCH_CHECKLIST.md](RULES_PROD_LAUNCH_CHECKLIST.md) – preflight steps for new envs.
- [docs/RULES_DB_SCHEMA.md](docs/RULES_DB_SCHEMA.md) – canonical DB schemas (`rules`, `addon_tokens`, `webhook_dedup`).
- [docs/RULES_OBSERVABILITY.md](docs/RULES_OBSERVABILITY.md) – metrics, logging, alerting guidance.

When in doubt, run through `docs/AI_ZERO_SHOT_PLAYBOOK.md` for the exact validation/test order expected in CI.

---

## 7. Support

Questions or PRs should follow the existing contribution guidelines:

1. Keep changes small and focused (no sweeping refactors).
2. Update/mention documentation for any externally visible change.
3. Run the relevant Maven targets locally before pushing.
4. Avoid adding new dependencies unless they come from Maven Central and are required.

Happy building! 🎉
