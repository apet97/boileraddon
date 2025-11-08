## Summary

- [ ] Concise title (fix/feat/docs/build/ci)
- [ ] Brief description of what changed and why (1–3 bullets)

## Validation Proof

Paste exact command/output snippets after running locally:

- [ ] `python3 tools/validate-manifest.py` → OK
- [ ] `make smoke` → all 3 modules pass
- [ ] `mvn -e -pl addons/addon-sdk -am test` → 0 failures, 0 errors
- [ ] `mvn -e -fae verify` → BUILD SUCCESS

## Checklist

- [ ] Manifests omit `$schema` and include `schemaVersion: "1.3"`
- [ ] Java 17 toolchain in place (Maven + test fork)
- [ ] Docs updated (AI START HERE/Recipes if behavior or routes changed)
- [ ] Security surfaces (CSP, rate limiting, signature verification) preserved or improved
- [ ] Minimal scopes and correct plan for the feature set

## Release Notes (draft)

Provide a short user‑facing changelog block (see docs/RELEASE_NOTES_TEMPLATE.md):

```
### Highlights
- ...

### Changes
- ...

### Upgrade Notes
- ...
```

