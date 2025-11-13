# Repository Structure

This document gives a high-level map of the repository to help you find code quickly and understand how modules relate.

## Modules

- Root `pom.xml` (packaging=pom)
  - Aggregates all add-ons and the in-repo SDK.
  - Pins Java 17, Surefire/Failsafe 3.2.5, JaCoCo 0.8.12, and uses Toolchains.

- addons/addon-sdk (jar)
  - SDK classes shared by all add-ons.
  - HTTP routing: `ClockifyAddon`, `AddonServlet`, `EmbeddedServer`
  - Middleware: `middleware/*` (CORS, Security Headers, RateLimiter, Request Logging)
  - Security: `security/*` (TokenStore, WebhookSignatureValidator)
  - Utilities: `util/PathSanitizer`
  - Tests cover routing, forwarded headers, middleware, and security helpers.

- addons/rules (jar)
  - Canonical production target. Includes `RulesConfiguration` (single source of truth for env vars), `RuntimeFlags` (dev-only toggles), JWT bootstrap (`JwtVerifier`, `WorkspaceContextFilter`, `PlatformAuthFilter`), persistence (`DatabaseRulesStore`, `PooledDatabaseTokenStore`), middleware (`SecurityHeadersFilter`, `SensitiveHeaderFilter`, optional RateLimiter/CorsFilter), webhook idempotency cache, health/readiness handlers, and Prometheus metrics.

- addons/auto-tag-assistant (jar)
  - Sample/demonstration add-on that analyzes time entries and applies/suggests tags.
  - Useful for quick experiments and smaller patterns (manifest wiring, lifecycle handlers, webhook processing, Clockify API calls).

- addons/_template-addon (jar)
  - Minimal template to scaffold a new add-on; build/run and install via ngrok.

- addons/overtime (jar)
  - Policy MVP example; can be used as a second starter add-on.

## Docs

- docs/BUILD_ENVIRONMENT.md — Java 17 + Toolchains playbook
- docs/SDK_OVERVIEW.md — SDK classes and how to wire endpoints
- docs/ZERO_SHOT.md — zero-shot path to a working add-on
- docs/TESTING_GUIDE.md — running tests/coverage, CI gates, PR coverage comment
- docs/IMPROVEMENTS-SUMMARY.md — changes and rationale
- docs/coverage/ — published aggregate coverage site, badge.svg, summary.json

## Tools

- tools/validate-manifest.py — validates add-on manifests against `tools/manifest.schema.json`
- tools/coverage_badge.py — generates `docs/coverage/badge.svg` and `summary.json` from `jacoco.xml`

## Workflows

- .github/workflows/validate.yml — schema validation and basic tests
- .github/workflows/build-and-test.yml — full build, artifacts, and PR coverage comment (with delta vs main)
- .github/workflows/jekyll-gh-pages.yml — publishes docs and coverage to Pages

## Developer Notes

- Use Java 17 for both Maven and the forked test JVM (see Toolchains).
- Routing is exact-path; pass identifiers via query/body or register additional paths.
- Always validate `clockify-webhook-signature` before processing webhooks; store installation tokens per workspace.
