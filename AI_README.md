# 🤖 AI Developer Guide - Clockify Addon Boilerplate

**Purpose**: Complete guide for AI agents (ChatGPT, Claude, etc.) to generate production-ready Clockify addons

**Goal**: Enable **ONE-SHOT** addon generation from specifications

---

## 🎯 Quick Start for AI

If you're an AI agent asked to create a Clockify addon:

### Step 1: Read These Files (in order)
1. **THIS FILE** (`AI_README.md`) - You're here! ✓
2. [`/prompts/SYSTEM_PROMPT.md`](prompts/SYSTEM_PROMPT.md) - Complete development reference (20+ pages)
3. [`/docs/QUICK-REFERENCE.md`](docs/QUICK-REFERENCE.md) - All parameters, scopes, components
4. [`/examples/spec-template.md`](examples/spec-template.md) - Specification format

### Step 2: Understand the Codebase
- Working Example: [`/addons/auto-tag-assistant/`](addons/auto-tag-assistant/)
- SDK Source: [`/addons/addon-sdk/src/main/java/`](addons/addon-sdk/src/main/java/)
- Patterns: [`/prompts/addon-patterns.json`](prompts/addon-patterns.json)
- Components: [`/prompts/component-catalog.json`](prompts/component-catalog.json)

### Step 3: Review Common Mistakes
- [`/docs/COMMON-MISTAKES.md`](docs/COMMON-MISTAKES.md) - **READ THIS** to avoid critical errors

### Step 4: Generate the Addon
Use the patterns and examples to generate a complete addon implementation.

### Step 5: Validate
Run validation script:
```bash
tools/validate-addon.sh addons/your-addon
```

---

## 📋 AI Generation Workflow

### Input: Addon Specification

The user will provide a specification (either structured or free-form). Parse it to extract:

**Required**:
- Addon name and key
- Purpose (what problem does it solve?)
- Required scopes
- Webhook events needed
- UI components needed

**Optional**:
- External integrations
- Data model
- Business logic details

### Output: Complete Addon Implementation

Generate these files:

```
addons/your-addon/
├── pom.xml                                    # Maven configuration
├── README.md                                  # Documentation
└── src/
    ├── main/java/com/example/youraddon/
    │   ├── YourAddonApp.java                 # Main entry point
    │   ├── ManifestController.java           # Manifest endpoint
    │   ├── SettingsController.java           # Settings UI
    │   ├── LifecycleHandlers.java            # INSTALLED/DELETED
    │   ├── WebhookHandlers.java              # Event processing
    │   ├── TokenStore.java                   # Token storage
    │   └── ClockifyApiClient.java            # API wrapper (if needed)
    └── test/java/com/example/youraddon/
        ├── ManifestValidationTest.java
        ├── LifecycleHandlersTest.java
        └── WebhookHandlersTest.java
```

---

## 🚨 Critical Rules - NEVER VIOLATE

These are the most common mistakes that **WILL BREAK** the addon:

### 1. Manifest Must NOT Include `$schema`
```json
❌ WRONG:
{
  "$schema": "../schema.json",
  "schemaVersion": "1.3"
}

✅ CORRECT:
{
  "schemaVersion": "1.3"
}
```

### 2. Must Use `X-Addon-Token` Header
```java
❌ WRONG:
conn.setRequestProperty("Authorization", "Bearer " + token);

✅ CORRECT:
conn.setRequestProperty("X-Addon-Token", token);
```

### 3. Must Store Installation Token
```java
✅ REQUIRED in INSTALLED handler:
String token = payload.get("installationToken").getAsString();
tokenStore.save(workspaceId, token); // CRITICAL!
```

### 4. Must Use `schemaVersion` (not `version`)
```json
❌ WRONG: "version": "1.3"
✅ CORRECT: "schemaVersion": "1.3"
```

### 5. BaseURL Must Match Server Endpoints
```json
Manifest: "baseUrl": "http://localhost:8080/my-addon"
Server must serve:
  - GET http://localhost:8080/my-addon/manifest.json
  - GET http://localhost:8080/my-addon/settings
  - POST http://localhost:8080/my-addon/webhook
```

**See full list**: [`/docs/COMMON-MISTAKES.md`](docs/COMMON-MISTAKES.md)

---

## 📚 Resource Library

### Core Documentation

| File | Purpose | When to Use |
|------|---------|-------------|
| [`SYSTEM_PROMPT.md`](prompts/SYSTEM_PROMPT.md) | Complete AI reference | **Always read first** |
| [`QUICK-REFERENCE.md`](docs/QUICK-REFERENCE.md) | One-page cheat sheet | Parameter lookup |
| [`API-COOKBOOK.md`](docs/API-COOKBOOK.md) | Copy-paste code examples | API integration |
| [`PATTERNS.md`](docs/PATTERNS.md) | Production patterns | Best practices |
| [`COMMON-MISTAKES.md`](docs/COMMON-MISTAKES.md) | Error prevention | **Must read** |

### Structured Data (JSON)

| File | Purpose | Format |
|------|---------|--------|
| [`addon-patterns.json`](prompts/addon-patterns.json) | Pattern library | JSON - Parseable |
| [`component-catalog.json`](prompts/component-catalog.json) | UI component specs | JSON - Parseable |
| [`openapi (1).json`](openapi%20(1).json) | Full Clockify API | OpenAPI 3.0.1 (1.1 MB) |

### Examples

| Location | Description | Complexity |
|----------|-------------|------------|
| [`/addons/_template-addon/`](addons/_template-addon/) | Minimal starter | Beginner |
| [`/addons/auto-tag-assistant/`](addons/auto-tag-assistant/) | **Complete working example** | Medium |
| [`/examples/`](examples/) | Specs & component examples | Various |
| [`EXAMPLES_INDEX.md`](examples/EXAMPLES_INDEX.md) | Example catalog | - |

### Templates

| File | Purpose |
|------|---------|
| [`spec-template.md`](examples/spec-template.md) | Addon specification template |

---

## 🔧 Generation Templates

### Template 1: Main Application Class

```java
package com.example.youraddon;

import com.clockify.addon.sdk.*;

public class YourAddonApp {
    public static void main(String[] args) throws Exception {
        // 1. Configuration
        String baseUrl = System.getenv().getOrDefault(
            "ADDON_BASE_URL",
            "http://localhost:8080/your-addon"
        );
        int port = Integer.parseInt(
            System.getenv().getOrDefault("ADDON_PORT", "8080")
        );

        // 2. Build manifest (NO $schema field!)
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
            .key("your-addon")
            .name("Your Addon")
            .description("What your addon does")
            .baseUrl(baseUrl)
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{
                "TIME_ENTRY_READ",
                // Add minimum required scopes
            })
            .build();

        // 3. Add components
        manifest.getComponents().add(
            new ClockifyManifest.ComponentEndpoint(
                "SETTINGS_SIDEBAR",
                "/settings",
                "Addon Settings",
                "ADMINS"
            )
        );

        // 4. Create addon
        ClockifyAddon addon = new ClockifyAddon(manifest);

        // 5. Register endpoints
        addon.registerCustomEndpoint("/manifest.json",
            new DefaultManifestController(manifest));
        addon.registerCustomEndpoint("/settings",
            new SettingsController());
        addon.registerCustomEndpoint("/health",
            request -> HttpResponse.ok("OK"));

        // 6. Register handlers
        LifecycleHandlers.register(addon);
        WebhookHandlers.register(addon);

        // 7. Start server
        String contextPath = extractContextPath(baseUrl);
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        System.out.println("Starting addon at " + baseUrl);
        server.start(port);
    }

    private static String extractContextPath(String baseUrl) {
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getPath();
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (Exception e) {
            return "/";
        }
    }
}
```

### Template 2: Lifecycle Handlers

```java
package com.example.youraddon;

import com.clockify.addon.sdk.*;
import com.google.gson.JsonObject;

public class LifecycleHandlers {
    public static void register(ClockifyAddon addon) {
        // INSTALLED - MUST store token!
        addon.registerLifecycleHandler("INSTALLED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();
            String token = payload.get("installationToken").getAsString();

            // CRITICAL: Store token
            TokenStore.save(workspaceId, token);

            System.out.println("Installed in workspace: " + workspaceId);
            return HttpResponse.ok("{\"success\": true}");
        });

        // DELETED - Clean up data
        addon.registerLifecycleHandler("DELETED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();

            // Clean up workspace data
            TokenStore.remove(workspaceId);

            System.out.println("Deleted from workspace: " + workspaceId);
            return HttpResponse.ok("{\"success\": true}");
        });
    }
}
```

### Template 3: Webhook Handlers

```java
package com.example.youraddon;

import com.clockify.addon.sdk.*;
import com.google.gson.JsonObject;

public class WebhookHandlers {
    public static void register(ClockifyAddon addon) {
        addon.registerWebhookHandler("TIMER_STOPPED", request -> {
            JsonObject payload = request.getPayload();
            String workspaceId = payload.get("workspaceId").getAsString();
            String timeEntryId = payload.get("timeEntryId").getAsString();

            // Get stored token
            String token = TokenStore.get(workspaceId);
            if (token == null) {
                return HttpResponse.unauthorized("No token for workspace");
            }

            // Process event
            processTimerStopped(workspaceId, timeEntryId, token);

            return HttpResponse.ok("Processed");
        });

        // Add more webhook handlers as needed
    }

    private static void processTimerStopped(String workspaceId,
                                           String timeEntryId,
                                           String token) {
        // Your business logic here
        System.out.println("Processing timer stopped: " + timeEntryId);
    }
}
```

### Template 4: Token Storage

```java
package com.example.youraddon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TokenStore {
    private static final Map<String, String> tokens = new ConcurrentHashMap<>();

    public static void save(String workspaceId, String token) {
        if (workspaceId == null || token == null) {
            throw new IllegalArgumentException("Workspace ID and token required");
        }
        tokens.put(workspaceId, token);
    }

    public static String get(String workspaceId) {
        return tokens.get(workspaceId);
    }

    public static void remove(String workspaceId) {
        tokens.remove(workspaceId);
    }

    public static boolean has(String workspaceId) {
        return tokens.containsKey(workspaceId);
    }
}
```

### Template 5: pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.clockify</groupId>
        <artifactId>clockify-addon-boilerplate</artifactId>
        <version>1.0.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>your-addon</artifactId>
    <version>0.1.0</version>
    <name>Your Addon</name>

    <dependencies>
        <dependency>
            <groupId>com.clockify</groupId>
            <artifactId>addon-sdk</artifactId>
            <version>0.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.7.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.example.youraddon.YourAddonApp</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>single</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## ✅ Validation Checklist

Before completing generation, verify:

- [ ] No `$schema` in manifest
- [ ] Using `schemaVersion` (not `version`)
- [ ] BaseURL matches server endpoints
- [ ] Using `X-Addon-Token` header (not `Authorization`)
- [ ] INSTALLED handler stores token
- [ ] DELETED handler cleans up data
- [ ] Tokens keyed by `workspaceId`
- [ ] Error handling for all API calls
- [ ] Webhook signature validation (if processing webhooks)
- [ ] No hardcoded secrets
- [ ] No sensitive data in logs
- [ ] Fat JAR configuration in pom.xml
- [ ] Tests included
- [ ] README.md created
- [ ] Health check endpoint

Run validation:
```bash
chmod +x tools/validate-addon.sh
tools/validate-addon.sh addons/your-addon
```

---

## 🎓 Learning Resources

### For Understanding Patterns
1. Read [`auto-tag-assistant`](addons/auto-tag-assistant/) - Complete example
2. Review [`addon-patterns.json`](prompts/addon-patterns.json) - Pattern library
3. Check [`PATTERNS.md`](docs/PATTERNS.md) - Code patterns

### For API Integration
1. [`API-COOKBOOK.md`](docs/API-COOKBOOK.md) - Copy-paste examples
2. [`openapi (1).json`](openapi%20(1).json) - Full API spec
3. [`DATA-MODELS.md`](docs/DATA-MODELS.md) - Entity schemas

### For UI Components
1. [`component-catalog.json`](prompts/component-catalog.json) - Component specs
2. [`examples/component-types/`](examples/component-types/) - HTML examples

---

## 🚀 Usage Example

**User Request**:
> "Create an addon that enforces required tags on time entries"

**AI Response Workflow**:

1. **Parse Request**:
   - Name: Tag Enforcer
   - Purpose: Validate required tags
   - Scopes: `TIME_ENTRY_READ`, `TAG_READ`
   - Webhooks: `NEW_TIME_ENTRY`, `TIME_ENTRY_UPDATED`
   - Components: `TIME_ENTRY_SIDEBAR`, `SETTINGS_SIDEBAR`

2. **Select Pattern**:
   - Check [`addon-patterns.json`](prompts/addon-patterns.json)
   - Find "tag-enforcement" pattern
   - Use as template

3. **Generate Code**:
   - Create directory: `addons/tag-enforcer/`
   - Generate all files using templates above
   - Customize business logic for tag validation
   - Add configuration for required tags list

4. **Add Tests**:
   - Manifest validation test
   - Lifecycle handler tests
   - Webhook processing tests

5. **Create Documentation**:
   - README.md with usage instructions
   - Configuration examples
   - Troubleshooting guide

6. **Validate**:
   - Run `tools/validate-addon.sh addons/tag-enforcer`
   - Fix any errors reported
   - Ensure all checks pass

7. **Provide to User**:
   - Complete file listing
   - Build instructions
   - Installation guide
   - Testing steps

---

## 📊 Success Metrics

An addon is successfully generated if:

✅ Builds without errors: `mvn clean package`

✅ Validation passes: `tools/validate-addon.sh addons/your-addon`

✅ Server starts: `java -jar target/your-addon-*-jar-with-dependencies.jar`

✅ Health check responds: `curl http://localhost:8080/your-addon/health`

✅ Manifest accessible: `curl http://localhost:8080/your-addon/manifest.json`

✅ Manifest valid: No `$schema`, correct `schemaVersion`, all required fields

✅ Tests pass: `mvn test`

---

## 🆘 Troubleshooting

### Common Generation Errors

**Error**: "Manifest rejected by Clockify"
- **Cause**: `$schema` field in manifest
- **Fix**: Remove `$schema` from runtime manifest

**Error**: "401 Unauthorized on API calls"
- **Cause**: Using wrong auth header or token not stored
- **Fix**: Use `X-Addon-Token` header, verify INSTALLED handler stores token

**Error**: "Build fails with 'cannot find symbol'"
- **Cause**: Missing imports or incorrect package names
- **Fix**: Ensure all imports are correct, check package structure

**Error**: "No main manifest attribute"
- **Cause**: pom.xml missing mainClass configuration
- **Fix**: Add `<mainClass>` to maven-assembly-plugin configuration

See full troubleshooting: [`COMMON-MISTAKES.md`](docs/COMMON-MISTAKES.md)

---

## 📞 Getting Help

**For AI Agents**:
- Re-read [`SYSTEM_PROMPT.md`](prompts/SYSTEM_PROMPT.md)
- Check [`COMMON-MISTAKES.md`](docs/COMMON-MISTAKES.md)
- Review working example: [`auto-tag-assistant`](addons/auto-tag-assistant/)

**For Human Developers**:
- GitHub Issues: https://github.com/apet97/boileraddon/issues
- Clockify Support: https://clockify.me/help

---

## 🎯 Final Checklist for AI

Before completing addon generation:

- [ ] Read SYSTEM_PROMPT.md completely
- [ ] Reviewed COMMON-MISTAKES.md
- [ ] Used appropriate pattern from addon-patterns.json
- [ ] Generated all required files
- [ ] No critical errors in code
- [ ] Validation checklist passed
- [ ] Tests included
- [ ] Documentation complete
- [ ] Build instructions provided
- [ ] Installation steps provided

---

**Repository**: https://github.com/apet97/boileraddon

**Version**: 1.0.0 (AI-Ready)

**Last Updated**: 2025-11-08

**Status**: ✅ Ready for one-shot addon generation

---

## 🌟 You're Ready!

This repository contains everything needed for instant addon generation. Read the resources, follow the patterns, avoid the common mistakes, and generate production-quality Clockify addons in one shot.

**Happy generating! 🚀**
