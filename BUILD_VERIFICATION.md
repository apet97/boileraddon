# Build Verification Guide

## ✅ Self-Contained Build Verification

This document verifies that the boilerplate can be built by any developer **without**:
- GitHub Packages authentication
- Private artifact repositories
- Hidden prerequisites
- External SDK dependencies

## Prerequisites

**Required (publicly available):**
- Java 17+ (`java -version`)
- Maven 3.x (`mvn -version`)
- Internet connection (to download from Maven Central on first build)

**Not Required:**
- ❌ GitHub Packages access token
- ❌ com.cake.clockify:addon-sdk from GitHub Packages
- ❌ com.cake.clockify:addon-sdk-annotation-processor
- ❌ Any proprietary or private artifacts

## Dependency Sources

All dependencies are resolved from **Maven Central ONLY**:

### auto-tag-assistant Dependencies

```xml
<!-- JSON processing -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.1</version>
</dependency>

<!-- Servlet API (Jetty 11 uses Jakarta EE) -->
<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
  <version>5.0.0</version>
</dependency>

<!-- Embedded HTTP server -->
<dependency>
  <groupId>org.eclipse.jetty</groupId>
  <artifactId>jetty-server</artifactId>
  <version>11.0.20</version>
</dependency>

<dependency>
  <groupId>org.eclipse.jetty</groupId>
  <artifactId>jetty-servlet</artifactId>
  <version>11.0.20</version>
</dependency>

<!-- Logging -->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>2.0.13</version>
</dependency>

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>2.0.13</version>
</dependency>

<!-- Testing -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.10.2</version>
  <scope>test</scope>
</dependency>
```

All of these artifacts are available on Maven Central without authentication.

## Inline SDK Architecture

Instead of depending on external `com.cake.clockify:addon-sdk`, this boilerplate now includes a **minimal inline SDK** directly in the `auto-tag-assistant` module:

```
addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/sdk/
├── ClockifyAddon.java          # Main addon coordinator
├── ClockifyManifest.java       # Manifest model with builder
├── AddonServlet.java           # HTTP servlet for routing
├── EmbeddedServer.java         # Jetty server wrapper
├── RequestHandler.java         # Request handler interface
└── HttpResponse.java           # Response helper
```

**Benefits:**
- No external dependencies
- No annotation processing complexity
- No GitHub Packages authentication
- Simple, readable code
- Easy to customize and extend

## Build Commands

### 1. Clean Build (from repo root)

```bash
mvn clean package -DskipTests
```

**Expected output:**
```
[INFO] Reactor Summary:
[INFO]
[INFO] Clockify Add-on Boilerplate ........................ SUCCESS
[INFO] java-basic-addon ................................... SUCCESS
[INFO] auto-tag-assistant ................................. SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**Artifacts produced:**
- `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0.jar`
- `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`

### 2. Build with Make

```bash
make build
```

### 3. Build Only auto-tag-assistant

```bash
mvn -f addons/auto-tag-assistant/pom.xml clean package -DskipTests
```

or

```bash
make build-auto-tag-assistant
```

## Runtime Verification

### Run the Add-on

```bash
ADDON_PORT=8080 ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant \
java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

or

```bash
make run-auto-tag-assistant
```

### Expected Startup Output

```
================================================================================
Auto-Tag Assistant Add-on Starting
================================================================================
Base URL: http://localhost:8080/auto-tag-assistant
Port: 8080

Endpoints:
  Manifest:  http://localhost:8080/auto-tag-assistant/manifest.json
  Settings:  http://localhost:8080/auto-tag-assistant/settings
  Lifecycle: http://localhost:8080/auto-tag-assistant/lifecycle
  Webhook:   http://localhost:8080/auto-tag-assistant/webhook
  Health:    http://localhost:8080/auto-tag-assistant/health
================================================================================
```

### Test Endpoints

```bash
# Health check
curl http://localhost:8080/auto-tag-assistant/health

# Manifest (should return valid JSON without $schema field)
curl http://localhost:8080/auto-tag-assistant/manifest.json

# Settings UI (returns HTML)
curl http://localhost:8080/auto-tag-assistant/settings
```

## Offline Build Verification

Since all dependencies are cached in `~/.m2/repository` after the first build, subsequent builds work **offline**:

```bash
mvn clean package -DskipTests -o  # -o = offline mode
```

## Troubleshooting

### "Could not resolve dependencies"

**Symptom:** Maven can't download dependencies

**Solution:**
1. Ensure you have internet connection
2. Check Maven Central is accessible: `curl -I https://repo.maven.apache.org/maven2/`
3. Clear Maven cache if corrupted: `rm -rf ~/.m2/repository`
4. Try again with `-U` flag: `mvn clean package -U -DskipTests`

### "Package jakarta.servlet does not exist"

**Symptom:** Compilation errors about missing Jakarta servlet classes

**Solution:**
- Ensure you're using Java 17+
- Check that `jakarta.servlet:jakarta.servlet-api:5.0.0` is in your POM
- Clear and rebuild: `mvn clean compile`

### Build succeeds but jar won't run

**Symptom:** `java.lang.NoClassDefFoundError` at runtime

**Solution:**
- Ensure you're running the **fat jar**: `auto-tag-assistant-0.1.0-jar-with-dependencies.jar`
- NOT the regular jar: `auto-tag-assistant-0.1.0.jar`

## What Changed from Previous Version?

**Before (required GitHub Packages):**
```xml
<dependency>
  <groupId>com.cake.clockify</groupId>
  <artifactId>addon-sdk</artifactId>
  <version>1.5.3</version>
  <!-- ❌ This artifact required GitHub Packages authentication -->
</dependency>
```

**After (self-contained):**
```xml
<!-- ✅ All from Maven Central -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.1</version>
</dependency>
<!-- ... rest from Maven Central ... -->
```

**SDK Location:**
- Before: `dev-docs-marketplace-cake-snapshot/extras/addon-java-sdk/` (required annotation processing, complex build)
- After: `addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/sdk/` (inline, simple)

## Verification Checklist

- [x] Java 17+ installed
- [x] Maven 3.x installed
- [x] Internet connection available (first build only)
- [x] No GitHub Packages authentication configured
- [x] `mvn clean package -DskipTests` succeeds
- [x] Fat jar produced: `addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`
- [x] Runtime startup succeeds
- [x] Health endpoint responds: `http://localhost:8080/auto-tag-assistant/health`
- [x] Manifest endpoint returns valid JSON: `http://localhost:8080/auto-tag-assistant/manifest.json`

## Summary

✅ **This boilerplate is now truly self-contained.**

Any developer can:
1. Clone the repo
2. Run `mvn clean package`
3. Get a working Clockify add-on

No secrets, no hidden auth, no proprietary artifacts.
