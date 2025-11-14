package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.example.rules.config.RuntimeFlags;
import com.clockify.addon.sdk.security.jwt.AuthTokenVerifier;
import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.example.rules.web.Nonce;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 
 * Renders the settings UI (no‑code rule builder) for the Rules add‑on.
 */
public class SettingsController implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EXPLORER_TEMPLATE = loadExplorerTemplate();

    private final AuthTokenVerifier jwtVerifier;
    // Force base URL injection so runtime config always flows through RulesConfiguration.
    private final String baseUrl;

    public SettingsController(AuthTokenVerifier jwtVerifier, String baseUrl) {
        this.jwtVerifier = jwtVerifier;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        String applyMode = RuntimeFlags.applyChangesEnabled() ? "Apply" : "Log-only";
        String skipSig = RuntimeFlags.skipSignatureVerification()
                ? "ON"
                : (RuntimeFlags.isDevEnvironment() ? "OFF" : "LOCKED");
        String envLabel = RuntimeFlags.environmentLabel();
        String base = this.baseUrl;

        // CRITICAL: Use the same nonce that SecurityHeadersFilter puts in CSP header
        // to avoid browser rejecting all scripts/styles due to nonce mismatch
        String nonce = (String) request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTR);
        if (nonce == null) {
            // Fallback for tests that don't run SecurityHeadersFilter
            nonce = Nonce.create();
        }
        SettingsBootstrap bootstrap = resolveBootstrap(request);
        String safeBootstrapJson = escapeForScript(serializeBootstrap(bootstrap));
        String runtimeJson = escapeForScript(buildRuntimePayload(base, envLabel, applyMode, skipSig, bootstrap));

        String html = EXPLORER_TEMPLATE
                .replace("{{NONCE}}", nonce)
                .replace("{{BODY_CLASS}}", themeClass(bootstrap.theme()))
                .replace("{{BOOTSTRAP_JSON}}", safeBootstrapJson)
                .replace("{{RUNTIME_JSON}}", runtimeJson);

        return HttpResponse.ok(html, "text/html; charset=utf-8");
    }

    SettingsBootstrap resolveBootstrap(HttpServletRequest request) {
        String requestId = Objects.toString(
                request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR),
                Nonce.create());

        // Priority 1: WorkspaceContextFilter attributes (set by filter from auth_token JWT)
        String workspaceId = (String) request.getAttribute(WorkspaceContextFilter.WORKSPACE_ID_ATTR);
        String userId = (String) request.getAttribute(WorkspaceContextFilter.USER_ID_ATTR);
        String userEmail = "";
        String theme = "light"; // Default theme
        String language = "en"; // Default language

        String rawJwt = firstNonBlank(request.getParameter("auth_token"), request.getParameter("jwt"));
        String bootstrapToken = "";
        if (jwtVerifier != null && rawJwt != null && !rawJwt.isBlank()) {
            try {
                JwtVerifier.DecodedJwt decoded = jwtVerifier.verify(rawJwt);
                JsonNode payload = decoded.payload();
                if (workspaceId == null || workspaceId.isBlank()) {
                    workspaceId = payload.path("workspaceId").asText("");
                }
                if (userId == null || userId.isBlank()) {
                    userId = payload.path("userId").asText("");
                }
                String payloadEmail = payload.path("userEmail").asText("");
                if (!payloadEmail.isBlank()) {
                    userEmail = payloadEmail;
                }
                theme = payload.path("theme").asText(theme).toLowerCase();
                language = payload.path("language").asText(language).toLowerCase();
                if (!workspaceId.isBlank()) {
                    request.setAttribute(DiagnosticContextFilter.WORKSPACE_ID_ATTR, workspaceId);
                }
                if (!userId.isBlank()) {
                    request.setAttribute(DiagnosticContextFilter.USER_ID_ATTR, userId);
                }
                bootstrapToken = rawJwt;
            } catch (JwtVerifier.JwtVerificationException e) {
                logger.warn("Settings JWT rejected: {}", e.getMessage());
            }
        } else if (rawJwt != null && jwtVerifier == null) {
            logger.debug("auth_token provided but JWT verifier not configured; token ignored");
        }

        // Normalize to empty string if null
        workspaceId = workspaceId != null ? workspaceId : "";
        userId = userId != null ? userId : "";

        return new SettingsBootstrap(workspaceId, userId, userEmail, requestId, theme, language, bootstrapToken);
    }

    String serializeBootstrap(SettingsBootstrap bootstrap) {
        try {
            return OBJECT_MAPPER.writeValueAsString(bootstrap);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize bootstrap payload: {}", e.getMessage());
            return "{\"workspaceId\":\"\",\"userId\":\"\",\"userEmail\":\"\",\"requestId\":\"\",\"theme\":\"light\",\"language\":\"en\"}";
        }
    }

    private String escapeForScript(String json) {
        return json.replace("</", "<\\/");
    }

    private static String loadExplorerTemplate() {
        try (InputStream in = SettingsController.class.getClassLoader()
                .getResourceAsStream("public/explorer/index.html")) {
            if (in == null) {
                logger.error("Workspace explorer template not found (public/explorer/index.html)");
                return "<!DOCTYPE html><html><body><h1>Workspace explorer template missing</h1></body></html>";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to load workspace explorer template", e);
            return "<!DOCTYPE html><html><body><h1>Unable to load workspace explorer</h1></body></html>";
        }
    }

    private String buildRuntimePayload(String baseUrl,
                                       String envLabel,
                                       String applyMode,
                                       String skipSig,
                                       SettingsBootstrap bootstrap) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("baseUrl", baseUrl);
        node.put("env", envLabel);
        node.put("applyMode", applyMode);
        node.put("skipSignature", skipSig);
        node.put("apiBase", buildExplorerApiBase(baseUrl));
        node.put("workspaceId", bootstrap.workspaceId());
        node.put("userId", bootstrap.userId());
        node.put("userEmail", bootstrap.userEmail());
        node.put("theme", bootstrap.theme());
        node.put("language", bootstrap.language());
        node.put("requestId", bootstrap.requestId());
        return node.toString();
    }

    private static String buildExplorerApiBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "/rules/api/rules/explorer";
        }
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalized + "/api/rules/explorer";
    }

    private static String themeClass(String theme) {
        if (theme == null || theme.isBlank()) {
            return "theme-light";
        }
        String normalized = theme.toLowerCase();
        return normalized.contains("dark") ? "theme-dark" : "theme-light";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    record SettingsBootstrap(String workspaceId,
                             String userId,
                             String userEmail,
                             String requestId,
                             String theme,
                             String language,
                             String authToken) {}

}
