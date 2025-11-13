# JWT Verification Guide

## Overview

JWT (JSON Web Token) verification ensures that lifecycle handlers and settings endpoints only accept authenticated requests from Clockify. The boilerplate uses RS256 (RSA SHA-256) signatures with automatic key rotation support.

### What It Does
- **Lifecycle Handlers**: Verifies `INSTALLED` and `DELETED` events come from Clockify
- **Settings Endpoints**: Verifies settings UI requests contain valid workspace context
- **Security Benefit**: Prevents unauthorized token seeding/wiping and workspace context spoofing

### When It's Used
- `POST /lifecycle/installed` - Workspace installation
- `POST /lifecycle/deleted` - Workspace uninstallation
- `GET /settings` - Settings sidebar iframe (extracts workspace context from JWT)

---

## Configuration

### Environment Variables

#### Required (Production)
```bash
CLOCKIFY_JWT_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
CLOCKIFY_JWT_EXPECTED_ISS="clockify"
```

#### Optional (Key Rotation)
```bash
CLOCKIFY_JWT_PUBLIC_KEY_MAP='{"kid-1": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----", "kid-2": "..."}'
```

#### Testing Only
```bash
ADDON_ACCEPT_JWT_SIGNATURE=true  # Accept any JWT in dev mode
ENV=dev                          # Enable development compatibility
```

### Setup Steps

1. **Get Public Key** (from Clockify Admin):
   ```bash
   export CLOCKIFY_JWT_PUBLIC_KEY="your-public-key-here"
   export CLOCKIFY_JWT_EXPECTED_ISS="clockify"
   ```

2. **Verify Installation**:
   ```bash
   echo "JWT verification enabled with key: $CLOCKIFY_JWT_PUBLIC_KEY"
   ```

3. **Test with curl**:
   ```bash
   # Generate test JWT (see Testing section)
   curl -X POST http://localhost:8080/lifecycle/installed \
     -H "Clockify-Signature: $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"workspaceId":"ws-123","authToken":"token"}'
   ```

---

## Implementation

### In Lifecycle Handlers

The SDK handles JWT verification automatically:

```java
// In LifecycleHandlers.java (already implemented)
var sig = WebhookSignatureValidator.verifyLifecycle(request, addonKey);
if (!sig.isValid()) {
    return sig.response();  // Returns 401 or 403
}
// Continue processing...
```

### In Settings Endpoints

Extract workspace context from JWT payload:

```java
// In SettingsController.java (already implemented)
JwtTokenDecoder.DecodedJwt decoded = JwtTokenDecoder.decode(jwtToken);
JsonNode payload = decoded.payload();

String workspaceId = payload.get("workspaceId").asText();
String userId = payload.get("userId").asText();
```

### Error Handling

Return appropriate HTTP status codes:
- **401 Unauthorized**: Missing or invalid signature
- **403 Forbidden**: Invalid issuer or claims
- **400 Bad Request**: Malformed JWT

---

## Testing

### Unit Tests with JWT Verification

Use `SignatureTestUtil` from addon-sdk test utilities:

```java
// Generate RSA key pair
var key = SignatureTestUtil.RsaFixture.generate("test-kid");
System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", key.pemPublic);
System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

// Create signed JWT
String jwt = SignatureTestUtil.rs256Jwt(
    key,
    new SignatureTestUtil.Builder()
        .sub("auto-tag-assistant")
        .workspaceId("workspace-123")
);

// Send with Clockify-Signature header
connection.setRequestProperty("Clockify-Signature", jwt);

// Cleanup
System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
```

### Integration Tests

For full integration testing, use real Clockify JWT tokens (requires test workspace setup).

See: [TESTING.md](TESTING.md) for comprehensive testing patterns.

---

## Common Issues & Troubleshooting

### 401 Unauthorized (No Signature)
**Cause**: Request missing `Clockify-Signature` header
**Fix**: Ensure header is set in request:
```java
connection.setRequestProperty("Clockify-Signature", jwt);
```

### 401 Unauthorized (Invalid Signature)
**Cause**: Signature verification failed
**Fix**:
- Verify `CLOCKIFY_JWT_PUBLIC_KEY` is correct
- Check JWT token is properly formatted
- Ensure JWT uses RS256 algorithm

### 403 Forbidden (Invalid Issuer)
**Cause**: JWT issuer doesn't match `CLOCKIFY_JWT_EXPECTED_ISS`
**Fix**: Set issuer in JWT builder:
```java
new SignatureTestUtil.Builder().iss("clockify")
```

### Key Rotation Issues
**Problem**: New keys not being recognized
**Solution**: Use `CLOCKIFY_JWT_PUBLIC_KEY_MAP` for multiple keys:
```bash
CLOCKIFY_JWT_PUBLIC_KEY_MAP='{
  "old-key": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
  "new-key": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
}'
```

---

## Related Documentation

- [SECURITY.md](SECURITY.md) - Comprehensive security features overview
- [DATABASE_TOKEN_STORE.md](DATABASE_TOKEN_STORE.md) - Persistent token storage
- [TESTING.md](TESTING.md) - Testing patterns including JWT verification
- [PRODUCTION-DEPLOYMENT.md](PRODUCTION-DEPLOYMENT.md) - Production security setup
