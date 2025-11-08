# AI START HERE — Clockify Add‑on Boilerplate

This is the single starting page for AI agents. It links to the exact commands, files, and docs you need to deliver zero‑shot results with high confidence.

## Guardrails
- Use Java 17 for both Maven and the forked test JVM (see docs/BUILD_ENVIRONMENT.md).
- Use Maven Central only; do not add external repos.
- Runtime manifests must omit `$schema` and use `schemaVersion: "1.3"`.
- SDK routing is exact‑path only. Put IDs in query/body or register additional exact paths.

## TL;DR — Zero‑Shot (Commands)
```
# 1) Toolchain
java -version && mvn -version

# 2) Validate manifests
python3 tools/validate-manifest.py

# 3) Tests (addon-sdk first)
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test

# 4) Full verify
mvn -e -DtrimStackTrace=false -fae verify

# 5) Optional run (choose a module)
TEMPLATE=auto-tag-assistant make zero-shot-run
# Or, fastest path for Rules add-on:
#   bash scripts/run-rules.sh --use-ngrok     # auto-detects https ngrok URL via 127.0.0.1:4040
# Or fully automatic (starts ngrok if needed):
#   make rules-up
# Then install using the printed manifest URL or 'make manifest-url'

# 6) Inspect & validate runtime manifest
make manifest-print
make manifest-validate-runtime
```

Commit proof template:
```
fix/feat/docs/ci: concise title

- What changed and why (1–3 bullets)
- Tied to failing test or validator output when applicable

Proof:
- python3 tools/validate-manifest.py → OK
- mvn -pl addons/addon-sdk -am test → Failures: 0, Errors: 0
- mvn -fae verify → BUILD SUCCESS
```

## Per‑Add‑on Zero‑Shot
Each add‑on can declare its own plan, scopes, components, webhooks, and lifecycle paths. Update the programmatic manifest in the entrypoint and use SDK helpers to keep the runtime manifest synchronized.

Quick references:
- Auto‑Tag Assistant: addons/auto-tag-assistant/README.md
- Rules: addons/rules/README.md
- Overtime: addons/overtime/README.md

Live checks (pick your add‑on):
```
export ADDON_BASE_URL=https://YOUR.ngrok-free.app/<addon>
make manifest-print
make manifest-validate-runtime
```
Validate multiple at once:
```
make manifest-validate-all URLS="https://.../rules https://.../auto-tag-assistant"
```

Quick customization checklist (per add‑on):
- Set manifest basics: `key`, `name`, `baseUrl`, `schemaVersion: "1.3"` (programmatically via `ClockifyManifest`)
- Choose `minimalSubscriptionPlan` and the smallest viable `scopes[]`
- Register lifecycle paths (`INSTALLED`, `DELETED`) and your webhook events/paths
- Register any UI components (e.g., `/settings` sidebar) and custom endpoints
- Validate: `make manifest-print` → confirm fields/paths; `make manifest-validate-runtime` → schema checks
- Test: module tests first, then full `mvn -fae verify`
- Observability: expose `/health` (optionally with DB probe) and `/metrics` (Prometheus), then monitor

## Common Hotspots (and files)
- Path Sanitization: addons/addon-sdk/src/main/java/com/clockify/addon/sdk/util/PathSanitizer.java
- Config Validation: addons/addon-sdk/src/main/java/com/clockify/addon/sdk/config/ConfigValidator.java
- CORS & Security Headers: addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/*
- Routing/Manifest: addons/addon-sdk/src/main/java/com/clockify/addon/sdk/*

Ripgrep to locate targets quickly:
```
rg -n "class PathSanitizer|ConfigValidator|registerLifecycleHandler|registerWebhookHandler" -S addons/addon-sdk/src/main/java
```

## Manifest & Lifecycle Rules
- Lifecycle entries use `{ "type": "INSTALLED|DELETED", "path": "/lifecycle/..." }`.
- Webhooks: `{ "event": "EVENT_NAME", "path": "/webhook" }` (or custom path).
- Default lifecycle paths are created when you call `registerLifecycleHandler("INSTALLED|DELETED", handler)`.
- Route → Manifest mapping tables live in each add‑on README and docs/MANIFEST_AND_LIFECYCLE.md.

Docs: docs/MANIFEST_AND_LIFECYCLE.md, docs/CLOCKIFY_PARAMETERS.md
Recipes and scope help: docs/MANIFEST_RECIPES.md, docs/PERMISSIONS_MATRIX.md

## CI & Coverage
- build-and-test runs tests on Temurin 17, uploads jacoco-aggregate; Pages fetches the artifact and generates coverage badge.
- Pages deploy runs after build-and-test succeeds on main.
 - smoke runs fast /health and /metrics checks on each add‑on.

Docs: docs/CI_OVERVIEW.md

## Observability
- `/health` — JSON health (add a DB check when `DB_URL`/`DB_USER` are set)
- `/metrics` — Prometheus scrape (Micrometer registry); not listed in manifest

Docs: docs/PRODUCTION-DEPLOYMENT.md#monitoring--observability, docs/SDK_OVERVIEW.md

## Validations (quick list)
- Manifests: `python3 tools/validate-manifest.py`
- Smoke tests: `make smoke` (or `mvn -e -pl addons/auto-tag-assistant,addons/rules,addons/overtime -am -Dtest=*SmokeIT test`)
- Module tests: `mvn -e -pl addons/addon-sdk -am test`
- Full verify (coverage): `mvn -e -fae verify`

Toolchain: Maven forks tests with Java 17 via Toolchains. Ensure `~/.m2/toolchains.xml` provides a JDK 17 (see docs/BUILD_ENVIRONMENT.md) if your host JDK is newer.

## Security Checklist (Minimum)
- Validate webhook signatures (HMAC-SHA256).
- Omit `$schema` in runtime manifests; use `schemaVersion: "1.3"`.
- Sanitize paths; block null bytes (`\u0000`, `%00`, `\\0`) and `..`.
- Store installation tokens per workspace (use DatabaseTokenStore for production).
- Add CSP frame‑ancestors; add CORS allowlist; enable rate limiting.

Docs: docs/SECURITY_CHECKLIST.md, docs/PRODUCTION-DEPLOYMENT.md

## Troubleshooting
- JDK mismatch (JDK 25+): set JAVA_HOME to 17; use Toolchains; run `mvn -Dprint.jvm.version=true` to show fork JVM.
- Schema failures: ensure lifecycle uses `type/path`; no `$schema` present; `schemaVersion` is "1.3".
- Coverage gates: write targeted tests near the changed code (addon-sdk has clear hotspots).

## Deep Links
- Zero‑Shot Playbook (strict commands): docs/AI_ZERO_SHOT_PLAYBOOK.md
- AI Onboarding (longer narrative): docs/AI_ONBOARDING.md
- Make Targets: docs/MAKE_TARGETS.md
- Quick Reference: docs/QUICK-REFERENCE.md
- Request/Response Examples: docs/REQUEST-RESPONSE-EXAMPLES.md
- PostgreSQL Guide: docs/POSTGRESQL_GUIDE.md
- Original Dev Docs snapshot: dev-docs-marketplace-cake-snapshot/
