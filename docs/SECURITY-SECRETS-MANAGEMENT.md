# Security: Secrets Management

**Purpose**: Guidelines for handling secrets (tokens, API keys, passwords) in the Clockify addon boilerplate.

**Status**: Security-critical document

---

## Table of Contents

1. [Never Hardcode Secrets](#never-hardcode-secrets)
2. [Environment Variables](#environment-variables)
3. [Configuration Files](#configuration-files)
4. [Testing with Secrets](#testing-with-secrets)
5. [Secret Rotation](#secret-rotation)
6. [Scanning for Secrets](#scanning-for-secrets)

---

## Never Hardcode Secrets

### ❌ DO NOT:

```java
// WRONG: Hardcoded API key
String apiKey = "sk-abc123xyz789";

// WRONG: Hardcoded password
String dbPassword = "admin123";

// WRONG: Hardcoded token
String installationToken = "raw-installation-jwt-value";

// WRONG: Placeholder that looks like real secret
String secret = "abc123secretdef";
```

### ✅ DO:

```java
// CORRECT: Load from environment
String apiKey = System.getenv("CLOCKIFY_API_KEY");
String dbPassword = System.getenv("DB_PASSWORD");
String installationToken = System.getenv("INSTALLATION_TOKEN");

// CORRECT: Validate it's not empty
if (apiKey == null || apiKey.isBlank()) {
    throw new IllegalStateException("CLOCKIFY_API_KEY environment variable is required");
}
```

---

## Environment Variables

### Required Environment Variables

All sensitive values MUST be provided via environment variables:

| Variable | Purpose | Example |
|----------|---------|---------|
| `DB_URL` | Database JDBC URL | `jdbc:postgresql://localhost/clockify_addon` |
| `DB_USERNAME` | Database user | `clockify_user` |
| `DB_PASSWORD` | Database password | *(generated at setup)* |
| `ADDON_WEBHOOK_SECRET` | Webhook signature secret | *(generated)* |
| `INSTALLATION_TOKEN` | Clockify auth token (for testing) | *(from Clockify workspace)* |
| `ADDON_ACCEPT_JWT_SIGNATURE` | Enable dev JWT (dev only) | `true` or `false` |

### Production Environment Setup

Use your deployment platform's secrets management:

**AWS Secrets Manager**:
```bash
aws secretsmanager create-secret --name clockify-addon-secrets \
  --secret-string '{
    "DB_PASSWORD": "...",
    "ADDON_WEBHOOK_SECRET": "..."
  }'
```

**GitHub Secrets**:
```bash
gh secret set DB_PASSWORD --body "..."
gh secret set ADDON_WEBHOOK_SECRET --body "..."
```

**Docker/Kubernetes**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: clockify-addon-secrets
type: Opaque
data:
  db-password: <base64-encoded>
  webhook-secret: <base64-encoded>
```

---

## Configuration Files

### .env.example (Safe to Commit)

The `.env.example` file shows required variables but MUST NOT contain real values:

```bash
# ✅ GOOD: Empty values
DB_PASSWORD=

# ✅ GOOD: Placeholder text
ADDON_WEBHOOK_SECRET=<generate-with-openssl-rand>

# ✅ GOOD: Instructions
# CLOCKIFY_API_KEY=<your-api-key-from-clockify>

# ❌ BAD: Real secrets
DB_PASSWORD=myRealPassword123
ADDON_WEBHOOK_SECRET=sk-abc123xyz789
```

### .env.local (Unsafe - Never Commit)

Local development `.env` files MUST be in `.gitignore`:

```gitignore
.env
.env.local
.env.*.local
*.pem
*.key
*.keystore
```

---

## Testing with Secrets

### ✅ Testing Approach

Use placeholder/test values in tests:

```java
@Test
public void testTokenValidation() {
    // Use clear test tokens - they're not real secrets
    String testToken = "test-token-" + UUID.randomUUID();

    TokenStore store = new InMemoryTokenStore();
    store.save("ws-test", testToken);

    Optional<String> retrieved = store.get("ws-test");
    assertEquals(testToken, retrieved.get());
}
```

### ✅ Test Configuration

Use `@TestPropertySource` or environment variable mocking:

```java
@Test
@TestPropertySource(properties = {
    "DB_URL=jdbc:h2:mem:test",
    "DB_USERNAME=sa",
    "DB_PASSWORD="
})
public void testDatabaseConnection() {
    // Test code here
}
```

### ❌ What NOT to Do

```java
// WRONG: Real token in test
@Test
public void testWebhook() {
    String realToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";  // ❌
}

// WRONG: Hardcoded credentials
@Before
public void setup() {
    System.setProperty("DB_PASSWORD", "admin123");  // ❌
}
```

---

## Secret Rotation

### Clockify Installation Tokens

1. **Identify all installed instances**:
   ```bash
   SELECT workspace_id FROM addon_tokens;
   ```

2. **Generate new tokens** (through Clockify workspace settings)

3. **Update running instances** (zero-downtime):
   ```bash
   # New addon version with new tokens
   docker pull clockify/addon:v2.0
   docker run -e INSTALLATION_TOKEN="new-token" ...
   ```

4. **Verify old tokens work** during transition period

5. **Disable old tokens** after verification

### Database Passwords

1. Generate strong new password:
   ```bash
   openssl rand -base64 32
   ```

2. Update database user:
   ```sql
   ALTER USER clockify_user WITH PASSWORD 'new-secure-password';
   ```

3. Update all applications with new password

4. Verify applications can connect

5. Keep old password as backup for 24 hours

### Webhook Secrets

1. Generate new secret:
   ```bash
   openssl rand -hex 32
   ```

2. Update environment variable:
   ```bash
   ADDON_WEBHOOK_SECRET="<new-secret>"
   ```

3. Redeploy applications

4. Clockify will continue using old secret during transition

5. After verification, disable old secret in Clockify

---

## Scanning for Secrets

### Pre-commit Scanning

Prevent accidental secret commits:

```bash
# Install detect-secrets
pip install detect-secrets

# Scan for secrets
detect-secrets scan --baseline .secrets.baseline

# Prevent commits with secrets
# (configure as git pre-commit hook)
```

### CI/CD Scanning

Add to GitHub Actions / GitLab CI:

```yaml
- name: Detect secrets
  uses: trufflesecurity/trufflehog@main
  with:
    path: ./
    base: ${{ github.event.repository.default_branch }}
    head: HEAD
```

### Manual Verification

Before committing, verify no secrets are exposed:

```bash
# Check for common patterns
grep -r "password\|secret\|api.key\|token" . \
  --include="*.java" \
  --include="*.properties" \
  --include="*.yml" \
  --exclude-dir=.git \
  | grep -v "SECURITY\|Security\|String.*secret\|test-token"

# Check staged files
git diff --cached | grep -i "password\|secret\|api.key"
```

---

## What to Do If a Secret Is Leaked

1. **IMMEDIATELY rotate the secret**:
   - Generate new credentials
   - Update all applications
   - Document what was exposed

2. **Revoke the old secret**:
   - Delete from Clockify workspace settings
   - Drop/update database users
   - Revoke API keys

3. **Audit access**:
   - Check logs for unauthorized use
   - Monitor for account takeover attempts
   - Alert security team

4. **Remove from history**:
   ```bash
   # If accidentally committed to git
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch secrets.env" \
     --prune-empty --tag-name-filter cat -- --all

   # Force push (be careful!)
   git push origin --force
   ```

---

## Summary

| Action | Status |
|--------|--------|
| Hardcode secrets in code | ❌ Never |
| Commit secrets to git | ❌ Never |
| Store in .env files | ✅ Only .env.local (gitignored) |
| Use environment variables | ✅ Always |
| Use secrets manager (prod) | ✅ Always |
| Use placeholder values in examples | ✅ Always |
| Rotate secrets regularly | ✅ Every 90 days |
| Scan for secrets | ✅ Every commit |

---

## See Also

- [PRODUCTION-DEPLOYMENT.md](./PRODUCTION-DEPLOYMENT.md) - Production security checklist
- [DATABASE-SETUP.md](./DATABASE-SETUP.md) - Database credential management
- OWASP: [Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [trufflesecurity/trufflehog](https://github.com/trufflesecurity/trufflehog) - Secret scanning tool
