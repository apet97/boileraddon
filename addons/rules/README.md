# Rules Add-on
[AI START HERE](../../docs/AI_START_HERE.md)

Automation starter for Clockify rules (if-this-then-that for time entries). The runtime manifest is built in `RulesApp` with key `rules`, baseUrl from `ADDON_BASE_URL` (default `http://localhost:8080/rules`), and scopes `TIME_ENTRY_READ`, `TIME_ENTRY_WRITE`, `TAG_READ`, `TAG_WRITE`. Apply mode is controlled by `RULES_APPLY_CHANGES` (default: dry-run).

- Endpoints: `/manifest.json`, `/health` + `/ready` (runtime health), `/metrics` (Prometheus), `/status`, `/settings` (sidebar UI), `/api/rules` (GET/POST/DELETE per-workspace rules), `/api/test`.
- Lifecycle: `/lifecycle/installed`, `/lifecycle/deleted` (TokenStore persistence).
- Webhook: `TIME_ENTRY_UPDATED` → verifies signature via `WebhookSignatureValidator.verify`, evaluates saved rules, and calls the Clockify API to apply matching tags (or dry-run if `RULES_APPLY_CHANGES=false`).

## Quick start
1. Export `ADDON_BASE_URL` (ngrok HTTPS recommended) and optional `ADDON_PORT`/`ENV`. Default is `http://localhost:8080/rules`.
2. Build and run: `mvn -q -pl addons/rules -am package` then `java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar`.
3. Register rules: `curl -X POST "$ADDON_BASE_URL/api/rules?workspaceId=<ws>" -H 'Content-Type: application/json' -d '{"matchText":"meeting","tag":"meetings"}'`. Delete with `DELETE /api/rules?id=<ruleId>&workspaceId=<ws>`.
4. Send a signed `TIME_ENTRY_UPDATED` webhook payload containing `workspaceId` and `timeEntry` to trigger actions. If `RULES_APPLY_CHANGES=false`, actions are logged but not sent to the Clockify API.
5. Sidebar UI: `/settings` lists and manages rules. In dev without platform JWT, append `?workspaceId=<ws>` to the URL so the UI can call `/api/rules`.
