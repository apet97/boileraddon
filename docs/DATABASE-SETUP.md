# Database Setup Guide for Production

**Purpose**: Complete guide for setting up persistent database storage for Clockify addons in production environments.

**Addresses**: Problem #25 from ADDON-CREATION-PROBLEMS.md

---

## Table of Contents

1. [Why Use Database Storage](#why-use-database-storage)
2. [Supported Databases](#supported-databases)
3. [PostgreSQL Setup](#postgresql-setup)
4. [MySQL Setup](#mysql-setup)
5. [Schema Management with Flyway](#schema-management-with-flyway)
6. [Configuration](#configuration)
7. [Switching from InMemoryTokenStore](#switching-from-inmemorytokenstore)
8. [Testing Database Connection](#testing-database-connection)
9. [Troubleshooting](#troubleshooting)

---

## Why Use Database Storage

### InMemoryTokenStore (Default - Development Only)

The template addon uses `InMemoryTokenStore` by default:

**Pros**:
- Zero configuration
- Fast
- Perfect for development/testing

**Cons**:
- ❌ **Tokens lost on restart** - Users must reinstall addon
- ❌ **No scalability** - Can't run multiple instances
- ❌ **No persistence** - Data lost on crash
- ❌ **NOT SUITABLE FOR PRODUCTION**

### DatabaseTokenStore (Production)

Use `DatabaseTokenStore` for production:

**Pros**:
- ✅ Tokens persist across restarts
- ✅ Supports horizontal scaling (multiple instances)
- ✅ Data survives crashes
- ✅ Audit trail / debugging capability

**Cons**:
- Requires database setup
- Slightly slower than in-memory (negligible)

---

## Supported Databases

The SDK supports any JDBC-compatible database. Tested with:

- **PostgreSQL** (recommended) - versions 12+
- **MySQL** / **MariaDB** - versions 8.0+ / 10.5+
- **H2** (development only)

---

## PostgreSQL Setup

### 1. Install PostgreSQL

#### macOS (Homebrew):
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### Docker:
```bash
docker run --name clockify-postgres \
  -e POSTGRES_PASSWORD=mypassword \
  -e POSTGRES_DB=clockify_addon \
  -p 5432:5432 \
  -d postgres:15
```

### 2. Create Database and User

```bash
# Connect as postgres superuser
psql -U postgres

# Create database
CREATE DATABASE clockify_addon;

# Create user
CREATE USER clockify_user WITH ENCRYPTED PASSWORD 'secure_password_here';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE clockify_addon TO clockify_user;

# Connect to the database
\c clockify_addon

# Grant schema privileges (PostgreSQL 15+)
GRANT ALL ON SCHEMA public TO clockify_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO clockify_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO clockify_user;

# Quit
\q
```

### 3. Create Token Storage Schema

Create `db/migrations/V1__create_token_table.sql`:

```sql
-- Token storage table for addon installations
CREATE TABLE IF NOT EXISTS addon_tokens (
    workspace_id VARCHAR(255) PRIMARY KEY,
    token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups
CREATE INDEX idx_tokens_workspace ON addon_tokens(workspace_id);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_addon_tokens_updated_at
BEFORE UPDATE ON addon_tokens
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

### 4. Run Migration

Using psql:
```bash
psql -U clockify_user -d clockify_addon -f db/migrations/V1__create_token_table.sql
```

Or use Flyway (recommended - see below).

### 5. Connection String

```
jdbc:postgresql://localhost:5432/clockify_addon?user=clockify_user&password=secure_password_here
```

---

## MySQL Setup

### 1. Install MySQL

#### macOS (Homebrew):
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
sudo mysql_secure_installation
```

#### Docker:
```bash
docker run --name clockify-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=clockify_addon \
  -e MYSQL_USER=clockify_user \
  -e MYSQL_PASSWORD=secure_password_here \
  -p 3306:3306 \
  -d mysql:8.0
```

### 2. Create Database and User

```bash
# Connect as root
mysql -u root -p

# Create database
CREATE DATABASE clockify_addon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Create user
CREATE USER 'clockify_user'@'localhost' IDENTIFIED BY 'secure_password_here';

# Grant privileges
GRANT ALL PRIVILEGES ON clockify_addon.* TO 'clockify_user'@'localhost';
FLUSH PRIVILEGES;

# Exit
EXIT;
```

### 3. Create Token Storage Schema

Create `db/migrations/V1__create_token_table.sql`:

```sql
-- Token storage table for addon installations
CREATE TABLE IF NOT EXISTS addon_tokens (
    workspace_id VARCHAR(255) PRIMARY KEY,
    token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for faster lookups
CREATE INDEX idx_tokens_workspace ON addon_tokens(workspace_id);
```

### 4. Run Migration

```bash
mysql -u clockify_user -p clockify_addon < db/migrations/V1__create_token_table.sql
```

### 5. Connection String

```
jdbc:mysql://localhost:3306/clockify_addon?user=clockify_user&password=secure_password_here
```

---

## Schema Management with Flyway

[Flyway](https://flywaydb.org/) automates database migrations.

### 1. Add Flyway to Your Addon

The boilerplate includes a Flyway profile. Create migration files in:

```
addons/your-addon/
  └── db/
      └── migrations/
          ├── V1__create_token_table.sql
          ├── V2__add_audit_columns.sql
          └── V3__add_custom_data_table.sql
```

### 2. Run Migrations

#### Using Maven:
```bash
# Set environment variables
export DB_URL="jdbc:postgresql://localhost:5432/clockify_addon"
export DB_USER="clockify_user"
export DB_PASSWORD="secure_password_here"

# Run migration
mvn flyway:migrate -Pflyway
```

#### Using Flyway CLI:
```bash
# Install Flyway CLI
brew install flyway  # macOS
# or download from https://flywaydb.org/download

# Run migration
flyway -url="jdbc:postgresql://localhost:5432/clockify_addon" \
       -user=clockify_user \
       -password=secure_password_here \
       -locations="filesystem:db/migrations" \
       migrate
```

### 3. Verify Migration

```bash
flyway -url="jdbc:postgresql://localhost:5432/clockify_addon" \
       -user=clockify_user \
       -password=secure_password_here \
       info
```

---

## Configuration

### Environment Variables

Set these in your production environment:

```bash
# Database connection
export DB_URL="jdbc:postgresql://localhost:5432/clockify_addon"
export DB_USERNAME="clockify_user"
export DB_PASSWORD="secure_password_here"

# Optional: Connection pool settings
export DB_POOL_SIZE="10"
export DB_CONNECTION_TIMEOUT="30000"
```

### Using DatabaseTokenStore

Update your addon's main class:

```java
import com.clockify.addon.sdk.security.DatabaseTokenStore;
import com.clockify.addon.sdk.security.TokenStore;

public class MyAddonApp {
    public static void main(String[] args) throws Exception {
        // Use database token store instead of in-memory
        TokenStore tokenStore = new DatabaseTokenStore(
            System.getenv("DB_URL"),
            System.getenv("DB_USERNAME"),
            System.getenv("DB_PASSWORD")
        );

        ClockifyAddon addon = ClockifyAddon.builder()
            .tokenStore(tokenStore)
            // ... other configuration
            .build();

        addon.start();
    }
}
```

---

## Switching from InMemoryTokenStore

If you've been using `InMemoryTokenStore` and want to migrate:

### 1. Update Code

Replace:
```java
TokenStore tokenStore = new InMemoryTokenStore();
```

With:
```java
TokenStore tokenStore = new DatabaseTokenStore(
    System.getenv("DB_URL"),
    System.getenv("DB_USERNAME"),
    System.getenv("DB_PASSWORD")
);
```

### 2. Set Up Database

Follow the setup instructions above for your chosen database.

### 3. Redeploy

After deploying with database storage:

1. **Existing installations will need to be reinstalled** (tokens were in memory)
2. Notify users to:
   - Uninstall the addon from Clockify workspace settings
   - Reinstall using the same manifest URL
3. New installations will automatically persist to database

### 4. Verify

Check that tokens are being stored:

```sql
-- PostgreSQL/MySQL
SELECT workspace_id, created_at FROM addon_tokens;
```

---

## Testing Database Connection

### Before Deploying

Test your database connection:

```java
import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✓ Database connection successful!");
            System.out.println("Database: " + conn.getCatalog());
            System.out.println("Driver: " + conn.getMetaData().getDriverName());
        } catch (Exception e) {
            System.err.println("✗ Database connection failed:");
            e.printStackTrace();
        }
    }
}
```

### Quick Test with psql/mysql

#### PostgreSQL:
```bash
psql -U clockify_user -d clockify_addon -c "SELECT 1;"
```

#### MySQL:
```bash
mysql -u clockify_user -p clockify_addon -e "SELECT 1;"
```

---

## Troubleshooting

### Connection Refused

**Error**: `Connection refused (Connection refused)`

**Causes**:
- Database not running
- Wrong host/port
- Firewall blocking connection

**Fix**:
```bash
# Check if database is running
# PostgreSQL:
pg_isready

# MySQL:
mysqladmin ping
```

### Authentication Failed

**Error**: `authentication failed for user`

**Causes**:
- Wrong username/password
- User doesn't have permissions

**Fix**:
```sql
-- PostgreSQL: Check user exists
SELECT usename FROM pg_user WHERE usename = 'clockify_user';

-- Reset password
ALTER USER clockify_user WITH PASSWORD 'new_password';

-- MySQL: Check user exists
SELECT user, host FROM mysql.user WHERE user = 'clockify_user';

-- Reset password
ALTER USER 'clockify_user'@'localhost' IDENTIFIED BY 'new_password';
```

### Table Does Not Exist

**Error**: `relation "addon_tokens" does not exist`

**Fix**:
```bash
# Run migrations
psql -U clockify_user -d clockify_addon -f db/migrations/V1__create_token_table.sql
```

### SSL Connection Error

**Error**: `SSL error` or `FATAL: no pg_hba.conf entry`

**Fix for PostgreSQL**:

Edit `/var/lib/postgresql/data/pg_hba.conf`:
```
# Allow local connections without SSL (development only!)
host    all             all             127.0.0.1/32            md5

# For production, use SSL:
hostssl all             all             0.0.0.0/0               md5
```

Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

### Connection Pool Exhausted

**Error**: `Cannot get a connection, pool exhausted`

**Fix**:
Increase pool size:
```java
// When creating DatabaseTokenStore
TokenStore tokenStore = new DatabaseTokenStore(
    System.getenv("DB_URL") + "?maximumPoolSize=20",
    System.getenv("DB_USERNAME"),
    System.getenv("DB_PASSWORD")
);
```

---

## Production Checklist

Before deploying with database storage:

- [ ] Database installed and running
- [ ] Database created with proper character encoding
- [ ] User created with appropriate permissions
- [ ] Schema migrations run successfully
- [ ] Connection tested from application server
- [ ] Environment variables configured
- [ ] DatabaseTokenStore configured in code
- [ ] Firewall rules allow database connections
- [ ] SSL/TLS enabled for production (recommended)
- [ ] Backups configured
- [ ] Monitoring/alerting set up

---

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [JDBC Connection Pool Tuning](https://github.com/brettwooldridge/HikariCP/wiki)

---

## See Also

- [PRODUCTION-DEPLOYMENT.md](./PRODUCTION-DEPLOYMENT.md) - Full deployment guide
- [SDK_OVERVIEW.md](./SDK_OVERVIEW.md) - SDK documentation
- [COMMON-MISTAKES.md](./COMMON-MISTAKES.md) - Common issues and solutions
