# Settings Sidebar Component

A settings panel that appears in the Clockify addons settings page.

## Purpose

Allows users to configure addon-specific settings for their workspace.

## Manifest Configuration

```json
{
  "components": [
    {
      "type": "SETTINGS_SIDEBAR",
      "name": "Settings",
      "url": "/settings"
    }
  ]
}
```

## URL Parameters

Your endpoint will receive:
- `jwt` - JWT token containing user context (workspaceId, userId, userEmail, userName)

Example request:
```
GET /settings?jwt=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

## JWT Payload

```json
{
  "sub": "64621faec4d2cc53b91fce6c",
  "workspaceId": "68adfddad138cb5f24c63b22",
  "userId": "64621faec4d2cc53b91fce6c",
  "userEmail": "user@example.com",
  "userName": "John Doe",
  "iat": 1730188800,
  "exp": 1730189400,
  "iss": "clockify.me"
}
```

## Java Handler Example

See [handler.java](handler.java) for complete implementation.

```java
@RequestMapping("/settings")
public HttpResponse handleSettings(HttpRequest request) {
    String jwt = request.getQueryParameter("jwt");

    // Verify JWT and extract user info
    JSONObject claims = jwtVerifier.verifyAndDecode(jwt);
    String workspaceId = claims.getString("workspaceId");

    // Load settings HTML
    String html = loadSettingsHtml();

    return HttpResponse.ok(html, "text/html");
}
```

## UI Example

See [example.html](example.html) for complete implementation.

## Best Practices

1. **Validate JWT** - Always verify the JWT signature before processing
2. **Save Settings** - Store settings per workspace
3. **Responsive Design** - Settings panel width is constrained (typically 300-400px)
4. **Loading States** - Show loading indicators when saving
5. **Error Handling** - Display user-friendly error messages
6. **Default Values** - Provide sensible defaults for first-time users

## Common Use Cases

- API key configuration
- Feature toggles
- Notification preferences
- Tagging rules
- Integration settings
- Display preferences
