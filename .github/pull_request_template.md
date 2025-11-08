## Summary

- [ ] Feature/fix summary and scope
- [ ] Linked issue(s):

## Pre‑merge checklist (please tick all)

- [ ] Java 17 in use locally (`java -version` shows 17.x)
- [ ] Maven uses Java 17 (`mvn -version` shows 17.x)
- [ ] Toolchains configured or CI runner uses Java 17 (see docs/BUILD_ENVIRONMENT.md)
- [ ] Manifests validated: `python3 tools/validate-manifest.py` → OK only
- [ ] Module tests: `mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test` → 0 failures
- [ ] Full build: `mvn -e -DtrimStackTrace=false -fae verify` → BUILD SUCCESS
- [ ] No `$schema` in runtime manifests; schemaVersion is `"1.3"`

## Outputs (paste or attach)

- Manifest validator output
- Surefire summaries (if any failed previously)
- `java -version` and `mvn -version`

## Notes for reviewers

- Anything special about environment, credentials, or steps
- Any follow‑ups needed (tests, docs, CI)

