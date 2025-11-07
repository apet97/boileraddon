# Boiler Add-on Dev Docs Bundle

This repo includes a complete snapshot of CAKE.com Marketplace developer docs for Clockify, plus extras to enable zero-shot, shippable add-ons.

- Snapshot root: `dev-docs-marketplace-cake-snapshot/`
  - `html/` — per-page HTML snapshots (48 pages)
  - `cake_marketplace_dev_docs.md` — combined Markdown
  - `urls.txt` — all discovered URLs
  - `crawl_log.txt` — crawl log
  - `extras/` — add-on manifest schema versions, Clockify OpenAPI, SDK, UI Kit shell, webhook payload samples, Maven setup

Generated on: $(date -u)

## AI Usage Guidelines
If you are an AI generating an add-on in this repo, follow this:
- Read `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md` first.
- Validate manifests against `dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json`.
- Use `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json` for Clockify API calls.
- Start from `templates/java-basic-addon` (or add a Node template similarly).
- Do not invent fields; only use what schemas define.
- For JWT verification examples, use `dev-docs-marketplace-cake-snapshot/extras/clockify-public-key.pem`.
- Observe rate limits in the OpenAPI (50 rps per addon per workspace).

## SDK Provenance
- Source: https://github.com/clockify/addon-java-sdk (vendored under `dev-docs-marketplace-cake-snapshot/extras/addon-java-sdk`)
- Version: latest commit at time of snapshot (for reference/tooling only). Check upstream for license and latest changes.
