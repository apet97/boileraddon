Addon Engineer Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Clone and customize add-ons using scaffold script and template guidance.

Implement manifest, lifecycle, webhook, and settings controllers with inline SDK helpers.

Manage workspace tokens, API calls, and webhook signature verification paths.

Follow Quickstart/Makefile workflow for local development, ngrok exposure, and Clockify installation.

Primary artifacts in repo:

Building Your Own Add-on guide

Scaffold script new-addon.sh

Auto-Tag Assistant implementation

How to do your job:

Run scripts/new-addon.sh to clone the template, rewrite packages, and register the module in the parent build.

Update manifest builder within your App class to set key, name, scopes, components, webhooks, and lifecycle endpoints.

Replace template TODOs for lifecycle persistence and webhook processing with production logic; store tokens via TokenStore (or your persistent implementation).

Validate manifests using the provided Python script before installing or committing.

Run the add-on locally via Makefile/Quickstart, expose with ngrok, and install in Clockify for end-to-end testing.

Implement webhook signature validation and API calls using ClockifyHttpClient and WebhookSignatureValidator patterns.

Decode JWT claims when your UI or webhooks require environment-specific URLs, using JwtTokenDecoder helpers.

Critical decisions already made:

Runtime manifest is generated programmatically and must stay schema-compliant without $schema field; helper SDK updates manifest automatically.

Token storage defaults to in-memory for demos, but production expects persistent storage following guide recommendations.

Local workflow uses Makefile + ngrok to mirror Clockify installation flow; documentation assumes this path for support.

Open questions and risks:

Owner	Source	Link
Addon Engineer	Template manifest description still TODO—update before publishing derived add-ons.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/manifest.json#L2-L18
Addon Engineer	Template settings and webhook handlers contain TODO placeholders; implement real UI and business logic for any shipped add-on.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/SettingsController.java#L10-L32
Commands or APIs you will call (if any):

scripts/new-addon.sh my-addon "My Add-on"

References:

Building Your Own Add-on guide.

Scaffold script for automation.

Auto-Tag Assistant README for architecture and troubleshooting.
