# AI Onboarding — Clockify Add-on Boilerplate

Use this guide to start building immediately. It’s optimized for AI agents and junior contributors. For a strict, action‑only checklist, see docs/AI_ZERO_SHOT_PLAYBOOK.md (preferred for zero‑shot).

Important: Build with Java 17
- Ensure your environment runs Maven on JDK 17 and the forked test JVM is also 17.
- If your host default is newer (e.g., JDK 21/25), set JAVA_HOME to 17 and add a Toolchains file (`~/.m2/toolchains.xml`). See docs/BUILD_ENVIRONMENT.md.

## 1) Read the essentials (10 minutes)
- README.md (Quickstart + runtime manifest rules)
- docs/ZERO_SHOT.md (Checklist for zero‑shot start)
- docs/ARCHITECTURE.md (components and flows)
- SECURITY.md + THREAT_MODEL.md (security posture)
- docs/DATABASE_TOKEN_STORE.md (production token storage)
- docs/BRIEFINGS_WORKFLOW.md (how briefings are generated and kept pinned)

Optional deep dives:
- addons/addon-sdk/** (SDK runtime: routing, filters, path safety)
- addons/auto-tag-assistant/** (reference implementation)
- addons/_template-addon/** (scaffold for new add-ons)

## 2) Environment + sanity check
```bash
make dev-check            # verifies java, maven, ngrok
make build-template       # or: make build
```

If Java is not 17:
```
echo "Install JDK 17 and set JAVA_HOME or configure ~/.m2/toolchains.xml"
```

## 3) Run the demo add-on
```bash
make run-auto-tag-assistant
# In another terminal (optional):
ngrok http 8080
# Restart with HTTPS ngrok domain, then install using the runtime manifest URL
#   ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/auto-tag-assistant make run-auto-tag-assistant
#   make manifest-url
# Use the https URL printed by manifest-url to install in Clockify
```

Runtime options:
- Security headers CSP: `export ADDON_FRAME_ANCESTORS="'self' https://*.clockify.me"`
- Rate limiting: `export ADDON_RATE_LIMIT=10`, `export ADDON_LIMIT_BY=ip|workspace`
- CORS allowlist: `export ADDON_CORS_ORIGINS=https://app.clockify.me`, optional credentials: `export ADDON_CORS_ALLOW_CREDENTIALS=true`

## 4) Validate manifests
```bash
make validate
# Strong schema (requires Python pkg):
pip install --user jsonschema
make schema-validate
```

## 5) Typical tasks
- Add a new custom endpoint
  - Register via `ClockifyAddon.registerCustomEndpoint("/my-endpoint", handler)`
  - Add code in the appropriate controller class and return `HttpResponse`
  - Routing note: SDK matches paths exactly. Use query/body for identifiers or register
    another exact path (e.g., `DELETE /api/items?id=...`).
- Add lifecycle/webhook handling
  - `registerLifecycleHandler("INSTALLED", handler)`
  - `registerWebhookHandler("NEW_TIME_ENTRY", handler)`
  - Keep manifest entries in sync (SDK auto-updates runtime manifest list)
- Security
  - Use `SecurityHeadersFilter`; set `ADDON_FRAME_ANCESTORS` in staging/prod
  - Validate webhooks (`WebhookSignatureValidator` in demo)
  - Consider enabling `RateLimiter` and `CorsFilter` via env
- Persistence (prod)
  - Implement or integrate a persistent token store (see docs/DATABASE_TOKEN_STORE.md and SDK DatabaseTokenStore class)

## 6) Tests and coverage
```bash
mvn -q -pl addons/addon-sdk -am test   # start narrow and expand
# See CI for coverage thresholds; aggregate site is uploaded as artifact and to Pages
```

To prove the forked JVM version in Surefire:
```
mvn -pl addons/addon-sdk -am test -Dprint.jvm.version=true
# Look for: FORK JVM: 17.x
```

## 7) Docs + briefings
- Update README/docs for any new flows.
- Regenerate briefings when code/paths change:
  - `tools/codex_prompts/BRIEFINGS_REGEN_WEB.md` with PREV_SHA from `_briefings/INDEX.md`
  - Replace `_briefings/*.md` and `make briefings-verify`

## 8) Pull request checklist
- `make validate` (and `make schema-validate` if available)
- `mvn test` in the changed module(s)
- Coverage ≥ thresholds (CI enforces)
- Updated docs and, if applicable, refreshed `_briefings` pin

## 9) Where things live (pointers)
- SDK runtime: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/*`
- Filters: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/*`
- Demo add-on: `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/*`
- Template add-on: `addons/_template-addon/src/main/java/com/example/templateaddon/*`
- Tools and scripts: `tools/*`, `scripts/*`
- CI: `.github/workflows/*`

If you need a starting target, pick “add a new endpoint with input validation” on the demo add‑on and wire it with tests. Follow the checklists above.
