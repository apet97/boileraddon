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
