# Quick Start Guide

This guide will get you up and running with the Clockify Add-on Boilerplate in minutes.

## Prerequisites

### Required Software
- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.6+**
- **Git**
- **PostgreSQL 15+** (for database-backed addons)

### Optional Tools
- **Docker & Docker Compose** (for containerized development)
- **ngrok** (for local testing with Clockify)
- **curl** (for API testing)

### Verify Installation

```bash
# Check Java version
java -version  # Should show 17 or higher

# Check Maven version
mvn -version   # Should show 3.6 or higher

# Check PostgreSQL (if using database)
psql --version
```

---

## Quick Setup (5 Minutes)

### Option 1: Using Existing Addon (Fastest)

Run the **Auto-Tag Assistant** addon:

```bash
# 1. Clone repository
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# 2. Build the addon
make build-auto-tag-assistant

# 3. Run the addon
make run-auto-tag-assistant

# 4. Verify it's running
curl http://localhost:8080/auto-tag-assistant/health
```

Expected output:
```json
{
  "name": "auto-tag-assistant",
  "version": "0.1.0",
  "status": "UP"
}
```

### Option 2: Create Your Own Addon

Use the scaffolding script to create a new addon:

```bash
# 1. Run the new addon script
./scripts/new-addon.sh

# Follow prompts:
# - Enter addon name: my-first-addon
# - Enter package name: com.example.myfirstaddon
# - Enter display name: My First Addon

# 2. Build your new addon
make build  # Builds all addons including yours

# 3. Run your addon
java -jar addons/my-first-addon/target/my-first-addon-0.1.0-jar-with-dependencies.jar

# 4. Test it
curl http://localhost:8080/my-first-addon/manifest.json
```

---

## Local Development with Clockify

To test your addon with Clockify, you need to expose it via public URL.

### Using ngrok

```bash
# 1. Install ngrok (if not already installed)
# Visit: https://ngrok.com/download

# 2. Start your addon locally
make run-auto-tag-assistant

# 3. In a new terminal, start ngrok
ngrok http 8080

# 4. Copy the ngrok URL (e.g., https://abc123.ngrok-free.app)

# 5. Restart addon with ngrok URL
ADDON_BASE_URL=https://abc123.ngrok-free.app/auto-tag-assistant make run-auto-tag-assistant
```

### Install in Clockify

1. Open Clockify workspace settings
2. Navigate to **Add-ons** section
3. Click **Install Custom Add-on**
4. Enter manifest URL: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`
5. Click **Install**

---

## Database Setup (Optional)

Many addons require database for persistent storage.

### Using Docker Compose (Recommended)

```bash
# 1. Start PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# 2. Verify database is running
docker compose -f docker-compose.dev.yml ps

# 3. Run migrations
mvn flyway:migrate -Pflyway

# 4. Configure addon to use database
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons
```

### Manual PostgreSQL Setup

```bash
# 1. Create database
createdb addons

# 2. Create user
psql -c "CREATE USER addons WITH PASSWORD 'addons';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE addons TO addons;"

# 3. Run migrations
mvn flyway:migrate -Pflyway \
  -Denv.DB_URL=jdbc:postgresql://localhost:5432/addons \
  -Denv.DB_USER=addons \
  -Denv.DB_PASSWORD=addons
```

---

## Environment Configuration

Create `.env` file in project root:

```bash
# Copy example environment file
cp .env.example .env

# Edit with your values
nano .env
```

Example `.env` file:

```bash
# Server configuration
ADDON_PORT=8080
ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant

# Database (optional, for database-backed addons)
DB_URL=jdbc:postgresql://localhost:5432/addons
DB_USERNAME=addons
DB_PASSWORD=addons

# Security (optional)
ADDON_WEBHOOK_SECRET=your-secret-key-here

# Development options
ADDON_REQUEST_LOGGING=true
ADDON_SKIP_SIGNATURE_VERIFY=true  # Only for local testing!
```

For Rules addon, use `.env.rules`:

```bash
cp .env.rules.example .env.rules
nano .env.rules
```

---

## Common Makefile Commands

The boilerplate includes a comprehensive Makefile with 40+ targets:

### Build Commands
```bash
make build                      # Build all modules
make build-rules                # Build rules addon only
make build-auto-tag-assistant   # Build auto-tag addon only
make clean                      # Clean all build artifacts
```

### Run Commands
```bash
make run-rules                  # Run rules addon
make run-auto-tag-assistant     # Run auto-tag addon
make dev                        # Run template with .env
```

### Test Commands
```bash
make test                       # Run all tests
make smoke                      # Run smoke tests (health/metrics)
make coverage                   # Generate coverage reports
```

### Validation Commands
```bash
make validate                   # Validate all manifests
make manifest-validate-runtime  # Validate running manifest
```

### Docker Commands
```bash
make docker-run                 # Build and run in Docker
TEMPLATE=rules make docker-run  # Run specific addon in Docker
```

---

## Project Structure Overview

```
boileraddon/
├── addons/
│   ├── addon-sdk/              # Core SDK (shared library)
│   ├── _template-addon/        # Starter template
│   ├── auto-tag-assistant/     # Example: Tag suggestions
│   ├── rules/                  # Example: Automation engine
│   └── overtime/               # Example: Overtime tracking
│
├── db/migrations/              # Database migrations
├── docs/                       # Comprehensive documentation
├── scripts/                    # Automation scripts
├── tools/                      # Development tools
│
├── .env.example                # Environment template
├── Makefile                    # Build automation
├── pom.xml                     # Parent Maven config
└── docker-compose.dev.yml      # Local PostgreSQL
```

---

## Next Steps

### 1. Explore Example Addons

Study the existing addons to understand patterns:

```bash
# Auto-Tag Assistant (simple)
cat addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java

# Rules Addon (complex with database)
cat addons/rules/src/main/java/com/example/rules/RulesApp.java
```

### 2. Read Documentation

- [Architecture Overview](./02-ARCHITECTURE.md) - Understand the system design
- [SDK Components](./03-SDK-COMPONENTS.md) - Learn SDK features
- [API Endpoints](./04-API-ENDPOINTS.md) - Explore endpoint patterns

### 3. Build Your Addon

Use the template or example addons as starting points:

```bash
# Copy template
cp -r addons/_template-addon addons/my-addon

# Edit pom.xml, update package names, etc.
# Build and run
make build
java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar
```

### 4. Test Locally

```bash
# Start ngrok
ngrok http 8080

# Update ADDON_BASE_URL and run
ADDON_BASE_URL=https://your-ngrok-url.app/my-addon make run-my-addon

# Install in Clockify workspace
```

### 5. Deploy to Production

See [Build & Deployment Guide](./09-BUILD-DEPLOYMENT.md) for production deployment strategies.

---

## Troubleshooting

### Issue: Port 8080 Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use different port
ADDON_PORT=9090 make run-auto-tag-assistant
```

### Issue: Database Connection Failed

```bash
# Check PostgreSQL is running
docker compose -f docker-compose.dev.yml ps

# Verify connection
psql -h localhost -U addons -d addons

# Check environment variables
echo $DB_URL
echo $DB_USERNAME
echo $DB_PASSWORD
```

### Issue: Manifest Validation Failed

```bash
# Validate manifest manually
python3 tools/validate-manifest.py addons/my-addon/src/main/resources/manifest.json

# Check runtime manifest
curl http://localhost:8080/my-addon/manifest.json | jq .
```

### Issue: Build Fails with Java Version Error

```bash
# Check Java version
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/jdk-17

# Verify Maven is using correct Java
mvn -version
```

---

## Useful Commands Cheat Sheet

```bash
# Quick build and run
mvn clean package -DskipTests && \
  java -jar addons/rules/target/rules-0.1.0-jar-with-dependencies.jar

# Build with tests and coverage
mvn clean verify

# Run specific addon with .env
make dev-rules

# Start database
docker compose -f docker-compose.dev.yml up -d

# Watch logs
tail -f logs/addon.log

# Validate everything
make validate && make test && make smoke
```

---

## Getting Help

- **Documentation:** [/docs/](/docs/)
- **AI Guide:** [/AI_README.md](/AI_README.md)
- **Examples:** [/examples/](/examples/)
- **Issues:** https://github.com/apet97/boileraddon/issues

---

**Next:** [Architecture Overview](./02-ARCHITECTURE.md)
