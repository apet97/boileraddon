Docs & Developer Experience Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Maintain documentation map optimized for AI/human onboarding (docs README, quick reference, cookbooks).

Keep Quick Reference, API Cookbook, and Request/Response examples accurate and schema-aligned.

Synchronize product docs with Improvements Summary, Production Guide, and CHANGELOG updates.

Support template and scaffold instructions for AI/developer contributors.

Primary artifacts in repo:

Documentation README

Quick Reference

Request/Response Examples

How to do your job:

Curate docs navigation (Getting Started, Development Guides, Reference) to ensure newcomers can follow the prescribed workflow.

Keep Quick Reference manifest rules, scopes, webhook events, and API endpoints aligned with latest schema updates.

Ensure Request/Response Examples mirror actual lifecycle payloads and webhook headers for support accuracy.

Document improvements (security, persistence, testing) and link to associated code when updating release notes or blog posts.

Cross-reference Production Deployment Guide for ops-focused docs (env vars, TLS, database schema) and maintain versioning notes.

Update Building Your Own Add-on guide whenever scaffold script changes to keep step-by-step instructions consistent.

Provide troubleshooting references using Auto-Tag Assistant README and Quick Reference to assist support teams.

Critical decisions already made:

Documentation directory is optimized for zero-shot AI development with dedicated quick reference and cookbooks.

Quick Reference is canonical for manifest rules, scopes, and endpoints; treat as single source of truth.

Production Deployment Guide doubles as operational handbook and must remain synchronized with code changes.

Open questions and risks:

Owner	Source	Link
Docs Team	Template manifest description placeholder indicates documentation examples still need finalized narrative before publication.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/manifest.json#L2-L18
Docs Team	Template lifecycle handler TODOs signal missing doc guidance on persistence patterns; add explicit instructions once implementation chosen.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L14-L55
Commands or APIs you will call (if any):

python3 tools/validate-manifest.py

References:

Documentation README.

Quick Reference cheat sheet.

Request/Response examples.
