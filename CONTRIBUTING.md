# Contributing

Thanks for contributing to the Clockify Add-on Boilerplate!

Getting started
- Java 17+, Maven 3.6+
- Build everything: `make build`
- Local quick start (Rules): `cp .env.rules.example .env.rules && make dev-rules`
- Ngrok testing: see docs/NGROK_TESTING.md

Before you open a PR
- `make validate` (and `make schema-validate` if available)
- `mvn -q -pl <module> -am test`
- Update docs if behavior or paths change
- Regenerate `_briefings` if inbound links changed (make briefings-verify)

Coding guidelines
- Java 17; SLF4J for logging (System.out only for demo prints)
- Prefer SDK helpers (TokenStore, WebhookSignatureValidator, ClockifyHttpClient)
- Keep routing exact-match; pass IDs in query/body

Docs
- docs/QUICK_START_LOCAL.md, docs/NGROK_TESTING.md
- docs/SDK_OVERVIEW.md and docs/ADDON_RULES.md for architecture

Security
- No `$schema` in runtime manifests
- Verify `clockify-webhook-signature` for webhooks
- Don’t log tokens; use validation scripts to catch issues

Thanks!

