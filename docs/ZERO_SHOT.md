# Zero‑Shot Developer Guide

This guide makes starting from scratch trivial. Follow exactly and you’ll have a working add‑on within minutes.

1) Pick your starting point
- Just try it (Rules): `cp .env.rules.example .env.rules && make dev-rules`
- Overtime MVP: `make build-overtime && ADDON_BASE_URL=http://localhost:8080/overtime make run-overtime`
- Create a new add‑on: `make new-addon NAME=my-addon DISPLAY="My Add-on"`
- One‑liner (zero‑shot): `TEMPLATE=auto-tag-assistant make zero-shot-run`

Tip: Pair with ngrok:

```
ngrok http 8080
TEMPLATE=auto-tag-assistant ADDON_BASE_URL=https://YOUR.ngrok-free.app/auto-tag-assistant make zero-shot-run
```

Docker zero‑shot (optional):

```
# Build and run selected add-on in Docker with a public base URL
ngrok http 8080
TEMPLATE=auto-tag-assistant \
ADDON_PORT=8080 \
ADDON_BASE_URL=https://YOUR.ngrok-free.app/auto-tag-assistant \
make docker-run
```
This target builds a runtime image, publishes port 8080, and forwards `ADDON_BASE_URL`/`ADDON_PORT` into the container.

2) Core rules (must comply)
- Manifest: `schemaVersion: "1.3"`; omit `$schema`; keep `key`, `name`, `baseUrl`, `components`, `webhooks`, `lifecycle`.
- Runtime manifest: serve via SDK (DefaultManifestController). Do not hand‑edit in production.
- Routing: exact path matching; pass IDs via query/body or register additional paths.
- Security: verify `clockify-webhook-signature` (HMAC-SHA256 of raw body) before processing.
- Tokens: store installation token per workspace (SDK TokenStore); use `x-addon-token` on API calls.
- HTTP: use SDK ClockifyHttpClient (timeouts, retries for 429/5xx); handle non‑2xx.

3) Minimal “Hello World” add‑on (template)
- Build: `make build-template`
- Run: `ADDON_BASE_URL=http://localhost:8080/_template-addon java -jar addons/_template-addon/target/_template-addon-0.1.0-jar-with-dependencies.jar`
- Verify:
  - `curl $ADDON_BASE_URL/health`
  - `curl $ADDON_BASE_URL/manifest.json`
  - `curl -X POST $ADDON_BASE_URL/api/test -H 'Content-Type: application/json' -d '{"hello":"world"}'`

4) Install via ngrok
- `ngrok http 8080`
- Restart with `ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/<addon>`
- `make manifest-url` → paste in Clockify Admin → Add‑ons → Install Custom Add‑on

Or use the one‑liner (replace <addon> with your module):

```
TEMPLATE=<addon> ADDON_BASE_URL=https://YOUR.ngrok-free.app/<addon> make zero-shot-run
```

5) Add a new endpoint (template)
- Register: `addon.registerCustomEndpoint("/api/items", handler);`
- Delete by id: `DELETE /api/items?id=<ID>` (or JSON body) — exact path is required.
- Dry‑run pattern: add `/api/test` endpoint to exercise logic without side effects.

6) Webhook best practice
- In your handler: `WebhookSignatureValidator.verify(request, workspaceId)`; 401/403 on gaps/mismatch.
- Parse payload (`clockify.jsonBody` or raw body) and extract fields.

7) Production switches
- Use DatabaseTokenStore (or your persistence) for tokens and app data.
- Enable CSP via `ADDON_FRAME_ANCESTORS`; CORS allowlist via `ADDON_CORS_ORIGINS`.
- Rate limiting via `ADDON_RATE_LIMIT`, `ADDON_LIMIT_BY` (ip|workspace).

8) Validate & test
- `make validate` (manifest checks)
- `mvn -q -pl <module> -am test`

Pointers
- Template code: addons/_template-addon/src/main/java/**
- Rules (automation): addons/rules/**
- Overtime (policy MVP): addons/overtime/**
- SDK: addons/addon-sdk/src/main/java/com/clockify/addon/sdk/**
