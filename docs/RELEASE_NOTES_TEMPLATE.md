# Release Notes Template

Use this template when drafting GitHub Releases.

### Highlights
- Short bullets of user‑visible improvements.

### Changes
- Group by area (SDK, Auto‑Tag, Rules, Overtime, Docs, CI).

### Breaking Changes
- Describe any behavior or API change requiring user action.

### Upgrade Notes
- Migration steps, environment or schema changes.

### Validation
- `python3 tools/validate-manifest.py` → OK
- `make smoke` → OK
- `mvn -e -fae verify` → BUILD SUCCESS

### Credits
- Contributors / PR links.

