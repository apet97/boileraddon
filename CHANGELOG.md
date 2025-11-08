# Changelog

All notable changes to the Clockify Addon Boilerplate project.

## [Unreleased] - 2024-11-08

### Added - Production Readiness Improvements

#### Security Enhancements
- **Path Sanitization** (`PathSanitizer.java`)
  - Prevents path traversal attacks (`../`)
  - Blocks null byte injection
  - Removes duplicate slashes (`//`)
  - Validates character whitelist
  - Applied to all endpoint registrations in `ClockifyAddon`

- **Rate Limiting Middleware** (`RateLimiter.java`)
  - Token bucket algorithm using Guava
  - Configurable limits per IP or workspace
  - Automatic cache cleanup (5min expiry)
  - 429 responses with `Retry-After` headers
  - Supports X-Forwarded-For for proxied requests

- **Input Validation Framework** (`ConfigValidator.java`)
  - Validates environment variables with helpful errors
  - Port validation (1-65535 range)
  - URL validation with malformed URL detection
  - Webhook secret strength validation (≥32 chars)
  - Boolean parsing (true/false/1/0/yes/no)
  - Fail-fast with detailed error messages

#### Storage & Persistence
- **Token Storage Interface** (`ITokenStore.java`)
  - Clean abstraction for pluggable storage backends
  - Tracks creation and last access timestamps
  - Consistent error handling with `StorageException`

- **In-Memory Implementation** (`InMemoryTokenStore.java`)
  - Thread-safe with `ConcurrentHashMap`
  - Development/testing suitable
  - Warns about non-production usage

- **Database Implementation** (`DatabaseTokenStore.java`)
  - PostgreSQL/MySQL compatible
  - Auto-initializes schema
  - Updates last accessed timestamp on reads
  - Connection pooling ready
  - Production-grade persistence

#### HTTP & Networking
- **Improved HTTP Client** (`ClockifyHttpClient.java`)
  - Connection timeout: 10s
  - Request timeout: 30s (configurable)
  - Exponential backoff retries (max 3 attempts)
  - Automatic 429 rate limit handling
  - Structured error responses with `HttpException`
  - GET/POST/PUT/DELETE helpers
  - Comprehensive logging

#### Error Handling
- **Standardized Error Responses** (`ErrorResponse.java`)
  - Consistent JSON error format
  - HTTP status codes
  - Error codes and timestamps
  - Helper methods: `validationError()`, `authenticationError()`, etc.
  - JSON serialization fallback

#### Observability
- **Health Check Endpoint** (`HealthCheck.java`)
  - `/health` endpoint with status checks
  - Memory and CPU metrics
  - Custom health check providers
  - JVM runtime information
  - Degraded status for partial failures

- **Structured Logging** (`logback.xml`)
  - SLF4J with Logback implementation
  - Console, JSON, and file appenders
  - Configurable via `LOG_LEVEL` and `LOG_APPENDER`
  - Rolling file policy (30 days, 1GB cap)
  - Async appender for performance

### Updated

#### Dependencies
- **Jetty**: 11.0.20 → 11.0.24 (security fixes)
- **Jackson**: 2.17.1 → 2.18.2 (latest stable)
- **SLF4J**: 2.0.13 → 2.0.16
- **Jakarta Servlet API**: 5.0.0 → 6.0.0
- **JUnit**: 5.10.2 → 5.11.3
- **Added Logback**: 1.5.12 (production logging)
- **Added Hibernate Validator**: 8.0.1.Final
- **Added Guava**: 33.3.1-jre (rate limiting)
- **Added Mockito**: 5.14.2 (testing)

#### CI/CD Pipeline
- **Enhanced GitHub Actions** (`.github/workflows/validate.yml`)
  - Separated `validate` and `build-and-test` jobs
  - Java 17 with Temurin distribution
  - Maven caching for faster builds
  - Runs full test suite
  - Uploads test results and coverage reports
  - Triggers on `claude/**` branches
  - Build verification for SDK and addons

### Testing
- **Added Comprehensive Tests**
  - `ConfigValidatorTest.java` - 12 test cases for input validation
  - `PathSanitizerTest.java` - 15 test cases for path security
  - Tests cover edge cases, error conditions, and security scenarios
  - Ready for integration tests framework

### Documentation
- **Production Deployment Guide** (`docs/PRODUCTION-DEPLOYMENT.md`)
  - Environment configuration
  - Security hardening checklist
  - Database setup (PostgreSQL/MySQL)
  - Docker deployment
  - Kubernetes deployment
  - systemd service configuration
  - Monitoring & observability setup
  - Scaling strategies
  - Troubleshooting guide
  - Pre-production checklist

### Fixed
- **Path Normalization Security** - `ClockifyAddon` now uses `PathSanitizer`
- **Missing Request Timeouts** - All HTTP clients now have timeouts
- **Generic Exception Handling** - Replaced with specific exception types
- **CI Build Disabled** - Re-enabled automated builds and tests
- **Missing Logging Implementation** - Added Logback configuration

---

## Previous Releases

See Git history for earlier changes.

---

## Upgrade Guide

### From Previous Versions

1. **Update Dependencies**
   ```bash
   mvn clean install
   ```

2. **Migrate Token Storage** (if using in-memory storage)
   ```java
   // Before
   import com.example.autotagassistant.TokenStore;
   TokenStore.save(workspaceId, token, baseUrl);

   // After - Use interface
   import com.clockify.addon.sdk.storage.ITokenStore;
   import com.clockify.addon.sdk.storage.DatabaseTokenStore;

   ITokenStore tokenStore = DatabaseTokenStore.fromEnvironment();
   tokenStore.save(workspaceId, token, baseUrl);
   ```

3. **Add Environment Variables**
   ```bash
   # Required for database storage
   DB_URL=jdbc:postgresql://localhost:5432/clockify_addons
   DB_USER=addon_user
   DB_PASSWORD=<secure-password>

   # Recommended
   LOG_LEVEL=INFO
   LOG_APPENDER=JSON
   ```

4. **Run Database Migration**
   ```sql
   -- See docs/PRODUCTION-DEPLOYMENT.md for schema
   ```

5. **Add Rate Limiting** (optional but recommended)
   ```java
   import com.clockify.addon.sdk.middleware.RateLimiter;

   RateLimiter rateLimiter = new RateLimiter(10.0, "ip");
   context.addFilter(new FilterHolder(rateLimiter), "/*",
       EnumSet.of(DispatcherType.REQUEST));
   ```

6. **Add Health Check**
   ```java
   import com.clockify.addon.sdk.health.HealthCheck;

   HealthCheck healthCheck = new HealthCheck("my-addon", "1.0.0");
   addon.registerCustomEndpoint("/health", healthCheck);
   ```

---

## Breaking Changes

### None

All changes are backward compatible. Existing addons will continue to work without modifications, but won't benefit from new features until updated.

---

## Deprecations

### None

No APIs have been deprecated in this release.

---

## Contributors

- Claude (Anthropic) - Comprehensive codebase analysis and improvements
- Original boilerplate authors

---

## License

See [LICENSE](LICENSE)
