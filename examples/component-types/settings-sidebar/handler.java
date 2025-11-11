package com.example.addon.components;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.example.addon.security.JwtVerifier;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Handler for the Settings Sidebar component.
 *
 * This component is displayed in the Clockify addons settings page
 * and allows users to configure addon-specific settings.
 */
public class SettingsSidebarHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(SettingsSidebarHandler.class);

    private final JwtVerifier jwtVerifier;
    private final Map<String, WorkspaceSettings> settingsStore;

    public SettingsSidebarHandler(JwtVerifier jwtVerifier, Map<String, WorkspaceSettings> settingsStore) {
        this.jwtVerifier = jwtVerifier;
        this.settingsStore = settingsStore;
    }

    /**
     * Handle GET request to /settings
     * Returns the settings HTML page with JWT validation
     */
    @Override
    public HttpResponse handle(Map<String, String> queryParams, Map<String, String> headers, String body) {
        String jwt = queryParams.get("jwt");

        if (jwt == null || jwt.isEmpty()) {
            return HttpResponse.badRequest("Missing JWT token");
        }

        try {
            // Verify JWT signature and extract claims
            JSONObject claims = jwtVerifier.verifyAndDecode(jwt);
            String workspaceId = claims.getString("workspaceId");
            String userId = claims.getString("userId");
            String userName = claims.getString("userName");

            logger.info("Settings page accessed by: {} (workspace: {})", userName, workspaceId);

            // Load and return the settings HTML
            String html = loadSettingsHtml();
            return HttpResponse.ok(html, "text/html; charset=UTF-8");

        } catch (SecurityException e) {
            logger.error("JWT verification failed: {}", e.getMessage(), e);
            return HttpResponse.unauthorized("Invalid or expired JWT token");
        } catch (Exception e) {
            logger.error("Failed to load settings page: {}", e.getMessage(), e);
            return HttpResponse.internalServerError("Failed to load settings");
        }
    }

    /**
     * Load the settings HTML file
     */
    private String loadSettingsHtml() throws IOException {
        // In production, you might want to use a template engine
        // or bundle the HTML as a resource
        String htmlPath = "src/main/resources/settings.html";
        return new String(Files.readAllBytes(Paths.get(htmlPath)));
    }

    /**
     * Handle POST request to save settings
     * This would typically be a separate endpoint like /api/settings
     */
    public HttpResponse saveSettings(Map<String, String> queryParams, String body) {
        try {
            JSONObject settings = new JSONObject(body);
            String workspaceId = settings.getString("workspaceId");

            // Validate settings
            if (workspaceId == null || workspaceId.isEmpty()) {
                return HttpResponse.badRequest("Missing workspaceId");
            }

            // Extract and store settings
            WorkspaceSettings workspaceSettings = new WorkspaceSettings();
            workspaceSettings.apiKey = settings.optString("apiKey", "");
            workspaceSettings.tagPrefix = settings.optString("tagPrefix", "auto-");
            workspaceSettings.keywords = settings.optString("keywords", "");
            workspaceSettings.autoTag = settings.optBoolean("autoTag", true);
            workspaceSettings.notifyOnTag = settings.optBoolean("notifyOnTag", false);

            settingsStore.put(workspaceId, workspaceSettings);

            logger.info("Settings saved for workspace: {}", workspaceId);

            return HttpResponse.ok("{\"success\": true, \"message\": \"Settings saved successfully\"}");

        } catch (Exception e) {
            logger.error("Failed to save settings: {}", e.getMessage(), e);
            return HttpResponse.internalServerError("Failed to save settings");
        }
    }

    /**
     * Handle GET request to load settings
     * This would typically be a separate endpoint like /api/settings
     */
    public HttpResponse loadSettings(Map<String, String> queryParams) {
        String workspaceId = queryParams.get("workspaceId");

        if (workspaceId == null || workspaceId.isEmpty()) {
            return HttpResponse.badRequest("Missing workspaceId");
        }

        WorkspaceSettings settings = settingsStore.get(workspaceId);

        if (settings == null) {
            // Return default settings
            settings = new WorkspaceSettings();
            settings.tagPrefix = "auto-";
            settings.autoTag = true;
        }

        JSONObject response = new JSONObject();
        response.put("apiKey", settings.apiKey);
        response.put("tagPrefix", settings.tagPrefix);
        response.put("keywords", settings.keywords);
        response.put("autoTag", settings.autoTag);
        response.put("notifyOnTag", settings.notifyOnTag);

        return HttpResponse.ok(response.toString(), "application/json");
    }

    /**
     * Simple POJO to store workspace settings
     */
    public static class WorkspaceSettings {
        public String apiKey = "";
        public String tagPrefix = "auto-";
        public String keywords = "";
        public boolean autoTag = true;
        public boolean notifyOnTag = false;
    }
}

/*
 * Usage in your main addon application:
 *
 * // Initialize
 * JwtVerifier jwtVerifier = new JwtVerifier(publicKeyPem);
 * Map<String, WorkspaceSettings> settingsStore = new ConcurrentHashMap<>();
 * SettingsSidebarHandler settingsHandler = new SettingsSidebarHandler(jwtVerifier, settingsStore);
 *
 * // Register endpoints
 * addon.registerHandler("GET", "/settings", settingsHandler);
 * addon.registerHandler("POST", "/api/settings", (params, headers, body) ->
 *     settingsHandler.saveSettings(params, body)
 * );
 * addon.registerHandler("GET", "/api/settings", (params, headers, body) ->
 *     settingsHandler.loadSettings(params)
 * );
 */
