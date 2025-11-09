# Rules Engine - Complete Analysis

**Location**: `addons/rules/src/main/java/com/example/rules/`

**Type**: Complex Automation Engine (IFTTT-style)

**Purpose**: Provides rule-based automation for time entry workflows; allows users to define conditions and actions via UI

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     RulesApp (Main)                     │
├─────────────────────────────────────────────────────────┤
│ • Manifest registration                                 │
│ • Handler registration (lifecycle, webhooks)            │
│ • Store initialization (database or in-memory)          │
│ • Cache management                                      │
└──────────────┬──────────────────────────────────────────┘
               │
    ┌──────────┴──────────┬──────────────┬──────────────┐
    │                     │              │              │
    ▼                     ▼              ▼              ▼
┌──────────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────┐
│ RulesStore   │  │Evaluator │  │WebhookHandler│  │ Settings │
│(Persistence) │  │(Logic)   │  │(Processing)  │  │Controller│
└──────────────┘  └──────────┘  └──────────────┘  └──────────┘
    │                 │              │                 │
    │                 │              │                 │
    ▼                 ▼              ▼                 ▼
┌──────────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────┐
│ In-Memory    │  │ Condition│  │ TimeEntry    │  │ UI HTML  │
│ Database     │  │Evaluator │  │ Context      │  │ + API    │
│              │  │          │  │              │  │          │
└──────────────┘  └──────────┘  └──────────────┘  └──────────┘
```

---

## Core Components

### 1. RulesApp.java - Main Entry Point

**Purpose**: Initialize addon with all components and register endpoints

**Initialization Sequence**:

```java
public static void main(String[] args) {
    // 1. Load environment configuration
    int port = ConfigValidator.validatePort(...);
    String baseUrl = ConfigValidator.validateUrl(...);

    // 2. Create manifest
    ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
        .key("rules")
        .name("Rules")
        .baseUrl(baseUrl)
        .scopes(new String[]{...})
        .build();

    // 3. Create addon
    ClockifyAddon addon = new ClockifyAddon(manifest);

    // 4. Initialize store (database or in-memory)
    String dbUrl = System.getenv("RULES_DB_URL")
        ?? System.getenv("DB_URL");

    RulesStoreSPI store = dbUrl != null
        ? new DatabaseRulesStore(dbUrl, ...)
        : new RulesStore();  // In-memory

    // 5. Create components
    RulesController controller = new RulesController(store);
    WebhookHandlers webhookHandlers = new WebhookHandlers(store);
    LifecycleHandlers lifecycleHandlers = new LifecycleHandlers(store);

    // 6. Register endpoints
    addon.registerCustomEndpoint("/manifest.json", ...);
    addon.registerCustomEndpoint("/settings", settingsController);
    addon.registerCustomEndpoint("/api/rules", controller);
    addon.registerLifecycleHandler("INSTALLED", lifecycleHandlers);
    addon.registerWebhookHandler("TIME_ENTRY_UPDATED", webhookHandlers);
    addon.registerWebhookHandler("TIMER_STOPPED", webhookHandlers);

    // 7. Register middleware filters
    EmbeddedServer server = new EmbeddedServer(servlet, contextPath);
    server.addFilter(new SecurityHeadersFilter());
    server.addFilter(new RateLimiter());
    server.start(port);
}
```

**Key Design Decisions**:
- Pluggable store interface (DatabaseRulesStore vs RulesStore)
- Separate controllers for different concerns
- Environment-based configuration

---

### 2. Rule.java - Rule Data Model

```java
public class Rule {
    private String id;                    // UUID
    private String name;                  // Display name
    private boolean enabled;              // Active/inactive toggle
    private String combinator;            // "AND" or "OR"
    private List<Condition> conditions;   // Match criteria
    private List<Action> actions;         // Things to do
}
```

**Fields Explained**:

| Field | Type | Purpose | Example |
|-------|------|---------|---------|
| `id` | String | Unique identifier | "550e8400-e29b-41d4-a716-446655440000" |
| `name` | String | Human-readable name | "Tag time entries over 8 hours" |
| `enabled` | boolean | Is rule active | true |
| `combinator` | String | Logical operator | "AND" (all conditions must match) |
| `conditions` | List<Condition> | When to apply | [{descriptionContains: "meeting"}, {isBillable: true}] |
| `actions` | List<Action> | What to do | [{addTag: "billable-work"}, {setProject: "Client A"}] |

**Lifecycle**:
```
User creates rule in UI
        ↓
SettingsController POST /api/rules
        ↓
RulesController.saveRule()
        ↓
Serialized to JSON
        ↓
Stored in RulesStore (or database)
        ↓
Webhook event received (TIME_ENTRY_UPDATED)
        ↓
WebhookHandlers loads rules for workspace
        ↓
Evaluator tests rule against time entry
        ↓
If conditions match: Actions applied via ClockifyApiClient
```

---

### 3. Condition.java - Match Criteria

```java
public class Condition {
    private String type;      // descriptionContains, projectIdEquals, etc.
    private String operator;  // EQUALS, NOT_EQUALS, CONTAINS, etc.
    private String value;     // Comparison value
}
```

**Condition Types Supported**:

| Type | Purpose | Example | Operator |
|------|---------|---------|----------|
| `descriptionContains` | Text search in description | "meeting" | CONTAINS |
| `descriptionEquals` | Exact description match | "Daily standup" | EQUALS |
| `hasTag` | Entry has specific tag | "tagId:123" | IN |
| `projectIdEquals` | Project matches ID | "projectId:456" | EQUALS |
| `projectNameContains` | Project name text search | "Client" | CONTAINS |
| `clientIdEquals` | Client matches ID | "clientId:789" | EQUALS |
| `clientNameContains` | Client name text search | "ACME" | CONTAINS |
| `isBillable` | Billable flag | true/false | EQUALS |

**Evaluation Logic** (in Evaluator.java):
```java
boolean evaluateCondition(Condition condition, TimeEntryContext context) {
    String type = condition.getType();

    if ("descriptionContains".equals(type)) {
        String desc = context.getDescription().toLowerCase();
        String value = condition.getValue().toLowerCase();
        return desc.contains(value);
    }

    if ("projectIdEquals".equals(type)) {
        return condition.getValue().equals(context.getProjectId());
    }

    if ("isBillable".equals(type)) {
        return String.valueOf(context.isBillable())
            .equals(condition.getValue());
    }

    // ... more conditions
}
```

---

### 4. Action.java - Automation Actions

```java
public class Action {
    private String type;           // add_tag, set_billable, etc.
    private Map<String, String> args;  // Arguments for action
}
```

**Action Types Supported**:

| Type | Purpose | Args | Example |
|------|---------|------|---------|
| `add_tag` | Add tag to entry | {tag: "tagName"} | {add_tag: {tag: "overtime"}} |
| `remove_tag` | Remove tag | {tag: "tagName"} | {remove_tag: {tag: "draft"}} |
| `set_description` | Replace description | {description: "new text"} | {set_description: {description: "Fixed"}} |
| `set_billable` | Set billable flag | {billable: "true"/"false"} | {set_billable: {billable: "true"}} |
| `set_project_by_id` | Change project | {projectId: "123"} | {set_project_by_id: {projectId: "456"}} |
| `set_project_by_name` | Change project by name | {projectName: "name"} | {set_project_by_name: {projectName: "Client A"}} |
| `set_task_by_id` | Assign task | {taskId: "789"} | {set_task_by_id: {taskId: "999"}} |
| `set_task_by_name` | Assign task by name | {taskName: "name"} | {set_task_by_name: {taskName: "Development"}} |

**Action Execution**:
```java
void executeAction(Action action, TimeEntry entry, ClockifyApiClient api) {
    String type = action.getType();
    Map<String, String> args = action.getArgs();

    if ("add_tag".equals(type)) {
        String tagName = args.get("tag");
        // Find or create tag
        // Add tag to entry
        api.addTagToEntry(entry.getId(), tagId);
    }

    if ("set_billable".equals(type)) {
        boolean billable = Boolean.parseBoolean(args.get("billable"));
        // Update entry billable flag
        api.updateEntryBillable(entry.getId(), billable);
    }

    // ... more actions
}
```

---

### 5. Evaluator.java - Rule Matching Engine

**Purpose**: Test if rule applies to time entry

**Core Method**:
```java
public boolean evaluate(Rule rule, TimeEntryContext context)
```

**Line-by-line Logic**:

1. **Skip if Disabled**:
   ```java
   if (!rule.isEnabled()) return false;
   ```

2. **Skip if No Conditions**:
   ```java
   if (rule.getConditions().isEmpty()) return false;
   ```

3. **Apply Combinator Logic**:
   ```java
   String combinator = rule.getCombinator();  // "AND" or "OR"

   if ("AND".equals(combinator)) {
       // All conditions must match
       for (Condition condition : rule.getConditions()) {
           if (!evaluateCondition(condition, context)) {
               return false;  // Fast-fail on first false
           }
       }
       return true;
   }

   if ("OR".equals(combinator)) {
       // At least one condition must match
       for (Condition condition : rule.getConditions()) {
           if (evaluateCondition(condition, context)) {
               return true;  // Fast-succeed on first true
           }
       }
       return false;
   }
   ```

4. **Evaluate Conditions**:
   ```java
   private boolean evaluateCondition(Condition condition, TimeEntryContext context) {
       // Type-specific evaluation
       // See Condition types above
   }
   ```

**Example Evaluation**:
```
Rule: "Tag time entries over 8 hours as overtime"
  name: "Overtime Detection"
  enabled: true
  combinator: "AND"
  conditions:
    - type: "descriptionContains", value: "overtime"
    - type: "isBillable", value: "true"
  actions:
    - type: "add_tag", args: {tag: "overtime"}

Time Entry arrives:
  description: "Project X - overtime work"
  billable: true
  tags: ["project-x"]

Evaluation:
  1. Rule enabled? YES
  2. Has conditions? YES
  3. Combinator: AND
  4. Condition 1: descriptionContains("overtime")
     → description contains "overtime"? YES
  5. Condition 2: isBillable(true)
     → billable == true? YES
  6. All AND conditions match? YES
  → Rule applies → Execute actions
     → Add tag "overtime" to entry
```

---

### 6. TimeEntryContext.java - Context for Evaluation

```java
public class TimeEntryContext {
    private JsonNode timeEntry;  // Raw time entry JSON from API
}
```

**Convenience Methods**:

| Method | Purpose | Returns |
|--------|---------|---------|
| `getDescription()` | Entry description | String |
| `getTagIds()` | Entry tag IDs | List<String> |
| `getProjectId()` | Project ID | String |
| `isBillable()` | Billable flag | boolean |
| `getTimeEntry()` | Raw JSON | JsonNode |

**Used By**: Evaluator to extract values for condition evaluation

**Example**:
```java
TimeEntryContext context = new TimeEntryContext(timeEntryJson);

// Evaluator uses these methods
context.getDescription()  // "Project X - overtime work"
context.getProjectId()    // "projectId123"
context.isBillable()      // true
```

---

### 7. RulesStore.java - In-Memory Persistence

```java
public class RulesStore implements RulesStoreSPI {
    private Map<String, Map<String, Rule>> rules;
    // workspaceId → { ruleId → Rule }
}
```

**Methods**:

| Method | Purpose | Complexity |
|--------|---------|-----------|
| `save(workspaceId, rule)` | Create or update rule | O(1) |
| `getAll(workspaceId)` | Fetch all rules | O(n) |
| `getEnabled(workspaceId)` | Fetch active rules | O(n) |
| `delete(workspaceId, ruleId)` | Remove rule | O(1) |
| `deleteAll(workspaceId)` | Clear workspace | O(n) |

**Thread Safety**: Uses ConcurrentHashMap for thread-safe concurrent access

**Persistence**: In-memory only (lost on restart)

---

### 8. DatabaseRulesStore.java - Persistent Storage

```java
public class DatabaseRulesStore implements RulesStoreSPI {
    // JDBC-based implementation
}
```

**Key Features**:
- Creates table if missing
- Serializes Rule to JSON for storage
- Supports PostgreSQL + MySQL
- Parameterized queries (SQL injection protection)
- Upsert pattern (INSERT or UPDATE)

**Table Schema**:
```sql
CREATE TABLE rules (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL,
    rule_json TEXT NOT NULL,  -- JSON serialized Rule
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

---

### 9. WebhookHandlers - Rule Application

**Purpose**: Apply rules when time entry events occur

**Event Flow**:

```
1. Webhook Received (TIME_ENTRY_UPDATED)
   └─ workspaceId, userId, timeEntryId, etc.

2. Load Rules
   └─ RulesStore.getEnabled(workspaceId)

3. Create Context
   └─ TimeEntryContext(timeEntryJson)

4. Evaluate Each Rule
   ├─ Rule 1: Conditions match? YES
   │  └─ Execute actions
   ├─ Rule 2: Conditions match? NO
   │  └─ Skip
   └─ Rule 3: Conditions match? YES
      └─ Execute actions

5. API Calls (if actions matched)
   ├─ GET /tags (fetch existing tags)
   ├─ POST /tags (create missing tags)
   └─ PUT /time-entries/{id} (update entry)

6. Return Response
   └─ HTTP 200 OK
```

---

### 10. SettingsController - Rule Management UI

**Endpoints**:

| Path | Method | Purpose |
|------|--------|---------|
| `/settings` | GET | HTML form for rule builder |
| `/api/rules` | GET | List rules (JSON API) |
| `/api/rules` | POST | Create/update rule |
| `/api/rules` | DELETE | Delete rule |
| `/api/test` | POST | Dry-run evaluation |
| `/api/cache` | GET | Cache status |
| `/api/catalog/triggers` | GET | Available webhook events |
| `/api/catalog/actions` | GET | Available actions |

**HTML UI Features**:
- Condition builder (add/remove conditions)
- Action builder (add/remove actions)
- Existing rules list (with delete buttons)
- Dry-run tester (evaluate without applying)
- Cache management (refresh workspace data)

---

## Data Flow - Complete Example

### Step 1: User Creates Rule in UI

```html
Rule Name: "Tag billable work"
Enabled: [X]
Combinator: AND

Conditions:
  - projectNameContains: "Client"
  - isBillable: true

Actions:
  - add_tag: "client-work"
```

### Step 2: Rule Submitted via API

```
POST /api/rules?workspaceId=workspace123
Content-Type: application/json

{
  "name": "Tag billable work",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "projectNameContains", "value": "Client"},
    {"type": "isBillable", "value": "true"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "client-work"}}
  ]
}
```

### Step 3: Rule Stored

```
RulesStore (or Database)
workspace123 → {
  "rule-uuid-123": {
    "id": "rule-uuid-123",
    "name": "Tag billable work",
    ...
  }
}
```

### Step 4: Time Entry Webhook Received

```
POST /webhook
Clockify-Webhook-Event-Type: TIME_ENTRY_UPDATED

{
  "workspaceId": "workspace123",
  "userId": "user456",
  "timeEntryId": "entry789",
  "projectName": "Client A - Website Redesign",
  "description": "Design mockups",
  "billable": true,
  "tags": []
}
```

### Step 5: WebhookHandlers Process

```java
1. Load rules for workspace123
   → Gets "Tag billable work" rule

2. Create TimeEntryContext
   → Wraps time entry JSON

3. Evaluator.evaluate(rule, context)
   → projectNameContains("Client") ? YES (matches "Client A")
   → isBillable(true) ? YES
   → Combinator AND: All conditions true? YES
   → Rule applies!

4. Execute actions
   → addTag("client-work")
   → API: POST /tags with name="client-work"
   → API: PUT /time-entries/entry789 with tag ID

5. Return HTTP 200 OK
```

### Step 6: Time Entry Updated

```
Time Entry entry789 now has:
  - tags: ["client-work"]
  - All other fields preserved
```

---

## Security Considerations

### Input Validation
- ✓ Rule names validated (max length, no injection)
- ✓ Condition values sanitized
- ✓ Action arguments validated
- ✓ Workspace ID checked for access control

### API Access
- ✓ Webhook signature validated before processing
- ✓ Workspace tokens stored securely (DatabaseTokenStore)
- ✓ All API calls use x-addon-token header

### Rule Safety
- ✓ Rules are workspace-scoped (can't affect other workspaces)
- ✓ Actions applied by addon with user's token
- ✓ No direct database access (goes through ClockifyApiClient)

---

## Testing Strategy

### Unit Tests
- Evaluator condition matching
- Rule storage and retrieval
- Action serialization/deserialization

### Integration Tests
- End-to-end webhook processing
- Database persistence (DatabaseRulesStore)
- API integration (ClockifyApiClient)

### System Tests
- Rule creation via UI
- Webhook trigger and application
- Action side-effects verified

---

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|-----------------|-------|
| Evaluate single rule | O(c) where c=conditions | Fast, linear in conditions |
| Load all rules | O(n) where n=rules | Database query cached |
| Apply single action | O(1) | Single API call per action |
| Webhook processing | O(n*c) | Load rules, evaluate all |

**Optimization**:
- WorkspaceCache reduces API calls (tags, projects, clients)
- getEnabled() filters to active rules
- Fast-fail combinator (AND stops on first false)

---

## Related Documentation

- RulesApp.java - Main entry point
- Rule.java - Data model
- Evaluator.java - Condition evaluation
- RulesStore.java - Persistence interface
- DatabaseRulesStore.java - SQL implementation
- SettingsController.java - UI

---

## Notes

1. **Rules are Workspace-Scoped**: Each workspace has independent rule sets
2. **Stateless Processing**: Rules don't maintain state between invocations
3. **API-Driven**: All modifications go through ClockifyApiClient (respects permissions)
4. **User Control**: Users can enable/disable rules without deletion
5. **Dry-Run**: UI supports testing rules without applying changes
