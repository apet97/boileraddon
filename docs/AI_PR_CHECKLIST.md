# AI PR Checklist

Use this to structure focused changes with proof lines so reviewers can verify quickly.

## Steps

- Validate manifests
  - `python3 tools/validate-manifest.py`
- Run module tests (closest to your change first)
  - `mvn -e -pl addons/addon-sdk -am test`
  - Optional: `mvn -e -pl addons/addon-sdk -Dtest=Class#method test`
- Full reactor
  - `mvn -e -fae verify`
- Optional runtime checks
  - `TEMPLATE=auto-tag-assistant make zero-shot-run`
  - `make manifest-print` and `make manifest-validate-runtime`

## Commit message template

```
fix/feat/docs/ci: concise title

- What changed and why (1–3 bullets)
- Tied to failing test or validator output (if any)

Proof:
- python3 tools/validate-manifest.py → OK
- mvn -pl addons/addon-sdk -am test → Failures: 0, Errors: 0
- mvn -fae verify → BUILD SUCCESS
```

## Heuristics

- Prefer small, surgical changes; update tests/docs alongside behavior changes.
- Keep runtime manifests `$schema`‑free with `schemaVersion: "1.3"`.
- Java 17 for Maven and forked test JVM (check `docs/BUILD_ENVIRONMENT.md`).
- Only Maven Central dependencies; don’t add external repos.

