QA & Test Lead Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Own automated test execution via Maven, Makefile, and GitHub Actions workflow.

Validate scaffolding flow using smoke-test script and manifest schema checks.

Expand coverage on manifest/lifecycle/webhook controllers and SDK utilities.

Verify production-readiness features (rate limiting, token storage, HTTP client) through targeted tests.

Primary artifacts in repo:

GitHub Actions workflow

Smoke-test script for scaffolds

ManifestController unit test

How to do your job:

Run make test locally to execute scaffold smoke test and Maven unit tests; ensure command remains green before merges.

Monitor GitHub Actions validate (manifest check) and build-and-test jobs for regressions; enforce blocking on failures.

Use scripts/test-new-addon.sh to validate new scaffolds automatically and ensure manifest fields/labels are rewritten correctly.

Expand unit tests for controllers (manifest, lifecycle, webhook) referencing existing patterns in Auto-Tag Assistant tests.

Validate rate limiting, token normalization, and HTTP client behaviors with targeted tests to uphold Improvements Summary guarantees.

Require manifest schema validation as part of test plans for any new add-on modules.

Collect coverage artifacts from CI (upload-artifact steps) for reporting and set thresholds for future enforcement.

Critical decisions already made:

CI pipeline uploads test and coverage artifacts; QA should rely on these outputs for gating releases.

Improvements Summary commits to higher-quality tests (ConfigValidator, PathSanitizer); QA is expected to extend this coverage.

Scaffold smoke test is part of make test and must stay healthy before shipping new templates.

Open questions and risks:

Owner	Source	Link
QA Lead	Template lifecycle handler lacks automated persistence tests; define acceptance criteria once persistence implementation lands.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/src/main/java/com/example/templateaddon/LifecycleHandlers.java#L14-L66
QA Lead	Auto lifecycle handler logs TODO on missing auth token; add test coverage for malformed installations to prevent silent failures.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java#L47-L63
Commands or APIs you will call (if any):

make test

References:

GitHub Actions workflow for CI expectations.

Smoke-test script for scaffold validation.

Improvements Summary testing commitments.
