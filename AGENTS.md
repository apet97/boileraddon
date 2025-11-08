# AI Contributor Guide (AGENTS.md)

Scope: Entire repository. These instructions apply to all files unless a more-specific AGENTS.md exists deeper in the tree.

## Principles
- Favor small, focused changes; avoid sweeping refactors.
- Keep links to the repo SHA‑pinned in docs and briefings — never link to `main`.
- Prefer Maven Central dependencies; do not add external repos.
- Java 17; use SLF4J for logging in SDK/runtime code. `System.out` is acceptable only in demo/CLI helper prints.
- Security first: validate config, never echo secrets, and keep manifests schema‑compliant.

## Build + Test
- Java & Maven: `java -version`, `mvn -version`
- Quick run targets:
  - `make build` or `make build-template`
  - `make run-auto-tag-assistant`
  - `make dev-rules` (reads `.env.rules`)
- Validate manifests:
  - `make validate` (built‑in checks)
  - `make schema-validate` (strong JSON Schema; requires `pip install jsonschema`)
- Tests and coverage:
  - `mvn test`
  - CI enforces JaCoCo min coverage for modules; aggregate report is published via Pages.

## Repo Map (AI‑useful)
- SDK core: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/**`
  - HTTP routing: `AddonServlet`, `ClockifyAddon`, `EmbeddedServer`
  - Middleware: `middleware/*` (RateLimiter, SecurityHeadersFilter, CorsFilter)
  - Security: `security/*` (TokenStore, WebhookSignatureValidator)
  - Path safety: `util/PathSanitizer`
- Demo add-on: `addons/auto-tag-assistant/**`
  - Entrypoint/wiring: `AutoTagAssistantApp.java`
  - Controllers: `ManifestController`, `SettingsController`, `WebhookHandlers`, `LifecycleHandlers`
  - Module security: `security/JwtTokenDecoder.java` (JWT claims helper)
- Template: `addons/_template-addon/**` (use for scaffolding new add‑ons)
- Docs: `docs/**` (Architecture, DB token store, Briefings workflow)
- Parameters: `docs/CLOCKIFY_PARAMETERS.md` (manifest fields, headers, env flags)
- SDK overview: `docs/SDK_OVERVIEW.md`
- Briefings: `_briefings/**` (pin to SHA; verify with `make briefings-verify`)
- Tools:
  - `tools/validate-manifest.py`, `tools/manifest.schema.json`
  - `tools/check_briefing_links.py`
- CI: `.github/workflows/**`

## Add/Change Features (Checklist)
1) Identify the module:
   - New add‑on → copy from `_template-addon` via `scripts/new-addon.sh` (if present) or replicate structure.
   - Demo add‑on → change `addons/auto-tag-assistant/**`.
   - SDK behavior → change `addons/addon-sdk/**`.
2) Wire endpoints via `ClockifyAddon`:
   - `registerCustomEndpoint`, `registerLifecycleHandler`, `registerWebhookHandler`.
   - Routing note: the SDK matches endpoint paths exactly (no wildcards). If you need identifiers,
     pass them via query parameters or JSON body (e.g., DELETE `/api/items?id=...`) or register
     an additional exact path. Keep manifest paths in sync (SDK auto‑updates runtime manifest entries).
   - For quick logic checks, add a dry‑run endpoint like `/api/test` that evaluates input without side effects.
3) Secure the surface:
   - Use `SecurityHeadersFilter`; enable CSP with `ADDON_FRAME_ANCESTORS` if required.
   - Rate limit via env: `ADDON_RATE_LIMIT`, `ADDON_LIMIT_BY`.
   - Use SDK security utilities: `security/TokenStore`, `security/WebhookSignatureValidator`.
4) Validate and test:
   - `make validate` / `make schema-validate`.
   - `mvn test` (module scope first), ensure coverage remains above thresholds.
5) Docs + briefings:
   - Update README/docs if user‑visible flows change.
   - If code/paths changed, regenerate briefings (see `tools/codex_prompts/BRIEFINGS_REGEN_WEB.md`).

## Manifests
- Must contain `"schemaVersion": "1.3"`; do not include `$schema`.
- Required: `key`, `name`, `schemaVersion`, `baseUrl`. Use programmatic runtime manifest where possible.

## Tokens and Persistence
- Demo uses in‑memory `TokenStore`. For production, implement/pick a persistent store (SDK includes `DatabaseTokenStore` class — integrate in your add‑on as needed).
- Provide envs: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` when you wire persistence.

## Env Flags (runtime)
- `ADDON_BASE_URL`, `ADDON_PORT`
- `ADDON_FRAME_ANCESTORS` (CSP frame‑ancestors)
- `ADDON_RATE_LIMIT` (double), `ADDON_LIMIT_BY` (ip|workspace)
- `ADDON_CORS_ORIGINS` (CSV), `ADDON_CORS_ALLOW_CREDENTIALS` (true|false)

## Commits
- Use focused messages:
  - `fix: ...`, `feat: ...`, `docs: ...`, `build: ...`, `ci: ...`, or the repo’s established patterns.
- Examples used in this repo: “Security/CI/DX: …”, “SDK/Middleware: …”.

## Don’ts
- Don’t link to `/blob/main/` in docs/briefings.
- Don’t add non‑Maven‑Central repos.
- Don’t commit secrets; use `.env` files and examples only.

## PR Checklist (AI)
- [ ] `make validate` + `make schema-validate`
- [ ] `mvn -q -pl <module> -am test` clean
- [ ] Coverage above thresholds (see CI)
- [ ] Docs updated (README or `docs/**`)
- [ ] Briefings regenerated if needed and links verified

## Quick Commands
- `make dev-check` — verifies local toolchain (java, mvn, ngrok)
- `make manifest-url` — prints the install URL
- `make briefings-open` / `make briefings-verify`
- `make rules-seed-demo` — seeds a demo rule and executes `/api/test`
- `make rules-webhook-sim` — computes HMAC and posts a signed webhook

---
If you’re an AI agent, start at `docs/AI_ONBOARDING.md` for a role‑friendly path and examples.
