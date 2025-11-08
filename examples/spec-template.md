# Addon Specification Template

**Purpose**: Structured specification format for AI-driven addon generation

**Instructions**: Fill out all sections with as much detail as possible. The more complete the spec, the better the generated addon will be.

---

## Addon Metadata

### Name
**Addon Name**: [Short, descriptive name]

**Addon Key**: [lowercase-with-dashes, must be unique]

**Display Name**: [User-facing name shown in Clockify]

**One-Sentence Description**: [What does this addon do in one sentence?]

**Detailed Description**: [2-3 sentences explaining the addon's purpose and value proposition]

---

## Problem Statement

### Business Problem
[Describe the problem this addon solves. Be specific about the pain point.]

**Example**: "Users waste time manually categorizing time entries. They forget to add required tags, leading to incomplete data and poor reporting accuracy."

### Target Users
- [ ] Individual users
- [ ] Team leads
- [ ] Workspace administrators
- [ ] Billing/finance teams
- [ ] Project managers

### Success Metrics
[How will you measure if this addon is successful?]
- Metric 1: [e.g., "95% of time entries have required tags"]
- Metric 2: [e.g., "Time spent on manual categorization reduced by 50%"]

---

## User Stories

**Format**: As a [user type], I want to [action] so that [benefit].

1. **Story 1**:
   - As a [user type]
   - I want to [action]
   - So that [benefit]

2. **Story 2**:
   - As a [user type]
   - I want to [action]
   - So that [benefit]

3. **Story 3**: [Add more as needed]

---

## Required Scopes

**Instructions**: Select MINIMUM scopes needed. Each scope requires user consent.

Data Access:
- [ ] `WORKSPACE_READ` - Workspace details
- [ ] `PROJECT_READ` - List projects
- [ ] `PROJECT_WRITE` - Create/update/delete projects
- [ ] `TAG_READ` - List tags
- [ ] `TAG_WRITE` - Create/update/delete tags
- [ ] `CLIENT_READ` - List clients
- [ ] `CLIENT_WRITE` - Create/update/delete clients
- [ ] `TIME_ENTRY_READ` - List time entries
- [ ] `TIME_ENTRY_WRITE` - Create/update/delete time entries
- [ ] `TASK_READ` - List tasks
- [ ] `TASK_WRITE` - Create/update/delete tasks
- [ ] `USER_READ` - List workspace users
- [ ] `CUSTOM_FIELD_READ` - List custom fields
- [ ] `CUSTOM_FIELD_WRITE` - Create/update custom fields

**Justification**: [Explain why each selected scope is needed]

---

## Webhook Events

**Instructions**: Select events the addon needs to react to.

- [ ] `NEW_TIME_ENTRY` - New time entry created
- [ ] `NEW_TIMER_STARTED` - Timer started
- [ ] `TIMER_STOPPED` - Timer stopped
- [ ] `TIME_ENTRY_UPDATED` - Time entry modified
- [ ] `TIME_ENTRY_DELETED` - Time entry deleted

**Event Processing Logic**:

### For `[EVENT_NAME]`:
**Trigger**: [When does this event fire?]

**Required Data from Payload**:
- Field 1: [e.g., `timeEntryId`]
- Field 2: [e.g., `projectId`]

**Processing Steps**:
1. [Step 1 - e.g., "Extract time entry description"]
2. [Step 2 - e.g., "Check if tags are missing"]
3. [Step 3 - e.g., "Apply auto-tagging rules"]

**API Calls Made**:
- `GET /workspaces/{workspaceId}/tags` - [Why needed?]
- `PUT /workspaces/{workspaceId}/time-entries/{id}` - [Why needed?]

**Expected Outcome**: [What should happen after processing?]

**Error Handling**: [What happens if processing fails?]

---

## UI Components

**Instructions**: Define which UI components are needed and what they display.

### Component 1: [Type]

**Type**: [SETTINGS_SIDEBAR | TIME_ENTRY_SIDEBAR | PROJECT_SIDEBAR | REPORT_TAB | WIDGET]

**Label**: [User-visible label]

**Access Level**: [ALL | ADMINS]

**URL Path**: `/[path]`

**Purpose**: [What does this component do?]

**Available Context**:
- Query Parameters: [`workspaceId`, `userId`, `timeEntryId`, etc.]
- JWT Claims: [`userId`, `userEmail`, `userName`]

**Content to Display**:
- Section 1: [What's shown in this section?]
  - Field 1: [e.g., "Tag compliance score"]
  - Field 2: [e.g., "Missing required tags"]
- Section 2: [Another section]

**User Actions**:
- Action 1: [e.g., "Add missing tags" - What happens when clicked?]
- Action 2: [e.g., "Save settings" - What data is saved?]

**Data Fetching**:
- API Call 1: `GET /api/...` - [What data is fetched?]
- API Call 2: `POST /api/...` - [What data is saved?]

**Mockup/Wireframe**: [Describe layout or attach mockup]
```
+---------------------------+
|  Title                    |
+---------------------------+
|  [Section 1]              |
|  - Field 1: Value         |
|  - Field 2: Value         |
|                           |
|  [Button] [Button]        |
+---------------------------+
```

---

## Data Model

### Entities

**Entity 1**: [e.g., Tag]
- Field 1: `id` (string) - Clockify tag ID
- Field 2: `name` (string) - Tag name
- Field 3: `category` (string) - Custom category

**Entity 2**: [e.g., TaggingRule]
- Field 1: `pattern` (string) - Regex pattern to match
- Field 2: `tagId` (string) - Tag to apply
- Field 3: `enabled` (boolean) - Is rule active?

### Storage Requirements

**What needs to be stored?**:
- Installation tokens (workspace → token mapping)
- Configuration data: [List all config that needs persistence]
- Cached data: [List any cached data and TTL]
- User preferences: [Any per-user settings?]

**Storage Strategy**:
- [ ] In-memory (development only)
- [ ] File-based (simple deployments)
- [ ] Database (production, specify: PostgreSQL / MySQL / MongoDB)

**Database Schema** (if using database):
```sql
CREATE TABLE installation_tokens (
  workspace_id VARCHAR(24) PRIMARY KEY,
  token TEXT NOT NULL,
  installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tagging_rules (
  id SERIAL PRIMARY KEY,
  workspace_id VARCHAR(24) NOT NULL,
  pattern VARCHAR(255) NOT NULL,
  tag_id VARCHAR(24) NOT NULL,
  enabled BOOLEAN DEFAULT true,
  FOREIGN KEY (workspace_id) REFERENCES installation_tokens(workspace_id)
);
```

---

## Business Logic

### Core Workflows

#### Workflow 1: [Name]

**Trigger**: [What starts this workflow?]

**Input**: [What data is needed?]

**Steps**:
```
1. [Step 1]
   IF [condition]
     THEN [action]
     ELSE [alternative action]

2. [Step 2]
   FOR EACH [item] IN [collection]
     [action]

3. [Step 3]
   [Final action]
```

**Output**: [What's the result?]

**Example**:
```
Input: Time entry with description "Fix bug in login page"
Steps:
  1. Extract keywords: ["fix", "bug", "login"]
  2. Match keywords against tagging rules:
     - "bug" → Tag: "Bug Fix" (confidence: 90%)
     - "login" → Tag: "Authentication" (confidence: 85%)
  3. Apply tags if confidence > 80%
Output: Time entry tagged with "Bug Fix" and "Authentication"
```

#### Workflow 2: [Another workflow]
[Repeat structure above]

---

## External Integrations

### Integration 1: [e.g., Jira]

**Purpose**: [Why integrate with this system?]

**Authentication**:
- Method: [OAuth 2.0 | API Key | Basic Auth]
- Credentials Storage: [Where/how are credentials stored?]
- Token Refresh: [How are tokens refreshed, if applicable?]

**API Base URL**: [e.g., `https://your-domain.atlassian.net`]

**Required Permissions/Scopes**: [What permissions needed in external system?]

**API Endpoints Used**:
| Endpoint | Method | Purpose | Rate Limit |
|----------|--------|---------|------------|
| `/rest/api/3/project` | GET | Fetch projects | 60 req/min |
| `/rest/api/3/issue/{key}/worklog` | POST | Create worklog | 60 req/min |

**Data Mapping**:
| Clockify Field | External System Field | Transformation |
|----------------|----------------------|----------------|
| `description` | `worklog.comment` | Direct mapping |
| `timeInterval.duration` | `worklog.timeSpentSeconds` | Convert ISO duration to seconds |

**Error Handling**:
- Network errors: [Retry with backoff]
- Auth errors: [Prompt user to re-authenticate]
- Rate limits: [Queue requests, process later]

**Sync Strategy**:
- [ ] Real-time (on each webhook)
- [ ] Batch (every X minutes)
- [ ] Manual (user-initiated)

---

## Configuration

### Admin Settings

**Settings Panel** (visible in SETTINGS_SIDEBAR):

1. **Setting 1**: [e.g., "Enable auto-tagging"]
   - Type: [Boolean | String | Number | Select]
   - Default Value: [true]
   - Validation: [None | Regex | Min/Max]
   - Help Text: ["Automatically apply tags based on description keywords"]

2. **Setting 2**: [e.g., "Tagging rules"]
   - Type: [List of objects]
   - Schema:
     ```json
     {
       "pattern": "string (regex)",
       "tagId": "string",
       "enabled": "boolean"
     }
     ```
   - UI: [Table with add/edit/delete buttons]

3. **Setting 3**: [External API credentials]
   - Type: [Password/Secret]
   - Storage: [Encrypted in database]
   - Validation: [Test connection on save]

### Per-User Preferences

[Are there any user-specific settings?]
- Preference 1: [e.g., "Notification preferences"]
- Preference 2: [e.g., "Default tags"]

---

## Edge Cases & Error Handling

### Edge Case 1: [Scenario]
**Problem**: [What's the edge case?]

**Example**: "User creates time entry without project (project is optional in workspace)"

**Handling**:
- Detection: [How to detect this case?]
- Action: [What should addon do?]
- User Feedback: [What does user see?]

### Edge Case 2: Missing Required Data
**Problem**: API returns incomplete data

**Handling**:
- Log warning
- Skip processing for this item
- Continue with next item
- Show error in sidebar (if applicable)

### Edge Case 3: Rate Limit Exceeded
**Problem**: Too many API calls

**Handling**:
- Implement exponential backoff (2s, 4s, 8s, 16s, 32s)
- Queue requests for later processing
- Show "processing delayed" status to user

### Edge Case 4: Token Expired/Invalid
**Problem**: Stored installation token no longer works

**Handling**:
- Return HTTP 401 to webhook requests
- Show "Please reinstall addon" message in UI
- Log error for debugging

---

## Performance Requirements

**Expected Load**:
- Users per workspace: [e.g., 10-100]
- Webhook events per day: [e.g., 1,000-10,000]
- API calls per webhook: [e.g., 2-5]

**Optimization Strategies**:
- [ ] Cache frequently-accessed data (tags, projects) for 5 minutes
- [ ] Batch API calls when possible
- [ ] Use async processing for webhooks
- [ ] Implement request queuing with rate limiting

**Response Time Requirements**:
- Webhook processing: < 3 seconds
- UI component load: < 2 seconds
- Settings save: < 1 second

---

## Testing Requirements

### Unit Tests
- [ ] Test manifest structure validation
- [ ] Test lifecycle handlers (INSTALLED/DELETED)
- [ ] Test webhook event processing
- [ ] Test business logic functions
- [ ] Test API client error handling
- [ ] Test token storage/retrieval

### Integration Tests
- [ ] Test end-to-end INSTALLED flow
- [ ] Test webhook signature validation
- [ ] Test API calls with real/mocked Clockify API
- [ ] Test UI component data fetching

### Manual Testing Checklist
- [ ] Install addon in Clockify workspace
- [ ] Verify INSTALLED event stores token
- [ ] Create time entry, verify webhook received
- [ ] Open settings, verify UI loads
- [ ] Modify settings, verify saved correctly
- [ ] Uninstall addon, verify data cleaned up

---

## Deployment Requirements

### Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `ADDON_PORT` | No | Server port | `8080` |
| `ADDON_BASE_URL` | Yes | Public URL | `https://my-addon.com/addon` |
| `DATABASE_URL` | Yes (prod) | Database connection | `postgres://...` |
| `EXTERNAL_API_KEY` | Yes (if integration) | External service API key | `sk_live_...` |

### Infrastructure Requirements

**Minimum Requirements**:
- CPU: [e.g., 1 core]
- RAM: [e.g., 512 MB]
- Disk: [e.g., 1 GB]
- Network: [Outbound HTTPS to Clockify API]

**Scaling Strategy**:
- Horizontal: [Can run multiple instances?]
- Database: [Shared or per-instance?]
- Caching: [Redis? In-memory?]

### Deployment Steps

1. Build fat JAR: `mvn clean package`
2. Upload JAR to server
3. Set environment variables
4. Run: `java -jar addon.jar`
5. Verify health: `curl https://my-addon.com/addon/health`
6. Install in Clockify: `https://my-addon.com/addon/manifest.json`

---

## Documentation Requirements

### README.md Contents
- [ ] Addon description and features
- [ ] Installation instructions
- [ ] Configuration guide
- [ ] Troubleshooting section
- [ ] Development setup
- [ ] License

### User-Facing Documentation
- [ ] Getting started guide
- [ ] Feature explanations with screenshots
- [ ] Common use cases
- [ ] FAQ

---

## Security Considerations

### Data Security
- [ ] All secrets in environment variables (never hardcoded)
- [ ] Installation tokens encrypted in database
- [ ] Webhook signatures validated
- [ ] JWT tokens verified for UI requests
- [ ] No sensitive data logged

### Network Security
- [ ] HTTPS only in production
- [ ] Rate limiting on all endpoints
- [ ] Input validation on all user inputs
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (sanitize HTML output)

### Compliance
- [ ] GDPR compliance (data deletion on uninstall)
- [ ] Data retention policy documented
- [ ] Privacy policy link in manifest

---

## Success Criteria

**Addon is considered successful if**:
1. [Criterion 1 - e.g., "Reduces manual tagging time by 50%"]
2. [Criterion 2 - e.g., "Achieves 95% tagging accuracy"]
3. [Criterion 3 - e.g., "Zero critical bugs in first 30 days"]

**Launch Checklist**:
- [ ] All unit tests passing
- [ ] Integration tests passing
- [ ] Manual testing completed
- [ ] Documentation complete
- [ ] Security review passed
- [ ] Performance requirements met
- [ ] Beta testing with 5+ users completed
- [ ] Monitoring and alerting configured

---

## References

**Related Addons**: [Link to similar addons for inspiration]

**External Documentation**: [Links to external API docs if applicable]

**Design Mockups**: [Links to Figma, screenshots, etc.]

---

**Template Version**: 1.0.0
**Last Updated**: 2025-11-08
**Author**: [Your name]
**Estimated Complexity**: [Simple | Medium | Advanced]
**Estimated Development Time**: [e.g., "2-3 days"]
