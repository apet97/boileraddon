# Rules Add-on - Quick Start Guide

Get the Clockify Rules Add-on running locally in **5 minutes**.

## What You Get

The Rules Add-on provides **declarative automation** for Clockify time entries:

- **Triggers**: Time entry events (created, updated)
- **Conditions**: Check description, tags, project, client, billable status
- **Actions**: Add/remove tags, set description, change project, set billable
- **IFTTT Mode**: Advanced automations with any webhook + OpenAPI actions

## Prerequisites

- Java 17+ (`java -version`)
- Maven 3.6+ (`mvn -version`)
- ngrok installed (`ngrok version`)
- Clockify workspace with admin access

## Quick Start

### Step 1: Build the Add-on (30 seconds)

```bash
cd /Users/15x/Downloads/boileraddon-main
mvn clean package -pl addons/rules -am -DskipTests
```

**Output**: `addons/rules/target/rules-0.1.0-jar-with-dependencies.jar` (~14MB)

### Step 2: Start ngrok (in a new terminal)

```bash
ngrok http 8080
```

**Copy the HTTPS URL** from the output (e.g., `https://abc123.ngrok-free.app`)

### Step 3: Set Base URL

```bash
export ADDON_BASE_URL=https://abc123.ngrok-free.app/rules
```

Replace `abc123.ngrok-free.app` with your actual ngrok URL.

### Step 4: Run the Add-on

```bash
./scripts/run-rules-local.sh
```

**What you'll see**:
```
========================================
  Clockify Rules Add-on - Local Setup
========================================

Configuration:
  Base URL:       https://abc123.ngrok-free.app/rules
  Port:           8080
  Apply Changes:  false
  Skip Signature: false
  Use Database:   false

Endpoints:
  Manifest:       https://abc123.ngrok-free.app/rules/manifest.json
  Settings UI:    https://abc123.ngrok-free.app/rules/settings
  IFTTT UI:       https://abc123.ngrok-free.app/rules/ifttt
  Health:         https://abc123.ngrok-free.app/rules/health

========================================
Starting Rules Add-on...
========================================
```

### Step 5: Install in Clockify

1. Open Clockify: **Admin > Add-ons > Install Custom Add-on**
2. Enter manifest URL: `https://abc123.ngrok-free.app/rules/manifest.json`
3. Review scopes and click **Install**

### Step 6: Create Your First Rule

**Option A: Use the UI**

1. Open: `https://abc123.ngrok-free.app/rules/settings`
2. Click **"Add Rule"**
3. Set conditions: `Description contains "meeting"`
4. Set actions: `Add tag "meetings"`
5. Save

**Option B: Use the API**

```bash
curl -X POST 'http://localhost:8080/rules/api/rules?workspaceId=<your-workspace-id>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Tag meeting entries",
    "enabled": true,
    "combinator": "AND",
    "conditions": [
      {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"}
    ],
    "actions": [
      {"type": "add_tag", "args": {"tag": "meetings"}}
    ]
  }'
```

### Step 7: Test the Rule (Dry-Run)

```bash
curl -X POST 'http://localhost:8080/rules/api/test' \
  -H 'Content-Type: application/json' \
  -d '{
    "workspaceId": "<your-workspace-id>",
    "timeEntry": {
      "id": "test-1",
      "description": "Team meeting",
      "tagIds": []
    }
  }'
```

**Expected output**:
```json
{
  "status": "actions_logged",
  "actionsCount": 1,
  "actions": [
    {"type": "add_tag", "args": {"tag": "meetings"}}
  ]
}
```

## Test in Clockify

1. Create a time entry with "meeting" in the description
2. Check the addon logs (terminal output)
3. Verify the action is logged: `"Would add tag: meetings"`

**Note**: By default, the addon runs in **log-only mode** (safe for testing). It won't actually modify Clockify data until you enable apply mode.

## Enable Mutations (Apply Changes)

Once you've tested and verified rules work correctly:

```bash
# Stop the addon (Ctrl+C)

# Restart with apply mode
./scripts/run-rules-local.sh --apply
```

Or set the environment variable:

```bash
export RULES_APPLY_CHANGES=true
./scripts/run-rules-local.sh
```

Now the addon will **actually modify** time entries based on your rules.

## Advanced Options

### Use Database Storage

```bash
# Start PostgreSQL (via Docker)
docker run -d \
  --name postgres-rules \
  -e POSTGRES_DB=addons \
  -e POSTGRES_USER=addons \
  -e POSTGRES_PASSWORD=addons \
  -p 5432:5432 \
  postgres:16-alpine

# Set database environment variables
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USERNAME=addons
export DB_PASSWORD=addons

# Run with database flag
./scripts/run-rules-local.sh --db --apply
```

### Skip Signature Verification (Dev Only)

```bash
./scripts/run-rules-local.sh --skip-signature
```

**Warning**: Only use in development. Never in production.

### Change Port

```bash
./scripts/run-rules-local.sh --port 9090
```

Don't forget to update ngrok: `ngrok http 9090`

## Verify Health

```bash
curl http://localhost:8080/rules/health
```

**Expected**:
```json
{
  "status": "healthy",
  "addon": "rules",
  "version": "0.1.0"
}
```

## Check Status

```bash
curl 'http://localhost:8080/rules/status?workspaceId=<your-workspace-id>'
```

**Expected**:
```json
{
  "workspaceId": "...",
  "tokenPresent": true,
  "applyChanges": false,
  "skipSignatureVerify": false,
  "baseUrl": "https://abc123.ngrok-free.app/rules"
}
```

## UI Builders

### Classic Rules Builder
**URL**: `https://abc123.ngrok-free.app/rules/settings`

Simple interface for time-entry focused automations.

### IFTTT Builder
**URL**: `https://abc123.ngrok-free.app/rules/ifttt`

Advanced builder with:
- Any Clockify webhook event as trigger
- JSONPath conditions for filtering
- OpenAPI actions (any Clockify API endpoint)
- Placeholder templating: `{{field.path}}`

## Troubleshooting

### "ngrok is not running"

Start ngrok in a separate terminal: `ngrok http 8080`

### "ADDON_BASE_URL not set"

```bash
export ADDON_BASE_URL=https://your-ngrok-url.ngrok-free.app/rules
```

### "JAR not found"

Build the addon: `mvn clean package -pl addons/rules -am -DskipTests`

### Rules not triggering

1. Check addon logs for webhook events
2. Verify rule is enabled: `GET /api/rules?workspaceId=<ws>`
3. Check apply mode: `GET /status?workspaceId=<ws>`
4. Test with dry-run: `POST /api/test`

### Can't access UI

1. Verify ngrok URL is correct
2. Check addon is running on port 8080
3. Test health endpoint: `curl http://localhost:8080/rules/health`

## Next Steps

1. **Create more rules** - Use UI or API
2. **Test IFTTT builder** - Advanced automations
3. **Enable database** - Persistent storage
4. **Deploy to production** - Use real domain, not ngrok

## Documentation

- **Full API Reference**: `/docs/ADDON_RULES.md`
- **IFTTT Guide**: `/docs/WEBHOOK_IFTTT.md`
- **Testing Guide**: `/docs/TESTING_GUIDE.md`
- **Troubleshooting**: `/docs/TROUBLESHOOTING.md`

## Example Rules

### Tag all billable entries
```json
{
  "name": "Tag billable work",
  "enabled": true,
  "conditions": [
    {"type": "isBillable", "operator": "EQUALS", "value": "true"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}
```

### Set project for client entries
```json
{
  "name": "Auto-assign ACME project",
  "enabled": true,
  "conditions": [
    {"type": "clientNameContains", "operator": "CONTAINS", "value": "ACME"}
  ],
  "actions": [
    {"type": "set_project_by_name", "args": {"projectName": "ACME Consulting"}}
  ]
}
```

### Tag and make billable for important clients
```json
{
  "name": "Important client workflow",
  "enabled": true,
  "combinator": "OR",
  "conditions": [
    {"type": "clientNameContains", "operator": "CONTAINS", "value": "ACME"},
    {"type": "clientNameContains", "operator": "CONTAINS", "value": "TechCorp"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "priority"}},
    {"type": "set_billable", "args": {"billable": "true"}}
  ]
}
```

---

**Ready to automate?** Run `./scripts/run-rules-local.sh` and start creating rules! 🚀
