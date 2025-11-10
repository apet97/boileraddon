# Clockify Developer Workspace Runbook (Zero → Working)

This runbook gets you from a fresh clone to a working Rules add‑on in a Clockify Developer workspace. It uses only native Clockify endpoints and a local server exposed via ngrok.

## 0) Prerequisites
- JDK 17 and Maven
- ngrok (free account is fine)

Check versions:
```
java -version
mvn -version
ngrok version
```
If Maven test JVMs fail on newer JDKs, configure Toolchains: see docs/BUILD_ENVIRONMENT.md (ready‑to‑copy ~/.m2/toolchains.xml included).

## 1) Clone the repo
```
# HTTPS
git clone https://github.com/apet97/boileraddon.git
cd boileraddon
```

## 2) Start ngrok (single agent)
In a separate terminal:
```
ngrok http 8080
```
- Free plan allows one ngrok agent. If you hit ERR_NGROK_108, stop stray agents: `killall ngrok`.
- You can always read the current public URL: `curl http://127.0.0.1:4040/api/tunnels`.

## 3) Run the Rules add‑on
Option A — auto‑detect the ngrok URL:
```
bash scripts/run-rules.sh --use-ngrok
```
Option B — pass the URL explicitly (quote it):
```
bash scripts/run-rules.sh --base-url "https://<sub>.ngrok-free.app/rules"
```
Option C — convenience wrapper (builds then delegates):
```
bash fix-and-run.sh --use-ngrok
```
Notes:
- Start the server first, then install the manifest (avoids caching a stale URL in Developer).
- You may enable mutations by adding `--apply` (or `RULES_APPLY_CHANGES=true`):
```
bash scripts/run-rules.sh --use-ngrok --apply
```
- Dev bypass (for local debug only): `ADDON_SKIP_SIGNATURE_VERIFY=true` (not needed in Developer; webhooks are signed and accepted).
 - Security defaults: the run script applies safe defaults so the UI is only embeddable in Clockify and CORS is restricted to Clockify origins:
   - `ADDON_FRAME_ANCESTORS='self' https://*.clockify.me`
   - `ADDON_CORS_ORIGINS=https://app.clockify.me,https://developer.clockify.me`
   - `ADDON_CORS_ALLOW_CREDENTIALS=false` (override to true if your flow needs it)

## 4) Install in Clockify Developer
- Open Developer (e.g., https://developer.clockify.me)
- Go to your workspace → Settings → “Install Custom Add‑on”
- Use the printed manifest URL:
```
https://<sub>.ngrok-free.app/rules/manifest.json
```
- Watch the server logs for:
  - LIFECYCLE EVENT: INSTALLED
  - Stored auth token for your workspace
  - Automatic preload of workspace cache (tags/projects/clients/users/tasks)

## 5) Verify status and cache
- Status (token present / runtime flags):
```
curl "https://<sub>.ngrok-free.app/rules/status?workspaceId=<YOUR_WS>"
```
- Cache summary and data (optional diagnostics):
```
curl "https://<sub>.ngrok-free.app/rules/api/cache?workspaceId=<YOUR_WS>"
curl "https://<sub>.ngrok-free.app/rules/api/cache/data?workspaceId=<YOUR_WS>" | jq . | head -n 60
```

## 6) Configure a rule (admin‑only sidebar)
- Open the add‑on in Clockify (Admins only). The UI is embeddable only inside Clockify when you set:
  - `ADDON_FRAME_ANCESTORS='self' https://*.clockify.me`
  - `ADDON_CORS_ORIGINS=https://app.clockify.me,https://developer.clockify.me` (optional)

### Option A: Simple Rules Builder (`/rules/settings`)
- Enter `workspaceId`
- Click "Load Data" (preloaded entities enable autocompletes)
- Create a rule, e.g.:
  - Condition: descriptionContains CONTAINS "meeting"
  - Action: add_tag tag "billable"
- Save

### Option B: IFTTT Builder (`/rules/ifttt`) — Advanced Automations
The IFTTT builder lets you create powerful automations with any webhook trigger and custom API actions:

1. **Navigate to the IFTTT Builder**:
   - Click "Open IFTTT Builder" from `/rules/settings`, or
   - Browse directly to `<baseUrl>/ifttt`

2. **Configure Workspace**:
   - Enter your `workspaceId`
   - Click "Load Workspace Data" to enable autocompletes

3. **Select a Trigger**:
   - Browse or search webhook events (e.g., NEW_TIME_ENTRY, PROJECT_UPDATED, NEW_CLIENT)
   - Click an event to select it
   - Optionally add filter conditions using JSON path queries (e.g., `project.name contains "ACME"`)

4. **Compose Actions**:
   - Click "+ Add Action"
   - Click "Select Endpoint" and choose from the OpenAPI catalog
   - Fill in required parameters and body fields
   - Use placeholders like `{{timeEntry.id}}`, `{{project.name}}` to insert dynamic values
   - Preview the HTTP request before saving

5. **Save and Test**:
   - Enter a rule name
   - Click "Save Rule"
   - Perform the trigger action in Clockify to test (e.g., create a time entry)
   - Watch server logs for webhook processing and action execution

**Example IFTTT Rule**:
- **Trigger**: NEW_TIME_ENTRY where `project.clientName` contains "ACME"
- **Action**: Update time entry to set `billable: true` and add tag
- **Result**: All time entries for ACME projects are automatically marked billable

## 7) Test in Developer
- Create/Update time entries that match your rule in the installed workspace.
- Watch server logs for webhook processing:
- Signatures: validator accepts HMAC (`clockify-webhook-signature`) and Developer JWT header (`Clockify-Signature`) when you opt in with `ADDON_ACCEPT_JWT_SIGNATURE=true` (default `false`). No need to skip verification once enabled.
  - Responses like:
```
{"event":"NEW_TIME_ENTRY","status":"actions_applied","actionsCount":1,"actions":[...]}
```
- If you see 401 “signature header missing”, confirm you installed after starting the server and that the Base URL matches your current ngrok URL. Also ensure `ADDON_ACCEPT_JWT_SIGNATURE` is true (default).

## 8) Apply real changes (optional)
- Start with `--apply` or set `RULES_APPLY_CHANGES=true`. The add‑on uses the stored installation token to call Clockify APIs (idempotent patterns; avoids re‑adding existing tags).

## 9) Useful endpoints
- Health/metrics:
```
https://<sub>.ngrok-free.app/rules/health
https://<sub>.ngrok-free.app/rules/metrics
```
- Manifest (runtime):
```
https://<sub>.ngrok-free.app/rules/manifest.json
```

## 10) Troubleshooting
- ngrok ERR_NGROK_108 → `killall ngrok`, restart ngrok, re‑run the add‑on with the new URL.
- UI stale or double slashes // → refresh the page; UI safely recomputes baseUrl and avoids `//`.
- Toolchains error `{version=17}` → see docs/BUILD_ENVIRONMENT.md and ensure JDK 17 is configured.

## Security defaults
- Webhook signature verification enforced.
- UI is Admins‑only (manifest component) and embeddable only in Clockify when you set the frame‑ancestors header.
- CORS is disabled unless explicitly allowed.

## Next
- See docs/WEBHOOK_IFTTT.md for a mapping of webhook triggers to native Clockify API actions (useful for designing automations).
- Autocomplete picks are backed by the preloaded workspace cache, so you can select names and the add‑on resolves IDs automatically.
