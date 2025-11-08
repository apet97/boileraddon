# Clockify Data Models Reference

**Complete entity schemas and field definitions**

This document provides detailed schemas for all major Clockify entities, based on real API responses and the official OpenAPI specification.

## Table of Contents

- [Workspace](#workspace)
- [Project](#project)
- [Tag](#tag)
- [Client](#client)
- [Time Entry](#time-entry)
- [Task](#task)
- [User](#user)
- [Custom Field](#custom-field)
- [Webhook Event Payloads](#webhook-event-payloads)
- [Lifecycle Event Payloads](#lifecycle-event-payloads)

---

## Workspace

Represents a Clockify workspace where users track time.

### Schema

```typescript
interface Workspace {
  id: string;                          // Unique workspace ID (24-char hex)
  name: string;                        // Workspace name
  hourlyRate: MoneyAmount | null;      // Default hourly rate
  costRate: MoneyAmount | null;        // Default cost rate
  memberships: Membership[];           // Workspace members
  workspaceSettings: WorkspaceSettings; // Workspace configuration
  imageUrl: string;                    // Workspace logo URL
  featureSubscriptionType: string;     // Subscription plan type
  features: string[];                  // Enabled features
  currencies: Currency[];              // Available currencies
  subdomain: Subdomain;                // Subdomain settings
  cakeOrganizationId: string;          // CAKE organization ID
}

interface WorkspaceSettings {
  timeRoundingInReports: boolean;
  onlyAdminsSeeBillableRates: boolean;
  activeBillableHours: boolean;
  onlyAdminsCanChangeBillableStatus: boolean;
  onlyAdminsCreateProject: boolean;
  onlyAdminsSeeDashboard: boolean;
  defaultBillableProjects: boolean;
  lockTimeEntries: string | null;
  lockTimeZone: string | null;
  round: RoundSettings;
  projectFavorites: boolean;
  canSeeTimeSheet: boolean;
  canSeeTracker: boolean;
  projectPickerSpecialFilter: boolean;
  forceProjects: boolean;
  forceTasks: boolean;
  forceTags: boolean;
  forceDescription: boolean;
  onlyAdminsSeeAllTimeEntries: boolean;
  onlyAdminsSeePublicProjectsEntries: boolean;
  trackTimeDownToSecond: boolean;
  projectGroupingLabel: string;
  adminOnlyPages: string[];
  automaticLock: any | null;
  onlyAdminsCreateTag: boolean;
  onlyAdminsCreateTask: boolean;
  timeTrackingMode: string;
  multiFactorEnabled: boolean;
  numberFormat: string;
  currencyFormat: string;
  durationFormat: string;
  entityCreationPermissions: EntityCreationPermissions;
  isProjectPublicByDefault: boolean;
}
```

### Example

```json
{
  "id": "68adfddad138cb5f24c63b22",
  "name": "WEBHOOKS",
  "hourlyRate": {
    "amount": 2000,
    "currency": "GPB"
  },
  "workspaceSettings": {
    "forceProjects": true,
    "forceTasks": false,
    "forceTags": false,
    "forceDescription": false,
    "trackTimeDownToSecond": true
  },
  "features": ["TIME_TRACKING", "APPROVAL", "CUSTOM_FIELDS"],
  "featureSubscriptionType": "BUNDLE_YEAR_2024"
}
```

---

## Project

Represents a project within a workspace.

### Schema

```typescript
interface Project {
  id: string;                   // Project ID
  name: string;                 // Project name
  workspaceId: string;          // Parent workspace ID
  clientId: string | null;      // Associated client ID
  clientName?: string;          // Client name (read-only)
  billable: boolean;            // Default billable status
  color: string;                // Hex color code
  archived: boolean;            // Is archived
  public: boolean;              // Is public (visible to all)
  template: boolean;            // Is project template
  note: string;                 // Project description
  duration: string;             // Total tracked time (ISO 8601 duration)
  hourlyRate: MoneyAmount | null;
  costRate: MoneyAmount | null;
  timeEstimate: TimeEstimate;
  budgetEstimate: BudgetEstimate | null;
  memberships: Membership[];    // Project members
  estimate: Estimate;           // Deprecated, use timeEstimate
}

interface TimeEstimate {
  estimate: string;             // ISO 8601 duration (e.g., "PT10H")
  type: "MANUAL" | "AUTO";
  resetOption: string | null;
  active: boolean;
  includeNonBillable: boolean;
}

interface BudgetEstimate {
  estimate: number;             // Amount in cents
  type: "MANUAL" | "AUTO";
  resetOption: string | null;
  active: boolean;
  includeExpenses: boolean;
}
```

### Example

```json
{
  "id": "68d1e16ef43fa22cf82c1724",
  "name": "API Discovery Project",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "clientId": "68d1e16e8ac1033711a69680",
  "clientName": "API Discovery Client",
  "billable": false,
  "color": "#2196F3",
  "archived": false,
  "public": true,
  "template": false,
  "hourlyRate": {
    "amount": 22200,
    "currency": "GPB"
  },
  "timeEstimate": {
    "estimate": "PT3H",
    "type": "MANUAL",
    "active": false,
    "includeNonBillable": true
  }
}
```

### Field Validation Rules

- `name`: Required, 1-250 characters
- `color`: Must be valid hex color (e.g., `#FF5722`)
- `billable`: Defaults to workspace setting
- `public`: Defaults to workspace setting

---

## Tag

Represents a tag that can be applied to time entries.

### Schema

```typescript
interface Tag {
  id: string;          // Tag ID
  name: string;        // Tag name
  workspaceId: string; // Parent workspace ID
  archived: boolean;   // Is archived
}
```

### Example

```json
{
  "id": "68d02fdf93acc646ebc1c6db",
  "name": "Sprint1",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "archived": false
}
```

### Field Validation Rules

- `name`: Required, 1-100 characters, must be unique per workspace

---

## Client

Represents a client associated with projects.

### Schema

```typescript
interface Client {
  id: string;               // Client ID
  name: string;             // Client name
  email: string | null;     // Client email
  ccEmails: string[] | null; // CC emails for invoices
  workspaceId: string;      // Parent workspace ID
  archived: boolean;        // Is archived
  address: string | null;   // Physical address
  note: string | null;      // Notes about client
  currencyId: string;       // Currency ID
  currencyCode: string;     // Currency code (e.g., "USD")
}
```

### Example

```json
{
  "id": "68d1e16e8ac1033711a69680",
  "name": "API Discovery Client",
  "email": null,
  "ccEmails": null,
  "workspaceId": "68adfddad138cb5f24c63b22",
  "archived": false,
  "address": null,
  "note": null,
  "currencyCode": "GPB"
}
```

### Field Validation Rules

- `name`: Required, 1-250 characters
- `email`: Must be valid email format if provided

---

## Time Entry

Represents a time tracking entry.

### Schema

```typescript
interface TimeEntry {
  id: string;                      // Time entry ID
  description: string;             // Entry description
  userId: string;                  // User who created entry
  workspaceId: string;             // Workspace ID
  projectId: string | null;        // Associated project ID
  projectName?: string;            // Project name (webhook only)
  taskId: string | null;           // Associated task ID
  taskName?: string | null;        // Task name (webhook only)
  tagIds: string[] | null;         // Applied tag IDs
  billable: boolean;               // Is billable
  timeInterval: TimeInterval;      // Start, end, duration
  customFieldValues: CustomFieldValue[];
  type: "REGULAR" | "TIME_OFF" | "BREAK";
  kioskId: string | null;          // Kiosk ID if tracked via kiosk
  hourlyRate: MoneyAmount;
  costRate: MoneyAmount;
  isLocked: boolean;               // Is locked for editing
  userName?: string;               // User name (webhook only)
  userEmail?: string;              // User email (webhook only)
}

interface TimeInterval {
  start: string;                   // ISO 8601 timestamp
  end: string | null;              // ISO 8601 timestamp (null if running)
  duration: string | null;         // ISO 8601 duration (null if running)
}

interface CustomFieldValue {
  customFieldId: string;
  timeEntryId: string;
  value: string | number | boolean;
  name: string;
  type: "TXT" | "NUMBER" | "CHECKBOX" | "LINK";
}
```

### Example

```json
{
  "id": "69017c7cf249396a237cfcce",
  "description": "Working on feature implementation",
  "userId": "64621faec4d2cc53b91fce6c",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "projectId": "68ffbce07bde82688ecb38fd",
  "taskId": null,
  "tagIds": ["68d02fdf93acc646ebc1c6db"],
  "billable": true,
  "timeInterval": {
    "start": "2025-10-29T02:31:00Z",
    "end": "2025-10-29T04:31:00Z",
    "duration": "PT2H"
  },
  "customFieldValues": [],
  "type": "REGULAR",
  "hourlyRate": {
    "amount": 20000,
    "currency": "GPB"
  },
  "isLocked": false
}
```

### Field Validation Rules

- `description`: Max 3000 characters
- `timeInterval.start`: Required, ISO 8601 format
- `timeInterval.end`: Can be null for running timer
- `projectId`: Required if workspace has `forceProjects: true`
- `taskId`: Required if workspace has `forceTasks: true`
- `tagIds`: Required if workspace has `forceTags: true`

---

## Task

Represents a task within a project.

### Schema

```typescript
interface Task {
  id: string;               // Task ID
  name: string;             // Task name
  projectId: string;        // Parent project ID
  workspaceId: string;      // Workspace ID
  assigneeIds: string[];    // Assigned user IDs
  assigneeId: string | null; // Deprecated, use assigneeIds
  estimate: string;         // Time estimate (ISO 8601 duration)
  status: "ACTIVE" | "DONE";
  duration: string;         // Total tracked time
  billable: boolean;
  hourlyRate: MoneyAmount | null;
  costRate: MoneyAmount | null;
  budgetEstimate: number;
}
```

### Example

```json
{
  "id": "68ffbd107bde82688ecb3a21",
  "name": "Authentication Module",
  "projectId": "68ffbce07bde82688ecb38fd",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "assigneeIds": ["64621faec4d2cc53b91fce6c"],
  "estimate": "PT8H",
  "status": "ACTIVE",
  "duration": "PT3H",
  "billable": true
}
```

---

## User

Represents a Clockify user.

### Schema

```typescript
interface User {
  id: string;                  // User ID
  email: string;               // User email
  name: string;                // Display name
  memberships: Membership[];   // Workspace memberships
  profilePicture: string;      // Profile picture URL
  activeWorkspace: string;     // Currently active workspace ID
  defaultWorkspace: string;    // Default workspace ID
  settings: UserSettings;
  status: "ACTIVE" | "INACTIVE" | "PENDING";
  customFields: CustomField[];
}

interface UserSettings {
  weekStart: "MONDAY" | "SUNDAY";
  timeZone: string;            // IANA timezone (e.g., "Europe/Belgrade")
  timeFormat: "HOUR12" | "HOUR24";
  dateFormat: string;          // e.g., "MM/DD/YYYY"
  sendNewsletter: boolean;
  weeklyUpdates: boolean;
  longRunning: boolean;
  scheduledReports: boolean;
  approval: boolean;
  pto: boolean;
  alerts: boolean;
  reminders: boolean;
  timeTrackingManual: boolean;
  summaryReportSettings: SummaryReportSettings;
  isCompactViewOn: boolean;
  dashboardSelection: "ME" | "TEAM";
  dashboardViewType: "PROJECT" | "BILLABILITY";
  theme: "DEFAULT" | "DARK";
  lang: string;                // e.g., "EN"
}
```

### Example

```json
{
  "id": "64621faec4d2cc53b91fce6c",
  "email": "user@example.com",
  "name": "John Doe",
  "activeWorkspace": "68adfddad138cb5f24c63b22",
  "defaultWorkspace": "68adfddad138cb5f24c63b22",
  "settings": {
    "weekStart": "MONDAY",
    "timeZone": "Europe/Belgrade",
    "timeFormat": "HOUR12",
    "dateFormat": "MM/DD/YYYY"
  },
  "status": "ACTIVE"
}
```

---

## Custom Field

Represents a custom field definition.

### Schema

```typescript
interface CustomField {
  id: string;
  name: string;
  workspaceId: string;
  type: "TXT" | "NUMBER" | "CHECKBOX" | "LINK";
  allowedValues?: string[];    // For dropdown types
  required: boolean;
  status: "ACTIVE" | "INACTIVE";
  projectDefaultValues?: any[];
}
```

---

## Webhook Event Payloads

### NEW_TIME_ENTRY / TIME_ENTRY_UPDATED

```typescript
interface WebhookTimeEntryEvent {
  workspaceId: string;
  userId: string;
  timeEntryId: string;
  event: "NEW_TIME_ENTRY" | "TIME_ENTRY_UPDATED" | "TIMER_STOPPED";
  timestamp: string;           // ISO 8601
  timeEntry: TimeEntry & {
    projectName?: string;
    taskName?: string | null;
    userName?: string;
    userEmail?: string;
  };
  changes?: {                  // Only in TIME_ENTRY_UPDATED
    [field: string]: {
      oldValue: any;
      newValue: any;
    };
  };
}
```

### NEW_TIMER_STARTED

```typescript
interface WebhookTimerStartedEvent {
  workspaceId: string;
  userId: string;
  timeEntryId: string;
  event: "NEW_TIMER_STARTED";
  timestamp: string;
  timeEntry: TimeEntry & {
    timeInterval: {
      start: string;
      end: null;               // Always null for running timer
      duration: null;          // Always null for running timer
    };
  };
}
```

### TIME_ENTRY_DELETED

```typescript
interface WebhookTimeEntryDeletedEvent {
  workspaceId: string;
  userId: string;
  timeEntryId: string;
  event: "TIME_ENTRY_DELETED";
  timestamp: string;
  timeEntry: {                 // Minimal info only
    id: string;
    description: string;
    projectId: string | null;
    timeInterval: TimeInterval;
  };
}
```

---

## Lifecycle Event Payloads

### INSTALLED

```typescript
interface InstalledEvent {
  event: "INSTALLED";
  workspaceId: string;
  userId: string;              // User who installed addon
  timestamp: string;           // ISO 8601
  installationToken: string;   // JWT token for API calls
  context: {
    workspaceName: string;
    userEmail: string;
    userName: string;
  };
}
```

### DELETED

```typescript
interface DeletedEvent {
  event: "DELETED";
  workspaceId: string;
  userId: string;              // User who uninstalled addon
  timestamp: string;
  context: {
    workspaceName: string;
    userEmail: string;
  };
}
```

---

## Common Types

### MoneyAmount

```typescript
interface MoneyAmount {
  amount: number;              // Amount in cents (100 = $1.00)
  currency: string;            // Currency code (e.g., "USD", "EUR", "GPB")
}
```

### Membership

```typescript
interface Membership {
  userId: string;
  hourlyRate: MoneyAmount | null;
  costRate: MoneyAmount | null;
  targetId: string;            // Workspace or project ID
  membershipType: "WORKSPACE" | "PROJECT";
  membershipStatus: "ACTIVE" | "INACTIVE" | "PENDING";
}
```

### Estimate

```typescript
interface Estimate {
  estimate: string;            // ISO 8601 duration (e.g., "PT10H30M")
  type: "MANUAL" | "AUTO";
}
```

### Currency

```typescript
interface Currency {
  id: string;
  code: string;                // ISO 4217 code (e.g., "USD")
  isDefault: boolean;
}
```

### Subdomain

```typescript
interface Subdomain {
  name: string | null;
  enabled: boolean;
}
```

---

## ISO 8601 Duration Format

Clockify uses ISO 8601 duration format for time values:

| Duration | ISO 8601 Format |
|----------|-----------------|
| 30 minutes | `PT30M` |
| 1 hour | `PT1H` |
| 1.5 hours | `PT1H30M` |
| 2 hours | `PT2H` |
| 8 hours | `PT8H` |
| 0 seconds | `PT0S` |

### Parsing in Java

```java
import java.time.Duration;

Duration duration = Duration.parse("PT2H30M");
long seconds = duration.getSeconds();        // 9000
long minutes = duration.toMinutes();         // 150
long hours = duration.toHours();             // 2
```

---

## ISO 8601 Timestamp Format

Clockify uses ISO 8601 format for timestamps (UTC):

```
2025-10-29T02:31:00Z
```

### Parsing in Java

```java
import java.time.Instant;
import java.time.ZonedDateTime;

Instant instant = Instant.parse("2025-10-29T02:31:00Z");
ZonedDateTime zdt = ZonedDateTime.parse("2025-10-29T02:31:00Z");
```

---

## Entity Relationships

```
Workspace
  ├─ Projects
  │   ├─ Tasks
  │   └─ Time Entries
  ├─ Clients
  ├─ Tags
  ├─ Custom Fields
  └─ Users (Memberships)

Time Entry
  ├─ belongs to User
  ├─ belongs to Workspace
  ├─ optionally belongs to Project
  ├─ optionally belongs to Task
  └─ can have multiple Tags
```

---

## Field Constraints Summary

| Entity | Required Fields | Max Length | Unique Constraints |
|--------|----------------|------------|-------------------|
| Project | name, workspaceId | name: 250 | name per workspace |
| Tag | name, workspaceId | name: 100 | name per workspace |
| Client | name, workspaceId | name: 250 | name per workspace |
| Time Entry | start, workspaceId, userId | description: 3000 | - |
| Task | name, projectId | name: 250 | name per project |

---

## Additional Resources

- [OpenAPI Specification](../dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json) - Complete API schema
- [API Cookbook](API-COOKBOOK.md) - API usage examples
- [Request/Response Examples](REQUEST-RESPONSE-EXAMPLES.md) - Full HTTP exchanges
- [Quick Reference](QUICK-REFERENCE.md) - Cheat sheet
