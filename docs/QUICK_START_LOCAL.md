Quick Start — Local Usage

This guide gets you running the add-ons locally in minutes, with no surprises.

Rules Add-on (local)
- Build: make build-rules
- Run (env file): cp .env.rules.example .env.rules, then make dev-rules
- One-liner (auto-detect ngrok if running): bash scripts/run-rules.sh --use-ngrok
- Fully automatic (starts ngrok if needed): make rules-up
- Health: curl http://localhost:8080/rules/health
- Manifest: make manifest-url (prints runtime URL)
- Seed + dry-run: export WORKSPACE_ID=your-ws-id; make rules-seed-demo
- Simulate webhook: export WORKSPACE_ID and CLOCKIFY_INSTALLATION_TOKEN; make rules-webhook-sim
- Apply real changes: set RULES_APPLY_CHANGES=true in .env.rules and restart (requires valid installation token)

Rules Add-on (ngrok)
- ngrok http 8080
- bash scripts/run-rules.sh --use-ngrok   # or: make run-rules-ngrok
- make manifest-url → install in Clockify using the printed https URL

Auto-Tag Assistant (ngrok)
- ngrok http 8080
- ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/auto-tag-assistant make run-auto-tag-assistant
- make manifest-url → Install in Clockify using the printed https URL

Notes
- Do not edit manifest.json for baseUrl; set ADDON_BASE_URL and restart.
- Webhook headers: `clockify-webhook-signature` (HMAC-SHA256 of raw body using installation token), and Developer’s `Clockify-Signature` (JWT) which is accepted only when `ADDON_ACCEPT_JWT_SIGNATURE=true` (default `false`).
- Use docs/NGROK_TESTING.md for a deeper, step-by-step runbook.
