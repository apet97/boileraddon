# Clockify Addon Examples Index

**Purpose**: Curated collection of example addons organized by complexity and use case

This index helps you find the right example addon to learn from or use as a starting point for your own addon.

---

## Quick Start

- **First time building addons?** Start with [hello-world (concept)](#beginner-examples)
- **Want a full working example?** See [auto-tag-assistant (complete)](#working-examples)
- **Need a template?** Use [_template-addon](#templates)

---

## Templates

### _template-addon
**Location**: `/addons/_template-addon/`

**Complexity**: Minimal

**Description**: Bare-bones starter template with minimal functionality

**Includes**:
- Basic manifest structure
- Lifecycle handlers (INSTALLED/DELETED)
- Settings sidebar component
- Webhook handler stub
- Health check endpoint

**Use this when**: Starting a new addon from scratch

**Build**:
```bash
make build-template
# or
mvn -pl addons/_template-addon clean package
```

**Customization**:
```bash
# Use scaffolding script to create customized copy
scripts/new-addon.sh my-addon "My Addon"
```

---

## Working Examples

### auto-tag-assistant
**Location**: `/addons/auto-tag-assistant/`

**Complexity**: Medium

**Description**: Complete production-ready addon that automatically detects and suggests tags for time entries

**Features**:
- ✅ Automatic tag detection based on time entry description
- ✅ Real-time webhook processing (TIMER_STOPPED, NEW_TIME_ENTRY)
- ✅ Settings UI for configuration
- ✅ Clockify API integration (tags, time entries)
- ✅ Token storage and management
- ✅ Comprehensive error handling
- ✅ Full test suite

**Demonstrates**:
- Manifest generation (no $schema in runtime)
- Lifecycle event handling
- Webhook event processing
- API client with retry logic
- Settings UI rendering
- Token storage patterns

**Scopes Used**:
- `TIME_ENTRY_READ`
- `TIME_ENTRY_WRITE`
- `TAG_READ`
- `TAG_WRITE`

**Build & Run**:
```bash
make build-auto-tag-assistant
make run-auto-tag-assistant
```

**Key Files**:
- `AutoTagAssistantApp.java` - Main entry point
- `LifecycleHandlers.java` - Token storage
- `WebhookHandlers.java` - Event processing
- `ClockifyApiClient.java` - API integration
- `TokenStore.java` - Token management

**Learn from this**: Best practices for production addons

---

## Example Specifications

These are detailed specifications without full implementations. Use them as references for planning your own addons.

### cost-center-addon
**Location**: `/examples/cost-center-addon/`

**Complexity**: Simple

**Description**: Automatically assign cost centers to time entries based on project mapping

**Spec Includes**:
- Problem statement
- Data model
- Webhook logic
- UI requirements
- Business rules

**Use Case**: Financial tracking and reporting by department/cost center

**Pattern**: Project-based categorization

**Implementation Status**: ⚠️ Specification only (no code yet)

**How to implement**: Use this spec with the [spec-template.md](spec-template.md) and let AI generate the implementation

---

### tag-enforcer-addon
**Location**: `/examples/tag-enforcer-addon/`

**Complexity**: Simple

**Description**: Enforce required tags on time entries before they can be saved

**Spec Includes**:
- Tag validation rules
- Warning/error display logic
- Configuration UI

**Use Case**: Ensure data quality and compliance with tagging policies

**Pattern**: Validation and compliance

**Implementation Status**: ⚠️ Specification only (no code yet)

---

### jira-sync-addon
**Location**: `/examples/jira-sync-addon/`

**Complexity**: Advanced

**Description**: Bi-directional synchronization between Clockify and Jira

**Spec Includes**:
- External API integration pattern
- OAuth flow (conceptual)
- Bi-directional sync logic
- Conflict resolution strategy

**Use Case**: Keep time tracking synchronized with project management tool

**Pattern**: External integration, bi-directional sync

**Implementation Status**: ⚠️ Specification only (no code yet)

**External Dependencies**: Jira API, OAuth credentials

---

## Component Type Examples

### Settings Sidebar Example
**Location**: `/examples/component-types/settings-sidebar.html`

**Shows**: Admin configuration panel with form inputs, validation, save functionality

**Features**:
- Form validation
- API integration
- Success/error notifications
- Responsive design

### Time Entry Sidebar Example
**Location**: `/examples/component-types/time-entry-sidebar.html`

**Shows**: Context panel for individual time entries

**Features**:
- Dynamic data loading
- Query parameter extraction
- Action buttons
- Loading states

### Project Sidebar Example
**Location**: `/examples/component-types/project-sidebar.html`

**Shows**: Project-specific information and statistics

---

## Examples by Use Case

### Data Validation & Quality
- ✅ **tag-enforcer-addon** - Ensure required tags present
- Validation of custom fields
- Description format validation

### Automation & Productivity
- ✅ **auto-tag-assistant** - Auto-apply tags
- Auto-assign projects based on description
- Timer reminders and notifications

### Financial & Reporting
- ✅ **cost-center-addon** - Department tracking
- Billable rate calculations
- Budget tracking and alerts

### External Integrations
- ✅ **jira-sync-addon** - Project management sync
- Slack notifications
- GitHub commit linking
- Google Sheets export

### Approval Workflows
- Multi-stage time entry approval
- Manager review dashboard
- Approval notifications

---

## Examples by Complexity

### Beginner (1-2 files, < 100 LOC)

**Concept: Hello World Addon**
```java
// Minimal addon that displays "Hello, Clockify!" in settings
public class HelloWorldApp {
    public static void main(String[] args) {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
            .key("hello-world")
            .name("Hello World")
            .description("A minimal example")
            .baseUrl("http://localhost:8080/hello-world")
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"WORKSPACE_READ"})
            .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Simple settings UI
        addon.registerCustomEndpoint("/settings", request -> {
            return HttpResponse.ok(
                "<!DOCTYPE html><html><body><h1>Hello, Clockify!</h1></body></html>",
                "text/html"
            );
        });

        // Lifecycle handlers
        addon.registerLifecycleHandler("INSTALLED", request ->
            HttpResponse.ok("Installed")
        );

        // Start server
        // ... (server setup code)
    }
}
```

**Files needed**: 1 Java file, pom.xml

**Learn**: Basic addon structure, manifest, lifecycle, UI

---

### Intermediate (3-5 files, 100-300 LOC)

**Examples**:
- ✅ **tag-enforcer-addon** (when implemented)
- ✅ **cost-center-addon** (when implemented)
- Tag suggestion addon
- Time rounding addon

**Typical Structure**:
```
addon/
├── MainApp.java              # Entry point
├── LifecycleHandlers.java    # INSTALLED/DELETED
├── WebhookHandlers.java      # Event processing
├── SettingsController.java   # UI
└── ConfigStore.java          # Configuration persistence
```

**Learn**: Configuration management, webhook processing, data persistence

---

### Advanced (6+ files, 300+ LOC)

**Examples**:
- ✅ **auto-tag-assistant** (working example)
- ✅ **jira-sync-addon** (spec only)
- Multi-stage approval workflow
- Advanced reporting dashboard

**Typical Structure**:
```
addon/
├── MainApp.java
├── LifecycleHandlers.java
├── WebhookHandlers.java
├── SettingsController.java
├── SidebarController.java
├── ReportController.java
├── ClockifyApiClient.java
├── ExternalApiClient.java
├── TokenStore.java
├── ConfigStore.java
├── DataSync.java
└── BusinessLogic.java
```

**Learn**: External integrations, async processing, complex state management

---

## How to Use These Examples

### 1. Learning Pattern
1. Read the specification (if available)
2. Review the implementation code
3. Run the example locally
4. Modify and experiment
5. Adapt for your use case

### 2. As a Starting Point
1. Copy the closest matching example
2. Rename files and packages
3. Modify manifest (key, name, scopes)
4. Customize business logic
5. Update tests
6. Deploy

### 3. For AI-Driven Development
1. Choose example spec that matches your need
2. Use [spec-template.md](spec-template.md) to expand specification
3. Provide spec to AI with [/prompts/SYSTEM_PROMPT.md](../prompts/SYSTEM_PROMPT.md)
4. Review and refine generated code
5. Test and deploy

---

## Example Selection Guide

**Choose based on**:

| Your Need | Recommended Example |
|-----------|-------------------|
| Learn addon basics | _template-addon |
| See complete working addon | auto-tag-assistant |
| Auto-categorization | auto-tag-assistant or tag-enforcer |
| Financial tracking | cost-center-addon |
| External integration | jira-sync-addon |
| Custom UI/reporting | auto-tag-assistant (sidebar examples) |
| Validation/compliance | tag-enforcer-addon |

---

## Contributing Examples

Want to add an example?

1. Create specification using [spec-template.md](spec-template.md)
2. Implement following repository structure
3. Add comprehensive tests
4. Document in README.md
5. Add entry to this index
6. Submit pull request

**Good examples**:
- Solve a real problem
- Well-documented
- Production-ready code quality
- Comprehensive error handling
- Full test coverage

---

## Resources

- **Spec Template**: [spec-template.md](spec-template.md)
- **Pattern Library**: [/prompts/addon-patterns.json](../prompts/addon-patterns.json)
- **Component Catalog**: [/prompts/component-catalog.json](../prompts/component-catalog.json)
- **Common Mistakes**: [/docs/COMMON-MISTAKES.md](../docs/COMMON-MISTAKES.md)
- **API Cookbook**: [/docs/API-COOKBOOK.md](../docs/API-COOKBOOK.md)
- **Quick Reference**: [/docs/QUICK-REFERENCE.md](../docs/QUICK-REFERENCE.md)

---

**Last Updated**: 2025-11-08
**Total Examples**: 6 (1 complete implementation, 3 specs, 1 template)
