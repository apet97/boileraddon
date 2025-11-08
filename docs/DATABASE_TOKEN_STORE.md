# Database Token Store (Example)

This repo uses an in-memory demo store by default. For production, persist installation tokens.

- Schema: see `extras/sql/token_store.sql`.
- Connection: provide `DB_URL`, `DB_USER` (or `DB_USERNAME`), `DB_PASSWORD` via environment.
- Implementation outline:
  - Interface: `ITokenStore` with `save(workspaceId, token)`, `get(workspaceId)`, `remove(workspaceId)`.
  - Backing: HikariCP or simple JDBC with a small connection pool.
  - Transactions: upsert on save; delete on uninstall.

Add-on configuration
- Use `DatabaseTokenStore.fromEnvironment()` to construct (reads `DB_URL`, `DB_USER`/`DB_USERNAME`, `DB_PASSWORD`).
- See docs/POSTGRESQL_GUIDE.md for end‑to‑end PostgreSQL setup, pooling, and migrations.
