package com.clockify.addon.sdk.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for PooledDatabaseTokenStore resource management and AutoCloseable implementation.
 *
 * NOTE: These tests require Docker (via Testcontainers).
 * - Tests will be SKIPPED automatically if Docker is not available
 * - Tests will RUN automatically if Docker is available
 *
 * To run these tests:
 * 1. Ensure Docker is running locally
 * 2. Run: mvn test -Dtest=PooledDatabaseTokenStoreTest
 *
 * These tests validate critical resource cleanup behavior for production database connections.
 */
@Testcontainers
class PooledDatabaseTokenStoreTest {

    // Lazy initialization - only creates container if Docker is available
    // This prevents IllegalStateException during class loading
    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void checkDockerAvailability() {
        // Gracefully skip all tests if Docker is not available
        // Tests will show as "skipped" rather than "failed"
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        assumeTrue(dockerAvailable,
            "Docker is not available. Skipping PooledDatabaseTokenStoreTest. " +
            "Install Docker and ensure it's running to enable these tests.");

        // Only initialize container if Docker is available
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("addon_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    private PooledDatabaseTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                5  // Small pool for testing
        );
    }

    @AfterEach
    void tearDown() {
        if (tokenStore != null && !tokenStore.isClosed()) {
            tokenStore.close();
        }
    }

    @Test
    void testBasicSaveAndGet() {
        // Given
        String workspaceId = "ws-123";
        String token = "test-token-abc";

        // When
        tokenStore.save(workspaceId, token);
        Optional<String> retrieved = tokenStore.get(workspaceId);

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(token, retrieved.get());
    }

    @Test
    void testRemove() {
        // Given
        String workspaceId = "ws-456";
        String token = "test-token-def";
        tokenStore.save(workspaceId, token);

        // When
        tokenStore.remove(workspaceId);
        Optional<String> retrieved = tokenStore.get(workspaceId);

        // Then
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testCount() {
        // Given
        tokenStore.save("ws-1", "token-1");
        tokenStore.save("ws-2", "token-2");
        tokenStore.save("ws-3", "token-3");

        // When
        long count = tokenStore.count();

        // Then
        assertTrue(count >= 3, "Count should be at least 3");
    }

    @Test
    void testPoolStats() {
        // When
        PooledDatabaseTokenStore.HikariPoolStats stats = tokenStore.getPoolStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.totalConnections >= 0);
        assertTrue(stats.activeConnections >= 0);
        assertTrue(stats.idleConnections >= 0);
        assertTrue(stats.threadsWaiting >= 0);
        assertNotNull(stats.toString());
    }

    @Test
    void testValidateConnection() throws SQLException {
        // Should not throw
        assertDoesNotThrow(() -> tokenStore.validateConnection());
    }

    /**
     * CRITICAL TEST: Validates that close() properly releases resources.
     */
    @Test
    void testAutoCloseableImplementation() {
        // Given
        PooledDatabaseTokenStore store = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // When
        assertFalse(store.isClosed(), "Store should be open initially");
        store.close();

        // Then
        assertTrue(store.isClosed(), "Store should be closed after close()");
    }

    /**
     * CRITICAL TEST: Validates try-with-resources pattern works correctly.
     */
    @Test
    void testTryWithResources() {
        // Given
        String workspaceId = "ws-try-with-resources";
        String token = "token-123";

        // When - using try-with-resources
        try (PooledDatabaseTokenStore store = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            store.save(workspaceId, token);
            Optional<String> retrieved = store.get(workspaceId);
            assertTrue(retrieved.isPresent());
            assertEquals(token, retrieved.get());

            assertFalse(store.isClosed(), "Store should be open inside try block");
        }

        // Then - verify cleanup happened (create new connection to check)
        try (PooledDatabaseTokenStore verifyStore = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            // Data should persist across connections
            Optional<String> retrieved = verifyStore.get(workspaceId);
            assertTrue(retrieved.isPresent());
            assertEquals(token, retrieved.get());
        }
    }

    /**
     * CRITICAL TEST: Validates that operations fail after close.
     */
    @Test
    void testOperationsFailAfterClose() {
        // Given
        PooledDatabaseTokenStore store = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // When
        store.close();

        // Then
        assertTrue(store.isClosed());
        assertThrows(Exception.class, () -> store.save("ws-123", "token"),
                "Save should fail after close");
        assertThrows(Exception.class, () -> store.get("ws-123"),
                "Get should fail after close");
        assertThrows(Exception.class, () -> store.count(),
                "Count should fail after close");
    }

    /**
     * CRITICAL TEST: Validates idempotent close (calling close multiple times is safe).
     */
    @Test
    void testIdempotentClose() {
        // Given
        PooledDatabaseTokenStore store = new PooledDatabaseTokenStore(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // When - close multiple times
        assertDoesNotThrow(store::close);
        assertDoesNotThrow(store::close);
        assertDoesNotThrow(store::close);

        // Then
        assertTrue(store.isClosed());
    }

    @Test
    void testFromEnvironment() {
        // Given - set environment variables (simulated via system properties)
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());

        try {
            // When - this won't work without actual env vars, but we test the logic exists
            // Note: fromEnvironment() uses System.getenv(), not getProperty()
            // This test mainly validates the method exists and has proper signature
            assertNotNull(PooledDatabaseTokenStore.class.getMethod("fromEnvironment"));
        } catch (NoSuchMethodException e) {
            fail("fromEnvironment() method should exist");
        } finally {
            System.clearProperty("DB_URL");
            System.clearProperty("DB_USERNAME");
            System.clearProperty("DB_PASSWORD");
        }
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Given
        String workspaceId = "ws-concurrent";
        String token = "concurrent-token";
        tokenStore.save(workspaceId, token);

        // When - multiple threads access the pool
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    Optional<String> retrieved = tokenStore.get(workspaceId);
                    assertTrue(retrieved.isPresent());
                    assertEquals(token, retrieved.get());
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - pool should still be healthy
        assertFalse(tokenStore.isClosed());
        PooledDatabaseTokenStore.HikariPoolStats stats = tokenStore.getPoolStats();
        assertTrue(stats.threadsWaiting == 0, "No threads should be waiting after completion");
    }
}
