Security & Privacy Lead Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Enforce environment variable validation, secrets management, and webhook hardening guidance.

Validate token storage approach, JWT decoding, and signature verification implementation.

Review rate limiting, input sanitization, and HTTP client timeout strategies for abuse prevention.

Ensure compliance with Clockify environment separation and regional endpoints.

Primary artifacts in repo:

Production Deployment Guide — security section

WebhookSignatureValidator.java

JwtTokenDecoder.java

How to do your job:

Enforce .env setup with strong webhook secret generation and validate config via ConfigValidator.

Mandate DatabaseTokenStore (or equivalent) in production to prevent credential loss and enable audit trails.

Require webhook signature verification path using installation tokens and base64-safe comparison.

Ensure JWT environment claims are parsed and normalized before routing requests to region-specific APIs.

Validate rate limiter deployment (per-IP/workspace) and align with upstream proxies or distributed caches.

Confirm HTTP client applies timeouts, retries, and rate-limit handling for safe API interactions.

Implement HTTPS-only connectors, security headers, and HSTS policies across environments.

Critical decisions already made:

Security hardening now includes path sanitization, rate limiting, config validation, and signature checks as baseline features.

Production guide mandates TLS, credential rotation, and database-backed token storage.

Webhook processing validates signatures against stored installation tokens before proceeding.

Open questions and risks:

Owner	Source	Link
Engineering Lead	Template lifecycle handler still lacks real persistence logic; security review needed once storage mechanism is chosen.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L14-L55
Engineering Lead	Auto add-on logs a TODO when auth token missing; determine fallback handling to avoid webhook bypass.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java#L47-L63
Commands or APIs you will call (if any):

openssl rand -hex 32

References:

Production deployment security checklist.

Webhook signature validator implementation.

JWT decoder guidance.
