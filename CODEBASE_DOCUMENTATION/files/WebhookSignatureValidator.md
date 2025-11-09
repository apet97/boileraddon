# WebhookSignatureValidator.java

**Location:** `addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/WebhookSignatureValidator.java`

**Package:** `com.clockify.addon.sdk.security`

**Lines:** 194

---

## Overview

`WebhookSignatureValidator` provides webhook signature validation using HMAC-SHA256 and JWT (for development workspaces). It ensures webhook requests originate from Clockify and haven't been tampered with.

## Purpose

- Validate HMAC-SHA256 webhook signatures
- Support JWT signatures from developer workspaces
- Provide high-level and low-level validation APIs
- Protect against replay attacks and tampering

---

## Security Headers

The validator checks these headers (in order):

1. `clockify-webhook-signature` (primary)
2. `x-clockify-webhook-signature`
3. `Clockify-Webhook-Signature`
4. `X-Clockify-Webhook-Signature`
5. `Clockify-Signature` (JWT for dev workspaces)

---

## Class Structure

```java
public final class WebhookSignatureValidator {
    // Constants
    public static final String SIGNATURE_HEADER = "clockify-webhook-signature";
    private static final String[] ALT_HEADERS = {...};

    // Inner class
    public static class VerificationResult {
        private final boolean valid;
        private final HttpResponse response;
    }
}
```

---

## High-Level API

### verify (Recommended)

**Signature:** `public static VerificationResult verify(HttpServletRequest request, String workspaceId)`

**Purpose:** Validate request using stored installation token

**Parameters:**
- `request` - HTTP servlet request
- `workspaceId` - Workspace identifier

**Returns:** VerificationResult with validation status and response

**Example:**

```java
String workspaceId = extractWorkspaceId(request);
WebhookSignatureValidator.VerificationResult result =
    WebhookSignatureValidator.verify(request, workspaceId);

if (!result.isValid()) {
    return result.response(); // 401 or 403
}

// Process webhook
return HttpResponse.ok("ok");
```

**Validation Flow:**

```
1. Check workspaceId present → 401 if missing
2. Retrieve installation token from TokenStore → 401 if not found
3. Extract signature header → 401 if missing
4. Read request body
5. Determine signature type:
   a. HMAC (sha256=<hex> or raw hex) → validate()
   b. JWT (contains dots) → decodeJwtPayload() + verify workspaceId
6. Return result:
   - Valid → VerificationResult.ok()
   - Invalid → 403 Forbidden
```

**Response Codes:**

| Code | Meaning | Body |
|------|---------|------|
| 200 | Valid signature | `{"ok"}` |
| 401 | Missing token/signature | `{"error":"..."}` |
| 403 | Invalid signature | `{"error":"invalid signature"}` |

---

## Low-Level API

### validate (Manual Validation)

**Signature:** `public static boolean validate(String signatureHeader, byte[] body, String sharedSecret)`

**Purpose:** Low-level HMAC-SHA256 validator

**Parameters:**
- `signatureHeader` - Signature from header (e.g., `"sha256=abc123..."` or `"abc123..."`)
- `body` - Raw request body bytes
- `sharedSecret` - Shared secret for HMAC (installation token)

**Returns:** `true` if signature valid, `false` otherwise

**Example:**

```java
String signature = request.getHeader("clockify-webhook-signature");
byte[] body = readBodyBytes(request);
String secret = getStoredToken(workspaceId);

boolean valid = WebhookSignatureValidator.validate(signature, body, secret);
if (!valid) {
    return HttpResponse.error(403, "Invalid signature");
}
```

**Algorithm:**

```
1. Compute expected HMAC-SHA256 of body using secret
2. Normalize provided signature (strip "sha256=" prefix)
3. Constant-time comparison of expected vs provided
```

---

### computeSignature (Utility)

**Signature:** `public static String computeSignature(String sharedSecret, String body)`

**Purpose:** Generate signature for testing/tooling

**Parameters:**
- `sharedSecret` - Shared secret
- `body` - Request body string

**Returns:** Signature in header format (`"sha256=<hex>"`)

**Example:**

```java
String secret = "my-secret-key";
String body = "{\"event\":\"TIME_ENTRY_CREATED\"}";

String signature = WebhookSignatureValidator.computeSignature(secret, body);
// Returns: "sha256=a1b2c3d4e5f6..."

// Use in test
mockRequest.setHeader("clockify-webhook-signature", signature);
```

---

## HMAC-SHA256 Implementation

### hmacHex (Internal)

**Signature:** `private static String hmacHex(byte[] key, byte[] data)`

**Purpose:** Compute HMAC-SHA256 and return as lowercase hex

**Algorithm:**

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(key, "HmacSHA256"));
byte[] out = mac.doFinal(data);
return toHex(out); // lowercase hex
```

**Example Output:**

```
Input: key="secret", data="hello"
Output: "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b"
```

---

## JWT Support (Developer Workspaces)

### looksLikeJwt

**Signature:** `private static boolean looksLikeJwt(String sig)`

**Purpose:** Detect JWT signature format

**Logic:**

```java
return sig != null && sig.contains(".") && sig.split("\\.").length >= 2;
```

**Example:**

```
"eyJhbGc...  .eyJzdWI...  .SflKxwR..."  → true (JWT)
"sha256=abc123..."                       → false (HMAC)
"abc123..."                              → false (HMAC)
```

---

### decodeJwtPayload

**Signature:** `private static String decodeJwtPayload(String jwt)`

**Purpose:** Extract JWT payload (claims) without validation

**Algorithm:**

```java
1. Split JWT by "." → [header, payload, signature]
2. Take payload (index 1)
3. Base64url decode (replace - with +, _ with /)
4. Return as UTF-8 string
```

**Example:**

```java
String jwt = "eyJhbGc...  .eyJ3b3Jrc3BhY2VJZCI6IjEyMyJ9...";
String payload = decodeJwtPayload(jwt);
// Returns: {"workspaceId":"123"}
```

**Note:** This is NOT cryptographic verification, just parsing.

---

### acceptJwtDevSignature

**Signature:** `private static boolean acceptJwtDevSignature()`

**Purpose:** Check if JWT signatures should be accepted

**Environment Variable:**

```bash
ADDON_ACCEPT_JWT_SIGNATURE=true   # Accept JWT (default)
ADDON_ACCEPT_JWT_SIGNATURE=false  # Reject JWT
```

**Default:** `true` (accept JWT)

**Usage:**

```java
// In verify()
if (looksLikeJwt(sigHeader) && acceptJwtDevSignature()) {
    // Decode and verify workspaceId
}
```

---

## Constant-Time Comparison

### constantTimeEquals

**Signature:** `private static boolean constantTimeEquals(String a, String b)`

**Purpose:** Compare strings in constant time to prevent timing attacks

**Algorithm:**

```java
if (a == null || b == null) return false;
if (a.length() != b.length()) return false; // Length leak is OK

int result = 0;
for (int i = 0; i < a.length(); i++) {
    result |= a.charAt(i) ^ b.charAt(i);
}
return result == 0;
```

**Why Constant Time?**

Non-constant time:
```java
for (int i = 0; i < a.length(); i++) {
    if (a.charAt(i) != b.charAt(i)) return false; // EARLY RETURN = TIMING LEAK
}
```

Attacker can measure timing to guess signature byte-by-byte.

Constant time:
```java
result |= a.charAt(i) ^ b.charAt(i); // NO EARLY RETURN
```

Always processes full string, preventing timing attacks.

---

## Signature Format Normalization

### normalize

**Signature:** `private static String normalize(String header)`

**Purpose:** Strip prefix from signature header

**Examples:**

```
Input: "sha256=abc123..."  → Output: "abc123..."
Input: "abc123..."         → Output: "abc123..."
```

---

### looksLikeHmac

**Signature:** `private static boolean looksLikeHmac(String sig)`

**Purpose:** Detect HMAC signature format

**Logic:**

```java
String s = sig.trim();
if (s.startsWith("sha256=")) {
    s = s.substring("sha256=".length());
}
return s.matches("[0-9a-fA-F]{64}"); // 64 hex chars = 256 bits
```

---

## VerificationResult

**Purpose:** Encapsulate validation result + response

```java
public static class VerificationResult {
    private final boolean valid;
    private final HttpResponse response;

    public boolean isValid() { return valid; }
    public HttpResponse response() { return response; }

    public static VerificationResult ok() {
        return new VerificationResult(true, HttpResponse.ok("ok"));
    }
}
```

**Usage:**

```java
VerificationResult result = WebhookSignatureValidator.verify(request, workspaceId);
if (!result.isValid()) {
    return result.response(); // Return pre-built error response
}
```

---

## Complete Usage Example

### Production Setup (HMAC)

```java
// 1. Webhook handler
addon.registerWebhookHandler("TIME_ENTRY_CREATED", request -> {
    // 2. Extract workspaceId from payload
    JsonNode payload = new ObjectMapper().readTree(request.getInputStream());
    String workspaceId = payload.get("workspaceId").asText();

    // 3. Verify signature
    WebhookSignatureValidator.VerificationResult result =
        WebhookSignatureValidator.verify(request, workspaceId);

    if (!result.isValid()) {
        return result.response(); // 401/403
    }

    // 4. Process webhook
    processTimeEntry(payload);

    return HttpResponse.ok("ok");
});
```

---

### Development Setup (Skip Verification)

```bash
# .env
ADDON_SKIP_SIGNATURE_VERIFY=true
```

```java
// Check environment variable before verification
boolean skipVerify = "true".equalsIgnoreCase(
    System.getenv("ADDON_SKIP_SIGNATURE_VERIFY")
);

if (!skipVerify) {
    VerificationResult result = WebhookSignatureValidator.verify(request, workspaceId);
    if (!result.isValid()) {
        return result.response();
    }
}
```

⚠️ **Warning:** Never skip verification in production!

---

### Testing with Manual Signature

```java
@Test
void testWebhookWithValidSignature() {
    String secret = "test-secret";
    String body = "{\"event\":\"TIME_ENTRY_CREATED\"}";

    // Compute signature
    String signature = WebhookSignatureValidator.computeSignature(secret, body);

    // Create mock request
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setHeader("clockify-webhook-signature", signature);
    request.setContent(body.getBytes(StandardCharsets.UTF_8));

    // Verify
    boolean valid = WebhookSignatureValidator.validate(
        signature,
        body.getBytes(StandardCharsets.UTF_8),
        secret
    );

    assertTrue(valid);
}
```

---

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `ADDON_ACCEPT_JWT_SIGNATURE` | `true` | Accept JWT signatures from dev workspaces |
| `ADDON_SKIP_SIGNATURE_VERIFY` | `false` | Skip verification (dev only!) |

---

## Security Considerations

### HMAC-SHA256
- ✅ Cryptographically secure
- ✅ Prevents tampering
- ✅ Prevents replay attacks (when combined with timestamp checking)
- ✅ Constant-time comparison prevents timing attacks

### JWT (Development Only)
- ⚠️ NOT cryptographically verified
- ⚠️ Only workspaceId extracted and compared
- ⚠️ Should NOT be used in production

### Best Practices

1. **Always verify signatures in production**
   ```bash
   ADDON_SKIP_SIGNATURE_VERIFY=false
   ```

2. **Use HTTPS**
   - Signature alone doesn't encrypt data
   - HTTPS prevents man-in-the-middle attacks

3. **Rotate secrets periodically**
   - Installation tokens should be rotated
   - Delete old tokens on DELETED lifecycle event

4. **Log verification failures**
   - Monitor for suspicious activity
   - Alert on repeated failures

---

## Error Messages

| Error | Meaning |
|-------|---------|
| `workspaceId missing` | No workspaceId provided |
| `installation token not found` | Token not in TokenStore |
| `signature header missing` | No signature header found |
| `invalid signature` | HMAC/JWT verification failed |
| `invalid jwt signature` | JWT workspaceId mismatch |

---

## Related Classes

- **TokenStore** - Stores installation tokens
- **HttpResponse** - Response helper
- **HttpServletRequest** - Jakarta servlet request

---

## See Also

- [TokenStore.md](./TokenStore.md) - Token storage
- [AddonServlet.md](./AddonServlet.md) - Request routing

---

**File Location:** `/home/user/boileraddon/addons/addon-sdk/src/main/java/com/clockify/addon/sdk/security/WebhookSignatureValidator.java`

**Last Updated:** 2025-11-09
