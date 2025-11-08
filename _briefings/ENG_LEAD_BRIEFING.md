Engineering Lead Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Oversee multi-module Maven build, dependency hygiene, and parent POM governance.

Maintain build/test automation (Makefile, scripts, CI workflow).

Coordinate feature module integration (SDK, template, auto-tag) and ensure manifest compliance checks.

Align engineering priorities with production-readiness improvements (security, persistence, observability).

Primary artifacts in repo:

Root POM

Makefile targets

CI workflow

How to do your job:

Ensure all modules remain declared in parent pom.xml and enforce Java 17 compilation targets across sub-projects.

Use Makefile commands (build, test, docker-run) to standardize local workflows and docs; keep targets updated as modules evolve.

Keep GitHub Actions workflow aligned with release gates—validate manifests, run tests, and package artifacts on every push/PR.

Mandate tools/validate-manifest.py in PR checklists to guarantee schema compliance for new add-ons.

Coordinate with security on persistence (DatabaseTokenStore) and rate limiting enhancements when introducing new modules.

Audit SDK utilities (ClockifyAddon, RateLimiter, ClockifyHttpClient) to confirm cross-module compatibility and maintainability.

Document engineering playbooks via Improvements Summary and CHANGELOG for stakeholders and new contributors.

Critical decisions already made:

Build system is Maven multi-module with shared parent and inline SDK; no external SDK dependencies required.

CI runs validate + build/test on every push/PR, uploading artifacts and reports as part of the workflow.

Security, persistence, and observability improvements are baseline engineering standards going forward.

Open questions and risks:

Owner	Source	Link
Addon Engineering	Template manifest still contains TODO description; enforce resolution before merging new scaffolds.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/manifest.json#L2-L34
QA Lead	Template lifecycle handlers lack automated persistence tests; define coverage expectations before broadening use.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L14-L66
Commands or APIs you will call (if any):

mvn clean install -DskipTests

References:

Makefile orchestration.

CI workflow definition.

Improvements Summary for engineering standards.
