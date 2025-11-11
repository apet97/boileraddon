# From Zero to Running - Complete Setup Guide

**Get the Clockify Add-on Boilerplate running from a fresh system in under 15 minutes.**

This guide walks you through every step from prerequisites to running tests and deploying your first add-on. Perfect for first-time contributors or fresh development environments.

## 🚀 Express Setup (5 Minutes)

**Already have Java 17 and Maven installed? Skip to the quick start!**

```bash
# 1. Apply Java 17 configuration (if just added to ~/.zshrc)
source ~/.zshrc
java -version  # Verify shows Java 17

# 2. Run the quick start script
./scripts/quick-start.sh
```

The script will automatically:
- ✓ Verify Java 17 and Maven
- ✓ Build the entire project
- ✓ Let you choose which addon to run
- ✓ Detect ngrok if running
- ✓ Start the addon with sensible defaults

**Need more control?** Use the advanced script:

```bash
# Run Rules addon with all options
./scripts/setup-and-run.sh --addon rules --clean --use-ngrok

# Or just validate your environment
./scripts/setup-and-run.sh --validate-only
```

**See full script documentation:** [SETUP_SCRIPT_GUIDE.md](docs/SETUP_SCRIPT_GUIDE.md)

---

**Need to install Java/Maven first?** Continue with the detailed setup below.

---

## Table of Contents

- [Prerequisites Installation](#prerequisites-installation)
- [Project Setup](#project-setup)
- [Running Tests](#running-tests)
- [Local Development](#local-development)
- [Ngrok Integration](#ngrok-integration)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

---

## Prerequisites Installation

### 1. Java 17+ (Required)

The project requires Java 17 or higher (up to Java 25).

#### macOS (Homebrew)

```bash
# Install OpenJDK 17
brew install openjdk@17

# Link it (makes java command available)
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify installation
java -version
# Should show: openjdk version "17.x.x"
```

#### Linux (Ubuntu/Debian)

```bash
# Install OpenJDK 17
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
# Should show: openjdk version "17.x.x"
```

#### Windows

1. Download OpenJDK 17 from [Adoptium](https://adoptium.net/)
2. Run the installer
3. Set `JAVA_HOME` environment variable:
   - Open "Environment Variables" settings
   - Add `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`
   - Add `%JAVA_HOME%\bin` to `PATH`
4. Verify in Command Prompt:
   ```cmd
   java -version
   ```

### 2. Maven 3.6+ (Required)

#### macOS (Homebrew)

```bash
brew install maven

# Verify installation
mvn -version
# Should show Maven 3.9.x or higher
```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install maven

# Verify installation
mvn -version
```

#### Windows

1. Download Maven from [maven.apache.org](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Maven`
3. Add `C:\Program Files\Maven\bin` to `PATH`
4. Verify in Command Prompt:
   ```cmd
   mvn -version
   ```

### 3. Configure Maven Toolchains (Critical!)

Maven toolchains ensure consistent Java version usage across all builds and tests. **This step is crucial for test stability.**

Create `~/.m2/toolchains.xml` with the following content (adjust paths for your system):

#### macOS (Homebrew)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find your JDK path on macOS:**
```bash
/usr/libexec/java_home -V
# Or for Homebrew:
brew --prefix openjdk@17
```

#### Linux

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-17-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find your JDK path on Linux:**
```bash
update-alternatives --config java
# Or:
ls /usr/lib/jvm/
```

#### Windows

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-17.0.x</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Why toolchains?** Without toolchains, tests may use a different Java version than compilation, causing hard-to-debug failures like "Test JVM died unexpectedly" or Mockito compatibility issues.

See [docs/TOOLCHAINS_SETUP.md](docs/TOOLCHAINS_SETUP.md) for detailed troubleshooting.

### 4. Docker (Optional, but Recommended)

Docker enables database-backed tests and local PostgreSQL development.

#### macOS

```bash
# Install Docker Desktop
brew install --cask docker

# Open Docker Desktop app to start the daemon

# Verify installation
docker --version
docker ps  # Should not error
```

#### Linux

```bash
# Install Docker Engine
sudo apt update
sudo apt install docker.io docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add your user to docker group (avoid sudo)
sudo usermod -aG docker $USER
newgrp docker  # Or logout/login

# Verify installation
docker --version
docker ps
```

#### Windows

1. Download [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
2. Run installer (requires WSL 2)
3. Start Docker Desktop
4. Verify in PowerShell:
   ```powershell
   docker --version
   docker ps
   ```

**What does Docker enable?**
- `PooledDatabaseTokenStoreTest` (database resource management tests)
- Local PostgreSQL via `docker-compose.dev.yml`
- Production-like testing environment

**Without Docker:** All basic tests still work. Database tests are automatically skipped.

### 5. ngrok (Optional, for Clockify Integration)

ngrok creates public HTTPS URLs for local development, required for testing with Clockify webhooks.

#### All Platforms

```bash
# macOS
brew install ngrok

# Linux (download from website)
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# Windows (download from website)
# https://ngrok.com/download

# Verify installation
ngrok version
```

**Sign up for ngrok (free tier):**
1. Go to [ngrok.com](https://ngrok.com/)
2. Sign up for free account
3. Get your auth token from dashboard
4. Authenticate:
   ```bash
   ngrok config add-authtoken YOUR_TOKEN
   ```

---

## Project Setup

### 1. Clone the Repository

```bash
# Clone the repository
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# Or if you forked it:
git clone https://github.com/YOUR_USERNAME/boileraddon.git
cd boileraddon
```

### 2. First Build (Without Tests)

This validates your environment before running tests:

```bash
# Clean build without tests (downloads dependencies, ~5MB from Maven Central)
mvn clean package -DskipTests

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: ~30-60 seconds (first run)
```

**What this does:**
- Downloads Maven dependencies (~100MB total)
- Compiles all modules (SDK + 4 add-ons)
- Creates executable JARs in `addons/*/target/` directories
- Skips tests for faster initial build

**Success indicators:**
- `BUILD SUCCESS` message
- JARs created in `addons/*/target/` directories
- No compilation errors

**If build fails**, see [Troubleshooting](#troubleshooting) section.

### 3. Understand Project Structure

```
boileraddon-main/
├── addons/
│   ├── addon-sdk/              # Core SDK (all add-ons depend on this)
│   ├── _template-addon/        # Template for creating new add-ons
│   ├── auto-tag-assistant/     # Example: AI-powered tagging
│   ├── rules/                  # Example: IFTTT-style automation
│   └── overtime/               # Example: Overtime tracking
├── docs/                       # Comprehensive documentation
├── scripts/                    # Helper scripts (run-rules-local.sh, etc.)
├── tools/                      # Development tools (pre-commit hooks, validators)
├── Makefile                    # Convenient build/test/run targets
├── pom.xml                     # Root Maven configuration
└── FROM_ZERO_SETUP.md         # This file!
```

---

## Running Tests

### Basic Tests (No Docker Required)

Run all tests except Docker-dependent ones:

```bash
# Run all tests
mvn test

# Expected output:
# Tests run: 296, Failures: 0, Errors: 0, Skipped: 11
# (11 skipped = PooledDatabaseTokenStoreTest automatically skipped without Docker)
```

**Run tests for specific module:**

```bash
# Test just the SDK
mvn test -pl addons/addon-sdk

# Test just Rules add-on
mvn test -pl addons/rules

# Test multiple modules
mvn test -pl addons/addon-sdk,addons/rules
```

### With Docker (Integration Tests)

If Docker is running, database tests will run automatically:

```bash
# Ensure Docker is running
docker ps

# Run all tests (including database tests)
mvn verify

# Expected output:
# Tests run: 307, Failures: 0, Errors: 0, Skipped: 0
# (PooledDatabaseTokenStoreTest runs automatically with 11 additional tests)
```

**What happens with Docker tests?**
- Testcontainers automatically downloads PostgreSQL 16-alpine (~50MB, once)
- Starts ephemeral PostgreSQL container per test class
- Runs database tests
- Cleans up containers after tests complete

### Generate Coverage Reports

```bash
# Run tests with coverage
mvn verify

# View coverage report
open target/site/jacoco-aggregate/index.html  # macOS
xdg-open target/site/jacoco-aggregate/index.html  # Linux
start target/site/jacoco-aggregate/index.html  # Windows
```

**Current coverage:** ~83% line coverage, ~70% branch coverage

### Using the Makefile

The project includes convenient Makefile targets:

```bash
# Build everything
make build

# Run all tests
make test

# Run tests with coverage
make coverage

# Clean build artifacts
make clean

# See all available targets
make help
```

---

## Local Development

### 1. Environment Configuration

Some add-ons support environment-based configuration:

```bash
# Optional: Create .env file (not committed)
cp .env.example .env

# Edit .env with your values
# DB_URL=jdbc:postgresql://localhost:5432/addons
# DB_USERNAME=addons
# DB_PASSWORD=addons
```

### 2. Run PostgreSQL Locally (Optional)

For development with database-backed token storage:

```bash
# Start PostgreSQL via Docker Compose
docker compose -f docker-compose.dev.yml up -d

# Verify it's running
docker ps
# Should show postgres container on port 5432

# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons

# Stop when done
docker compose -f docker-compose.dev.yml down
```

### 3. Run Add-ons Locally

Each add-on can run standalone for local testing:

```bash
# Using Makefile (recommended)
make dev  # Runs template add-on on port 8080

# Or run specific add-on manually
java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# Or use dedicated scripts
./scripts/run-rules-local.sh --help
```

**What runs locally:**
- Jetty HTTP server (default port 8080)
- Manifest endpoint: `http://localhost:8080/rules/manifest.json`
- Settings UI: `http://localhost:8080/rules/settings`
- Health check: `http://localhost:8080/rules/health`

**Test the health endpoint:**

```bash
curl http://localhost:8080/rules/health

# Expected response:
# {
#   "status": "healthy",
#   "addon": "rules",
#   "version": "0.1.0"
# }
```

### 4. Install Pre-commit Hook (Optional)

Automated validation checks before each commit:

```bash
# Install pre-commit hook
cp tools/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# What it checks:
# - Secrets detection (blocks commits with passwords/tokens)
# - Manifest validation
# - Fast compilation
# - File size warnings
# - TODO/FIXME detection
```

**Skip checks when needed:**

```bash
# Skip pre-commit checks (not recommended)
git commit --no-verify

# Skip just compilation check
SKIP_COMPILE_CHECK=1 git commit
```

---

## Ngrok Integration

To test add-ons with Clockify webhooks, you need a public HTTPS URL.

### 1. Start ngrok

```bash
# Start ngrok tunnel (in a separate terminal)
ngrok http 8080

# Output will show:
# Forwarding: https://abc123.ngrok-free.app -> http://localhost:8080
```

**Copy the HTTPS URL** (e.g., `https://abc123.ngrok-free.app`)

### 2. Configure Add-on with Public URL

```bash
# Set base URL environment variable
export ADDON_BASE_URL=https://abc123.ngrok-free.app/rules

# Run the add-on
./scripts/run-rules-local.sh
```

The script will display:

```
========================================
  Clockify Rules Add-on - Local Setup
========================================

Configuration:
  Base URL:       https://abc123.ngrok-free.app/rules
  Port:           8080
  Apply Changes:  false

Endpoints:
  Manifest:       https://abc123.ngrok-free.app/rules/manifest.json
  Settings UI:    https://abc123.ngrok-free.app/rules/settings
  Health:         https://abc123.ngrok-free.app/rules/health
```

### 3. Install in Clockify

1. **Open Clockify** web app
2. Navigate to: **Admin → Add-ons → Install Custom Add-on**
3. Enter manifest URL: `https://abc123.ngrok-free.app/rules/manifest.json`
4. Review requested scopes
5. Click **Install**

### 4. Test the Integration

Create a time entry in Clockify and watch the add-on logs for webhook events:

```
[INFO] Received webhook: TIME_ENTRY_CREATED
[INFO] Processing rule: "Tag meeting entries"
[INFO] Would add tag: meetings (dry-run mode)
```

**Enable mutations (apply changes):**

```bash
# Stop the add-on (Ctrl+C)

# Restart with apply mode
./scripts/run-rules-local.sh --apply

# Now rules will actually modify time entries!
```

---

## Troubleshooting

### Java/Maven Issues

#### "Error: toolchain not found"

**Cause:** Maven can't find Java 17 in toolchains.xml

**Fix:**
1. Verify Java 17 is installed: `java -version`
2. Find JDK path:
   - macOS: `/usr/libexec/java_home -V`
   - Linux: `update-alternatives --config java`
   - Windows: `where java`
3. Update `~/.m2/toolchains.xml` with correct path
4. See [docs/TOOLCHAINS_SETUP.md](docs/TOOLCHAINS_SETUP.md)

#### "Test JVM died unexpectedly"

**Cause:** Test JVM using different Java version than compilation

**Fix:**
1. Ensure toolchains.xml is configured correctly
2. Run: `mvn clean test` (clean removes stale class files)
3. If still fails, check Maven is using Java 17: `mvn -version`

#### "Cannot access class file for java.lang.Object"

**Cause:** Mismatched Java versions or corrupted Maven cache

**Fix:**
```bash
# Clean Maven cache
rm -rf ~/.m2/repository/org/junit
rm -rf ~/.m2/repository/org/mockito

# Clean build
mvn clean install -DskipTests

# Try tests again
mvn test
```

### Docker/Testcontainers Issues

#### "Could not find a valid Docker environment"

**Cause:** Docker is not running or not accessible

**Fix:**
1. Start Docker Desktop (macOS/Windows) or Docker daemon (Linux)
2. Verify: `docker ps`
3. On Linux, ensure user is in docker group: `sudo usermod -aG docker $USER`
4. Tests will auto-skip if Docker unavailable (see test output: "Skipped: 11")

#### "Port 5432 already in use"

**Cause:** PostgreSQL already running on default port

**Fix:**
```bash
# Stop local PostgreSQL
sudo systemctl stop postgresql  # Linux
brew services stop postgresql   # macOS

# Or stop Docker Compose
docker compose -f docker-compose.dev.yml down
```

#### "Failed to pull image: postgres:16-alpine"

**Cause:** Docker can't download image (network issue)

**Fix:**
1. Check internet connection
2. Retry: `docker pull postgres:16-alpine`
3. Or use cached version if available

### Build Issues

#### "BUILD FAILURE: Failed to execute goal compile"

**Cause:** Compilation errors in code

**Fix:**
1. Check for syntax errors in recent changes
2. Run: `mvn clean compile` to see full error
3. Ensure you're on a working branch: `git status`

#### "BUILD FAILURE: spotbugs execution failed"

**Cause:** SpotBugs incompatible with Java 17

**Fix:** Already handled! SpotBugs is disabled in pom.xml. If you see this error:
1. Ensure you're using latest pom.xml: `git pull`
2. Verify property is set: `grep spotbugs.skip pom.xml`
3. Should show: `<spotbugs.skip>true</spotbugs.skip>`

### Runtime Issues

#### "Address already in use: bind"

**Cause:** Port 8080 already in use

**Fix:**
```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process
kill -9 PID  # macOS/Linux
taskkill /PID pid /F  # Windows

# Or use different port
./scripts/run-rules-local.sh --port 9090
```

#### "Webhook signature verification failed"

**Cause:** Clockify webhook signature doesn't match

**Fix:**
```bash
# For local development only, skip signature verification
./scripts/run-rules-local.sh --skip-signature

# WARNING: Never use --skip-signature in production
```

#### "Failed to connect to database"

**Cause:** PostgreSQL not running or wrong credentials

**Fix:**
1. Verify PostgreSQL is running: `docker ps`
2. Check environment variables:
   ```bash
   echo $DB_URL
   echo $DB_USERNAME
   echo $DB_PASSWORD
   ```
3. Test connection:
   ```bash
   psql $DB_URL -U $DB_USERNAME
   ```

### Getting Help

If you're still stuck:

1. **Check existing documentation:**
   - [TESTING_GUIDE.md](docs/TESTING_GUIDE.md)
   - [ARCHITECTURE.md](docs/ARCHITECTURE.md)
   - [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

2. **Search GitHub issues:**
   - [github.com/apet97/boileraddon/issues](https://github.com/apet97/boileraddon/issues)

3. **Open a new issue:**
   - Include: OS, Java version, Maven version, error message
   - Attach: Build logs, test output

---

## Next Steps

### Learn the Architecture

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Component overview
- **[docs/SDK_OVERVIEW.md](docs/SDK_OVERVIEW.md)** - SDK routing and middleware
- **[docs/MANIFEST_AND_LIFECYCLE.md](docs/MANIFEST_AND_LIFECYCLE.md)** - Add-on lifecycle

### Explore Example Add-ons

1. **auto-tag-assistant** - AI-powered tagging with OpenAI
   - Location: `addons/auto-tag-assistant/`
   - Features: OpenAI integration, tag suggestions, caching

2. **rules** - IFTTT-style automation engine
   - Location: `addons/rules/`
   - Quick start: [QUICKSTART_RULES.md](QUICKSTART_RULES.md)
   - Features: Conditions, actions, IFTTT builder UI

3. **overtime** - Overtime tracking and notifications
   - Location: `addons/overtime/`
   - Features: Time thresholds, notifications, reporting

### Create Your First Add-on

Use the template to scaffold a new add-on:

```bash
# Create new add-on
./scripts/new-addon.sh my-addon

# Build it
mvn clean package -pl addons/my-addon -am

# Run it
java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar
```

### Contribute Back

Read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:
- Code style
- Testing requirements
- Pull request process
- Documentation standards

---

## Summary Checklist

Before you start developing, ensure you have:

- [ ] Java 17+ installed (`java -version`)
- [ ] Maven 3.6+ installed (`mvn -version`)
- [ ] Toolchains configured (`~/.m2/toolchains.xml`)
- [ ] Project cloned and built (`mvn clean package -DskipTests`)
- [ ] Tests pass (`mvn test`)
- [ ] Docker installed (optional, for database tests)
- [ ] ngrok installed (optional, for Clockify integration)
- [ ] Pre-commit hook installed (optional)

**You're ready!** Start exploring the code, running add-ons, and building your own automations.

---

**Questions?** See [docs/](docs/) directory for comprehensive guides or open an issue on GitHub.
