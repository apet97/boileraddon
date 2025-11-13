# AI Zero‑Shot Playbook

This is a deterministic, minimal set of steps for an AI agent to scaffold, validate, test, and run an add‑on in this repository. Follow it verbatim unless the task specifies otherwise.

Guardrails
- Use Java 17 for both Maven and the forked test JVM. See docs/BUILD_ENVIRONMENT.md for Toolchains if your host JDK is newer.
- Do not add external Maven repos; use Maven Central only.
- Runtime manifests must omit `$schema` and use `schemaVersion: "1.3"`.
- Routing is exact-path only. Put IDs in query/body or register additional exact paths.
- Keep changes small and targeted. Update tests and docs when behavior changes.

Fast Path (commands only)
1) Print toolchain
```
java -version && mvn -version
```

2) Validate manifests
```
python3 tools/validate-manifest.py
```
If invalid:
- Edit `tools/manifest.schema.json` if schema is too strict/loose for known fields (e.g., lifecycle array of objects with type/path).
- Keep runtime manifests free of `$schema`, and ensure `schemaVersion` is "1.3".

3) Run focused tests
```
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test
```
Reproduce a single failing test explicitly:
```
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am -Dtest=<Class>#<method> test
```

4) Apply minimal code fixes
- Use ripgrep to jump to the right files:
```
rg -n "class <Name>|package com\.clockify" -S addons/addon-sdk/src/main/java
```
Common hotspots in this repo:
- Path sanitization: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/util/PathSanitizer.java`
- Config validation: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/config/ConfigValidator.java`
- CORS and security headers: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/middleware/*`
- Routing/manifest wiring: `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/*`

5) Re-run tests and verify
```
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test
mvn -e -DtrimStackTrace=false -fae verify
```

6) Commit with proof
Summarize changes and append proof-of-green lines (test counts, BUILD SUCCESS) to the commit message.

7) Optional run
```
# Production example (recommended)
cp .env.rules.example .env.rules
make dev-rules                               # loads .env.rules into RulesConfiguration
# Need Docker? ADDON_BASE_URL=... make docker-build TEMPLATE=rules

# Demo sample (smaller surface)
TEMPLATE=auto-tag-assistant make zero-shot-run

ngrok http 8080                              # expose whichever add-on you're running
make manifest-print                          # pretty-print runtime manifest
make manifest-validate-runtime               # schema-validate runtime manifest
```
Patterns & Tips
- Null bytes and encodings to block (`PathSanitizer`): real `\u0000`, percent-encoded `%00`, and literal `\\0`.
- Config errors should fail fast with helpful messages; keep `Configuration validation failed:` prefix for aggregates.
- CORS: exact origins and wildcard subdomains (e.g., `https://*.example.com`), always add `Vary: Origin`.
- Coverage: tests run in build-and-test; Pages pulls the aggregate coverage artifact and generates a badge.

Commit Template (edit lines 2–4)
```
fix/feat/docs/ci: concise title

- What changed and why (1–3 bullets)
- Tied to failing test or validator output (if any)

Proof:
- python3 tools/validate-manifest.py → OK
- mvn -pl addons/addon-sdk -am test → Failures: 0, Errors: 0
- mvn -fae verify → BUILD SUCCESS
```

Escalation Heuristics (AI)
- If tests fail under a newer JDK (e.g., JDK 25), pin to 17 locally, and ensure Surefire/Failsafe are modern (≥3.2.5). Use Toolchains if necessary. See docs/BUILD_ENVIRONMENT.md.
- If coverage gates fail, write or adjust minimal tests near the changed code. Prefer targeted tests over broad changes.
- Do not disable tests in build-and-test. Only the Pages job runs without tests and fetches the coverage artifact.

That’s it. For broader docs, see docs/AI_ONBOARDING.md and AGENTS.md.
