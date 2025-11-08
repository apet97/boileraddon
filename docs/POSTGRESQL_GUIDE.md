# Using PostgreSQL with the Clockify Add‑on Boilerplate

This repo (Java 17 + embedded Jetty) supports persistent storage with relational databases such as PostgreSQL. This guide covers setup, configuration, schema patterns, migrations, query practices, security, environment management, backups, monitoring, and testing.

## 1) Install PostgreSQL (local)

- Version: PostgreSQL 13+ recommended.
- Easiest (Docker): the repo includes a compose file.

```bash
# Start local Postgres (addons/addon_user/addon_password defaults)
docker compose -f docker-compose.dev.yml up -d

# Environment for the app
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USER=addons
export DB_PASSWORD=addons

# Run an add-on with DB-backed token store
make build-auto-tag-assistant
ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant   DB_URL=$DB_URL DB_USER=$DB_USER DB_USERNAME=$DB_USER DB_PASSWORD=$DB_PASSWORD   make run-auto-tag-assistant-db
```

- Native install: create DB and user.

```sql
CREATE DATABASE addons;
CREATE USER addon_user WITH ENCRYPTED PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE addons TO addon_user; -- dev convenience
```

Compose file reference: docker-compose.dev.yml.

## 2) App configuration

- JDBC driver: add to your add‑on module POM (example):

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.7.4</version>
</dependency>
```

- Environment variables
  - `DB_URL` — e.g., `jdbc:postgresql://localhost:5432/addons`
  - `DB_USER` (preferred) or `DB_USERNAME` — database username
  - `DB_PASSWORD` — database password

- Wiring the persistent token store

```java
import com.clockify.addon.sdk.security.DatabaseTokenStore;

DatabaseTokenStore tokenStore = DatabaseTokenStore.fromEnvironment();
// Use tokenStore.save(workspaceId, token) instead of in-memory facade for production
```

Note: `DatabaseTokenStore.fromEnvironment()` reads `DB_URL`, `DB_USER` (or `DB_USERNAME` if you pass it through), and `DB_PASSWORD`.

## 3) Schema patterns

- Token store (per-workspace installation token):

```sql
CREATE TABLE addon_tokens (
  workspace_id VARCHAR(255) PRIMARY KEY,
  auth_token   TEXT NOT NULL,
  api_base_url VARCHAR(512) NOT NULL,
  created_at   BIGINT NOT NULL,
  last_accessed_at BIGINT NOT NULL
);
CREATE INDEX idx_tokens_created  ON addon_tokens(created_at);
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);
```

- Rules store (multi-tenant by composite key):

```sql
CREATE TABLE rules (
  workspace_id VARCHAR(128) NOT NULL,
  rule_id      VARCHAR(128) NOT NULL,
  rule_json    TEXT NOT NULL,
  PRIMARY KEY (workspace_id, rule_id)
);
```

- See also: `extras/sql/token_store.sql`.

Guidelines
- Always define PKs/NOT NULL; prefer composite keys for multi-tenant tables.
- Use suitable types (VARCHAR for IDs/URLs, JSONB or TEXT for JSON payloads).
- Add indexes for frequent lookups/sorts.

## 4) Migrations

- Dev convenience: `DatabaseTokenStore` attempts `CREATE TABLE IF NOT EXISTS` on startup.
- For evolving schemas, adopt migrations:
  - Keep versioned SQL in `db/migrations/` (`V1__init.sql`, `V2__...sql`).
  - Or integrate Flyway/Liquibase (Maven plugin or app startup).
  - Run migrations in staging first; back up before prod.

## 5) Query execution & performance

- Use `PreparedStatement` for all SQL (safety + plan reuse).
- Upsert pattern: `UPDATE` then `INSERT` if 0 rows (portable), or Postgres `INSERT ... ON CONFLICT`.
- Transactions: group multi‑statement operations in a transaction when atomicity is required.
- Fetch only needed columns; avoid `SELECT *`.
- Add connection pooling (HikariCP) for production:

```xml
<dependency>
  <groupId>com.zaxxer</groupId>
  <artifactId>HikariCP</artifactId>
  <version>5.1.0</version>
</dependency>
```

```java
HikariConfig cfg = new HikariConfig();
cfg.setJdbcUrl(System.getenv("DB_URL"));
cfg.setUsername(System.getenv("DB_USER"));
cfg.setPassword(System.getenv("DB_PASSWORD"));
cfg.setMaximumPoolSize(10);
HikariDataSource ds = new HikariDataSource(cfg);
```

## 6) Security (database)

- Use a least‑privilege DB user for the app (SELECT/INSERT/UPDATE/DELETE only).
- Store secrets in env/secret manager; never commit them.
- Restrict network access to the DB; require SSL in prod.
- Use SCRAM‑SHA‑256 for password auth; keep Postgres patched.
- Continue safe patterns in app (prepared statements; `PathSanitizer` for paths).

## 7) Environments (dev/staging/prod)

- Configure via environment variables; keep separate configs for each env.
- Typical prod Docker env:

```yaml
services:
  addon:
    environment:
      - ADDON_BASE_URL=https://your-addon.example.com
      - ADDON_WEBHOOK_SECRET=${WEBHOOK_SECRET}
      - DB_URL=jdbc:postgresql://db:5432/clockify_addons
      - DB_USER=addon_user
      - DB_PASSWORD=${DB_PASSWORD}
      - LOG_LEVEL=INFO
      - LOG_APPENDER=JSON
```

- Use `.env` for local dev; do not commit real secrets.

### Health endpoint with DB check

Add the SDK `HealthCheck` endpoint and register a database provider to catch DB outages:

```java
HealthCheck health = new HealthCheck("my-addon", "1.0.0");
health.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
  public String getName() { return "database"; }
  public HealthCheck.HealthCheckResult check() {
    try {
      DatabaseTokenStore store = DatabaseTokenStore.fromEnvironment();
      long count = store.count();
      return new HealthCheck.HealthCheckResult("database", true, "Connected", count);
    } catch (Exception e) {
      return new HealthCheck.HealthCheckResult("database", false, e.getMessage());
    }
  }
});
addon.registerCustomEndpoint("/health", health);
```

## 8) Backup & recovery

- Automate backups (managed service snapshots or `pg_dump` + retention/rotation).
- Store backups off‑site and encrypted.
- Regularly test restores in staging; document DR steps.
- Prioritize backing up installation tokens and user configuration (rules), which are critical for addon continuity.

## 9) Logging & monitoring

- App logs: set `LOG_APPENDER=JSON` and `LOG_LEVEL=INFO` in prod for structured logs.
- DB logs: use PostgreSQL `log_min_duration_statement` or `pg_stat_statements` for slow query analysis.
- Metrics: consider Prometheus/Grafana; add basic health checks that include DB connectivity.

## 10) Testing with PostgreSQL

- Unit tests: prefer in‑memory stores or mocks for fast logic tests.
- Integration tests: use a local Postgres or Testcontainers to run DAO tests against a real DB.
- Migrations: test applying migrations on a clean DB in CI.
- CI: add a Postgres service, or rely on Testcontainers.

### Testcontainers example (integration test)

```java
@Testcontainers
class DatabaseTokenStoreIT {
  @Container
  static final PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15-alpine");

  @Test void saveGet() {
    DatabaseTokenStore store = new DatabaseTokenStore(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    store.save("ws1", "tkn");
    assertEquals("tkn", store.get("ws1").orElseThrow());
  }
}
```

This pattern is already included under `addons/addon-sdk` as `DatabaseTokenStoreIT`. It starts an ephemeral PostgreSQL and exercises the JDBC store end‑to‑end.

### Flyway sample migrations (profile)

Place migrations under `db/migrations` (example `V1__init.sql` is included). Configure Flyway in a Maven profile or as a plugin, pointing at `${env.DB_URL}`, `${env.DB_USER}`, and `${env.DB_PASSWORD}` to reuse your environment.

```xml
The root POM defines a `flyway` profile configured to read `DB_URL`, `DB_USER`, and `DB_PASSWORD` from the environment and to load migrations from `db/migrations`.
```

Run with (from repo root):

```
mvn -Pflyway -DskipTests flyway:migrate

Make target shortcut:

```
DB_URL=jdbc:postgresql://localhost:5432/addons DB_USER=addons DB_PASSWORD=addons \
  make db-migrate
```
```

Keep Flyway optional (via a profile) so regular builds don’t require a running DB.

## Quick links
- Compose: `docker-compose.dev.yml`
- Example schema: `extras/sql/token_store.sql`
- Token store (JDBC): `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/DatabaseTokenStore.java`
- Production deployment: `docs/PRODUCTION-DEPLOYMENT.md`
- Security checklist: `docs/SECURITY_CHECKLIST.md`
