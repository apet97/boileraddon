# Complete File Structure Reference

Comprehensive directory and file structure of the Clockify Add-on Boilerplate.

## Repository Root

```
/home/user/boileraddon/
├── addons/                      # Main addons directory
├── db/                          # Database migrations
├── docs/                        # Existing documentation
├── examples/                    # Example specifications
├── scripts/                     # Automation scripts
├── tools/                       # Development tools
├── templates/                   # Legacy templates
├── extras/                      # Additional resources
├── .github/                     # CI/CD workflows
├── prompts/                     # AI prompts
├── gpt_projects/                # GPT project configs
├── briefings-kit/               # SHA-pinned briefings
├── CODEBASE_DOCUMENTATION/      # This documentation
│
├── pom.xml                      # Parent Maven POM
├── README.md                    # Main README
├── AI_README.md                 # AI developer guide
├── CHANGELOG.md                 # Version history
├── Makefile                     # Build automation (16KB, 40+ targets)
├── Dockerfile                   # Multi-stage Docker build
├── docker-compose.dev.yml       # PostgreSQL dev environment
├── .env.example                 # Environment template
├── .env.rules.example           # Rules addon config template
└── .gitignore                   # Git ignore patterns
```

---

## Addons Directory

```
addons/
├── addon-sdk/                   # Core SDK module (shared library)
│   ├── pom.xml                  # SDK dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/clockify/addon/sdk/
│       │   │   ├── ClockifyAddon.java           # Central coordinator
│       │   │   ├── ClockifyManifest.java        # Manifest builder
│       │   │   ├── AddonServlet.java            # HTTP router
│       │   │   ├── EmbeddedServer.java          # Jetty wrapper
│       │   │   ├── RequestHandler.java          # Handler interface
│       │   │   ├── HttpResponse.java            # Response helper
│       │   │   ├── BaseUrlDetector.java         # URL parsing
│       │   │   ├── ConfigValidator.java         # Config validation
│       │   │   ├── DefaultManifestController.java
│       │   │   │
│       │   │   ├── middleware/                  # Middleware components
│       │   │   │   ├── SecurityHeadersFilter.java
│       │   │   │   ├── RateLimiter.java
│       │   │   │   ├── CorsFilter.java
│       │   │   │   └── RequestLoggingFilter.java
│       │   │   │
│       │   │   ├── security/                    # Security utilities
│       │   │   │   ├── TokenStore.java
│       │   │   │   ├── DatabaseTokenStore.java
│       │   │   │   ├── TokenStoreSPI.java
│       │   │   │   └── WebhookSignatureValidator.java
│       │   │   │
│       │   │   ├── http/                        # HTTP client
│       │   │   │   └── ClockifyHttpClient.java
│       │   │   │
│       │   │   ├── health/                      # Health checks
│       │   │   │   └── HealthCheck.java
│       │   │   │
│       │   │   ├── metrics/                     # Metrics
│       │   │   │   └── MetricsHandler.java
│       │   │   │
│       │   │   ├── util/                        # Utilities
│       │   │   │   └── PathSanitizer.java
│       │   │   │
│       │   │   └── error/                       # Error handling
│       │   │       └── ErrorResponse.java
│       │   │
│       │   └── resources/
│       │       └── logback.xml                  # Logging config
│       │
│       └── test/                                # Test suite
│           └── java/com/clockify/addon/sdk/
│
├── _template-addon/             # Blank starter template
│   ├── pom.xml
│   └── src/main/java/com/example/templateaddon/
│       ├── TemplateAddonApp.java
│       ├── ManifestController.java
│       ├── SettingsController.java
│       ├── LifecycleHandlers.java
│       ├── WebhookHandlers.java
│       ├── EnvConfig.java
│       └── TestController.java
│
├── auto-tag-assistant/          # Production example addon
│   ├── pom.xml
│   ├── README.md
│   └── src/main/java/com/example/autotagassistant/
│       ├── AutoTagAssistantApp.java       # Main entry point
│       ├── ManifestController.java
│       ├── SettingsController.java
│       ├── LifecycleHandlers.java
│       └── WebhookHandlers.java
│
├── rules/                       # Automation rules addon (most complex)
│   ├── pom.xml
│   ├── README.md
│   └── src/main/java/com/example/rules/
│       ├── RulesApp.java                  # Main entry point
│       ├── ManifestController.java
│       ├── SettingsController.java
│       ├── IftttController.java           # IFTTT UI
│       ├── RulesController.java           # CRUD API
│       ├── LifecycleHandlers.java
│       ├── WebhookHandlers.java
│       ├── DynamicWebhookHandlers.java
│       ├── ClockifyClient.java
│       │
│       ├── engine/                        # Rule evaluation engine
│       │   ├── Rule.java
│       │   ├── Condition.java
│       │   ├── Action.java
│       │   ├── Evaluator.java
│       │   ├── TimeEntryContext.java
│       │   └── PlaceholderResolver.java
│       │
│       ├── store/                         # Persistence layer
│       │   ├── RulesStore.java            # In-memory
│       │   ├── DatabaseRulesStore.java    # PostgreSQL
│       │   └── RulesStoreSPI.java
│       │
│       ├── cache/                         # Workspace caching
│       │   └── WorkspaceCache.java
│       │
│       └── spec/                          # OpenAPI catalog
│           ├── TriggersCatalog.java
│           └── OpenAPISpecLoader.java
│
└── overtime/                    # Overtime policy addon
    └── src/main/java/com/example/overtime/
        └── OvertimeApp.java
```

---

## Database Directory

```
db/
└── migrations/
    └── V1__init.sql              # Initial schema (tokens + rules tables)
```

---

## Documentation Directory

```
docs/                             # Existing documentation (47 files)
├── README.md                     # Documentation index
├── AI_START_HERE.md              # AI onboarding
├── QUICK_START_LOCAL.md          # Quick start guide
├── ARCHITECTURE.md               # Architecture overview
├── SDK_OVERVIEW.md               # SDK documentation
├── API-COOKBOOK.md               # API examples (25KB)
├── PATTERNS.md                   # Code patterns (29KB)
├── COMMON-MISTAKES.md            # Error prevention (20KB)
├── MANIFEST_AND_LIFECYCLE.md     # Lifecycle guide
├── CLOCKIFY_PARAMETERS.md        # Parameters reference
├── PRODUCTION-DEPLOYMENT.md      # Deployment guide
├── SECURITY_CHECKLIST.md         # Security guide
├── TESTING_GUIDE.md              # Testing documentation
├── POSTGRESQL_GUIDE.md           # Database guide
├── DATA-MODELS.md                # Entity schemas
├── ADDON_RULES.md                # Rules addon docs
└── coverage/                     # Coverage reports
```

---

## Examples Directory

```
examples/                         # Example specifications
├── EXAMPLES_INDEX.md             # Example catalog
├── spec-template.md              # Specification template
│
├── component-types/              # UI component examples
│   ├── settings-sidebar/
│   │   └── example.html
│   └── time-entry-sidebar/
│       └── example.html
│
├── cost-center-addon/
├── jira-sync-addon/
└── tag-enforcer-addon/
```

---

## Scripts Directory

```
scripts/
├── new-addon.sh                  # Addon scaffolding (10KB)
├── test-new-addon.sh             # Validation tests
├── rules-demo.sh                 # Demo data seeder
├── rules-webhook-sim.sh          # Webhook simulator
├── run-rules.sh                  # Rules runner
├── dev-env-check.sh              # Environment checker
└── git-*.sh                      # Git automation scripts
```

---

## Tools Directory

```
tools/
├── validate-manifest.py          # Manifest validator
├── validate-addon.sh             # Addon validator
├── coverage_badge.py             # Coverage badge generator
├── check_briefing_links.py       # Documentation checker
├── verify-jwt-example.py         # JWT validator
└── manifest.schema.json          # Manifest JSON schema
```

---

## Extras Directory

```
extras/
├── sql/
│   └── token_store.sql           # Alternative token schema
├── lifecycle-schemas.json        # Lifecycle event schemas
├── webhook-schemas.json          # Webhook event schemas
├── environments.md               # Environment documentation
└── versions.md                   # Version compatibility
```

---

## GitHub Directory

```
.github/
├── workflows/                    # CI/CD workflows
│   ├── build-and-test.yml        # Main CI pipeline
│   ├── smoke.yml                 # Smoke tests
│   ├── validate.yml              # Manifest validation
│   ├── jekyll-gh-pages.yml       # Docs deployment
│   └── db-migrate.yml            # Database migrations
│
└── ISSUE_TEMPLATE/               # Issue templates
```

---

## AI Prompts Directory

```
prompts/
├── SYSTEM_PROMPT.md              # AI development reference
├── addon-patterns.json           # Pattern library
└── component-catalog.json        # Component specifications
```

---

## GPT Projects Directory

```
gpt_projects/
├── ARCH/                         # Architecture agent
├── DEV/                          # Development agent
├── DOCS/                         # Documentation agent
├── QA/                           # Quality assurance agent
├── SECURITY/                     # Security agent
└── PM/                           # Project management agent
```

---

## Key Configuration Files

### Parent POM
**File:** `/pom.xml`
- Multi-module Maven configuration
- Dependency management
- Flyway profile for migrations
- JaCoCo coverage configuration

### Dockerfile
**File:** `/Dockerfile`
- Multi-stage build (Maven + JRE)
- Configurable addon directory (build arg)
- Runtime environment setup

### Docker Compose
**File:** `/docker-compose.dev.yml`
- PostgreSQL 15 Alpine
- Health checks
- Volume persistence

### Makefile
**File:** `/Makefile` (16KB)
- 40+ build targets
- Build automation
- Test execution
- Docker orchestration
- Ngrok integration

---

## Environment Files

```
.env.example                      # Core environment template
.env.rules.example                # Rules addon template
```

### .env.example Structure

```bash
# Server
ADDON_PORT=8080
ADDON_BASE_URL=http://localhost:8080/addon

# Database
DB_URL=jdbc:postgresql://localhost:5432/addons
DB_USERNAME=addons
DB_PASSWORD=addons

# Security
ADDON_WEBHOOK_SECRET=

# Middleware
ADDON_RATE_LIMIT=
ADDON_LIMIT_BY=ip
ADDON_CORS_ORIGINS=
ADDON_REQUEST_LOGGING=false
```

---

## Build Output Locations

```
addons/addon-sdk/target/
├── classes/                      # Compiled classes
├── test-classes/                 # Test classes
├── addon-sdk-0.1.0.jar           # SDK JAR
└── jacoco.exec                   # Coverage data

addons/rules/target/
├── classes/
├── rules-0.1.0.jar               # Addon JAR
└── rules-0.1.0-jar-with-dependencies.jar  # Fat JAR (runnable)
```

---

## Important File Sizes

| File/Directory | Size | Description |
|----------------|------|-------------|
| Makefile | 16KB | Build automation |
| API-COOKBOOK.md | 25KB | API examples |
| PATTERNS.md | 29KB | Code patterns |
| COMMON-MISTAKES.md | 20KB | Error prevention |
| docs/ | ~500KB | All documentation |
| addon-sdk JAR | ~50KB | SDK library |
| rules JAR (fat) | ~5MB | Rules addon with deps |

---

## Generated Files (Git-Ignored)

```
target/                           # Maven build output
*.jar                             # Compiled JARs
*.class                           # Compiled classes
.env                              # Local environment
.env.local                        # Local overrides
*.log                             # Log files
.DS_Store                         # macOS metadata
```

---

## Test Files

```
addon-sdk/src/test/java/
├── ClockifyAddonTest.java
├── AddonServletTest.java
├── TokenStoreTest.java
├── DatabaseTokenStoreIT.java     # Integration test
├── WebhookSignatureValidatorTest.java
└── middleware/
    ├── RateLimiterTest.java
    └── CorsFilterTest.java
```

---

## Coverage Reports

```
addon-sdk/target/site/jacoco/
├── index.html                    # Coverage overview
├── com.clockify.addon.sdk/
│   ├── index.html
│   └── ClockifyAddon.html
└── jacoco.xml                    # XML report (CI)
```

---

**Next:** [Technology Stack](./13-TECHNOLOGY-STACK.md)
