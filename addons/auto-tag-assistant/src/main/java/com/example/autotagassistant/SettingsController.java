package com.example.autotagassistant;

import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.HttpResponse;
import com.example.autotagassistant.security.JwtTokenDecoder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves the settings/sidebar UI for the Auto-Tag Assistant.
 *
 * Parses JWT token from query parameters to extract workspace/user context
 * and displays workspace-specific configuration interface.
 */
public class SettingsController implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        // Extract and parse JWT token from query parameters
        String jwtToken = request.getParameter("token");
        if (jwtToken == null || jwtToken.isBlank()) {
            jwtToken = request.getParameter("jwt");
        }

        String workspaceId = "unknown";
        String userId = "unknown";
        String workspaceName = "Unknown Workspace";

        if (jwtToken != null && !jwtToken.isBlank()) {
            try {
                JwtTokenDecoder.DecodedJwt decoded = JwtTokenDecoder.decode(jwtToken);
                JsonNode payload = decoded.payload();

                // Extract workspace and user information
                if (payload.hasNonNull("workspaceId")) {
                    workspaceId = payload.get("workspaceId").asText();
                }
                if (payload.hasNonNull("userId")) {
                    userId = payload.get("userId").asText();
                }
                if (payload.hasNonNull("workspaceName")) {
                    workspaceName = payload.get("workspaceName").asText();
                }

                logger.info("Settings page accessed by user {} in workspace {} ({})",
                           userId, workspaceId, workspaceName);
            } catch (Exception e) {
                logger.warn("Failed to decode JWT token in settings page: {}", e.getMessage());
                return HttpResponse.error(401, "{\"error\":\"Invalid or missing JWT token\"}", "application/json");
            }
        } else {
            logger.warn("Settings page accessed without JWT token");
            return HttpResponse.error(401, "{\"error\":\"JWT token required\"}", "application/json");
        }

        // Generate workspace-specific HTML with extracted context
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
            <h3>Workspace Context</h3>
            <p><strong>Workspace:</strong> %s</p>
            <p><strong>Workspace ID:</strong> <code>%s</code></p>
            <p><strong>User ID:</strong> <code>%s</code></p>
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
            <p><strong>Current:</strong> Production-ready with JWT authentication</p>
            <p><strong>Features:</strong></p>
            <ul>
                <li>JWT token validation ✓</li>
                <li>Workspace context extraction ✓</li>
                <li>Database token persistence ✓</li>
                <li>Lifecycle endpoint security ✓</li>
            </ul>
            <p><strong>Next steps:</strong> Configure auto-tagging rules in <code>WebhookHandlers.java</code></p>
        </div>

        <p style="margin-top: 24px; font-size: 12px; color: #999; text-align: center;">
            Auto-Tag Assistant v0.1.0 | Workspace: %s
        </p>
    </div>

    <script>
        // Workspace context is now parsed server-side from JWT token
        console.log('Auto-Tag Assistant Settings loaded');
        console.log('Workspace ID: %s');
        console.log('User ID: %s');
    </script>
</body>
</html>
                """;

        // Format HTML with workspace context
        String formattedHtml = String.format(html,
            escapeHtml(workspaceName),    // Workspace name in context box
            escapeHtml(workspaceId),       // Workspace ID in context box
            escapeHtml(userId),            // User ID in context box
            escapeHtml(workspaceId),       // Workspace ID in footer
            escapeHtml(workspaceId),       // Workspace ID in console
            escapeHtml(userId)             // User ID in console
        );

        return HttpResponse.ok(formattedHtml, "text/html");
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
