# Local Secrets & Configuration

Guidance for handling secrets and configuration in local and CI environments.

## Local development

- Use `.env` for local settings. The example file `.env.example` shows common variables.
- Never commit real secrets; `.env` is ignored by `.gitignore`.
- The apps read from `.env` first, then from real environment variables.

Recommended `.env` fields:
- `ADDON_BASE_URL`, `ADDON_PORT`
- `CLOCKIFY_WORKSPACE_ID`, `CLOCKIFY_INSTALLATION_TOKEN` (optional for validating local webhooks)
- `DB_URL`, `DB_USER`/`DB_USERNAME`, `DB_PASSWORD` (when using a DB)
- `ADDON_FRAME_ANCESTORS`, `ADDON_RATE_LIMIT`, `ADDON_CORS_ORIGINS`

## CI secrets

- Store secrets in the platform’s secret store (GitHub Actions: repo/environment secrets).
- Use those secrets to configure deploy/migration jobs (e.g., `db-migrate.yml`).
- Avoid printing secrets in logs; validate configuration and fail fast with generic messages.

## JDK Toolchains

- If your workstation JDK is newer than 17, configure a JDK 17 toolchain at `~/.m2/toolchains.xml`.
- CI already pins Java 17 via actions/setup-java; no additional config needed.

## Database credentials

- Use least privilege DB users for runtime; use elevated users only for migrations.
- Prefer SSL/TLS to connect to remote DB; keep credentials in secret manager.

