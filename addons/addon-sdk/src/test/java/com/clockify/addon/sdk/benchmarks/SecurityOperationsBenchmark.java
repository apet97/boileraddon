package com.clockify.addon.sdk.benchmarks;

import com.clockify.addon.sdk.security.*;
import com.clockify.addon.sdk.util.PathSanitizer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for security operations.
 *
 * Critical paths:
 * - Token validation performance
 * - CSRF protection overhead
 * - Path sanitization
 * - Request size validation
 * - Audit logging
 *
 * These operations are critical for security and happen frequently.
 *
 * Run with: mvn test -Dtest=SecurityOperationsBenchmark -pl addons/addon-sdk
 * Or: java -jar target/benchmarks.jar SecurityOperationsBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = "-Xmx2g")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class SecurityOperationsBenchmark {

    private DatabaseTokenStore tokenStore;
    private PooledDatabaseTokenStore pooledTokenStore;
    private RotatingTokenStore rotatingTokenStore;

    private String validToken;
    private String invalidToken;
    private String workspaceId;
    private String[] testPaths;
    private String[] maliciousPaths;

    @Setup
    public void setup() throws Exception {
        // Setup test data
        workspaceId = "ws-security-bench-001";
        validToken = "valid-token-1234567890abcdef";
        invalidToken = "invalid-token-xxxxxxxxxxxx";

        // Initialize security components with H2 in-memory database
        String jdbcUrl = "jdbc:h2:mem:security-benchmark;DB_CLOSE_DELAY=-1";
        String username = "sa";
        String password = "";

        tokenStore = new DatabaseTokenStore(jdbcUrl, username, password);
        pooledTokenStore = new PooledDatabaseTokenStore(jdbcUrl, username, password);
        rotatingTokenStore = new RotatingTokenStore(tokenStore);
        // AuditLogger and PathSanitizer are utility classes with static methods

        // Pre-populate token stores
        tokenStore.save(workspaceId, validToken);
        pooledTokenStore.save(workspaceId, validToken);
        rotatingTokenStore.save(workspaceId, validToken);

        // Test paths for sanitization
        testPaths = new String[]{
            "/api/v1/webhook",
            "/settings/save",
            "/manifest.json",
            "/metrics/prometheus",
            "/health/readiness"
        };

        maliciousPaths = new String[]{
            "/test/../admin",
            "/api/../../etc/passwd",
            "/path/with/../../../escape",
            "/test%00",
            "/test\0",
            "/test\u0000",
            "/test%2e%2e%2fadmin"
        };
    }

    /**
     * Benchmark: Database token store validation (valid token)
     * Measures performance of successful token validation.
     */
    @Benchmark
    public void databaseTokenStoreValidationValid(Blackhole bh) {
        boolean result = tokenStore.isValidToken(workspaceId, validToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Database token store validation (invalid token)
     * Measures performance of failed token validation.
     */
    @Benchmark
    public void databaseTokenStoreValidationInvalid(Blackhole bh) {
        boolean result = tokenStore.isValidToken(workspaceId, invalidToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Pooled database token store validation
     * Measures performance of pooled token validation.
     */
    @Benchmark
    public void pooledTokenStoreValidation(Blackhole bh) {
        boolean result = pooledTokenStore.isValidToken(workspaceId, validToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Rotating token store validation
     * Measures performance of rotating token validation.
     */
    @Benchmark
    public void rotatingTokenStoreValidation(Blackhole bh) {
        boolean result = rotatingTokenStore.isValidToken(workspaceId, validToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Token store save operation
     * Measures performance of token storage.
     */
    @Benchmark
    public void tokenStoreSaveOperation(Blackhole bh) {
        String newWorkspaceId = "ws-new-" + System.currentTimeMillis();
        String newToken = "token-" + System.currentTimeMillis();
        tokenStore.save(newWorkspaceId, newToken);
        bh.consume(newWorkspaceId);
    }

    /**
     * Benchmark: Token store delete operation
     * Measures performance of token removal.
     */
    @Benchmark
    public void tokenStoreDeleteOperation(Blackhole bh) {
        String tempWorkspaceId = "ws-temp-" + System.currentTimeMillis();
        tokenStore.save(tempWorkspaceId, "temp-token");
        tokenStore.remove(tempWorkspaceId);
        bh.consume(tempWorkspaceId);
    }

    /**
     * Benchmark: Path sanitization for safe paths
     * Measures performance of sanitizing normal paths.
     */
    @Benchmark
    public void pathSanitizationSafePaths(Blackhole bh) {
        for (String path : testPaths) {
            String sanitized = PathSanitizer.sanitize(path);
            bh.consume(sanitized);
        }
    }

    /**
     * Benchmark: Path sanitization for malicious paths
     * Measures performance of detecting and sanitizing malicious paths.
     */
    @Benchmark
    public void pathSanitizationMaliciousPaths(Blackhole bh) {
        for (String path : maliciousPaths) {
            try {
                String sanitized = PathSanitizer.sanitize(path);
                bh.consume(sanitized);
            } catch (IllegalArgumentException e) {
                // Expected for malicious paths - consume the exception
                bh.consume(e);
            }
        }
    }

    /**
     * Benchmark: Audit logging performance
     * Measures overhead of security audit logging.
     */
    @Benchmark
    public void auditLoggingPerformance(Blackhole bh) {
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS)
            .workspace(workspaceId)
            .clientIp("192.168.1.100")
            .detail("result", "valid")
            .info();
        bh.consume(workspaceId);
    }

    /**
     * Benchmark: Audit logging with complex data
     * Measures performance with detailed audit information.
     */
    @Benchmark
    public void auditLoggingComplexData(Blackhole bh) {
        AuditLogger.log(AuditLogger.AuditEvent.RATE_LIMIT_EXCEEDED)
            .workspace(workspaceId)
            .clientIp("203.0.113.5")
            .detail("path", "/webhook")
            .detail("limit", 100)
            .detail("window", "1s")
            .warn();
        bh.consume(workspaceId);
    }

    /**
     * Benchmark: Multiple security operations in sequence
     * Measures combined performance of common security operations.
     */
    @Benchmark
    public void combinedSecurityOperations(Blackhole bh) {
        // Token validation
        boolean tokenValid = tokenStore.isValidToken(workspaceId, validToken);
        bh.consume(tokenValid);

        // Path sanitization
        String sanitizedPath = PathSanitizer.sanitize("/api/v1/webhook");
        bh.consume(sanitizedPath);

        // Audit logging
        AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS)
            .workspace(workspaceId)
            .clientIp("192.168.1.100")
            .detail("operations", "[\"token_validation\",\"path_sanitization\",\"audit_logging\"]")
            .info();
    }

    /**
     * Benchmark: Concurrent token validation
     * Measures performance under concurrent access patterns.
     */
    @Benchmark
    @Threads(4)
    public void concurrentTokenValidation(Blackhole bh) {
        boolean result = pooledTokenStore.isValidToken(workspaceId, validToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Token rotation performance
     * Measures overhead of token rotation operations.
     */
    @Benchmark
    public void tokenRotationPerformance(Blackhole bh) {
        String newToken = "rotated-token-" + System.currentTimeMillis();
        rotatingTokenStore.rotate(workspaceId, newToken);
        bh.consume(newToken);
    }

    /**
     * Benchmark: Security event batch processing
     * Measures performance of processing multiple security events.
     */
    @Benchmark
    public void securityEventBatchProcessing(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS)
                .workspace(workspaceId)
                .clientIp("192.168.1." + (100 + i))
                .detail("eventId", i)
                .detail("timestamp", System.currentTimeMillis())
                .info();
        }
        bh.consume(workspaceId);
    }

    /**
     * Benchmark: Path sanitization with edge cases
     * Measures performance with various edge case paths.
     */
    @Benchmark
    public void pathSanitizationEdgeCases(Blackhole bh) {
        String[] edgeCases = {
            "",
            "/",
            "//",
            "/../",
            "./",
            "./",
            "/./",
            "/../",
            "/.../",
            "/..../"
        };

        for (String path : edgeCases) {
            try {
                String sanitized = PathSanitizer.sanitize(path);
                bh.consume(sanitized);
            } catch (IllegalArgumentException e) {
                // Expected for malicious paths - consume the exception
                bh.consume(e);
            }
        }
    }

    /**
     * Benchmark: Token validation with non-existent workspace
     * Measures performance when workspace doesn't exist.
     */
    @Benchmark
    public void tokenValidationNonExistentWorkspace(Blackhole bh) {
        boolean result = tokenStore.isValidToken("non-existent-workspace", validToken);
        bh.consume(result);
    }

    /**
     * Benchmark: Security operations with high frequency
     * Measures performance under high request frequency.
     */
    @Benchmark
    @Threads(8)
    public void highFrequencySecurityOperations(Blackhole bh) {
        // Simulate high-frequency security operations
        for (int i = 0; i < 5; i++) {
            boolean tokenValid = tokenStore.isValidToken(workspaceId, validToken);
            String sanitizedPath = PathSanitizer.sanitize(testPaths[i % testPaths.length]);
            AuditLogger.log(AuditLogger.AuditEvent.TOKEN_VALIDATION_SUCCESS)
                .workspace(workspaceId)
                .clientIp("192.168.1." + (100 + i))
                .detail("iteration", i)
                .info();
            bh.consume(tokenValid);
            bh.consume(sanitizedPath);
        }
    }
}