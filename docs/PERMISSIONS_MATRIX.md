# Permissions Matrix (Scopes)

Choose the smallest viable set of scopes for your add‑on. Use this matrix to map features to required scopes. Add more only when your code actually needs them.

Docs: docs/QUICK-REFERENCE.md (scopes list), docs/MANIFEST_RECIPES.md (recipes), docs/MANIFEST_AND_LIFECYCLE.md

## Common Features → Scopes

- Read time entries
  - `TIME_ENTRY_READ`
- Create/update time entries
  - `TIME_ENTRY_WRITE`
- Read tags
  - `TAG_READ`
- Create/update/delete tags
  - `TAG_WRITE`
- Read projects
  - `PROJECT_READ`
- Read tasks
  - `TASK_READ`
- Read workspace users
  - `USER_READ`
- Read/modify custom fields (if your UI uses them)
  - `CUSTOM_FIELDS_READ`, `CUSTOM_FIELDS_WRITE`

## Scenario Bundles

- UI‑only (read‑only sidebar)
  - `TIME_ENTRY_READ`, `PROJECT_READ`, `TAG_READ` (add more as your UI fetches more resources)
- Automation/Rules (apply tags or modify entries)
  - `TIME_ENTRY_READ`, `TIME_ENTRY_WRITE`, `TAG_READ`, `TAG_WRITE`
- Reporting (aggregate reads)
  - `TIME_ENTRY_READ`, `PROJECT_READ`, `TASK_READ` (+ any filter support you need)
- Admin/Setup (manages entities)
  - Add `PROJECT_WRITE`, `TASK_WRITE`, `TAG_WRITE` only if actually creating/updating entities

## Plan Requirements

- Start with `minimalSubscriptionPlan("FREE")` for demos.
- Raise to `STANDARD`/`PRO`/`ENTERPRISE` if your features require higher plan capabilities or SLAs.
- Document the reason in the module README when raising plans.

## Tips

- Don’t request write scopes unless your code uses them (Clockify reviewers check least‑privilege).
- Keep scopes located near code that depends on them (manifest builder in the app entrypoint).
- Document scope choices in your add‑on README so reviewers and maintainers can verify intent.

