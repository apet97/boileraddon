# Component Type Examples

This directory contains minimal working examples for each Clockify addon component type.

## Available Component Types

Each subdirectory contains:
- `README.md` - Component description and manifest configuration
- `example.html` - Minimal working HTML implementation
- `handler.java` - Java handler code example

### Component Types

1. **[settings-sidebar](settings-sidebar/)** - Settings panel for addon configuration
2. **[time-entry-sidebar](time-entry-sidebar/)** - Context panel shown when viewing a time entry
3. **[project-sidebar](project-sidebar/)** - Context panel shown when viewing a project
4. **[report-tab](report-tab/)** - Custom tab in the Reports section
5. **[widget](widget/)** - Dashboard widget

## Quick Start

To add a component to your addon:

1. Choose the component type
2. Copy the manifest configuration from the component's README
3. Add the manifest entry to your `manifest.json`
4. Implement the handler endpoint in your addon
5. Return the HTML/UI from the component's example

## Component Features

| Component | Receives JWT | Context Parameters | Use Case |
|-----------|--------------|-------------------|----------|
| Settings Sidebar | ✅ | workspaceId, userId | Addon configuration |
| Time Entry Sidebar | ✅ | workspaceId, userId, timeEntryId | Time entry details/actions |
| Project Sidebar | ✅ | workspaceId, userId, projectId | Project details/actions |
| Report Tab | ✅ | workspaceId, userId | Custom reports |
| Widget | ✅ | workspaceId, userId | Dashboard visualizations |

## Common Patterns

All components:
- Are loaded in an iframe within Clockify UI
- Receive a `jwt` query parameter with user context
- Should be responsive and work within constrained width
- Can make API calls to your addon backend
- Can use the Clockify API via your addon's installation token

## Example Manifest

```json
{
  "schemaVersion": "1.3",
  "key": "my-addon",
  "name": "My Addon",
  "baseUrl": "https://my-server.com/my-addon",
  "scopes": ["WORKSPACE_READ"],
  "components": [
    {
      "type": "SETTINGS_SIDEBAR",
      "name": "Settings",
      "url": "/settings"
    },
    {
      "type": "TIME_ENTRY_SIDEBAR",
      "name": "Details",
      "url": "/time-entry-sidebar?timeEntryId={timeEntryId}"
    }
  ],
  "lifecycle": [
    {"event": "INSTALLED", "url": "/lifecycle/installed"},
    {"event": "DELETED", "url": "/lifecycle/deleted"}
  ]
}
```

## Additional Resources

- [Building Your Own Addon](../../docs/BUILDING-YOUR-OWN-ADDON.md)
- [Quick Reference](../../docs/QUICK-REFERENCE.md)
- [Auto Tag Assistant Example](../../addons/auto-tag-assistant/)
