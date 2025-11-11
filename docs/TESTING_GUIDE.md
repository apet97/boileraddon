# Testing Infrastructure Guide

This comprehensive guide covers the testing infrastructure for the Clockify Addon SDK, including unit tests, contract tests, benchmarks, and testing patterns.

## Quick Start

### Run All Tests
```bash
mvn test -pl addons/addon-sdk
```

### Run Specific Test Class
```bash
mvn test -Dtest=ClockifyHttpClientTest -pl addons/addon-sdk
```

### Run with Coverage Report
```bash
mvn test -pl addons/addon-sdk
# Coverage report: target/site/jacoco/index.html
```

### Run JMH Benchmarks
```bash
mvn test -Dtest=WebhookSignatureBenchmark -pl addons/addon-sdk
mvn test -Dtest=JsonSerializationBenchmark -pl addons/addon-sdk
```

## Testing Architecture

### Layer 1: Unit Tests (Core Functionality)
Tests individual components in isolation with mocks.

- **HTTP Client Tests** (`ClockifyHttpClientTest.java`)
  - Request building, parameter handling
  - Retry logic with exponential backoff
  - Timeout and error scenarios
  - ~30 tests, 468 lines

- **Middleware Tests** (RequestSizeLimitFilterTest, HttpsEnforcementFilterTest, etc.)
  - Request filtering and validation
  - Security enforcement
  - CORS, CSRF, rate limiting
  - ~75 tests across multiple test files

- **Error Handler Tests** (`ErrorHandlerTest.java`)
  - Exception handling and masking
  - Sensitive data protection
  - Error ID generation
  - ~41 tests, 471 lines

### Layer 2: Contract Tests (API Compliance)
Tests that objects conform to expected schemas without external dependencies.

- **Webhook Event Contract** (`WebhookEventContractTest.java`)
  - Validates webhook payload structure
  - Tests all event types (TIME_ENTRY_*, TIMER_*)
  - Field presence, types, and formats
  - ~44 tests, 485 lines

- **Lifecycle Event Contract** (`LifecycleEventContractTest.java`)
  - INSTALLED and DELETED event schemas
  - Token field validation
  - Context object structure
  - ~29 tests, 298 lines

- **HTTP Response Contract** (`HttpResponseContractTest.java`)
  - Response format and immutability
  - Status codes and content types
  - Body content handling
  - ~62 tests, 420 lines

### Layer 3: Integration Tests (Component Interaction)
Tests that verify components work together.

- **Lifecycle Integration Test** (`LifecycleIntegrationTest.java`)
  - Token storage and retrieval
  - Addon installation/deletion flow
  - ~5 tests

- **Addon Servlet Test** (`AddonServletTest.java`)
  - Request routing
  - Handler invocation
  - Response serialization
  - ~8 tests with embedded server

### Layer 4: Benchmarks (Performance)
JMH benchmarks for performance-critical paths.

- **Webhook Signature Benchmark** (`WebhookSignatureBenchmark.java`)
  - HMAC-SHA256 validation performance
  - Large payload handling
  - Error path performance
  - ~6 benchmark methods

- **JSON Serialization Benchmark** (`JsonSerializationBenchmark.java`)
  - JSON parsing/serialization speed
  - Field extraction performance
  - Array iteration and filtering
  - ~10 benchmark methods

### Layer 5: Docker-Dependent Tests (Database Integration)
Tests that require Docker and Testcontainers for database operations.

- **PooledDatabaseTokenStoreTest** (`PooledDatabaseTokenStoreTest.java`)
  - Database connection pooling (HikariCP)
  - Token CRUD operations against real PostgreSQL
  - Resource management (AutoCloseable pattern)
  - Try-with-resources validation
  - Concurrent access patterns
  - Idempotent close behavior
  - ~11 tests, 274 lines

**Docker Availability Handling:**
- Tests automatically **skip** if Docker is not available
- Tests automatically **run** if Docker is detected
- Uses JUnit 5 `Assumptions` for graceful skipping
- No manual intervention required

**Running Docker Tests:**
```bash
# Ensure Docker is running
docker ps

# Run all tests (Docker tests included automatically)
mvn test -pl addons/addon-sdk

# Run only Docker-dependent tests
mvn test -Dtest=PooledDatabaseTokenStoreTest -pl addons/addon-sdk

# Run without Docker (tests will be skipped)
# Just stop Docker - tests handle it gracefully
```

**Expected Output (Docker Available):**
```
[INFO] Tests run: 307, Failures: 0, Errors: 0, Skipped: 0
```

**Expected Output (Docker NOT Available):**
```
[INFO] Tests run: 296, Failures: 0, Errors: 0, Skipped: 11
[INFO] Skipped: PooledDatabaseTokenStoreTest (Docker not available)
```

**What Testcontainers Does:**
1. Detects Docker environment availability
2. Downloads PostgreSQL 16-alpine image (~50MB, once)
3. Starts ephemeral PostgreSQL container for tests
4. Runs Flyway migrations automatically
5. Executes database tests
6. Stops and removes container after tests

**Troubleshooting Docker Tests:**

| Issue | Cause | Solution |
|-------|-------|----------|
| Tests skipped | Docker not running | Start Docker: `docker ps` should work |
| "Port 5432 in use" | PostgreSQL already running | Stop local PostgreSQL or use different port |
| "Failed to pull image" | Network issue | Pre-pull: `docker pull postgres:16-alpine` |
| Tests timeout | Slow Docker | Increase timeout in test or check Docker resources |

**See also:** [FROM_ZERO_SETUP.md](../FROM_ZERO_SETUP.md#docker-optional-but-recommended) for Docker installation guide.

## Test Utilities

### Test Data Builders (Fluent API)
Located in `com.clockify.addon.sdk.testing.builders`:

#### TimeEntryBuilder
```java
TimeEntryBuilder.create()
    .withId("entry-123")
    .withDescription("Implementation task")
    .withDuration(3600)        // seconds
    .withBillable(true)
    .withTag("development")
    .build()
```

#### WebhookEventBuilder
```java
WebhookEventBuilder.create()
    .eventType("TIME_ENTRY_CREATED")
    .workspaceId("ws-123")
    .workspaceName("My Workspace")
    .userId("user-456")
    .userName("John Doe")
    .userEmail("john@example.com")
    .withTimeEntry(TimeEntryBuilder.create()
        .withId("entry-789")
        .withDescription("Testing")
        .withDuration(3600)
        .build())
    .build()
```

#### ManifestBuilder
```java
ManifestBuilder.create()
    .key("my-addon")
    .name("My Addon")
    .baseUrl("http://localhost:8080")
    .withScope("TIME_ENTRY_READ")
    .withWebhook("TIME_ENTRY_CREATED", "/webhook")
    .build()
```

## Test Statistics

### Test Count
- **Total tests**: 296
- **Unit tests**: 180+
- **Contract tests**: 95
- **Integration tests**: 20+

### Coverage
- **Line coverage**: >80%
- **Branch coverage**: >75%
- **Critical paths**: 100% coverage

### Performance
- **Total test suite**: ~30 seconds
- **Unit tests only**: ~15 seconds
- **Single test**: <1 second (median)

## Writing New Tests

### Unit Test Pattern
```java
class MyComponentTest {
    @BeforeEach
    void setup() {
        // Initialize component with mocks
    }

    @Test
    void happyPath_succeeds() {
        // Act
        Result result = component.process(input);
        // Assert
        assertEquals(expected, result);
    }
}
```

### Contract Test Pattern
```java
class MyPayloadContractTest {
    @Test
    void payload_hasRequiredFields() throws Exception {
        ObjectNode payload = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED;
        assertTrue(payload.has("workspaceId"));
        assertNotNull(payload.get("timeEntry"));
    }
}
```

## Testing Best Practices

1. **Test Organization**: One assertion concept per test
2. **Descriptive Names**: Use `test_{condition}_{expected_outcome}` pattern
3. **Test Structure**: Arrange-Act-Assert pattern
4. **Data Management**: Use test builders, not hardcoded data
5. **Isolation**: Mock external dependencies
6. **Timeout Protection**: Use `@Timeout` for server tests

## Common Testing Tasks

### Run Tests
```bash
# All tests
mvn test -pl addons/addon-sdk

# Single test class
mvn test -Dtest=WebhookEventContractTest -pl addons/addon-sdk

# Single test method
mvn test -Dtest=WebhookEventContractTest#timeEntryCreatedEvent_hasRequiredFields -pl addons/addon-sdk
```

### Add New Webhook Type
1. Add test fixture in `TestFixtures.java`
2. Add contract test in `WebhookEventContractTest.java`
3. Update webhook handler tests

### Add New Benchmark
1. Create `*Benchmark.java` in `benchmarks` package
2. Add `@Setup` method for initialization
3. Add benchmarks for happy path and error paths

## Troubleshooting

### Test Timeout
- Increase timeout: `@Timeout(value = 10, unit = TimeUnit.SECONDS)`
- Check server startup: Verify port availability

### Flaky Tests
- Add appropriate waits if needed
- Check for race conditions in TokenStore/cache
- Mock external services

### Build Failures
- Clean and rebuild: `mvn clean test`
- Requires Java 17+
- Some tests need >1GB RAM

## Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core)
- [JMH Benchmarking Guide](https://github.com/openjdk/jmh)
