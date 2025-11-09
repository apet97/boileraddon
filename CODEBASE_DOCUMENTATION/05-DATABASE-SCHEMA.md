# Database Schema Documentation

Complete database schema documentation for the Clockify Add-on Boilerplate.

## Table of Contents
- [Overview](#overview)
- [Schema Tables](#schema-tables)
- [Data Models](#data-models)
- [Migrations](#migrations)
- [Usage Examples](#usage-examples)

---

## Overview

### Database System

- **RDBMS:** PostgreSQL 15+
- **Migration Tool:** Flyway 10.18.2
- **Connection:** JDBC
- **Pooling:** Built-in connection pooling

### Connection Configuration

```bash
DB_URL=jdbc:postgresql://localhost:5432/addons
DB_USERNAME=addons
DB_PASSWORD=addons
```

### Migration Location

- **Directory:** `/db/migrations/`
- **Pattern:** `V{version}__{description}.sql`
- **Example:** `V1__init.sql`

---

## Schema Tables

### Table: addon_tokens

**Purpose:** Store Clockify installation tokens per workspace

**Schema:**

```sql
CREATE TABLE IF NOT EXISTS addon_tokens (
  workspace_id VARCHAR(255) PRIMARY KEY,
  auth_token TEXT NOT NULL,
  api_base_url VARCHAR(512),
  created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000,
  last_accessed_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000
);

CREATE INDEX idx_tokens_created ON addon_tokens(created_at);
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);
```

**Columns:**

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `workspace_id` | VARCHAR(255) | NOT NULL | Clockify workspace identifier (Primary Key) |
| `auth_token` | TEXT | NOT NULL | Installation token from INSTALLED lifecycle event |
| `api_base_url` | VARCHAR(512) | NULL | Clockify API base URL (defaults to https://api.clockify.me/api) |
| `created_at` | BIGINT | NOT NULL | Installation timestamp (epoch milliseconds) |
| `last_accessed_at` | BIGINT | NOT NULL | Last API call timestamp (epoch milliseconds) |

**Indexes:**
- `PRIMARY KEY` on `workspace_id`
- `idx_tokens_created` on `created_at` (for cleanup queries)
- `idx_tokens_accessed` on `last_accessed_at` (for inactive token cleanup)

**File:** `/db/migrations/V1__init.sql:1-8`

---

### Table: rules

**Purpose:** Store automation rules (Rules addon only)

**Schema:**

```sql
CREATE TABLE IF NOT EXISTS rules (
  workspace_id VARCHAR(128) NOT NULL,
  rule_id VARCHAR(128) NOT NULL,
  rule_json TEXT NOT NULL,
  PRIMARY KEY (workspace_id, rule_id)
);
```

**Columns:**

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `workspace_id` | VARCHAR(128) | NOT NULL | Workspace identifier (Composite PK) |
| `rule_id` | VARCHAR(128) | NOT NULL | Unique rule identifier (Composite PK) |
| `rule_json` | TEXT | NOT NULL | Serialized rule definition (JSON) |

**Primary Key:** Composite key on (`workspace_id`, `rule_id`)

**JSON Structure (rule_json):**

```json
{
  "id": "rule-001",
  "name": "Auto-tag development work",
  "enabled": true,
  "trigger": "TIME_ENTRY_CREATED",
  "logicOperator": "AND",
  "conditions": [
    {
      "field": "project.name",
      "operator": "contains",
      "value": "Development"
    }
  ],
  "actions": [
    {
      "type": "add_tag",
      "params": {
        "tagName": "Development"
      }
    }
  ]
}
```

**File:** `/db/migrations/V1__init.sql:10-15`

---

## Data Models

### Java Record: WorkspaceToken

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/TokenStore.java:15`

```java
public record WorkspaceToken(String token, String apiBaseUrl) {}
```

**Usage:**

```java
TokenStore.save(workspaceId, "eyJhbG...", "https://api.clockify.me/api");
Optional<WorkspaceToken> tokenOpt = TokenStore.get(workspaceId);

if (tokenOpt.isPresent()) {
    String token = tokenOpt.get().token();
    String apiBaseUrl = tokenOpt.get().apiBaseUrl();
}
```

---

### Java Class: Rule

**Location:** `addons/rules/src/main/java/com/example/rules/engine/Rule.java`

```java
public class Rule {
    private String id;
    private String name;
    private boolean enabled;
    private String trigger;           // Webhook event type
    private String logicOperator;     // "AND" | "OR"
    private List<Condition> conditions;
    private List<Action> actions;

    // Getters and setters...
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique rule identifier |
| `name` | String | Human-readable rule name |
| `enabled` | boolean | Whether rule is active |
| `trigger` | String | Webhook event type (e.g., "TIME_ENTRY_CREATED") |
| `logicOperator` | String | Condition logic ("AND" or "OR") |
| `conditions` | List\<Condition\> | List of conditions to evaluate |
| `actions` | List\<Action\> | List of actions to execute when matched |

---

### Java Class: Condition

**Location:** `addons/rules/src/main/java/com/example/rules/engine/Condition.java`

```java
public class Condition {
    private String field;      // e.g., "project.name", "description"
    private String operator;   // e.g., "equals", "contains", "matches"
    private String value;      // Comparison value

    // Getters and setters...
}
```

**Supported Operators:**

| Operator | Description | Example |
|----------|-------------|---------|
| `equals` | Exact match | `project.name equals "Alpha"` |
| `notEquals` | Not equal | `project.name notEquals "Beta"` |
| `contains` | Substring match | `description contains "meeting"` |
| `notContains` | Does not contain | `description notContains "lunch"` |
| `startsWith` | Prefix match | `description startsWith "Dev:"` |
| `endsWith` | Suffix match | `description endsWith "[DONE]"` |
| `matches` | Regex match | `description matches "^DEV-\d+"` |
| `isEmpty` | Field is empty | `description isEmpty` |
| `isNotEmpty` | Field is not empty | `description isNotEmpty` |

**Supported Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `description` | String | Time entry description |
| `project.id` | String | Project ID |
| `project.name` | String | Project name |
| `client.id` | String | Client ID |
| `client.name` | String | Client name |
| `user.id` | String | User ID |
| `user.name` | String | User name |
| `billable` | Boolean | Whether entry is billable |
| `tags` | Array | Tag names (array) |
| `duration` | Number | Duration in seconds |

---

### Java Class: Action

**Location:** `addons/rules/src/main/java/com/example/rules/engine/Action.java`

```java
public class Action {
    private String type;       // Action type identifier
    private Map<String, Object> params;  // Action parameters

    // Getters and setters...
}
```

**Supported Action Types:**

| Type | Description | Parameters |
|------|-------------|------------|
| `add_tag` | Add tag to time entry | `tagName`: String |
| `remove_tag` | Remove tag from time entry | `tagName`: String |
| `set_project` | Change project | `projectId`: String or `projectName`: String |
| `set_billable` | Set billable status | `billable`: Boolean |
| `update_description` | Update description | `description`: String (supports {{placeholders}}) |
| `append_description` | Append to description | `text`: String |
| `prepend_description` | Prepend to description | `text`: String |
| `api_call` | Custom API call | `method`: String, `endpoint`: String, `body`: Object |

**Example Actions:**

```json
// Add tag
{
  "type": "add_tag",
  "params": {
    "tagName": "Development"
  }
}

// Update description with placeholder
{
  "type": "update_description",
  "params": {
    "description": "[{{project.name}}] {{description}}"
  }
}

// Set project
{
  "type": "set_project",
  "params": {
    "projectName": "Internal"
  }
}

// Custom API call
{
  "type": "api_call",
  "params": {
    "method": "PUT",
    "endpoint": "/workspaces/{{workspaceId}}/time-entries/{{timeEntry.id}}",
    "body": {
      "billable": true
    }
  }
}
```

---

## Migrations

### Flyway Configuration

**Parent POM:** `/pom.xml`

```xml
<profile>
  <id>flyway</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-maven-plugin</artifactId>
        <version>10.18.2</version>
        <configuration>
          <url>${env.DB_URL}</url>
          <user>${env.DB_USER}</user>
          <password>${env.DB_PASSWORD}</password>
          <locations>
            <location>filesystem:db/migrations</location>
          </locations>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

### Running Migrations

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USER=addons
export DB_PASSWORD=addons

# Run migrations
mvn flyway:migrate -Pflyway

# Check migration status
mvn flyway:info -Pflyway

# Validate migrations
mvn flyway:validate -Pflyway
```

### Migration Files

**Location:** `/db/migrations/`

**File:** `V1__init.sql`

```sql
-- Initial schema
CREATE TABLE IF NOT EXISTS addon_tokens (
  workspace_id VARCHAR(255) PRIMARY KEY,
  auth_token TEXT NOT NULL,
  api_base_url VARCHAR(512),
  created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000,
  last_accessed_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000
);

CREATE INDEX idx_tokens_created ON addon_tokens(created_at);
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);

CREATE TABLE IF NOT EXISTS rules (
  workspace_id VARCHAR(128) NOT NULL,
  rule_id VARCHAR(128) NOT NULL,
  rule_json TEXT NOT NULL,
  PRIMARY KEY (workspace_id, rule_id)
);
```

### Future Migrations

To add new migrations, create files with incremented version numbers:

- `V2__add_audit_log.sql`
- `V3__add_rule_metadata.sql`

**Example:** `V2__add_audit_log.sql`

```sql
CREATE TABLE IF NOT EXISTS audit_log (
  id SERIAL PRIMARY KEY,
  workspace_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  event_data TEXT,
  created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000
);

CREATE INDEX idx_audit_workspace ON audit_log(workspace_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
```

---

## Usage Examples

### Token Storage (In-Memory)

**File:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/TokenStore.java`

```java
// Save token during installation
TokenStore.save(workspaceId, installationToken, apiBaseUrl);

// Retrieve token for API calls
Optional<WorkspaceToken> tokenOpt = TokenStore.get(workspaceId);
if (tokenOpt.isPresent()) {
    String token = tokenOpt.get().token();
    String apiBaseUrl = tokenOpt.get().apiBaseUrl();

    ClockifyHttpClient client = new ClockifyHttpClient(apiBaseUrl);
    HttpResponse<String> resp = client.get("/workspaces/" + workspaceId + "/tags",
                                           token, Map.of());
}

// Delete token on uninstallation
boolean deleted = TokenStore.delete(workspaceId);

// Clear all tokens (testing)
TokenStore.clear();
```

---

### Token Storage (Database)

**File:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/DatabaseTokenStore.java`

```java
// Initialize database token store
DatabaseTokenStore store = new DatabaseTokenStore(
    System.getenv("DB_URL"),
    System.getenv("DB_USERNAME"),
    System.getenv("DB_PASSWORD")
);

// Save token
store.save(workspaceId, installationToken, apiBaseUrl);

// Retrieve token
Optional<WorkspaceToken> tokenOpt = store.get(workspaceId);

// Delete token
boolean deleted = store.delete(workspaceId);

// Health check (count tokens)
long tokenCount = store.count();
```

---

### Rules Storage (Database)

**File:** `addons/rules/src/main/java/com/example/rules/store/DatabaseRulesStore.java`

```java
// Initialize rules store
DatabaseRulesStore store = new DatabaseRulesStore(
    System.getenv("DB_URL"),
    System.getenv("DB_USERNAME"),
    System.getenv("DB_PASSWORD")
);

// Save rule
Rule rule = new Rule();
rule.setId("rule-001");
rule.setName("Auto-tag dev work");
rule.setEnabled(true);
rule.setTrigger("TIME_ENTRY_CREATED");
// ... set conditions and actions ...

store.save(workspaceId, rule);

// List all rules for workspace
List<Rule> rules = store.list(workspaceId);

// Get specific rule
Optional<Rule> ruleOpt = store.get(workspaceId, "rule-001");

// Delete rule
boolean deleted = store.delete(workspaceId, "rule-001");
```

---

### Database Connection Handling

**Connection Pattern:**

```java
public class DatabaseTokenStore {
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public void save(String workspaceId, String token, String apiBaseUrl) {
        String sql = """
            INSERT INTO addon_tokens (workspace_id, auth_token, api_base_url)
            VALUES (?, ?, ?)
            ON CONFLICT (workspace_id)
            DO UPDATE SET auth_token = EXCLUDED.auth_token,
                          api_base_url = EXCLUDED.api_base_url,
                          last_accessed_at = EXTRACT(EPOCH FROM NOW())*1000
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, workspaceId);
            stmt.setString(2, token);
            stmt.setString(3, apiBaseUrl);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }
}
```

---

### Transaction Handling

For complex operations requiring transactions:

```java
public void saveMultipleRules(String workspaceId, List<Rule> rules) {
    try (Connection conn = getConnection()) {
        conn.setAutoCommit(false);

        try {
            for (Rule rule : rules) {
                saveRule(conn, workspaceId, rule);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new RuntimeException("Failed to save rules", e);
        } finally {
            conn.setAutoCommit(true);
        }
    } catch (SQLException e) {
        throw new RuntimeException("Database connection failed", e);
    }
}
```

---

### Cleanup Queries

**Delete inactive tokens:**

```sql
-- Delete tokens not accessed in 90 days
DELETE FROM addon_tokens
WHERE last_accessed_at < EXTRACT(EPOCH FROM NOW() - INTERVAL '90 days')*1000;
```

**Archive old audit logs:**

```sql
-- Archive logs older than 1 year
INSERT INTO audit_log_archive
SELECT * FROM audit_log
WHERE created_at < EXTRACT(EPOCH FROM NOW() - INTERVAL '1 year')*1000;

DELETE FROM audit_log
WHERE created_at < EXTRACT(EPOCH FROM NOW() - INTERVAL '1 year')*1000;
```

---

## Database Best Practices

### Connection Pooling

For production, consider using HikariCP:

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl(dbUrl);
config.setUsername(dbUser);
config.setPassword(dbPassword);
config.setMaximumPoolSize(10);

HikariDataSource ds = new HikariDataSource(config);
```

### Indexing Strategy

Add indexes for frequently queried columns:

```sql
-- Workspace-scoped queries
CREATE INDEX idx_rules_workspace ON rules(workspace_id);

-- Timestamp-based cleanup
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);
```

### Backup Strategy

```bash
# Backup database
pg_dump -h localhost -U addons addons > backup_$(date +%Y%m%d).sql

# Restore database
psql -h localhost -U addons addons < backup_20251109.sql
```

---

**Next:** [Security & Authentication Guide](./06-SECURITY-AUTHENTICATION.md)
