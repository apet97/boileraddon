# Rules Improvement Pass 2 (2025-11-16)

## Changes by Theme

### Idempotency backend visibility
- `addons/rules/README.md` now has a dedicated “Webhook idempotency backend visibility” section that spells out the log signature, Prometheus gauge, and `/debug/config` field so ops can confirm whether dedupe is DB-backed or node-local with zero guesswork.
- `RULES_ADDON_PRODUCTION_SUMMARY.md` calls out the same signals (log line, gauge, dev endpoint) in the webhook section so runbooks highlight how to verify the active backend.

### Debug/config endpoint ergonomics
- Added a “Debugging configuration (dev only)” section in the README describing what `/debug/config` returns (environment, TTL, backend mode, token store mode, JWT source, runtime flags) and reiterating that it is only wired when `ENV=dev`.
- The docs emphasize that staging/prod still rely on logs + metrics, keeping the endpoint safely scoped to local tooling.

### Dev flag safety
- The new README section also documents the `runtimeFlags` snapshot (`applyChanges`, `skipSignatureVerify`), reinforcing that `ADDON_SKIP_SIGNATURE_VERIFY` only activates in dev/local environments and making the guardrails visible to anyone reusing the endpoint.

### 412 regression coverage
- Added controller tests for clients (`ClientsControllerTest`), tasks (`TasksControllerTest`), and tags (`TagsControllerTest`) to ensure create/update/delete flows continue to return HTTP 412 with `RULES.MISSING_TOKEN` whenever the installation token is missing.
- Extended `WorkspaceExplorerControllerTest` with a token-missing scenario to prove the HTTP layer propagates the `EXPLORER.TOKEN_NOT_FOUND` 412 emitted by the gateway/service.

## Validation
- `mvn -q -pl addons/rules -am test -DtrimStackTrace=false` → OK
- `mvn -q clean verify -DtrimStackTrace=false` → OK

## Known limitations
- Webhook dedupe remains TTL-based; in-memory mode is node-local, so multi-replica deployments still need the shared DB settings enabled for consistent results.
- `/debug/config` stays dev-only by design, so observability in staging/prod relies on the log line + Prometheus gauge noted above.
