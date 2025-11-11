# Setup Script Guide

Complete guide for using the Clockify Add-on setup and run scripts.

## Overview

Two scripts are provided for maximum convenience:

| Script | Purpose | Best For |
|--------|---------|----------|
| **quick-start.sh** | Zero-config quick start | First-time users, demos |
| **setup-and-run.sh** | Full-featured launcher | Development, production testing |

Both scripts automatically handle Java 17 configuration, so you don't have to worry about Java versions.

---

## Quick Start Script

### Basic Usage

```bash
./scripts/quick-start.sh
```

That's it! The script will:
1. Check and fix Java version to 17
2. Verify Maven is installed
3. Build the entire project
4. Ask which addon you want to run
5. Detect if ngrok is running
6. Start the addon with sensible defaults

### What You'll See

```
   ____ _            _    _  __
  / ___| | ___   ___| | _(_)/ _|_   _
 | |   | |/ _ \ / __| |/ / | |_| | | |
 | |___| | (_) | (__|   <| |  _| |_| |
  \____|_|\___/ \___|_|\_\_|_|  \__, |
                                |___/
  Add-on Quick Start

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Environment Check
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ℹ Checking Java version...
✓ Java 17 detected: 17.0.17

ℹ Checking Maven...
✓ Maven found: 3.9.11

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Building Project
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ℹ This will take about 1-2 minutes on first run...

✓ Build successful!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Choose Add-on to Run
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Available add-ons:

  1) rules              - IFTTT-style automation engine
  2) auto-tag-assistant - AI-powered time entry tagging
  3) overtime           - Overtime tracking and notifications

Enter number (1-3) [1]:
```

### When to Use

- **First time** trying the project
- **Quick demos** to stakeholders
- **Testing** after fresh git clone
- When you want **zero configuration**

---

## Setup-and-Run Script

### Basic Usage

```bash
# Run Rules addon with defaults
./scripts/setup-and-run.sh --addon rules

# Build from scratch and use ngrok
./scripts/setup-and-run.sh --addon rules --clean --use-ngrok

# Run with all options
./scripts/setup-and-run.sh \
  --addon auto-tag-assistant \
  --port 9090 \
  --clean \
  --use-ngrok \
  --apply \
  --db
```

### All Options

#### Required Options

| Option | Description | Example |
|--------|-------------|---------|
| `--addon <name>` | Which addon to run | `--addon rules` |

Available addons: `rules`, `auto-tag-assistant`, `overtime`

#### Build Options

| Option | Description | Default |
|--------|-------------|---------|
| `--clean` | Clean build (mvn clean) | false |
| `--skip-build` | Skip build, use existing JAR | false |

#### Network Options

| Option | Description | Example |
|--------|-------------|---------|
| `--port <port>` | Port to run on | `--port 8080` (default) |
| `--base-url <url>` | Explicit base URL | `--base-url https://my-domain.com/rules` |
| `--use-ngrok` | Auto-detect ngrok URL | `--use-ngrok` |

#### Runtime Options

| Option | Description | Use Case |
|--------|-------------|----------|
| `--apply` | Enable mutations | When you want rules to actually modify data |
| `--skip-signature` | Skip webhook signature verification | Dev only, testing webhooks |
| `--db` | Use database storage | Production-like testing |
| `--env-file <file>` | Load environment variables | `--env-file .env.prod` |

#### Utility Options

| Option | Description | Use |
|--------|-------------|-----|
| `--validate-only` | Only check environment | Pre-flight check |
| `--help` | Show help message | Documentation |

### Common Workflows

#### First Time Setup

```bash
# Clean build and run Rules addon
./scripts/setup-and-run.sh --addon rules --clean
```

#### Development with ngrok

```bash
# In terminal 1: Start ngrok
ngrok http 8080

# In terminal 2: Run addon with ngrok
./scripts/setup-and-run.sh --addon rules --use-ngrok
```

#### Testing with Apply Mode

```bash
# WARNING: This will actually modify Clockify data!
./scripts/setup-and-run.sh --addon rules --apply --use-ngrok
```

#### Production-like Environment

```bash
# Set up database first
docker compose -f docker-compose.dev.yml up -d

# Run with database backend
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons

./scripts/setup-and-run.sh --addon rules --db --use-ngrok
```

#### Quick Rebuild

```bash
# Rebuild just the addon (faster)
./scripts/setup-and-run.sh --addon rules
```

#### Environment Validation

```bash
# Just check if everything is set up correctly
./scripts/setup-and-run.sh --validate-only
```

Output:
```
✓ Java 17 detected
✓ Maven found
✓ Maven using Java 17
✓ Maven toolchains.xml configured
✓ Environment validation complete!
```

### Script Output

The script provides detailed feedback:

```
========================================
  Clockify Add-on Setup & Run
========================================

ℹ Step 1: Validating Java 17 environment...

✓ Java 17 detected: 17.0.17

ℹ Step 2: Validating Maven...

✓ Maven found: Apache Maven 3.9.11
✓ Maven using Java 17: 17.0.17

ℹ Step 3: Checking Maven toolchains...

✓ Maven toolchains.xml configured for Java 17

✓ Addon selected: rules

ℹ Step 5: Building project...

✓ Build successful

ℹ Step 6: Locating addon JAR...

✓ Found JAR: addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

ℹ Step 7: Configuring addon URL...

✓ Detected ngrok URL: https://abc123.ngrok-free.app
✓ Detected ngrok URL: https://abc123.ngrok-free.app

========================================
  Configuration Summary
========================================

Addon:          rules
Port:           8080
Base URL:       https://abc123.ngrok-free.app/rules
Apply Changes:  false
Skip Signature: false
Use Database:   false

Endpoints:
  Manifest:       https://abc123.ngrok-free.app/rules/manifest.json
  Health:         https://abc123.ngrok-free.app/rules/health
  Status:         https://abc123.ngrok-free.app/rules/status?workspaceId=<ws>
  Settings UI:    https://abc123.ngrok-free.app/rules/settings
  IFTTT UI:       https://abc123.ngrok-free.app/rules/ifttt

To install in Clockify:
  1. Go to: Admin → Add-ons → Install Custom Add-on
  2. Enter manifest URL: https://abc123.ngrok-free.app/rules/manifest.json
  3. Approve scopes and install

========================================
  Starting rules addon...
========================================
```

---

## Java 17 Handling

Both scripts automatically detect and fix Java version issues.

### How It Works

1. **Check current Java version** with `java -version`
2. **If not Java 17:**
   - Look for Java 17 at `/opt/homebrew/opt/openjdk@17`
   - Export `JAVA_HOME` and update `PATH`
   - Verify the switch worked
3. **If Java 17 not found:**
   - Provide installation instructions
   - Exit with error

### Permanent Fix

For permanent Java 17 configuration, the scripts have already added to your `~/.zshrc`:

```bash
# Java 17 for Clockify Add-on Development
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

**Apply the changes:**

```bash
source ~/.zshrc
java -version  # Should now show Java 17
```

After this, you won't need the auto-fix anymore—Java 17 will be your default!

---

## Ngrok Integration

### Setup

```bash
# Install ngrok
brew install ngrok

# Sign up at ngrok.com and get auth token
ngrok config add-authtoken YOUR_TOKEN

# Start ngrok
ngrok http 8080
```

### Auto-Detection

When you use `--use-ngrok`, the script:

1. Checks if `ngrok` command exists
2. Checks if ngrok process is running
3. Queries ngrok API at `http://localhost:4040/api/tunnels`
4. Extracts the HTTPS public URL
5. Constructs addon base URL: `https://abc123.ngrok-free.app/rules`

### Manual URL

If auto-detection fails or you want to use a custom domain:

```bash
./scripts/setup-and-run.sh --addon rules --base-url https://my-domain.com/rules
```

---

## Database Mode

### Prerequisites

```bash
# Start PostgreSQL via Docker
docker compose -f docker-compose.dev.yml up -d

# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons
```

### Run with Database

```bash
./scripts/setup-and-run.sh --addon rules --db
```

The script will:
- Check if `DB_URL` is set
- Pass it through to the addon
- Addon will use database-backed token storage instead of in-memory

---

## Troubleshooting

### "Java 17 not found"

**Error:**
```
✗ Java 17 not found at /opt/homebrew/opt/openjdk@17
Install with: brew install openjdk@17
```

**Solution:**
```bash
brew install openjdk@17
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

### "Maven not found"

**Error:**
```
✗ Maven not found. Install with: brew install maven
```

**Solution:**
```bash
brew install maven
mvn -version  # Verify installation
```

### "JAR not found"

**Error:**
```
✗ JAR not found in addons/rules/target/
Try running with --clean to rebuild
```

**Solution:**
```bash
./scripts/setup-and-run.sh --addon rules --clean
```

### "ngrok is not running"

**Error:**
```
✗ ngrok is not running. Start with: ngrok http 8080
```

**Solution:**
```bash
# In a separate terminal
ngrok http 8080

# Then run script with --use-ngrok
./scripts/setup-and-run.sh --addon rules --use-ngrok
```

### "Build failed"

**Symptoms:**
```
✗ Build failed
```

**Debug steps:**

1. **Check Java version:**
   ```bash
   java -version  # Should show 17.x
   mvn -version   # Should show Java 17
   ```

2. **Clean and retry:**
   ```bash
   ./scripts/setup-and-run.sh --addon rules --clean
   ```

3. **Manual build for detailed error:**
   ```bash
   cd /path/to/boileraddon
   mvn clean install -pl addons/addon-sdk -am
   mvn clean package -pl addons/rules -am
   ```

4. **Check disk space:**
   ```bash
   df -h .
   ```

### Port Already in Use

**Error:** When addon starts, you see:
```
Address already in use: bind
```

**Solution:**

```bash
# Find process on port 8080
lsof -i :8080

# Kill it
kill -9 PID

# Or use different port
./scripts/setup-and-run.sh --addon rules --port 9090
```

---

## Advanced Usage

### Custom Environment Files

Create environment-specific configurations:

**`.env.dev`:**
```bash
RULES_APPLY_CHANGES=false
ADDON_SKIP_SIGNATURE_VERIFY=true
LOG_LEVEL=DEBUG
```

**`.env.prod`:**
```bash
RULES_APPLY_CHANGES=true
ADDON_SKIP_SIGNATURE_VERIFY=false
LOG_LEVEL=INFO
DB_URL=jdbc:postgresql://prod-db:5432/addons
DB_USERNAME=addons_prod
DB_PASSWORD=<secure-password>
```

**Usage:**
```bash
./scripts/setup-and-run.sh --addon rules --env-file .env.dev
```

### CI/CD Integration

```bash
#!/bin/bash
# deploy.sh - Example deployment script

# Build
./scripts/setup-and-run.sh --addon rules --clean --skip-build

# Deploy JAR
scp addons/rules/target/rules-*.jar user@server:/opt/addons/

# Run on server
ssh user@server "java -jar /opt/addons/rules-*.jar"
```

### Multiple Addons Simultaneously

```bash
# Terminal 1: Rules on port 8080
./scripts/setup-and-run.sh --addon rules --port 8080

# Terminal 2: Auto-tag on port 8081
./scripts/setup-and-run.sh --addon auto-tag-assistant --port 8081 --skip-build

# Terminal 3: Overtime on port 8082
./scripts/setup-and-run.sh --addon overtime --port 8082 --skip-build
```

---

## Script Source Code

Both scripts are located in the `scripts/` directory:

- **scripts/quick-start.sh** - ~150 lines, interactive
- **scripts/setup-and-run.sh** - ~300 lines, full-featured

Feel free to customize them for your workflow!

---

## Summary

**For first-time users:**
```bash
./scripts/quick-start.sh
```

**For development:**
```bash
./scripts/setup-and-run.sh --addon rules --use-ngrok
```

**For testing with database:**
```bash
./scripts/setup-and-run.sh --addon rules --db --apply
```

**For just validation:**
```bash
./scripts/setup-and-run.sh --validate-only
```

---

## See Also

- [FROM_ZERO_SETUP.md](../FROM_ZERO_SETUP.md) - Complete environment setup
- [TOOLCHAINS_SETUP.md](TOOLCHAINS_SETUP.md) - Maven toolchains configuration
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Testing infrastructure
- [PRODUCTION-DEPLOYMENT.md](PRODUCTION-DEPLOYMENT.md) - Production deployment

---

**Questions?** Check the troubleshooting section or open an issue on GitHub.
