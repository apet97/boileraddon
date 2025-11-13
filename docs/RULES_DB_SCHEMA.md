# Rules Add-on Database Schema

Rules persists two distinct data sets:

1. **Automation rules** (`rules` table) &mdash; workspace-scoped JSON blobs created via the settings UI or APIs.
2. **Installation tokens** (`addon_tokens` table) &mdash; optional, only when `ENABLE_DB_TOKEN_STORE=true`.

Both tables can be auto-created by the built-in JDBC stores, but production environments should manage them via a migration tool (Flyway, Liquibase, etc.) so DDL changes are versioned and reviewed like code.

## `rules` table (RulesStore)

`DatabaseRulesStore` stores declarative automations per workspace. The schema is intentionally minimal:

```sql
CREATE TABLE IF NOT EXISTS rules (
    workspace_id VARCHAR(128) NOT NULL,
    rule_id      VARCHAR(128) NOT NULL,
    rule_json    TEXT NOT NULL,
    PRIMARY KEY (workspace_id, rule_id)
);
```

- **Composite primary key** allows fast lookups by `(workspaceId, ruleId)`.
- `rule_json` contains the canonical rule payload produced by the settings UI.
- Recommended indexes: the PK above is sufficient because every access pattern starts with `workspaceId`.
- Migrations: add new columns via additive migrations (e.g., `ALTER TABLE rules ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb`) and deploy the code that reads them afterwards.

## `addon_tokens` table (persistent TokenStore)

When `ENABLE_DB_TOKEN_STORE=true`, `PooledDatabaseTokenStore` uses the shared database to persist workspace installation tokens and associated metadata. Expected schema:

```sql
CREATE TABLE IF NOT EXISTS addon_tokens (
    workspace_id     VARCHAR(255) PRIMARY KEY,
    auth_token       TEXT NOT NULL,
    api_base_url     VARCHAR(512),
    created_at       BIGINT NOT NULL,
    last_accessed_at BIGINT NOT NULL
);
```

- Timestamps are stored as epoch milliseconds to keep the schema portable across PostgreSQL/MySQL.
- `api_base_url` lets the token store remember which Clockify environment issued the token (EU/US).
- The SDK falls back to INSERT+UPDATE if `ON CONFLICT` is unavailable, but production deployments should keep the schema aligned with the statements above to avoid surprises.

### Migration guidance

1. **Track DDL in source control.** Create a migration for each schema change (Flyway/Liquibase/etc.) rather than relying on the auto-create helpers.
2. **Apply migrations before new code rolls out.** Run migrations in a maintenance step (CI/CD job, Kubernetes init container, etc.) so application pods always see the final schema.
3. **Back up tables before destructive changes.** The `rules` table contains customer automations; take a snapshot before altering or dropping columns.
4. **Monitor migrations.** Include migrations in release checklists (see `RULES_PROD_LAUNCH_CHECKLIST.md`) and verify `/ready` after each deploy to confirm both stores are reachable.

> ℹ️ **Multi-node deployments:** `WebhookIdempotencyCache` is still in-memory. For cross-node deduplication you can add a durable cache table (e.g., Redis, Postgres) following the same approach &mdash; add the schema here, then wire it into the cache implementation.

