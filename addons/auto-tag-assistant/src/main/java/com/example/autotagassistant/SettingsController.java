package com.example.autotagassistant;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.clockify.addon.sdk.security.jwt.AuthTokenVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
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

    private final AuthTokenVerifier jwtVerifier;
    private final boolean devMode;

    public SettingsController(AuthTokenVerifier jwtVerifier, boolean devMode) {
        this.jwtVerifier = jwtVerifier;
        this.devMode = devMode;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        JsonNode payload = resolvePayload(request);
        if (payload == null) {
            return HttpResponse.error(401, "{\"error\":\"Valid JWT token required\"}", "application/json");
        }

        String workspaceIdAttr = attributeAsString(request, WorkspaceContextFilter.WORKSPACE_ID_ATTR);
        String userIdAttr = attributeAsString(request, WorkspaceContextFilter.USER_ID_ATTR);
        String workspaceId = workspaceIdAttr != null
                ? workspaceIdAttr
                : (payload.hasNonNull("workspaceId") ? payload.get("workspaceId").asText() : "unknown");
        String userId = userIdAttr != null
                ? userIdAttr
                : (payload.hasNonNull("userId") ? payload.get("userId").asText() : "unknown");
        String workspaceName = payload.hasNonNull("workspaceName") ? payload.get("workspaceName").asText() : "Unknown Workspace";

        logger.info("Settings page accessed by user {} in workspace {} ({})", userId, workspaceId, workspaceName);

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

    private JsonNode resolvePayload(HttpServletRequest request) {
        String rawToken = extractJwt(request);
        if (rawToken == null || rawToken.isBlank()) {
            if (attributeAsString(request, WorkspaceContextFilter.WORKSPACE_ID_ATTR) != null) {
                return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            }
            logger.warn("Settings page accessed without JWT token");
            return null;
        }

        try {
            if (jwtVerifier != null) {
                JwtVerifier.DecodedJwt decoded = jwtVerifier.verify(rawToken);
                return decoded.payload();
            }
            if (devMode) {
                return JwtTokenDecoder.decode(rawToken).payload();
            }
            logger.warn("JWT token provided but verifier unavailable (ENV!=dev)");
            return null;
        } catch (JwtVerifier.JwtVerificationException e) {
            logger.warn("Failed to verify JWT token in settings page: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to decode JWT token in settings page: {}", e.getMessage());
            return null;
        }
    }

    private String extractJwt(HttpServletRequest request) {
        String token = firstNonBlank(
                request.getParameter("auth_token"),
                request.getParameter("token"),
                request.getParameter("jwt"));
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private static String attributeAsString(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
