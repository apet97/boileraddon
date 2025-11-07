package com.example.autotagassistant;

import com.cake.clockify.addonsdk.shared.RequestHandler;
import com.cake.clockify.addonsdk.shared.response.HttpResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Serves the settings/sidebar UI for the Auto-Tag Assistant.
 *
 * This HTML is loaded as an iframe in the Clockify time entry sidebar.
 * The iframe receives context via query parameters from Clockify.
 *
 * In a real implementation, this would:
 * - Parse JWT token from query params to get user/workspace context
 * - Display current auto-tagging rules
 * - Allow users to configure tag detection patterns
 * - Show statistics about auto-tagged entries
 */
public class SettingsController implements RequestHandler {

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Auto-Tag Assistant Settings</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
            color: #333;
        }
        .container {
            max-width: 600px;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            margin: 0 0 10px 0;
            font-size: 24px;
            color: #03a9f4;
        }
        .subtitle {
            color: #666;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .info-box {
            background: #e3f2fd;
            border-left: 4px solid #03a9f4;
            padding: 12px;
            margin: 16px 0;
            border-radius: 4px;
        }
        .info-box h3 {
            margin: 0 0 8px 0;
            font-size: 16px;
            color: #0277bd;
        }
        .info-box p {
            margin: 4px 0;
            font-size: 14px;
            color: #555;
        }
        code {
            background: #f5f5f5;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
        }
        .status {
            display: inline-block;
            padding: 4px 12px;
            background: #4caf50;
            color: white;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🏷️ Auto-Tag Assistant</h1>
        <div class="subtitle">
            <span class="status">✓ Active</span>
        </div>

        <div class="info-box">
            <h3>How It Works</h3>
            <p>This add-on monitors time entry events and automatically suggests or applies tags based on:</p>
            <ul>
                <li>Project context</li>
                <li>Task descriptions</li>
                <li>Historical tagging patterns</li>
            </ul>
        </div>

        <div class="info-box">
            <h3>Monitored Events</h3>
            <p>• <code>NEW_TIMER_STARTED</code> - When you start a new timer</p>
            <p>• <code>TIMER_STOPPED</code> - When you stop a timer</p>
            <p>• <code>NEW_TIME_ENTRY</code> - When a new entry is created</p>
            <p>• <code>TIME_ENTRY_UPDATED</code> - When an entry is modified</p>
        </div>

        <div class="info-box">
            <h3>Implementation Status</h3>
            <p><strong>Current:</strong> Logging mode (check server logs)</p>
            <p><strong>To implement:</strong> Edit <code>WebhookHandlers.java</code> and use the Clockify API client in <code>ClockifyApiClient.java</code> to apply tags.</p>
        </div>

        <p style="margin-top: 24px; font-size: 12px; color: #999; text-align: center;">
            Auto-Tag Assistant v0.1.0
        </p>
    </div>

    <script>
        // In a real implementation, you would:
        // 1. Parse the JWT token from URL query params
        // 2. Decode to get workspaceId, userId, etc.
        // 3. Load user-specific settings from your backend
        // 4. Allow configuration of tagging rules
        console.log('Auto-Tag Assistant Settings loaded');
        console.log('Query params:', window.location.search);
    </script>
</body>
</html>
                """;

        return HttpResponse.ok(html, "text/html");
    }
}
