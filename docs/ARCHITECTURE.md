# Architecture Overview

Components
- ManifestController — serves runtime manifest at `/{addon}/manifest.json`.
- LifecycleHandlers — processes INSTALLED/DELETED and stores workspace tokens via TokenStore.
- WebhookHandlers — handles time entry events using stored tokens and Clockify API.
- TokenStore — InMemory (demo) or DatabaseTokenStore (prod) selected by env.
- AutoTagAssistantApp — wires manifest, endpoints, and embedded server.

Key flows
1. Discovery: Clockify fetches `{baseUrl}/manifest.json`.
2. Install: Clockify posts `INSTALLED` with workspace token; token persisted via TokenStore.
3. Webhooks: Clockify posts events; signature verified using stored installation token.
4. UI: Sidebar loads `{baseUrl}/settings` in an iframe.

Configuration
- ADDON_BASE_URL and ADDON_PORT define runtime URLs.
- DB_URL/DB_USERNAME/DB_PASSWORD select DatabaseTokenStore.

See also
- README.md (Quickstart)
- docs/DATABASE_TOKEN_STORE.md
- SECURITY.md, THREAT_MODEL.md
