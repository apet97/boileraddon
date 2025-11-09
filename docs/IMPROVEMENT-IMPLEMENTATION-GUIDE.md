# Improvement Implementation Guide

**Version**: 1.0.0
**Date**: 2025-11-09
**Target Audience**: Developers implementing improvements

---

## Purpose

This guide provides step-by-step instructions for implementing the improvements outlined in the [Boilerplate Improvement Roadmap](BOILERPLATE-IMPROVEMENT-ROADMAP.md). Each section includes code examples, testing strategies, and acceptance criteria.

---

## Table of Contents

1. [Setup & Prerequisites](#setup--prerequisites)
2. [Phase 1: Quick Wins](#phase-1-quick-wins)
3. [Phase 2: Core Improvements](#phase-2-core-improvements)
4. [Phase 3: Advanced Features](#phase-3-advanced-features)
5. [Testing Guidelines](#testing-guidelines)
6. [Deployment Checklist](#deployment-checklist)

---

## Setup & Prerequisites

### Development Environment

**Required Tools**:
```bash
# Java 17+
java -version

# Maven 3.6+
mvn -version

# Python 3.8+ (for scripts)
python3 --version

# Docker (for integration tests)
docker --version

# Git
git --version
```

**Clone and Setup**:
```bash
# Clone repository
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# Create feature branch
git checkout -b improvements/phase-1

# Build project
mvn clean install

# Run tests
mvn test
```

**IDE Setup** (IntelliJ IDEA):
```
1. File → Open → Select boileraddon/pom.xml
2. Import as Maven project
3. Set SDK to Java 17
4. Enable annotation processing
5. Install plugins: Lombok, CheckStyle
```

---

## Phase 1: Quick Wins

### 1.1 Interactive Addon Wizard

**Objective**: Create Python-based interactive wizard for addon creation

**Implementation Steps**:

#### Step 1: Install Dependencies
```bash
# Create virtual environment
python3 -m venv scripts/venv
source scripts/venv/bin/activate

# Install dependencies
pip install questionary rich click
```

#### Step 2: Create Wizard Script
```bash
# Create new file: scripts/create-addon-interactive.py
touch scripts/create-addon-interactive.py
chmod +x scripts/create-addon-interactive.py
```

#### Step 3: Implement Wizard
```python
#!/usr/bin/env python3
"""
Interactive addon creation wizard.
Usage: ./scripts/create-addon-interactive.py
"""

import questionary
from rich.console import Console
from rich.table import Table
import re
import subprocess
import sys

console = Console()

def validate_addon_name(name):
    """Validate addon name format"""
    if not re.match(r'^[a-z][a-z0-9-]*$', name):
        return "Name must start with lowercase letter, use only lowercase, numbers, and hyphens"
    if len(name) < 3:
        return "Name must be at least 3 characters"
    if len(name) > 50:
        return "Name must be less than 50 characters"
    return True

def detect_ngrok_url():
    """Try to detect running ngrok tunnel"""
    try:
        result = subprocess.run(
            ['curl', '-s', 'http://localhost:4040/api/tunnels'],
            capture_output=True,
            text=True,
            timeout=2
        )
        if result.returncode == 0:
            import json
            data = json.loads(result.stdout)
            if data.get('tunnels'):
                url = data['tunnels'][0]['public_url']
                console.print(f"[green]✓[/green] Detected ngrok: {url}")
                return url
    except:
        pass
    return None

def select_template():
    """Let user choose addon template"""
    templates = {
        'minimal': {
            'name': 'Minimal (manifest + health only)',
            'description': 'Bare minimum addon with just manifest and health endpoints',
            'files': ['manifest.json', 'TemplateAddonApp.java', 'HealthController.java']
        },
        'webhook': {
            'name': 'Webhook Processor (event-driven)',
            'description': 'Addon focused on processing Clockify webhook events',
            'files': ['+ WebhookHandler.java', '+ EventProcessor.java']
        },
        'settings': {
            'name': 'Settings UI (configuration)',
            'description': 'Addon with settings page for user configuration',
            'files': ['+ SettingsController.java', '+ settings.html', '+ styles.css']
        },
        'full': {
            'name': 'Full-Featured (all capabilities)',
            'description': 'Complete addon with webhooks, settings, and API endpoints',
            'files': ['All files from _template-addon']
        }
    }

    choices = [
        questionary.Choice(
            title=f"{info['name']}\n  {info['description']}",
            value=key
        )
        for key, info in templates.items()
    ]

    return questionary.select(
        "Choose addon template:",
        choices=choices
    ).ask()

def create_wizard():
    """Main wizard flow"""
    console.print("\n[bold cyan]Clockify Addon Creation Wizard[/bold cyan]\n")

    # Template selection
    template = select_template()

    # Addon name
    addon_name = questionary.text(
        "Addon name (lowercase, hyphens):",
        validate=validate_addon_name
    ).ask()

    if not addon_name:
        console.print("[red]Cancelled[/red]")
        sys.exit(1)

    # Display name
    default_display = addon_name.replace('-', ' ').title()
    display_name = questionary.text(
        "Display name:",
        default=default_display
    ).ask()

    # Base URL
    ngrok_url = detect_ngrok_url()
    default_base_url = (
        f"{ngrok_url}/{addon_name}" if ngrok_url
        else f"http://localhost:8080/{addon_name}"
    )

    base_url = questionary.text(
        "Base URL:",
        default=default_base_url
    ).ask()

    # Port
    port = questionary.text(
        "Port:",
        default="8080"
    ).ask()

    # Preview
    console.print("\n[bold]Configuration Preview:[/bold]")
    table = Table(show_header=False)
    table.add_row("Template", template)
    table.add_row("Addon Name", addon_name)
    table.add_row("Display Name", display_name)
    table.add_row("Base URL", base_url)
    table.add_row("Port", port)
    console.print(table)

    # Confirm
    if not questionary.confirm("\nCreate addon?", default=True).ask():
        console.print("[yellow]Cancelled[/yellow]")
        sys.exit(0)

    # Create addon using existing script
    console.print("\n[bold]Creating addon...[/bold]")

    cmd = [
        './scripts/new-addon.sh',
        '--port', port,
        addon_name,
        display_name
    ]

    try:
        result = subprocess.run(cmd, check=True)
        if result.returncode == 0:
            console.print(f"\n[green]✓ Addon created successfully![/green]")
            print_next_steps(addon_name, base_url, port)
    except subprocess.CalledProcessError as e:
        console.print(f"\n[red]✗ Failed to create addon: {e}[/red]")
        sys.exit(1)

def print_next_steps(addon_name, base_url, port):
    """Print next steps after creation"""
    console.print("\n[bold cyan]Next Steps:[/bold cyan]\n")

    steps = f"""
1. Navigate to addon directory:
   [yellow]cd addons/{addon_name}[/yellow]

2. Review generated files:
   [yellow]ls -la src/main/java/[/yellow]

3. Start development server:
   [yellow]make run-{addon_name}[/yellow]

4. Verify endpoints:
   • Health: [blue]http://localhost:{port}/{addon_name}/health[/blue]
   • Manifest: [blue]http://localhost:{port}/{addon_name}/manifest.json[/blue]

5. Install in Clockify:
   • Admin → Add-ons → Custom Add-on
   • Manifest URL: [blue]{base_url}/manifest.json[/blue]

6. Start coding!
   • Edit: [yellow]src/main/java/com/example/{addon_name.replace('-', '')}/*[/yellow]
   • Test: [yellow]src/test/java/[/yellow]
"""

    console.print(steps)

if __name__ == '__main__':
    try:
        create_wizard()
    except KeyboardInterrupt:
        console.print("\n[yellow]Cancelled by user[/yellow]")
        sys.exit(130)
```

#### Step 4: Test the Wizard
```bash
# Run wizard
./scripts/create-addon-interactive.py

# Follow prompts and verify addon creation
```

#### Step 5: Add to Makefile
```makefile
# Add to Makefile
.PHONY: create-addon-interactive
create-addon-interactive:
	@./scripts/create-addon-interactive.py
```

**Acceptance Criteria**:
- [ ] Wizard validates addon name format
- [ ] Auto-detects ngrok if running
- [ ] Shows preview before creation
- [ ] Creates addon successfully
- [ ] Prints next steps
- [ ] Total time < 2 minutes

---

### 1.2 Dev Environment Setup Script

**Objective**: One-command setup of development environment

**Implementation Steps**:

#### Step 1: Create Setup Script
```bash
#!/bin/bash
# scripts/dev-setup.sh
set -e

ADDON_NAME="${1:-}"
if [ -z "$ADDON_NAME" ]; then
  echo "Usage: ./scripts/dev-setup.sh <addon-name>"
  exit 1
fi

echo "🚀 Setting up dev environment for $ADDON_NAME..."

# 1. Check if addon exists
if [ ! -d "addons/$ADDON_NAME" ]; then
  echo "❌ Addon not found: addons/$ADDON_NAME"
  exit 1
fi

# 2. Create .env file
cat > "addons/$ADDON_NAME/.env" <<EOF
# Development environment configuration
ADDON_PORT=8080
ADDON_BASE_URL=http://localhost:8080/$ADDON_NAME
DB_URL=jdbc:postgresql://localhost:5432/${ADDON_NAME}_dev
DB_USERNAME=dev
DB_PASSWORD=dev
ADDON_REQUEST_LOGGING=true
ADDON_WEBHOOK_SECRET=dev-secret-change-in-production
EOF

echo "✓ Created .env file"

# 3. Create docker-compose.dev.yml
cat > "addons/$ADDON_NAME/docker-compose.dev.yml" <<EOF
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ${ADDON_NAME}_postgres
    environment:
      POSTGRES_DB: ${ADDON_NAME}_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
EOF

echo "✓ Created docker-compose.dev.yml"

# 4. Start PostgreSQL
cd "addons/$ADDON_NAME"
docker-compose -f docker-compose.dev.yml up -d

echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 5

# 5. Run database migrations
cd ../..
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/${ADDON_NAME}_dev \
  -Dflyway.user=dev \
  -Dflyway.password=dev \
  -pl "addons/$ADDON_NAME"

echo "✓ Database migrations completed"

# 6. Build addon
mvn clean package -pl "addons/$ADDON_NAME" -DskipTests

echo "✓ Addon built successfully"

# 7. Start addon
echo ""
echo "🎉 Dev environment ready!"
echo ""
echo "To start the addon:"
echo "  cd addons/$ADDON_NAME"
echo "  source .env && java -jar target/*-jar-with-dependencies.jar"
echo ""
echo "Or use: make run-$ADDON_NAME"
echo ""
echo "Endpoints:"
echo "  Health:   http://localhost:8080/$ADDON_NAME/health"
echo "  Manifest: http://localhost:8080/$ADDON_NAME/manifest.json"
echo ""
echo "Database:"
echo "  Host: localhost:5432"
echo "  Database: ${ADDON_NAME}_dev"
echo "  User: dev"
echo "  Password: dev"
echo ""
echo "To stop PostgreSQL:"
echo "  cd addons/$ADDON_NAME && docker-compose -f docker-compose.dev.yml down"
```

#### Step 2: Make Executable
```bash
chmod +x scripts/dev-setup.sh
```

#### Step 3: Test Script
```bash
# Test with existing addon
./scripts/dev-setup.sh rules

# Verify environment is ready
curl http://localhost:8080/rules/health
```

**Acceptance Criteria**:
- [ ] Creates .env file with correct values
- [ ] Starts PostgreSQL in Docker
- [ ] Runs database migrations
- [ ] Builds addon
- [ ] Total time < 2 minutes
- [ ] Works with any addon

---

### 1.3 Integration Test Framework

**Objective**: Base class for integration tests with Testcontainers

**Implementation Steps**:

#### Step 1: Add Dependencies to addon-sdk/pom.xml
```xml
<!-- Add to addon-sdk/pom.xml -->
<dependencies>
  <!-- Existing dependencies... -->

  <!-- Testcontainers -->
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>

  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>

  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

#### Step 2: Create Base Test Class
```java
// addons/addon-sdk/src/test/java/com/clockify/addon/sdk/testing/IntegrationTestBase.java
package com.clockify.addon.sdk.testing;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

/**
 * Base class for integration tests.
 * Provides PostgreSQL container and HTTP test utilities.
 */
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    protected static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    protected static ClockifyAddon addon;
    protected static HttpClient httpClient;
    protected static String baseUrl;

    @BeforeAll
    static void setupIntegrationTest() throws Exception {
        // Create addon instance
        addon = createAddon();

        // Start addon
        addon.start();

        // Setup HTTP client
        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + addon.getPort();
    }

    @AfterAll
    static void teardownIntegrationTest() throws Exception {
        if (addon != null) {
            addon.stop();
        }
    }

    /**
     * Override this to create your addon instance
     */
    protected static ClockifyAddon createAddon() throws Exception {
        throw new UnsupportedOperationException(
            "Override createAddon() in your test class"
        );
    }

    /**
     * Send HTTP GET request
     */
    protected HttpResponse get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .GET()
            .build();

        java.net.http.HttpResponse<String> response =
            httpClient.send(request, BodyHandlers.ofString());

        return new HttpResponse(
            response.statusCode(),
            response.body(),
            Map.of("Content-Type", "application/json")
        );
    }

    /**
     * Send HTTP POST request
     */
    protected HttpResponse post(String path, String body)
        throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        java.net.http.HttpResponse<String> response =
            httpClient.send(request, BodyHandlers.ofString());

        return new HttpResponse(
            response.statusCode(),
            response.body(),
            Map.of("Content-Type", "application/json")
        );
    }

    /**
     * Simulate addon installation
     */
    protected void installAddon(String workspaceId, String token)
        throws IOException, InterruptedException {

        String payload = String.format(
            "{\"workspaceId\":\"%s\",\"installationToken\":\"%s\"}",
            workspaceId, token
        );

        HttpResponse response = post("/lifecycle/installed", payload);
        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Installation failed: " + response.getBody());
        }
    }

    /**
     * Send test webhook
     */
    protected HttpResponse sendWebhook(String event, String workspaceId, String data)
        throws IOException, InterruptedException {

        String payload = String.format(
            "{\"event\":\"%s\",\"workspaceId\":\"%s\",\"data\":%s}",
            event, workspaceId, data
        );

        return post("/webhook", payload);
    }

    /**
     * Get database JDBC URL for tests
     */
    protected static String getDatabaseUrl() {
        return postgres.getJdbcUrl();
    }

    /**
     * Get database username for tests
     */
    protected static String getDatabaseUsername() {
        return postgres.getUsername();
    }

    /**
     * Get database password for tests
     */
    protected static String getDatabasePassword() {
        return postgres.getPassword();
    }
}
```

#### Step 3: Create Example Test
```java
// Example usage in addon test
package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RulesAddonIntegrationTest extends IntegrationTestBase {

    @Override
    protected static ClockifyAddon createAddon() throws Exception {
        // Configure addon with test database
        System.setProperty("ADDON_BASE_URL", "http://localhost:8080/rules");
        System.setProperty("DB_URL", getDatabaseUrl());
        System.setProperty("DB_USERNAME", getDatabaseUsername());
        System.setProperty("DB_PASSWORD", getDatabasePassword());

        return new RulesApp().createAddon();
    }

    @Test
    void testHealthEndpoint() throws Exception {
        var response = get("/rules/health");
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    void testInstallationFlow() throws Exception {
        // Install addon
        installAddon("ws-123", "token-123");

        // Verify token was stored
        // (This would check database or in-memory store)
        assertNotNull(addon.getTokenStore().get("ws-123"));
    }

    @Test
    void testWebhookProcessing() throws Exception {
        // Install first
        installAddon("ws-123", "token-123");

        // Send webhook
        var response = sendWebhook(
            "TIME_ENTRY.CREATED",
            "ws-123",
            "{\"timeEntryId\":\"te-456\"}"
        );

        assertEquals(200, response.getStatusCode());
    }
}
```

**Acceptance Criteria**:
- [ ] PostgreSQL container starts automatically
- [ ] Tests can install addon programmatically
- [ ] Tests can send webhooks
- [ ] Tests can verify database state
- [ ] All existing tests still pass

---

## Testing Guidelines

### Unit Test Standards

**Required Coverage**:
```
Minimum: 80% line coverage
Target: 90% line coverage
Focus: Business logic, error handling, edge cases
```

**Test Naming Convention**:
```java
@Test
void methodName_condition_expectedResult() {
    // Given
    var input = createTestData();

    // When
    var result = methodUnderTest(input);

    // Then
    assertEquals(expected, result);
}
```

### Integration Test Standards

**What to Test**:
- Full request/response flow
- Database interactions
- External API calls (mocked)
- Lifecycle handlers
- Webhook processing

**Example Structure**:
```java
@Test
void testCompleteWorkflow() {
    // 1. Install addon
    installAddon("ws-1", "token-1");

    // 2. Configure settings
    saveSettings("ws-1", settings);

    // 3. Send webhook
    sendWebhook("TIME_ENTRY.CREATED", "ws-1", data);

    // 4. Verify processing
    verify(processor).handle(any());

    // 5. Check database state
    assertEquals(expected, getFromDatabase());
}
```

---

## Deployment Checklist

### Before Deploying Improvements

- [ ] All tests pass (`mvn test`)
- [ ] Integration tests pass (`mvn verify`)
- [ ] Code coverage ≥ 80%
- [ ] No security vulnerabilities (`mvn dependency-check:check`)
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Migration guide created (if needed)

### Deployment Steps

1. **Create PR**:
   ```bash
   git push origin improvements/phase-1
   gh pr create --title "Phase 1: Quick Wins" --body "..."
   ```

2. **Review**:
   - Code review by 2+ developers
   - All CI checks pass
   - Documentation reviewed

3. **Merge**:
   ```bash
   gh pr merge --squash
   ```

4. **Tag Release**:
   ```bash
   git tag -a v1.1.0 -m "Phase 1 improvements"
   git push origin v1.1.0
   ```

5. **Update Docs**:
   - Update README.md
   - Publish release notes
   - Update examples

---

## Troubleshooting

### Common Issues

**Issue**: Testcontainers fails to start
```bash
# Solution: Check Docker is running
docker ps

# Check Docker resources
docker info | grep -i memory
```

**Issue**: PostgreSQL port conflict
```bash
# Solution: Stop existing PostgreSQL
docker-compose down
sudo systemctl stop postgresql

# Or use different port
POSTGRES_PORT=5433 ./scripts/dev-setup.sh rules
```

**Issue**: Maven dependency resolution fails
```bash
# Solution: Clear Maven cache
rm -rf ~/.m2/repository
mvn clean install
```

---

## Getting Help

- **Documentation**: Check [docs/](docs/) folder
- **Issues**: Create GitHub issue with label `improvement`
- **Discussions**: Use GitHub Discussions for questions
- **Slack**: Join #addon-development channel

---

**Version**: 1.0.0
**Last Updated**: 2025-11-09
**Maintained By**: Development Team
