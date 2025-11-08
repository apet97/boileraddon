# Time Entry Sidebar Component

A context panel that appears when viewing a time entry in Clockify.

## Purpose

Provides additional context, actions, or information about a specific time entry.

## Manifest Configuration

```json
{
  "components": [
    {
      "type": "TIME_ENTRY_SIDEBAR",
      "name": "Entry Details",
      "url": "/time-entry-sidebar?timeEntryId={timeEntryId}"
    }
  ]
}
```

## URL Parameters

Your endpoint will receive:
- `jwt` - JWT token containing user context (workspaceId, userId, userEmail, userName)
- `timeEntryId` - ID of the time entry being viewed

Example request:
```
GET /time-entry-sidebar?timeEntryId=69017c7cf249396a237cfcce&jwt=eyJhbGci...
```

## Common Use Cases

- Display additional analytics about the time entry
- Show related external data (e.g., Jira issue details)
- Provide quick actions (tag, categorize, export)
- Display time entry history/changes
- Show AI suggestions or insights
- Calculate cost/billing information

## Implementation Notes

1. **Verify JWT** - Always validate the JWT token
2. **Fetch Entry Data** - Use timeEntryId to fetch details from Clockify API
3. **Responsive Design** - Sidebar width is typically 300-400px
4. **Real-time Updates** - Consider polling or WebSocket for live updates
5. **Error Handling** - Handle cases where entry no longer exists

## Example Response

See [example.html](example.html) for complete implementation.
