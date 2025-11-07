# Auto-Tag Assistant

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
