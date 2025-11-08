# CI Overview

This repository runs three workflows:

- validate.yml — schema and lightweight checks
  - Installs Python 3, runs `tools/validate-manifest.py` and light Maven sanity.
  - Also runs `tools/validate-addon.sh` for lifecycle verification on sample modules.

- build-and-test.yml — main build and tests on Temurin 17
  - Validates manifests again for safety.
  - Runs `mvn test` across modules (addon-sdk, rules, etc.).
  - Runs `mvn verify -DskipTests` to produce the aggregate coverage site at `target/site/jacoco-aggregate/`.
  - Uploads artifacts:
    - `surefire-reports`
    - `jacoco-reports` (per-module sites)
    - `jacoco-aggregate` (aggregate site + `jacoco.xml`)
  - On pull requests: posts a compact coverage comment and delta vs the current Pages baseline.

- jekyll-gh-pages.yml — docs + coverage badge deployment
  - Triggers automatically after `build-and-test` succeeds on `main` (and can be run manually).
  - Checks out the exact tested commit.
  - Downloads the latest `jacoco-aggregate` artifact from the triggering run (fallback: latest repo artifact).
  - Generates `docs/coverage/badge.svg` and `docs/coverage/summary.json` from the `jacoco.xml` (falls back to N/A if missing).
  - Builds the docs (Jekyll) and deploys the site to GitHub Pages.

Local equivalents
- Manifests: `python3 tools/validate-manifest.py`
- addon-sdk tests: `mvn -e -pl addons/addon-sdk -am test`
- Full reactor (tests + coverage): `mvn -e -fae verify`

Troubleshooting
- Ensure Java 17 for both Maven and the forked test JVM.
- If the Pages badge shows `N/A`, ensure `build-and-test` ran on `main` and produced an artifact. Re-run `build-and-test` if necessary; Pages will follow.

