# Rules Add‑on — Declarative Automations

The Rules add‑on lets admins define “if … then …” automations for Clockify time entries (AND/OR logic).

- Conditions: descriptionContains, descriptionEquals, hasTag, projectIdEquals, isBillable
- Actions: add_tag, remove_tag, set_description, set_billable
- Manifest key: `rules`; base path: `/rules`

## Install and Run

```bash
# Build the module
make build-rules

# Run locally
ADDON_BASE_URL=http://localhost:8080/rules make run-rules

# Expose via ngrok (separate terminal)
ngrok http 8080
# Restart with ngrok base URL
#   ADDON_BASE_URL=https://<ngrok-domain>/rules make run-rules
# Install in Clockify using: https://<ngrok-domain>/rules/manifest.json
```

Optional runtime safeguards:
- Security headers: `export ADDON_FRAME_ANCESTORS="'self' https://*.clockify.me"`
- Rate limiting: `export ADDON_RATE_LIMIT=10` and `export ADDON_LIMIT_BY=ip|workspace`
- CORS allowlist: `export ADDON_CORS_ORIGINS=https://app.clockify.me` (optional credentials: `export ADDON_CORS_ALLOW_CREDENTIALS=true`)
- Request logging: `export ADDON_REQUEST_LOGGING=true`

## API

- `GET /rules/manifest.json` — runtime manifest (v1.3; no `$schema`)
- `GET /rules/settings` — sidebar UI
- `GET /rules/api/rules?workspaceId=...` — list rules
- `POST /rules/api/rules?workspaceId=...` — create/update rule (id auto‑generated if omitted)
- `DELETE /rules/api/rules?id=<id>&workspaceId=...` — delete rule by id
- `POST /rules/api/test` — evaluate rules against provided payload; no side effects

### Rule JSON schema (example)

```json
{
  "name": "Tag client meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"},
    {"type": "hasTag", "operator": "EQUALS", "value": "client"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}
```

### Quick curl examples

```bash
# Create/update a rule
curl -s -X POST \
  "http://localhost:8080/rules/api/rules?workspaceId=workspace-1" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Tag client meetings",
    "enabled":true,
    "combinator":"AND",
    "conditions":[{"type":"descriptionContains","operator":"CONTAINS","value":"meeting"}],
    "actions":[{"type":"add_tag","args":{"tag":"billable"}}]
  }'

# List rules
curl -s "http://localhost:8080/rules/api/rules?workspaceId=workspace-1"

# Delete rule by id
curl -s -X DELETE "http://localhost:8080/rules/api/rules?id=<ID>&workspaceId=workspace-1"

# Dry‑run evaluation (no side effects)
curl -s -X POST http://localhost:8080/rules/api/test \
  -H 'Content-Type: application/json' \
  -d '{
    "workspaceId":"workspace-1",
    "timeEntry":{"id":"e1","description":"Client meeting","tagIds":[]}
  }'
```

## Security
- Webhook signature verification enforced; missing/invalid signature → 401/403
- Token storage is in‑memory for demo; use a persistent store before production
- See SECURITY.md and THREAT_MODEL.md for the full posture

## Notes
- The demo logs matched actions by default; to apply changes, set `RULES_APPLY_CHANGES=true`.
- In production, call Clockify APIs to apply changes and ensure idempotence (e.g., don’t re‑add existing tags).

### Applying actions (example)

Use the SDK `ClockifyHttpClient` with the stored workspace token:

```java
var wk = com.clockify.addon.sdk.security.TokenStore.get(workspaceId).orElseThrow();
var http = new com.clockify.addon.sdk.http.ClockifyHttpClient(wk.apiBaseUrl());

// Example: add a tag to a time entry (pseudo-path; consult API-COOKBOOK for exact endpoint)
String body = "{\"tagIds\":[\"" + tagId + "\"]}";
var resp = http.putJson("/v1/workspaces/" + workspaceId + "/time-entries/" + timeEntryId, wk.token(), body, java.util.Map.of());
if (resp.statusCode() / 100 != 2) {
  // handle error, retry logic embedded for 429/5xx
}
```

```
make validate           # basic manifest checks
pip install jsonschema  # once
make schema-validate    # strong schema checks
```
