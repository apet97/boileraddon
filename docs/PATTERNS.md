# Common Patterns for Clockify Addons

**Reusable code patterns and best practices**

This document provides production-ready code patterns for common addon scenarios. All examples are based on the existing auto-tag-assistant addon and follow Java best practices.

## Table of Contents

- [Token Storage](#token-storage)
- [Webhook Signature Validation](#webhook-signature-validation)
- [API Client with Error Handling](#api-client-with-error-handling)
- [Rate Limiting and Retry Logic](#rate-limiting-and-retry-logic)
- [Caching Strategies](#caching-strategies)
- [Multi-Workspace State Management](#multi-workspace-state-management)
- [Async Processing](#async-processing)
- [Input Validation](#input-validation)
- [JWT Token Verification](#jwt-token-verification)
- [Configuration Management](#configuration-management)

---

## Token Storage

### In-Memory Token Store (Development)

```java
package com.example.addon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Simple in-memory token storage for development.
 * WARNING: Tokens are lost on restart. Use persistent storage in production.
 */
public class InMemoryTokenStore implements TokenStore {
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Override
    public void saveToken(String workspaceId, String token) {
        if (workspaceId == null || token == null) {
            throw new IllegalArgumentException("Workspace ID and token must not be null");
        }
        tokens.put(workspaceId, token);
        System.out.println("Stored token for workspace: " + workspaceId);
    }

    @Override
    public String getToken(String workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        return tokens.get(workspaceId);
    }

    @Override
    public void removeToken(String workspaceId) {
        if (workspaceId == null) {
            return;
        }
        tokens.remove(workspaceId);
        System.out.println("Removed token for workspace: " + workspaceId);
    }

    @Override
    public boolean hasToken(String workspaceId) {
        return workspaceId != null && tokens.containsKey(workspaceId);
    }
}
```

### File-Based Token Store (Production)

```java
package com.example.addon;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-based token storage with thread-safe read/write operations.
 * Stores tokens in a properties file with encryption (add encryption as needed).
 */
public class FileBasedTokenStore implements TokenStore {
    private final Path tokenFile;
    private final Properties tokens;
    private final ReadWriteLock lock;

    public FileBasedTokenStore(String tokenFilePath) {
        this.tokenFile = Paths.get(tokenFilePath);
        this.tokens = new Properties();
        this.lock = new ReentrantReadWriteLock();

        // Create file if it doesn't exist
        try {
            if (!Files.exists(tokenFile)) {
                Files.createDirectories(tokenFile.getParent());
                Files.createFile(tokenFile);
            }
            loadTokens();
        } catch (IOException e) {
            System.err.println("Failed to initialize token store: " + e.getMessage());
        }
    }

    @Override
    public void saveToken(String workspaceId, String token) {
        lock.writeLock().lock();
        try {
            tokens.setProperty(workspaceId, token);
            persistTokens();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save token", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getToken(String workspaceId) {
        lock.readLock().lock();
        try {
            return tokens.getProperty(workspaceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeToken(String workspaceId) {
        lock.writeLock().lock();
        try {
            tokens.remove(workspaceId);
            persistTokens();
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove token", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasToken(String workspaceId) {
        lock.readLock().lock();
        try {
            return tokens.containsKey(workspaceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void loadTokens() throws IOException {
        if (Files.exists(tokenFile)) {
            try (InputStream in = Files.newInputStream(tokenFile)) {
                tokens.load(in);
            }
        }
    }

    private void persistTokens() throws IOException {
        try (OutputStream out = Files.newOutputStream(tokenFile)) {
            tokens.store(out, "Clockify Addon Installation Tokens");
        }
    }
}
```

### Database-Ready Token Store Interface

```java
package com.example.addon;

public interface TokenStore {
    /**
     * Save installation token for a workspace.
     * @param workspaceId Clockify workspace ID
     * @param token Installation token from INSTALLED event
     */
    void saveToken(String workspaceId, String token);

    /**
     * Retrieve installation token for a workspace.
     * @param workspaceId Clockify workspace ID
     * @return Installation token or null if not found
     */
    String getToken(String workspaceId);

    /**
     * Remove installation token for a workspace (on DELETED event).
     * @param workspaceId Clockify workspace ID
     */
    void removeToken(String workspaceId);

    /**
     * Check if token exists for a workspace.
     * @param workspaceId Clockify workspace ID
     * @return true if token exists
     */
    boolean hasToken(String workspaceId);
}
```

---

## Webhook Signature Validation

### Complete Signature Validator

```java
package com.example.addon.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Validates HMAC-SHA256 signatures from Clockify webhooks.
 * Based on auto-tag-assistant/security/WebhookSignatureValidator.java
 */
public class WebhookSignatureValidator {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String signingSecret;

    /**
     * @param signingSecret The webhook signing secret provided by Clockify
     */
    public WebhookSignatureValidator(String signingSecret) {
        if (signingSecret == null || signingSecret.isEmpty()) {
            throw new IllegalArgumentException("Signing secret must not be null or empty");
        }
        this.signingSecret = signingSecret;
    }

    /**
     * Validates webhook signature.
     * @param payload Raw request body as string
     * @param signature Value of x-clockify-signature header
     * @return true if signature is valid
     */
    public boolean validateSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            System.err.println("Invalid signature format: " + signature);
            return false;
        }

        try {
            String expectedSignature = signature.substring(SIGNATURE_PREFIX.length());
            String computedSignature = computeSignature(payload);
            return constantTimeEquals(computedSignature, expectedSignature);
        } catch (Exception e) {
            System.err.println("Signature validation failed: " + e.getMessage());
            return false;
        }
    }

    private String computeSignature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes(), HMAC_SHA256);
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes());
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
```

### Usage in Webhook Handler

```java
public HttpResponse handleWebhook(HttpRequest request) {
    // 1. Read raw body
    String rawBody = request.getBodyAsString();

    // 2. Get signature header
    String signature = request.getHeader("x-clockify-signature");

    // 3. Validate
    if (!signatureValidator.validateSignature(rawBody, signature)) {
        System.err.println("Invalid webhook signature");
        return HttpResponse.unauthorized("Invalid signature");
    }

    // 4. Process webhook
    JSONObject body = new JSONObject(rawBody);
    // ... handle event
}
```

---

## API Client with Error Handling

### Production-Ready API Client

```java
package com.example.addon.api;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class ClockifyApiClient {
    private final String apiBaseUrl;
    private final TokenStore tokenStore;

    public ClockifyApiClient(String apiBaseUrl, TokenStore tokenStore) {
        this.apiBaseUrl = apiBaseUrl;
        this.tokenStore = tokenStore;
    }

    /**
     * Make an authenticated API request to Clockify.
     */
    public ApiResponse request(String workspaceId, String method, String endpoint, String body)
            throws ApiException {
        String token = tokenStore.getToken(workspaceId);
        if (token == null) {
            throw new ApiException("No installation token found for workspace: " + workspaceId, 401);
        }

        try {
            URL url = new URL(apiBaseUrl + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("X-Addon-Token", token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000); // 10 seconds
            conn.setReadTimeout(30000);    // 30 seconds

            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
            }

            int statusCode = conn.getResponseCode();
            String responseBody;

            if (statusCode >= 200 && statusCode < 300) {
                responseBody = readStream(conn.getInputStream());
                return new ApiResponse(statusCode, responseBody, null);
            } else {
                String errorBody = readStream(conn.getErrorStream());
                return new ApiResponse(statusCode, null, errorBody);
            }
        } catch (IOException e) {
            throw new ApiException("Network error: " + e.getMessage(), 0, e);
        }
    }

    /**
     * Convenience method for GET requests.
     */
    public ApiResponse get(String workspaceId, String endpoint) throws ApiException {
        return request(workspaceId, "GET", endpoint, null);
    }

    /**
     * Convenience method for POST requests.
     */
    public ApiResponse post(String workspaceId, String endpoint, String body) throws ApiException {
        return request(workspaceId, "POST", endpoint, body);
    }

    /**
     * Convenience method for PUT requests.
     */
    public ApiResponse put(String workspaceId, String endpoint, String body) throws ApiException {
        return request(workspaceId, "PUT", endpoint, body);
    }

    /**
     * Convenience method for DELETE requests.
     */
    public ApiResponse delete(String workspaceId, String endpoint) throws ApiException {
        return request(workspaceId, "DELETE", endpoint, null);
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * API response container.
     */
    public static class ApiResponse {
        public final int statusCode;
        public final String body;
        public final String errorBody;

        public ApiResponse(int statusCode, String body, String errorBody) {
            this.statusCode = statusCode;
            this.body = body;
            this.errorBody = errorBody;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        public boolean isUnauthorized() {
            return statusCode == 401;
        }

        public boolean isForbidden() {
            return statusCode == 403;
        }

        public boolean isNotFound() {
            return statusCode == 404;
        }

        public boolean isRateLimited() {
            return statusCode == 429;
        }
    }

    /**
     * Custom exception for API errors.
     */
    public static class ApiException extends Exception {
        public final int statusCode;

        public ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public ApiException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }
    }
}
```

---

## Rate Limiting and Retry Logic

### Exponential Backoff Retry

```java
package com.example.addon.api;

import java.util.concurrent.TimeUnit;

public class RetryableApiClient {
    private final ClockifyApiClient apiClient;
    private final int maxRetries;
    private final long initialBackoffMs;

    public RetryableApiClient(ClockifyApiClient apiClient, int maxRetries, long initialBackoffMs) {
        this.apiClient = apiClient;
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
    }

    /**
     * Execute API request with exponential backoff retry on rate limit (429) and server errors (5xx).
     */
    public ClockifyApiClient.ApiResponse requestWithRetry(
            String workspaceId, String method, String endpoint, String body)
            throws ClockifyApiClient.ApiException, InterruptedException {

        ClockifyApiClient.ApiResponse response = null;
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                response = apiClient.request(workspaceId, method, endpoint, body);

                // Success - return immediately
                if (response.isSuccess()) {
                    return response;
                }

                // Don't retry client errors (except 429)
                if (response.statusCode >= 400 && response.statusCode < 500 && !response.isRateLimited()) {
                    return response;
                }

                // Rate limited or server error - retry with backoff
                if (response.isRateLimited() || response.statusCode >= 500) {
                    if (attempt < maxRetries) {
                        long backoffMs = initialBackoffMs * (long) Math.pow(2, attempt);
                        System.out.println("Retrying after " + backoffMs + "ms (attempt " + (attempt + 1) + "/" + maxRetries + ")");
                        TimeUnit.MILLISECONDS.sleep(backoffMs);
                        attempt++;
                        continue;
                    }
                }

                // Other errors - return
                return response;

            } catch (ClockifyApiClient.ApiException e) {
                // Network errors - retry
                if (attempt < maxRetries) {
                    long backoffMs = initialBackoffMs * (long) Math.pow(2, attempt);
                    System.err.println("Network error, retrying after " + backoffMs + "ms: " + e.getMessage());
                    TimeUnit.MILLISECONDS.sleep(backoffMs);
                    attempt++;
                } else {
                    throw e;
                }
            }
        }

        return response;
    }
}
```

---

## Caching Strategies

### Simple Time-Based Cache

```java
package com.example.addon.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SimpleCache<K, V> {
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public SimpleCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    private static class CacheEntry<V> {
        final V value;
        final long expiresAt;

        CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}

// Usage example:
// SimpleCache<String, List<Tag>> tagCache = new SimpleCache<>(60000); // 1 minute TTL
// tagCache.put(workspaceId, tags);
// List<Tag> cached = tagCache.get(workspaceId);
```

---

## Multi-Workspace State Management

### Workspace-Scoped Configuration

```java
package com.example.addon.workspace;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class WorkspaceConfigManager {
    private final Map<String, WorkspaceConfig> configs = new ConcurrentHashMap<>();

    public void initializeWorkspace(String workspaceId) {
        configs.putIfAbsent(workspaceId, new WorkspaceConfig(workspaceId));
    }

    public WorkspaceConfig getConfig(String workspaceId) {
        return configs.get(workspaceId);
    }

    public void removeWorkspace(String workspaceId) {
        configs.remove(workspaceId);
    }

    public static class WorkspaceConfig {
        private final String workspaceId;
        private boolean enabled;
        private Map<String, String> settings;

        public WorkspaceConfig(String workspaceId) {
            this.workspaceId = workspaceId;
            this.enabled = true;
            this.settings = new ConcurrentHashMap<>();
        }

        public String getWorkspaceId() {
            return workspaceId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSetting(String key) {
            return settings.get(key);
        }

        public void setSetting(String key, String value) {
            settings.put(key, value);
        }

        public Map<String, String> getAllSettings() {
            return new ConcurrentHashMap<>(settings);
        }
    }
}
```

---

## Async Processing

### Background Task Executor

```java
package com.example.addon.async;

import java.util.concurrent.*;

public class BackgroundTaskExecutor {
    private final ExecutorService executor;

    public BackgroundTaskExecutor(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Submit a task for async execution.
     */
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    /**
     * Submit a task with result.
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * Shutdown executor gracefully.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Usage in webhook handler:
public HttpResponse handleNewTimeEntry(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String timeEntryId = (String) body.get("timeEntryId");

    // Process asynchronously to avoid blocking webhook response
    backgroundExecutor.submit(() -> {
        try {
            processTimeEntry(workspaceId, timeEntryId);
        } catch (Exception e) {
            System.err.println("Failed to process time entry: " + e.getMessage());
        }
    });

    // Return immediately
    return HttpResponse.ok("{\"processed\": true}");
}
```

---

## Input Validation

### Request Validator

```java
package com.example.addon.validation;

import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern WORKSPACE_ID_PATTERN = Pattern.compile("^[a-f0-9]{24}$");
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]{1,100}$");
    private static final int MAX_DESCRIPTION_LENGTH = 3000;

    public static void validateWorkspaceId(String workspaceId) throws ValidationException {
        if (workspaceId == null || !WORKSPACE_ID_PATTERN.matcher(workspaceId).matches()) {
            throw new ValidationException("Invalid workspace ID format");
        }
    }

    public static void validateTagName(String tagName) throws ValidationException {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new ValidationException("Tag name cannot be empty");
        }
        if (!TAG_NAME_PATTERN.matcher(tagName).matches()) {
            throw new ValidationException("Tag name contains invalid characters or exceeds length limit");
        }
    }

    public static void validateDescription(String description) throws ValidationException {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH);
        }
    }

    public static String sanitizeHtml(String input) {
        if (input == null) return null;
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
```

---

## JWT Token Verification

### JWT Verifier

```java
package com.example.addon.security;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.json.JSONObject;

public class JwtVerifier {
    private final PublicKey publicKey;

    public JwtVerifier(String publicKeyPem) throws Exception {
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    public JSONObject verifyAndDecode(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String headerAndPayload = parts[0] + "." + parts[1];
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);

        // Verify signature
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(headerAndPayload.getBytes());

        if (!sig.verify(signature)) {
            throw new SecurityException("Invalid JWT signature");
        }

        // Decode payload
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        String payloadJson = new String(payloadBytes);
        JSONObject payload = new JSONObject(payloadJson);

        // Verify expiration
        long exp = payload.getLong("exp");
        if (System.currentTimeMillis() / 1000 > exp) {
            throw new SecurityException("JWT token expired");
        }

        return payload;
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
```

---

## Configuration Management

### Environment-Based Configuration

```java
package com.example.addon.config;

public class AddonConfig {
    private final String addonPort;
    private final String addonBaseUrl;
    private final String apiBaseUrl;
    private final String signingSecret;
    private final boolean debugMode;

    private AddonConfig(Builder builder) {
        this.addonPort = builder.addonPort;
        this.addonBaseUrl = builder.addonBaseUrl;
        this.apiBaseUrl = builder.apiBaseUrl;
        this.signingSecret = builder.signingSecret;
        this.debugMode = builder.debugMode;
    }

    public static AddonConfig fromEnvironment() {
        return new Builder()
            .addonPort(getEnv("ADDON_PORT", "8080"))
            .addonBaseUrl(getEnv("ADDON_BASE_URL", "http://localhost:8080/addon"))
            .apiBaseUrl(getEnv("CLOCKIFY_API_BASE_URL", "https://api.clockify.me/api/v1"))
            .signingSecret(getEnv("CLOCKIFY_SIGNING_SECRET", ""))
            .debugMode(Boolean.parseBoolean(getEnv("DEBUG", "false")))
            .build();
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public String getAddonPort() { return addonPort; }
    public String getAddonBaseUrl() { return addonBaseUrl; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public String getSigningSecret() { return signingSecret; }
    public boolean isDebugMode() { return debugMode; }

    public static class Builder {
        private String addonPort;
        private String addonBaseUrl;
        private String apiBaseUrl;
        private String signingSecret;
        private boolean debugMode;

        public Builder addonPort(String addonPort) {
            this.addonPort = addonPort;
            return this;
        }

        public Builder addonBaseUrl(String addonBaseUrl) {
            this.addonBaseUrl = addonBaseUrl;
            return this;
        }

        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public Builder signingSecret(String signingSecret) {
            this.signingSecret = signingSecret;
            return this;
        }

        public Builder debugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        public AddonConfig build() {
            return new AddonConfig(this);
        }
    }
}
```

---

## Best Practices Summary

1. **Token Storage**
   - ✅ Store installation tokens from INSTALLED events
   - ✅ Use thread-safe storage (ConcurrentHashMap, file locks)
   - ✅ Remove tokens on DELETED events
   - ✅ Never log or expose tokens

2. **Security**
   - ✅ Always validate webhook signatures
   - ✅ Verify JWT tokens for settings/sidebars
   - ✅ Sanitize all user inputs
   - ✅ Use constant-time comparisons for secrets

3. **Error Handling**
   - ✅ Implement exponential backoff for rate limits
   - ✅ Handle network errors gracefully
   - ✅ Log errors with context (no sensitive data)
   - ✅ Return appropriate HTTP status codes

4. **Performance**
   - ✅ Cache frequently accessed data
   - ✅ Use async processing for heavy tasks
   - ✅ Implement connection pooling
   - ✅ Set reasonable timeouts

5. **Multi-Workspace**
   - ✅ Isolate workspace data
   - ✅ Clean up on workspace deletion
   - ✅ Use workspace-scoped caching

---

## Additional Resources

- [API Cookbook](API-COOKBOOK.md) - API examples
- [Request/Response Examples](REQUEST-RESPONSE-EXAMPLES.md) - Full HTTP exchanges
- [Quick Reference](QUICK-REFERENCE.md) - Cheat sheet
- [Auto-Tag Assistant](../addons/auto-tag-assistant/) - Complete working example
