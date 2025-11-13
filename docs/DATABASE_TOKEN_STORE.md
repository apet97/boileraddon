# Database Token Store

## Overview

By default, installation tokens are stored **in-memory** and lost when the service restarts. For production deployments, use **DatabaseTokenStore** for persistent token storage across service restarts.

### Why Persistent Storage Matters
- **Production Requirement**: Tokens survive service restarts, container crashes, pod evictions
- **High Availability**: Enables zero-downtime deployments with stateless service replicas
- **Audit Trail**: Database provides token lifecycle history and monitoring
- **Security**: Centralized token management with backup and recovery options

---

## Quick Start

### 1. Database Setup

**PostgreSQL** (Recommended)
```bash
# Create database
createdb clockify_addons

# Load schema
psql clockify_addons < extras/sql/token_store.sql

# Verify table created
psql clockify_addons -c "\dt"
```

**MySQL/MariaDB**
```bash
# Create database
mysql -u root -p -e "CREATE DATABASE clockify_addons;"

# Load schema (schema file is PostgreSQL; MySQL syntax differs)
# Manual migration required - see POSTGRESQL_GUIDE.md
```

See: [POSTGRESQL_GUIDE.md](POSTGRESQL_GUIDE.md) for detailed database setup.

### 2. Configure Environment Variables

```bash
# Required
DB_URL="jdbc:postgresql://localhost:5432/clockify_addons"
DB_USER="postgres"
DB_PASSWORD="your-secure-password"

# Optional: Override username
DB_USERNAME="postgres"  # Alternative to DB_USER
```

### 3. Enable in Your Addon

**AutoTagAssistantApp.java** pattern (already implemented):
```java
String dbUrl = System.getenv("DB_URL");
String dbUser = System.getenv().getOrDefault("DB_USER", System.getenv("DB_USERNAME"));
String dbPassword = System.getenv("DB_PASSWORD");

if (dbUrl != null && !dbUrl.isBlank() && dbUser != null && !dbUser.isBlank()) {
    try {
        DatabaseTokenStore dbStore = new DatabaseTokenStore(dbUrl, dbUser, dbPassword);
        TokenStore.configurePersistence(dbStore);
        System.out.println("✓ DatabaseTokenStore configured");
    } catch (Exception e) {
        System.err.println("⚠ Failed to initialize database token store: " + e.getMessage());
        System.err.println("  Falling back to in-memory storage");
    }
}
```

### 4. Verify It's Working

Check logs:
```
✓ TokenStore configured with database persistence (PostgreSQL)
```

Or test by:
1. Install addon (lifecycle/installed event)
2. Check database: `SELECT * FROM token_store;`
3. Restart service
4. Verify token still exists and addon still works

---

## Migration from InMemoryTokenStore

### Step 1: Set Up Database
Complete section "Database Setup" above.

### Step 2: Add Environment Variables
```bash
DB_URL="jdbc:postgresql://localhost:5432/clockify_addons"
DB_USER="postgres"
DB_PASSWORD="your-password"
```

### Step 3: Deploy with DatabaseTokenStore
Restart your addon with the database environment variables set.

### Step 4: Reinstall Addons (Workspace Migration)
For each workspace:
1. Go to Clockify Admin > Add-ons
2. Uninstall the addon
3. Reinstall the addon (triggers `lifecycle/installed` event)
4. Verify in database: `SELECT * FROM token_store;`

### Rollback Strategy
If issues occur:
1. Unset database environment variables
2. Restart addon (reverts to in-memory storage)
3. Previous tokens in database remain for recovery if needed

---

## Production Configuration

### Connection Pooling (HikariCP)

DatabaseTokenStore uses **HikariCP** for connection pooling automatically:

**Default Settings**:
- Maximum pool size: 10
- Minimum idle connections: 2
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes

**Custom Configuration** (via environment):
```bash
# Connection pool sizing
DB_MAX_POOL_SIZE=20          # Default: 10, increase for high volume
DB_IDLE_TIMEOUT=600000       # Default: 10 min, in milliseconds

# Connection testing
DB_CONNECTION_TIMEOUT=30000  # Default: 30 sec
```

### Performance Tuning
- **High Volume**: Increase `DB_MAX_POOL_SIZE` (recommended: 15-20)
- **Low Volume**: Reduce pool size or use smaller instances
- **Monitor**: Check database connection count: `SELECT count(*) FROM pg_stat_activity;`

---

## Troubleshooting

### Error: "Failed to connect to database"
**Causes**:
- Database not running
- Wrong connection string format
- Incorrect credentials
- Network/firewall blocking connection

**Solution**:
```bash
# Test connection manually
psql -h localhost -U postgres -d clockify_addons -c "SELECT 1;"

# Verify environment variables
echo $DB_URL
echo $DB_USER
```

### Error: "Table token_store not found"
**Cause**: Schema not created

**Solution**:
```bash
# Load schema
psql clockify_addons < extras/sql/token_store.sql

# Verify table exists
psql clockify_addons -c "\dt token_store;"
```

### Addon Falls Back to In-Memory Storage
**Cause**: Environment variables not set or database error

**Solution**:
1. Check logs: `grep -i "database" logs/*.log`
2. Verify environment variables: `echo $DB_URL`
3. Check database connectivity: `psql $DB_URL -c "SELECT 1;"`
4. Check firewall: Is database port open?

### Connection Pool Exhaustion
**Symptom**: Slow responses, database connection timeouts

**Fix**:
- Increase pool size: `DB_MAX_POOL_SIZE=20`
- Monitor connections: `SELECT count(*) FROM pg_stat_activity;`
- Check for idle connections: tune `DB_IDLE_TIMEOUT`

---

## Testing with DatabaseTokenStore

### Testcontainers (Recommended)
Use PostgreSQL testcontainers in unit tests:

```java
@ClassRule
static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db");

@Before
public void setup() {
    DatabaseTokenStore store = new DatabaseTokenStore(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword()
    );
    TokenStore.configurePersistence(store);
}
```

See: [TESTING.md](TESTING.md) for complete testing guide.

---

## Related Documentation

- [POSTGRESQL_GUIDE.md](POSTGRESQL_GUIDE.md) - Database setup and pooling
- [JWT_VERIFICATION_GUIDE.md](JWT_VERIFICATION_GUIDE.md) - Lifecycle handler security
- [SECURITY.md](SECURITY.md) - Security overview including token management
- [PRODUCTION-DEPLOYMENT.md](PRODUCTION-DEPLOYMENT.md) - Production deployment checklist
