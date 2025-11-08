# Rules Add-on

Declarative automation rules for Clockify that automatically modify time entries based on conditions.

## Overview

The Rules add-on allows workspace administrators to define automation rules that evaluate time entries when they are created or updated. When a time entry matches the specified conditions, the configured actions are automatically applied.

### Key Features

- **Flexible Conditions**: Support for description matching, tag checking, project filtering, and billable status
- **Logical Operators**: Combine conditions using AND/OR logic
- **Automated Actions**: Add/remove tags, set descriptions, update billable status
- **Real-time Processing**: Evaluates rules on TIME_ENTRY_CREATED and TIME_ENTRY_UPDATED webhook events
- **Per-Workspace Rules**: Each workspace maintains its own set of rules
- **Security**: Webhook signature verification, CSP headers, optional rate limiting and CORS

## Rule Schema

A rule consists of:

```json
{
  "id": "optional-uuid",
  "name": "Rule name",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {
      "type": "conditionType",
      "operator": "EQUALS",
      "value": "someValue"
    }
  ],
  "actions": [
    {
      "type": "actionType",
      "args": {
        "key": "value"
      }
    }
  ]
}
```

### Supported Conditions

| Type | Description | Example |
|------|-------------|---------|
| `descriptionContains` | Check if description contains text | `{"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"}` |
| `descriptionEquals` | Check if description matches exactly | `{"type": "descriptionEquals", "operator": "EQUALS", "value": "Daily standup"}` |
| `hasTag` | Check if time entry has a specific tag ID | `{"type": "hasTag", "operator": "EQUALS", "value": "client-tag-id"}` |
| `projectIdEquals` | Check if project ID matches | `{"type": "projectIdEquals", "operator": "EQUALS", "value": "project-123"}` |
| `isBillable` | Check billable status | `{"type": "isBillable", "operator": "EQUALS", "value": "true"}` |

### Supported Operators

- `EQUALS` - Exact match
- `NOT_EQUALS` - Does not match
- `CONTAINS` - Contains substring (for descriptions)
- `NOT_CONTAINS` - Does not contain substring
- `IN` - In a list
- `NOT_IN` - Not in a list

### Supported Actions

| Type | Description | Example |
|------|-------------|---------|
| `add_tag` | Add a tag to the time entry | `{"type": "add_tag", "args": {"tag": "billable"}}` |
| `remove_tag` | Remove a tag from the time entry | `{"type": "remove_tag", "args": {"tag": "personal"}}` |
| `set_description` | Set the description | `{"type": "set_description", "args": {"value": "Client work"}}` |
| `set_billable` | Set billable status | `{"type": "set_billable", "args": {"value": "true"}}` |

## API Endpoints

### List Rules

```bash
GET /rules/api/rules?workspaceId=YOUR_WORKSPACE_ID
```

Returns all rules for the workspace.

### Create/Update Rule

```bash
POST /rules/api/rules?workspaceId=YOUR_WORKSPACE_ID
Content-Type: application/json

{
  "name": "Tag client meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"},
    {"type": "hasTag", "operator": "EQUALS", "value": "client"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}
```

If `id` is present in the payload, the rule will be updated; otherwise, a new rule is created.

### Delete Rule

```bash
DELETE /rules/api/rules/{ruleId}?workspaceId=YOUR_WORKSPACE_ID
```

Deletes the specified rule.

## Example Rules

### Tag all client meetings as billable

```json
{
  "name": "Tag client meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"},
    {"type": "hasTag", "operator": "EQUALS", "value": "client"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}
```

### Auto-tag urgent work

```json
{
  "name": "Tag urgent work",
  "enabled": true,
  "combinator": "OR",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "urgent"},
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "asap"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "priority"}}
  ]
}
```

### Mark specific project as billable

```json
{
  "name": "Mark project work as billable",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "projectIdEquals", "operator": "EQUALS", "value": "project-abc-123"}
  ],
  "actions": [
    {"type": "set_billable", "args": {"value": "true"}}
  ]
}
```

## Installation

### 1. Build the Add-on

```bash
make build-rules
```

### 2. Run Locally

```bash
make run-rules
```

This starts the add-on on `http://localhost:8080/rules`.

### 3. Expose via ngrok

In another terminal:

```bash
ngrok http 8080
```

Note the ngrok URL (e.g., `https://abc123.ngrok-free.app`).

### 4. Install in Clockify

1. Go to Clockify Admin > Add-ons
2. Click "Install Custom Add-on"
3. Enter the manifest URL: `https://abc123.ngrok-free.app/rules/manifest.json`
4. Approve the requested permissions
5. The add-on will receive an INSTALLED webhook with the workspace token

## Environment Variables

### Required

- `ADDON_PORT` - Server port (default: 8080)
- `ADDON_BASE_URL` - Base URL for the add-on (e.g., `http://localhost:8080/rules`)

### Optional - Security

- `ADDON_FRAME_ANCESTORS` - CSP frame-ancestors value (e.g., `'https://app.clockify.me'`)
- `ADDON_RATE_LIMIT` - Requests per second (e.g., `10.0`)
- `ADDON_LIMIT_BY` - Rate limit by `ip` or `workspace` (default: `ip`)
- `ADDON_CORS_ORIGINS` - Comma-separated list of allowed origins
- `ADDON_REQUEST_LOGGING` - Enable request logging (`true` or `1`)

### Optional - Development

- `CLOCKIFY_WORKSPACE_ID` - Preload workspace ID for local testing
- `CLOCKIFY_INSTALLATION_TOKEN` - Preload installation token for local testing
- `CLOCKIFY_API_BASE_URL` - Custom Clockify API base URL

## Security

The Rules add-on implements several security measures:

### 1. Webhook Signature Verification

All webhook requests are verified using HMAC-SHA256:

- The installation token is used as the signing key
- Signatures are compared using constant-time comparison
- Invalid signatures result in 401/403 responses

### 2. Security Headers

The add-on sets the following security headers:

- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Strict-Transport-Security` (for HTTPS)
- `Content-Security-Policy` (frame-ancestors, configurable via env)

### 3. Rate Limiting (Optional)

Enable rate limiting to prevent abuse:

```bash
ADDON_RATE_LIMIT=10.0 ADDON_LIMIT_BY=workspace make run-rules
```

### 4. CORS (Optional)

Restrict origins that can call the API:

```bash
ADDON_CORS_ORIGINS=https://app.clockify.me make run-rules
```

## Production Considerations

### 1. Persistent Storage

The demo implementation uses in-memory storage for both rules and tokens. For production:

- Migrate to a database-backed `RulesStore` (PostgreSQL, MySQL, etc.)
- Use `DatabaseTokenStore` from the SDK for token persistence
- Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables

### 2. High Availability

- Deploy multiple instances behind a load balancer
- Use database-backed storage for state sharing
- Implement health checks (`/health` endpoint)

### 3. Monitoring

- Enable structured logging (`LOG_APPENDER=JSON`)
- Monitor webhook processing times
- Track rule evaluation metrics
- Set up alerts for signature validation failures

### 4. API Integration

The current implementation logs actions but does not apply them via the Clockify API. To enable full automation:

1. Create a `ClockifyApiClient` similar to auto-tag-assistant
2. In `WebhookHandlers`, call the API to apply actions:
   - `PUT /workspaces/{workspaceId}/time-entries/{timeEntryId}` to update entries
   - Include the installation token in the `X-Auth-Token` header
3. Handle idempotency (e.g., don't re-add existing tags)
4. Implement retry logic for transient failures

## Testing

Run the test suite:

```bash
mvn -q -pl addons/rules test
```

Coverage report:

```bash
open addons/rules/target/site/jacoco/index.html
```

## Troubleshooting

### Rules not triggering

1. Check that the rule is enabled: `"enabled": true`
2. Verify the workspace ID matches
3. Ensure webhook signature validation passes (check logs)
4. Test the rule conditions manually

### Webhook signature failures

1. Verify the installation token is stored correctly
2. Check that the token matches what Clockify sent during INSTALLED event
3. Ensure the raw request body is used for signature verification

### Missing rules

1. Rules are stored in-memory by default
2. Restarting the add-on will clear all rules
3. For persistence, implement database-backed storage

## License

Part of the Clockify Add-on Boilerplate project.
