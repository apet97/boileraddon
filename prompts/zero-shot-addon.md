# Zero‑Shot Add-on Prompt (Clockify / CAKE Marketplace)

Use these files as ground truth:
- `dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md`
- `dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json`
- `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json`
- `templates/java-basic-addon` (reference runnable skeleton)

Strict rules:
- Always generate manifests that validate by running: `python3 tools/validate-manifest.py`.
- Use the Java template unless the user explicitly requests another stack.
- Do NOT invent manifest fields, lifecycle events, or webhook shapes. Only use what is present in the schemas and docs.
- Use `dev-docs-marketplace-cake-snapshot/extras/clockify-public-key.pem` for JWT verification examples.
- Respect rate limits from `dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json` (50 rps per addon per workspace).

When implementing:
- Start by copying `templates/java-basic-addon` and adjusting `manifest.json`, routes, and handlers.
- Resolve environment base URLs and claims from the token as described in the docs bundle.
- For any Clockify API calls, consult the OpenAPI file for paths, parameters, and models.
- Provide a short "how to run" with Maven build + ngrok exposure.
