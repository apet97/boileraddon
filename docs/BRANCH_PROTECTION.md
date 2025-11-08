# Branch Protection & Required Checks

To ensure quality and fast feedback, protect the default branch with the following required status checks:

## Recommended required checks
- validate (schema checks)
- smoke (runtime wiring: /health and /metrics)
- build-and-test (full tests + coverage)
- jekyll-gh-pages (optional; publish docs and coverage badge on main)

## GitHub setup
1. Go to Settings → Branches → Branch protection rules → Add rule
2. Branch name pattern: `main`
3. Enable “Require status checks to pass before merging” and select:
   - `validate`
   - `smoke`
   - `build-and-test`
4. Optional: enable “Require pull request reviews before merging”
5. Save changes

## Optional advanced wiring
- If you prefer a single pipeline, you can create one workflow with three jobs (validate → smoke → build) and set `needs:` between them. This repo uses separate workflows for flexibility, so branch protection is the simplest way to enforce the order.

