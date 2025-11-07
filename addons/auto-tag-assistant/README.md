# Auto-Tag Assistant

A Clockify add-on that automatically manages tags on time entries, ensuring all entries are properly categorized.

## Overview

**Auto-Tag Assistant** monitors time entry events (start, stop, create, update) and checks if required tags are present. When tags are missing, it can automatically add default tags or notify users to add appropriate tags.

### How it Works

When a user starts or stops a time entry:

1. **Webhook Event Received**: Clockify sends a webhook event (`NEW_TIMER_STARTED`, `TIMER_STOPPED`, `TIME_ENTRY_UPDATED`, or `NEW_TIME_ENTRY`)
2. **Tag Detection**: The add-on parses the time entry and checks the `tagIds` array
3. **Missing Tag Detection**: If `tagIds` is empty or doesn't contain required tags
4. **Auto-Tagging**: The add-on would add a default tag (e.g., "Untagged")

> **Note**: The current implementation logs tag detection. To enable full auto-tagging, you need to:
> - Store the addon token from the `INSTALLED` lifecycle event in a database
> - Use `ClockifyClient` methods to fetch workspace tags
> - Update time entries via the Clockify API

## Features

- 🏷️ **Automatic Tag Detection**: Monitors all time entry events
- ⚙️ **Configurable Rules**: Admin UI for managing tag requirements
- 🔄 **Real-time Processing**: Webhook-based instant response
- 🎯 **Multiple Event Support**: Works with timer start, stop, and manual entries
- 🔐 **Workspace Scoped**: Each workspace has independent configuration

## Architecture

### Components

- **Webhook Handler** (`WebhookHandlers.java`): Processes time entry events
- **Lifecycle Handler** (`LifecycleHandlers.java`): Manages installation and configuration
- **Clockify Client** (`ClockifyClient.java`): API wrapper for Clockify operations
- **Settings UI** (`settings.html`): React-based configuration interface

### Event Flow

```
Clockify Event → Webhook → Tag Detection → (Optional) Auto-Tag → API Update
```

### Lifecycle Flow

```
Install → Store Token → Configure Rules → Process Events → Uninstall
```

## Prerequisites

- Java 17+
- Maven 3.6+
- GitHub Packages authentication configured (see below)
- ngrok (for local testing)

## GitHub Packages Setup

The addon-java-sdk is hosted on GitHub Packages. Configure authentication:

```bash
# Create ~/.m2/settings.xml with:
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

Generate a GitHub token with `read:packages` scope at: https://github.com/settings/tokens

## Local Development

### 1. Build the Add-on

```bash
A working Clockify add-on that detects missing tags on time entries and provides a foundation for automatic tagging logic.

## What It Does

- **Monitors time entry events**: Receives webhooks when timers start/stop or entries are created/updated
- **Detects missing tags**: Analyzes time entries to identify those without tags
- **Logs suggestions**: Currently logs what tags it would apply (implement real logic in `WebhookHandlers.java`)
- **Provides sidebar UI**: Shows settings and status in the Clockify time entry sidebar

## Project Structure

```
addons/auto-tag-assistant/
├── manifest.json                           # Runtime manifest (no $schema)
├── pom.xml                                 # Maven build config
├── README.md                               # This file
└── src/main/java/com/example/autotagassistant/
    ├── AutoTagAssistantApp.java           # Main entry point
    ├── ManifestController.java            # Serves /manifest.json
    ├── SettingsController.java            # Serves /settings sidebar
    ├── LifecycleHandlers.java             # INSTALLED/DELETED events
    ├── WebhookHandlers.java               # Time entry webhooks
    └── ClockifyApiClient.java             # Helper for Clockify API calls
```

## How Clockify Calls This Add-on

1. **Manifest Discovery**: Clockify fetches `{baseUrl}/manifest.json`
2. **Installation**: Admin installs add-on → POST to `{baseUrl}/lifecycle` (INSTALLED event with addon token)
3. **Sidebar**: User opens time entry → GET `{baseUrl}/settings` (loaded as iframe)
4. **Webhooks**: Time entry event occurs → POST to `{baseUrl}/webhook` with event payload

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.6+
- ngrok (for exposing localhost to Clockify)

### Build

From the repository root:

```bash
# Build all modules including SDK
mvn clean package

# Or just build this addon
cd addons/auto-tag-assistant
mvn clean package
```

This creates: `target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar`

### 2. Run Locally

```bash
export ADDON_KEY=auto-tag-assistant
export ADDON_BASE_URL=http://localhost:8080/auto-tag-assistant
java -jar target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

The addon will start on port 8080 with these endpoints:

- `GET /health` - Health check
- `POST /lifecycle` - Installation/deletion events
- `POST /webhook` - Time entry events
- `GET /settings` - Configuration UI
- `GET /manifest.json` - Addon manifest

### 3. Expose with ngrok

In a separate terminal:
### Run

```bash
# Option 1: From repo root
make run-auto-tag-assistant

# Option 2: With custom port
ADDON_PORT=8080 java -jar addons/auto-tag-assistant/target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar

# Option 3: From addon directory
cd addons/auto-tag-assistant
java -jar target/auto-tag-assistant-0.1.0-jar-with-dependencies.jar
```

The server starts on port 8080 by default.

### Expose with ngrok

In another terminal:

```bash
ngrok http 8080
```

Copy the ngrok URL (e.g., `https://abc123.ngrok.io`) and update:

```bash
export ADDON_BASE_URL=https://abc123.ngrok.io/auto-tag-assistant
```

Restart the addon with the new base URL.

### 4. Register in Clockify Developer Portal

1. Go to [Clockify Developer Portal](https://developer.marketplace.cake.com/)
2. Create new add-on
3. Upload `manifest.json` or enter details manually:
   - **Key**: `auto-tag-assistant`
   - **Name**: Auto-Tag Assistant
   - **Base URL**: Your ngrok URL
   - **Lifecycle endpoint**: `/lifecycle`
   - **Webhooks**:
     - `NEW_TIMER_STARTED` → `/webhook`
     - `TIMER_STOPPED` → `/webhook`
     - `TIME_ENTRY_UPDATED` → `/webhook`
     - `NEW_TIME_ENTRY` → `/webhook`

4. Install the add-on on your workspace

### 5. Test the Add-on

1. **Check Installation**:
   - Look for lifecycle logs showing INSTALLED event
   - Verify addon token received

2. **Start a Timer** (without tags):
   - Check webhook logs
   - Should see: "Time entry {id} has 0 tags"
   - Should see: "No tags found - would add default tag"

3. **Access Settings**:
   - In Clockify, find "Tag Rules" in the sidebar
   - Configure default tag name
   - Add required tags

## Configuration

### Admin Settings

Access the Settings UI from the Clockify sidebar:

- **Enable Auto-Tagging**: Toggle on/off
- **Default Tag Name**: Tag to add when none exist (default: "Untagged")
- **Required Tags**: List of tags, at least one must be present

### API Scopes

The add-on requests these Clockify API scopes:

- `TIME_ENTRY_READ` - Read time entries
- `TIME_ENTRY_WRITE` - Update time entries with tags
- `TAG_READ` - List available tags
- `TAG_WRITE` - Create new tags if needed

## Production Deployment

To deploy for production use:

### 1. Database Setup

Store installation data for each workspace:

```sql
CREATE TABLE installations (
  workspace_id VARCHAR PRIMARY KEY,
  addon_token TEXT NOT NULL,
  api_url VARCHAR NOT NULL,
  webhook_token TEXT NOT NULL,
  settings JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);
```

### 2. Enable Auto-Tagging

Update `WebhookHandlers.java` line 102-115 to:

```java
if (tagIds.isEmpty()) {
    // Retrieve stored token for this workspace
    Installation install = db.getInstallation(workspaceId);
    ClockifyClient client = new ClockifyClient(install.apiUrl, install.addonToken);

    // Get or create default tag
    List<Tag> tags = parseTagList(client.listTags(workspaceId).body());
    String defaultTagId = findOrCreateTag(tags, DEFAULT_TAG_NAME, workspaceId, client);

    // Update time entry with tag
    updateTimeEntryTags(client, workspaceId, timeEntryId, List.of(defaultTagId));

    log.info("Added default tag to time entry: " + timeEntryId);
}
```

### 3. Verify Webhook Signatures

Implement JWT verification in `WebhookHandlers.java`:

```java
String signature = request.getHeaders().get("clockify-signature");
Installation install = db.getInstallation(workspaceId);
if (!verifyJWT(signature, install.webhookToken)) {
    return HttpResponse.error(403, "Invalid signature");
}
```

### 4. Handle Rate Limits

Implement retry logic with exponential backoff:

```java
// Max 50 requests/second per workspace per addon
RateLimiter limiter = RateLimiter.create(50.0); // 50 permits per second
limiter.acquire();
```

## File Structure

```
addons/auto-tag-assistant/
├── manifest.json                           # Add-on configuration
├── pom.xml                                 # Maven build file
├── README.md                               # This file
└── src/
    ├── main/
    │   ├── java/com/example/auto_tag_assistant/
    │   │   ├── AddonApplication.java       # Main entry point
    │   │   ├── ClockifyClient.java         # API client
    │   │   ├── LifecycleHandlers.java      # Install/delete handling
    │   │   ├── ManifestController.java     # Manifest endpoint
    │   │   └── WebhookHandlers.java        # Tag detection logic
    │   └── resources/
    │       ├── application.yml             # Spring config (if needed)
    │       └── public/
    │           └── settings.html           # React settings UI
    └── test/
        └── java/com/example/auto_tag_assistant/
            └── ManifestValidationTest.java # Manifest validation test
```

## Troubleshooting

### Webhook Not Firing

1. Check ngrok is running and URL is correct
2. Verify webhook events registered in developer portal
3. Check Clockify webhook logs in add-on settings

### Add-on Token Missing

- Token is provided in `INSTALLED` lifecycle event
- Must be stored and retrieved for each API call
- Production requires database storage

### Tag Not Added

- Current implementation only logs detection
- Implement API calls as described in Production Deployment
- Verify API scopes are granted

### Build Failures

```bash
# Clear Maven cache
rm -rf ~/.m2/repository/com/cake/clockify

# Verify GitHub token
cat ~/.m2/settings.xml

# Try with Maven debug
mvn -X package
```

## Manifest Schema

The `manifest.json` validates against Clockify's schema:

- **Version**: 1.3
- **Components**: Sidebar settings page
- **Webhooks**: 4 time entry event types
- **Lifecycle**: INSTALLED, DELETED
- **Scopes**: Time entry and tag read/write

Validate with:

```bash
python3 ../../tools/validate-manifest.py manifest.json
```

## API Documentation

- [Clockify API Docs](https://docs.clockify.me/)
- [Marketplace Dev Docs](https://dev-docs.marketplace.cake.com/)
- [Add-on SDK](https://github.com/clockify/addon-java-sdk)

## Contributing

This is a reference implementation for educational purposes. To extend:

1. Add settings persistence (database)
2. Implement full auto-tagging with API calls
3. Add tag suggestion based on project/description
4. Implement tag rules engine (required tags per project)
5. Add notification system for missing tags

## License

See parent repository for licensing details.

## Support

For issues or questions:
- Check the [Developer Forum](https://dev-forum.marketplace.cake.com/)
- Review the marketplace documentation
- Open an issue in the repository
You'll see output like:

```
Forwarding  https://abc123.ngrok-free.app -> http://localhost:8080
```

Copy the `https://` URL.

### Install in Clockify

1. **Update baseUrl**: Edit `manifest.json` and set:
   ```json
   "baseUrl": "https://abc123.ngrok-free.app/auto-tag-assistant"
   ```

2. **Install Add-on**:
   - Go to Clockify → Admin → Add-ons
   - Click "Install Custom Add-on"
   - Enter manifest URL: `https://abc123.ngrok-free.app/auto-tag-assistant/manifest.json`
   - Click Install

3. **Verify Installation**:
   - Check server logs for INSTALLED lifecycle event
   - Open a time entry → should see "Auto-Tag Settings" in sidebar
   - Create/update time entries → check logs for webhook events

## Implementing Real Auto-Tagging

Currently, the add-on **logs** what it would do. To implement real tagging:

### 1. Store Addon Token

Edit `LifecycleHandlers.java:22`:

```java
// Extract and store the token
String addonToken = payload.get("addonToken").getAsString();
// Store in database: tokenStore.save(workspaceId, addonToken);
```

### 2. Implement Tagging Logic

Edit `WebhookHandlers.java:80`:

```java
// Retrieve stored token
String addonToken = tokenStore.get(workspaceId);
String baseUrl = "https://api.clockify.me/api/v1"; // or from token claims

// Create API client
ClockifyApiClient client = new ClockifyApiClient(baseUrl, addonToken);

// Get available tags
JsonNode tags = client.getTags(workspaceId);

// Match tags based on your logic
String[] tagIds = {"tag-id-1", "tag-id-2"};

// Apply tags
client.updateTimeEntryTags(workspaceId, timeEntryId, tagIds);
```

### 3. Customize Tag Matching

The current implementation uses simple keyword matching in `suggestTagsForTimeEntry()`. Replace with:

- **NLP-based analysis**: Use libraries like Stanford CoreNLP
- **Machine learning**: Train model on historical tagging patterns
- **Rule engine**: Define project-specific tagging rules
- **External integration**: Query external systems for context

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ADDON_PORT` | `8080` | Port to listen on |
| `ADDON_BASE_URL` | `http://localhost:8080/auto-tag-assistant` | Base URL for the addon |

## API Endpoints

All endpoints are prefixed with `/auto-tag-assistant`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/manifest.json` | Runtime manifest (no $schema) |
| GET | `/settings` | Sidebar UI (iframe) |
| POST | `/lifecycle` | INSTALLED/DELETED events |
| POST | `/webhook` | Time entry webhooks |
| GET | `/health` | Health check |

## Troubleshooting

### Build fails with "cannot find symbol: package addonsdk"

**Cause**: SDK modules not built yet.

**Solution**: Build from repo root:
```bash
cd /path/to/boileraddon
mvn clean install
```

### Clockify rejects manifest

**Cause**: Manifest contains `$schema` or invalid fields.

**Solution**: The `/manifest.json` endpoint serves programmatically (no $schema). Never copy-paste the file directly.

### Webhooks not received

**Causes**:
1. ngrok URL changed (restart ngrok, reinstall addon)
2. baseUrl mismatch in manifest
3. Events not configured in manifest

**Solution**: Check server logs, verify manifest URL matches running server.

### "401 Unauthorized" when calling Clockify API

**Cause**: Invalid or missing addon token.

**Solution**:
1. Ensure you stored the token from INSTALLED event
2. Use correct token for the workspace
3. Check token hasn't expired (shouldn't for addon tokens)

## Next Steps

1. **Add token storage**: Use PostgreSQL, Redis, or encrypted file storage
2. **Implement tag matching**: Build real NLP or rule-based logic
3. **Add configuration UI**: Let users customize tagging rules in sidebar
4. **Add tests**: Unit tests for tag matching, integration tests for API calls
5. **Deploy to production**: Use proper hosting (not ngrok), set up monitoring
6. **Handle edge cases**: API rate limits, network errors, invalid payloads

## References

- [Clockify Add-on Docs](../dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md)
- [Manifest Schema](../dev-docs-marketplace-cake-snapshot/extras/manifest-schema-latest.json)
- [Clockify OpenAPI](../dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json)
- [SDK Source](../dev-docs-marketplace-cake-snapshot/extras/addon-java-sdk/)
