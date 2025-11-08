# FAQ

Answers to common issues when developing and running add‑ons with this boilerplate.

## Why does Maven suggest “resume with -rf :addon-sdk” and no surefire reports are created?

Your host Maven/JDK is newer than the test fork JVM JaCoCo/Surefire support. Fix by pinning tests to Java 17 via Toolchains.

- Add `~/.m2/toolchains.xml` with a JDK 17 entry (see docs/BUILD_ENVIRONMENT.md)
- The root POM already pins Surefire/Failsafe to use `<jdkToolchain><version>17</version></jdkToolchain>`
- Verify: `java -version && mvn -version` (both should show 17)

## JaCoCo fails with “Unsupported class file major version 69”

Mockito/Byte Buddy can generate classes with the host JDK class version. If the host is newer than the agent understands, JaCoCo fails.

- Ensure tests fork Java 17 via Toolchains (above)
- Exclude Mockito/Byte Buddy generated classes from instrumentation when needed (see addons/rules/pom.xml)

## “No tests matching pattern *SmokeIT” in smoke workflow

Smoke tests run per module. If you run a reactor‑wide `-Dtest=*SmokeIT`, modules without SmokeIT cause a failure.

- Our CI runs SmokeIT per module. Locally use:
  - `make smoke` (installs `addon-sdk`, then runs per‑module tests)

## Why does Clockify reject my manifest?

Runtime manifests must:
- Omit `$schema`
- Include `"schemaVersion": "1.3"`
- Use exact `type/path` for lifecycle and `event/path` for webhooks

Validate with:
- `python3 tools/validate-manifest.py`
- `make manifest-validate-runtime` (validates a live manifest)

## Webhook signature failing

- Ensure you are verifying `clockify-webhook-signature` with the installation token stored per workspace
- Use SDK `WebhookSignatureValidator.verify(request, workspaceId)`
- For tests, preload `CLOCKIFY_WORKSPACE_ID` and `CLOCKIFY_INSTALLATION_TOKEN` envs when running locally

## My add‑on responds with 404

Routing is exact‑path. Register the exact path or use query/body parameters for identifiers.
- Example: register `/api/items` and call `DELETE /api/items?id=...` (or JSON body)

## How do I change required scopes or plan?

Use the programmatic manifest builder and update `minimalSubscriptionPlan` and `scopes[]` in your app entrypoint.
- See docs/MANIFEST_RECIPES.md and docs/PERMISSIONS_MATRIX.md

## What should I do for production?

- Persist installation tokens in a database (`DatabaseTokenStore`) — avoid in‑memory in production
- Enable HTTPS, rate limiting, and CSP (SecurityHeadersFilter)
- Set up backups, monitoring, structured logs, and `/health` checks
- See docs/PRODUCTION-DEPLOYMENT.md and docs/SECURITY_CHECKLIST.md

