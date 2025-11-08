# Make Targets

Helpful shortcuts for common flows. Override environment variables inline as needed.

Variables
- `TEMPLATE` — selected add-on module (default `_template-addon`).
- `ADDON_PORT` — local port (default `8080`).
- `ADDON_BASE_URL` — external base URL; defaults to `http://localhost:<port>/<TEMPLATE>`.

Core
- `make setup` — print Java/Maven versions.
- `make validate` — run manifest validation.
- `make build` — build all modules with tests skipped.
- `make clean` — clean artifacts.

Targeted builds
- `make build-template` — build `_template-addon` only.
- `make build-auto-tag-assistant` — build `auto-tag-assistant`.
- `make build-rules` — build `rules`.
- `make build-overtime` — build `overtime`.

Run locally (native JVM)
- `make dev` — build and run template add-on using `.env`.
- `make run-auto-tag-assistant` — run demo add-on at `$ADDON_BASE_URL` (set `ADDON_BASE_URL` accordingly).
- `make run-rules` — run rules add-on at `$ADDON_BASE_URL`.
- `make rules-apply` — run rules with `RULES_APPLY_CHANGES=true`.
- `make rules-seed-demo` — seed a demo rule and dry-run test.
- `make rules-webhook-sim` — simulate a signed webhook locally.
- `make dev-rules` — run rules using `.env.rules`.

Docker
- `make docker-run TEMPLATE=<module>` — build and run selected add-on in Docker, forwarding `ADDON_PORT` and `ADDON_BASE_URL`.

Scaffolding
- `make new-addon NAME=my-addon DISPLAY="My Add-on"` — scaffold a new add-on from the template.

Zero‑shot
- `TEMPLATE=auto-tag-assistant make zero-shot-run` — build & run the selected module and print the manifest URL (pair with ngrok).
- `make manifest-url` — print the current manifest URL for installation.
- `make manifest-print` — fetch and pretty‑print the runtime manifest (uses `ADDON_BASE_URL` or defaults).
- `make manifest-validate-runtime` — fetch the runtime manifest and validate it against `tools/manifest.schema.json`.

Testing
- `make test` — run tests for the selected module or full reactor depending on setup; prefer Maven directly for fine control:
  - `mvn -e -pl addons/addon-sdk -am test`
  - `mvn -e -fae verify`
