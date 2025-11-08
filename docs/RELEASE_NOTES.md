# Release Notes — Clockify Add‑on Boilerplate

## v0.1.1 — Consolidation and Developer UX (2025‑11‑08)

Highlights
- Developer signatures: accept both HMAC `clockify-webhook-signature` and Developer JWT header `Clockify-Signature` by default; toggle with `ADDON_ACCEPT_JWT_SIGNATURE=true|false`.
- Status endpoint docs: `/rules/status` reports `tokenPresent`, `applyChanges`, `skipSignatureVerify`, and `baseUrl`.
- Run experience: `scripts/run-rules.sh` guards against spaces in `--base-url` and prints clear hints; docs add ngrok single‑agent tips (`ERR_NGROK_108`).
- Settings UI: added “Copy manifest URL” and “Open install page” buttons to speed install.
- Event consistency: prefer `NEW_TIME_ENTRY` (docs and tests), alongside `TIME_ENTRY_UPDATED`.
- Tests: added a JWT acceptance test and `/status` smoke test for Rules.
- Build env: ready‑to‑copy `~/.m2/toolchains.xml` with OS paths and guidance for the common toolchain error.

Breaking changes
- None. Event alignment affects only examples/tests (use `NEW_TIME_ENTRY`).

Migration notes
- Install flow: Start the server first, then install the manifest URL printed by the app/UI.
- If older tests or internal examples referenced `TIME_ENTRY_CREATED`, switch to `NEW_TIME_ENTRY`.

Quick verify (local)
```
# Toolchain check (Java 17)
java -version && mvn -version

# Module tests first
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test

# Full reactor
mvn -e -DtrimStackTrace=false -fae verify
```

Install/run (Rules)
```
# Start ngrok in another terminal: ngrok http 8080
bash scripts/run-rules.sh --use-ngrok
# Install: use “Copy manifest URL” in /rules/settings or run: make manifest-url
```

Security
- Signature bypass is for local debugging only: `ADDON_SKIP_SIGNATURE_VERIFY=true`.
- JWT acceptance is intended for Developer flows; use HMAC in production where possible.

