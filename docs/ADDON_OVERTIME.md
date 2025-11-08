# Overtime Add-on — Product Spec (PM)

Purpose
- Automatically detect and act on overtime conditions (daily/weekly/monthly) using Clockify events and API.
- Provide guardrails (notifications, tags, status changes) and a clear audit trail.

Primary Personas
- Workspace Admins (configure rules and policies)
- Team Leads (monitor exceptions)
- Individual Users (see notices and time entry annotations)

MVP Scope
- Inputs
  - Webhooks: `NEW_TIMER_STARTED`, `TIMER_STOPPED`, `NEW_TIME_ENTRY`, `TIME_ENTRY_UPDATED`.
  - Configurable thresholds: daily hours, weekly hours; optional per-user overrides.
  - Exclusions: projects/tags to ignore (e.g., admin tasks), holidays (manual list for MVP).
- Actions
  - Apply an “Overtime” tag on affected entries (configurable tag name)
  - Post a sidebar notice via settings UI (soft feedback)
  - Optional: create a comment/annotation via API if supported
- Non-goals (MVP): auto-approval flows, payroll export, cross-workspace rollups.

Settings & Configuration
- Sidebar UI (`/settings`):
  - Global thresholds: daily N hours, weekly M hours
  - Exclusions: projects (by id), tags, users
  - Tag name for overtime (default: "Overtime")
  - Behavior: preview-only vs. apply changes
  - Dry-run endpoint: `/api/test` to evaluate a payload without mutations

Event Flow
1) Timer stopped or entry updated
2) Add-on calculates total time for the user in the relevant window (day/week)
3) If total exceeds thresholds (and not excluded), mark the entry:
   - Apply configured tag
   - Record a note (if enabled)
   - Replay-safe: re-check idempotently to avoid double-tagging

API Usage
- Fetch user time totals for window (day/week) via Clockify Reports/Entries APIs
- Update time entry tags via REST (idempotent operations)
- Respect rate limits (429) and implement retries

Security & Compliance
- Validate webhook signatures (SDK WebhookSignatureValidator)
- Store installation tokens via SDK TokenStore (use DatabaseTokenStore in prod)
- Apply SecurityHeadersFilter and consider CORS allowlist for settings UI

Manifest (example)
```json
{
  "key": "overtime",
  "name": "Overtime Policy",
  "schemaVersion": "1.3",
  "baseUrl": "https://YOUR_DOMAIN/overtime",
  "components": {"sidebar": {"path": "/settings", "accessLevel": "ADMINS"}},
  "webhooks": [
    {"event": "TIMER_STOPPED", "path": "/webhook"},
    {"event": "TIME_ENTRY_UPDATED", "path": "/webhook"}
  ],
  "lifecycle": [
    {"type": "INSTALLED", "path": "/lifecycle/installed"},
    {"type": "DELETED", "path": "/lifecycle/deleted"}
  ]
}
```

Endpoints
- `GET /health` — liveness
- `GET /manifest.json` — runtime manifest (no `$schema`)
- `GET /settings` — admin UI
- `POST /webhook` — process events (signature validated)
- `POST /api/test` — dry-run evaluator (no external mutations)

Acceptance Criteria
- Given a user exceeds daily threshold, the latest modified entry gets tagged within one webhook cycle
- Excluded projects/tags are never marked
- Retries handle 429/5xx without duplicate tagging
- Settings persisted and applied on subsequent events

KPIs
- % of overtime entries correctly tagged
- Mean webhook processing latency
- Rate of failed API calls (429/5xx)

Operational Notes
- Use SDK ClockifyHttpClient for timeouts/retries with `x-addon-token`
- Consider batching reads to minimize API calls for weekly rollups
- Provide clear logs with workspaceId and userId for observability

References
- docs/CLOCKIFY_PARAMETERS.md — canonical parameters and headers
- docs/REQUEST-RESPONSE-EXAMPLES.md — lifecycle and webhook examples
- docs/API-COOKBOOK.md — API call patterns

