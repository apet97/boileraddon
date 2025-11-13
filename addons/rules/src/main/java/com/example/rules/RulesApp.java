package com.example.rules;

import com.clockify.addon.sdk.AddonServlet;
import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.ConfigValidator;
import com.clockify.addon.sdk.EmbeddedServer;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.health.DatabaseHealthCheck;
import com.clockify.addon.sdk.health.HealthCheck;
import com.clockify.addon.sdk.middleware.CorsFilter;
import com.clockify.addon.sdk.middleware.RateLimiter;
import com.clockify.addon.sdk.middleware.RequestLoggingFilter;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import com.clockify.addon.sdk.middleware.WorkspaceContextFilter;
import com.example.rules.config.RuntimeFlags;
import com.example.rules.api.ErrorResponse;
import com.example.rules.store.RulesStore;
import com.example.rules.store.RulesStoreSPI;
import com.example.rules.store.DatabaseRulesStore;
import com.clockify.addon.sdk.metrics.MetricsHandler;
import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import com.example.rules.security.JwtVerifier;
import com.example.rules.web.RequestContext;
import com.clockify.addon.sdk.logging.LoggingContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rules Add-on for Clockify
 *
 * Provides declarative automation rules for time entries:
 * - Define conditions (AND/OR logic)
 * - Execute actions when conditions match
 * - Automatic tag management, description updates, and more
 *
 * How Clockify calls this addon:
 * 1. Manifest URL: Clockify fetches {baseUrl}/manifest.json to discover endpoints
 * 2. Lifecycle INSTALLED: POST to {baseUrl}/lifecycle/installed with workspace token
 * 3. Sidebar component: GET to {baseUrl}/settings renders iframe
 * 4. Webhooks: POST to {baseUrl}/webhook when time entry events occur
 * 5. API: CRUD operations on rules via {baseUrl}/api/rules
 *
 * To run locally:
 * 1. Build: mvn clean package
 * 2. Run: java -jar target/rules-0.1.0-jar-with-dependencies.jar
 * 3. Start ngrok: ngrok http 8080
 * 4. Update baseUrl env to: https://YOUR-SUBDOMAIN.ngrok-free.app/rules
 * 5. In Clockify Admin > Add-ons, install using manifest URL
 */
public class RulesApp {
    private static final Logger logger = LoggerFactory.getLogger(RulesApp.class);
    private static final String MEDIA_JSON = "application/json";

    /**
     * Static reference to the rules store used by the application.
     * Accessible for health checks and monitoring.
     */
    private static RulesStoreSPI rulesStore;

    public static void main(String[] args) throws Exception {
        // Read and validate configuration from environment
        String baseUrl = ConfigValidator.validateUrl(
                System.getenv("ADDON_BASE_URL"),
                "http://localhost:8080/rules",
                "ADDON_BASE_URL"
        );
        int port = ConfigValidator.validatePort(
                System.getenv("ADDON_PORT"),
                8080,
                "ADDON_PORT"
        );
        String addonKey = "rules";

        // Build manifest programmatically (v1.3, no $schema in runtime)
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key(addonKey)
                .name("Rules")
                .description("Declarative automations for Clockify: if conditions then actions")
                .baseUrl(baseUrl)
                .minimalSubscriptionPlan("PRO")
                .scopes(new String[]{
                        "TIME_ENTRY_READ",
                        "TIME_ENTRY_WRITE",
                        "TAG_READ",
                        "TAG_WRITE",
                        "PROJECT_READ",
                        "PROJECT_WRITE",
                        "CLIENT_READ",
                        "CLIENT_WRITE",
                        "TASK_READ",
                        "TASK_WRITE",
                        "WORKSPACE_READ"
                })
                .build();

        // Add sidebar component to manifest
        manifest.getComponents().add(
                new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Rules", "ADMINS")
        );

        ClockifyAddon addon = new ClockifyAddon(manifest);

        // Initialize stores
        rulesStore = selectRulesStore();
        RulesController rulesController = new RulesController(rulesStore, addon);

        // Initialize Clockify client for Projects/Clients/Tasks CRUD operations
        ClockifyClient clockifyClient = new ClockifyClient(
            System.getenv().getOrDefault("CLOCKIFY_API_BASE_URL", "https://api.clockify.me/api"),
            null // Token will be provided per-workspace from TokenStore
        );
        ProjectsController projectsController = new ProjectsController(clockifyClient);
        ClientsController clientsController = new ClientsController(clockifyClient);
        TasksController tasksController = new TasksController(clockifyClient);
        TagsController tagsController = new TagsController(clockifyClient);

        // Register endpoints
        // GET /rules/manifest.json - Returns runtime manifest (NO $schema field)
        addon.registerCustomEndpoint("/manifest.json", new ManifestController(manifest));

        // GET /rules/settings - Sidebar iframe (register common aliases to avoid 404 on trailing-slash)
        JwtVerifier jwtVerifier = initializeJwtVerifier();

        SettingsController settings = new SettingsController(jwtVerifier);
        addon.registerCustomEndpoint("/settings", settings);
        addon.registerCustomEndpoint("/settings/", settings);
        // Convenience: serve settings at root as well (direct browsing to /rules)
        addon.registerCustomEndpoint("/", settings);

        // GET /rules/ifttt - IFTTT builder page
        IftttController ifttt = new IftttController();
        addon.registerCustomEndpoint("/ifttt", ifttt);
        addon.registerCustomEndpoint("/ifttt/", ifttt);

        // GET /rules/simple - Simple rule builder with templates
        SimpleSettingsController simpleSettings = new SimpleSettingsController();
        addon.registerCustomEndpoint("/simple", simpleSettings);
        addon.registerCustomEndpoint("/simple/", simpleSettings);

        // Rules CRUD API
        addon.registerCustomEndpoint("/api/rules", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return rulesController.listRules().handle(request);
            } else if ("POST".equals(method)) {
                return rulesController.saveRule().handle(request);
            } else if ("DELETE".equals(method)) {
                return rulesController.deleteRule().handle(request);
            } else {
                return ErrorResponse.of(405, "RULES.METHOD_NOT_ALLOWED", "Method not allowed", request, false)
                        .withHeader("Allow", "GET,POST,DELETE");
            }
        });

        // POST /rules/api/test — dry-run evaluation (no side effects)
        addon.registerCustomEndpoint("/api/test", rulesController.testRules());

        // Projects CRUD API
        addon.registerCustomEndpoint("/api/projects", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return projectsController.listProjects().handle(request);
            } else if ("POST".equals(method)) {
                return projectsController.createProject().handle(request);
            } else if ("PUT".equals(method)) {
                return projectsController.updateProject().handle(request);
            } else if ("DELETE".equals(method)) {
                return projectsController.deleteProject().handle(request);
            } else {
                return ErrorResponse.of(405, "PROJECTS.METHOD_NOT_ALLOWED", "Method not allowed", request, false)
                        .withHeader("Allow", "GET,POST,PUT,DELETE");
            }
        });

        // Clients CRUD API
        addon.registerCustomEndpoint("/api/clients", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return clientsController.listClients().handle(request);
            } else if ("POST".equals(method)) {
                return clientsController.createClient().handle(request);
            } else if ("PUT".equals(method)) {
                return clientsController.updateClient().handle(request);
            } else if ("DELETE".equals(method)) {
                return clientsController.deleteClient().handle(request);
            } else {
                return ErrorResponse.of(405, "CLIENTS.METHOD_NOT_ALLOWED", "Method not allowed", request, false)
                        .withHeader("Allow", "GET,POST,PUT,DELETE");
            }
        });

        // Tasks CRUD API
        addon.registerCustomEndpoint("/api/tasks", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return tasksController.listTasks().handle(request);
            } else if ("POST".equals(method)) {
                return tasksController.createTask().handle(request);
            } else if ("PUT".equals(method)) {
                return tasksController.updateTask().handle(request);
            } else if ("DELETE".equals(method)) {
                return tasksController.deleteTask().handle(request);
            } else {
                return ErrorResponse.of(405, "TASKS.METHOD_NOT_ALLOWED", "Method not allowed", request, false)
                        .withHeader("Allow", "GET,POST,PUT,DELETE");
            }
        });

        // Tags CRUD API
        addon.registerCustomEndpoint("/api/tags", request -> {
            String method = request.getMethod();
            if ("GET".equals(method)) {
                return tagsController.listTags().handle(request);
            } else if ("POST".equals(method)) {
                return tagsController.createTag().handle(request);
            } else if ("PUT".equals(method)) {
                return tagsController.updateTag().handle(request);
            } else if ("DELETE".equals(method)) {
                return tagsController.deleteTag().handle(request);
            } else {
                return ErrorResponse.of(405, "TAGS.METHOD_NOT_ALLOWED", "Method not allowed", request, false)
                        .withHeader("Allow", "GET,POST,PUT,DELETE");
            }
        });

        // Cache endpoints: GET /rules/api/cache?workspaceId=... (summary), POST /rules/api/cache/refresh?workspaceId=...
        addon.registerCustomEndpoint("/api/cache", request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String ws = request.getParameter("workspaceId");
                if (ws == null || ws.isBlank()) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, ws);
                var snap = com.example.rules.cache.WorkspaceCache.get(ws);
                String json = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                        .put("workspaceId", ws)
                        .put("tags", snap.tagsById.size())
                        .put("projects", snap.projectsById.size())
                        .put("clients", snap.clientsById.size())
                        .put("users", snap.usersById.size())
                        .put("hasTasks", !snap.tasksByProjectNameNorm.isEmpty())
                        .toString();
                return HttpResponse.ok(json, MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.CACHE_SUMMARY_FAILED", "Failed to load workspace cache", e, true);
            }
        });
        addon.registerCustomEndpoint("/api/cache/refresh", request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String ws = request.getParameter("workspaceId");
                if (ws == null || ws.isBlank()) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, ws);
                var wkOpt = com.clockify.addon.sdk.security.TokenStore.get(ws);
                if (wkOpt.isEmpty()) {
                    return ErrorResponse.of(404, "RULES.TOKEN_NOT_FOUND", "Workspace installation token not found", request, false);
                }
                var wk = wkOpt.get();
                com.example.rules.cache.WorkspaceCache.refresh(ws, wk.apiBaseUrl(), wk.token());
                return HttpResponse.ok("{\"status\":\"refreshed\"}", MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.CACHE_REFRESH_FAILED", "Failed to refresh cache", e, true);
            }
        });

        // GET /rules/api/cache/data?workspaceId=... — expanded snapshot for autocompletes
        addon.registerCustomEndpoint("/api/cache/data", request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String ws = request.getParameter("workspaceId");
                if (ws == null || ws.isBlank()) {
                    return workspaceRequired(request);
                }
                RequestContext.attachWorkspace(request, ctx, ws);
                var snap = com.example.rules.cache.WorkspaceCache.get(ws);
                var om = new com.fasterxml.jackson.databind.ObjectMapper();
                var root = om.createObjectNode();
                root.put("workspaceId", ws);
                var tagsArr = om.createArrayNode();
                snap.tagsById.forEach((id, name) -> {
                    var n = om.createObjectNode(); n.put("id", id); n.put("name", name); tagsArr.add(n);
                });
                var projectsArr = om.createArrayNode();
                snap.projectsById.forEach((id, name) -> { var n = om.createObjectNode(); n.put("id", id); n.put("name", name); projectsArr.add(n); });
                var clientsArr = om.createArrayNode();
                snap.clientsById.forEach((id, name) -> { var n = om.createObjectNode(); n.put("id", id); n.put("name", name); clientsArr.add(n); });
                var usersArr = om.createArrayNode();
                snap.usersById.forEach((id, name) -> { var n = om.createObjectNode(); n.put("id", id); n.put("name", name); usersArr.add(n); });

                var tasksArr = om.createArrayNode();
                java.util.Map<String, String> projectNameByNorm = new java.util.HashMap<>();
                snap.projectsById.forEach((id, name) -> projectNameByNorm.put(name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT), name));
                snap.tasksByProjectNameNorm.forEach((projectNorm, tmap) -> {
                    String pName = projectNameByNorm.getOrDefault(projectNorm, projectNorm);
                    tmap.forEach((taskNorm, taskId) -> {
                        var n = om.createObjectNode();
                        n.put("id", taskId);
                        n.put("name", snap.taskNamesById.getOrDefault(taskId, taskNorm));
                        n.put("projectName", pName);
                        tasksArr.add(n);
                    });
                });

                root.set("tags", tagsArr);
                root.set("projects", projectsArr);
                root.set("clients", clientsArr);
                root.set("users", usersArr);
                root.set("tasks", tasksArr);
                return HttpResponse.ok(root.toString(), MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.CACHE_DATA_FAILED", "Failed to load cache data", e, true);
            }
        });

        // GET /rules/api/catalog/triggers — list all webhook triggers
        addon.registerCustomEndpoint("/api/catalog/triggers", request -> {
            try {
                com.fasterxml.jackson.databind.JsonNode json = com.example.rules.spec.TriggersCatalog.triggersToJson();
                return HttpResponse.ok(json.toString(), MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.TRIGGERS_FAILED", "Failed to load triggers catalog", e, true);
            }
        });

        // GET /rules/api/catalog/actions — list all OpenAPI endpoints
        addon.registerCustomEndpoint("/api/catalog/actions", request -> {
            try {
                com.fasterxml.jackson.databind.JsonNode json = com.example.rules.spec.OpenAPISpecLoader.endpointsToJson();
                return HttpResponse.ok(json.toString(), MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.ACTIONS_FAILED", "Failed to load actions catalog", e, true);
            }
        });

        // GET /rules/api/cache/stats — rule cache statistics
        addon.registerCustomEndpoint("/api/cache/stats", request -> {
            try {
                var stats = com.example.rules.cache.RuleCache.getStats();
                var json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(stats);
                return HttpResponse.ok(json, MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.CACHE_STATS_FAILED", "Failed to load cache stats", e, true);
            }
        });

        // GET /rules/status — runtime status (token present, modes)
        addon.registerCustomEndpoint("/status", request -> {
            try (LoggingContext ctx = RequestContext.logging(request)) {
                String ws = request.getParameter("workspaceId");
                if (ws != null && !ws.isBlank()) {
                    RequestContext.attachWorkspace(request, ctx, ws);
                }
                boolean tokenPresent = ws != null && !ws.isBlank() &&
                        com.clockify.addon.sdk.security.TokenStore.get(ws).isPresent();
                boolean apply = RuntimeFlags.applyChangesEnabled();
                boolean skipSig = RuntimeFlags.skipSignatureVerification();
                String json = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                        .put("workspaceId", ws == null ? "" : ws)
                        .put("tokenPresent", tokenPresent)
                        .put("applyChanges", apply)
                        .put("skipSignatureVerify", skipSig)
                        .put("baseUrl", baseUrl)
                        .toString();
                return HttpResponse.ok(json, MEDIA_JSON);
            } catch (Exception e) {
                return internalError(request, "RULES.STATUS_FAILED", "Failed to load status", e, true);
            }
        });

        // POST /rules/lifecycle/installed & /lifecycle/deleted - Lifecycle events
        LifecycleHandlers.register(addon, rulesStore);

        // POST /rules/webhook - Handle time entry events (legacy + new actions)
        WebhookHandlers.register(addon, rulesStore);

        // Register dynamic webhook handlers for IFTTT-style rules (all events)
        DynamicWebhookHandlers.registerDynamicEvents(addon, rulesStore);

        // Preload local secrets for development
        preloadLocalSecrets();

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv().getOrDefault("DB_USER", System.getenv("DB_USERNAME"));
        String dbPassword = System.getenv("DB_PASSWORD");

        String envLabel = RuntimeFlags.environmentLabel();
        boolean persistentTokens =
                Boolean.parseBoolean(System.getenv().getOrDefault("ENABLE_DB_TOKEN_STORE",
                        String.valueOf("prod".equalsIgnoreCase(envLabel))));
        PooledDatabaseTokenStore tokenStore = initializeTokenStore(
                persistentTokens,
                envLabel,
                dbUrl,
                dbUser,
                dbPassword,
                () -> new PooledDatabaseTokenStore(dbUrl, dbUser, dbPassword)
        );

        // Register health checks
        HealthCheck health = new HealthCheck("rules", "0.1.0");
        if (rulesStore instanceof com.example.rules.store.DatabaseRulesStore) {
            health.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
                @Override public String getName() { return "database"; }
                @Override public HealthCheck.HealthCheckResult check() {
                    try {
                        DatabaseRulesStore dbStore = (DatabaseRulesStore) rulesStore;
                        int n = dbStore.getAll("health-probe").size();
                        return new HealthCheck.HealthCheckResult("database", true, "Connected", n);
                    } catch (Exception e) {
                        return new HealthCheck.HealthCheckResult("database", false, e.getMessage());
                    }
                }
            });
        }
        if (tokenStore != null) {
            health.addHealthCheckProvider(new DatabaseHealthCheck(tokenStore));
        }
        addon.registerCustomEndpoint("/health", health);
        addon.registerCustomEndpoint("/metrics", new MetricsHandler());

        // Extract context path from base URL
        String contextPath = sanitizeContextPath(baseUrl);

        // Start embedded Jetty server with middleware
        AddonServlet servlet = new AddonServlet(addon);
        EmbeddedServer server = new EmbeddedServer(servlet, contextPath);

        // Add workspace context filter to extract workspace/user from settings JWT
        // This must run before SecurityHeadersFilter to ensure request attributes are available
        if (jwtVerifier != null) {
            server.addFilter(new WorkspaceContextFilter(jwt -> {
                try {
                    JwtVerifier.DecodedJwt decoded = jwtVerifier.verify(jwt);
                    return decoded != null ? decoded.payload() : null;
                } catch (Exception e) {
                    logger.debug("JWT verification failed in WorkspaceContextFilter: {}", e.getMessage());
                    return null;
                }
            }));
            logger.info("WorkspaceContextFilter registered with JWT verification");
        }

        // Always add basic security headers; configure frame-ancestors via ADDON_FRAME_ANCESTORS
        server.addFilter(new SecurityHeadersFilter());

        // Optional rate limiter via env: ADDON_RATE_LIMIT (double, requests/sec), ADDON_LIMIT_BY (ip|workspace)
        String rateLimit = System.getenv("ADDON_RATE_LIMIT");
        if (rateLimit != null && !rateLimit.isBlank()) {
            try {
                double permits = Double.parseDouble(rateLimit.trim());
                String limitBy = System.getenv().getOrDefault("ADDON_LIMIT_BY", "ip");
                server.addFilter(new RateLimiter(permits, limitBy));
                logger.info("Rate limiter enabled: {} req/sec by {}", permits, limitBy);
            } catch (NumberFormatException nfe) {
                logger.warn("Invalid ADDON_RATE_LIMIT value. Expected number, got: {}", rateLimit);
            }
        }

        // Optional CORS allowlist via env: ADDON_CORS_ORIGINS (comma-separated origins)
        String cors = System.getenv("ADDON_CORS_ORIGINS");
        if (cors != null && !cors.isBlank()) {
            server.addFilter(new CorsFilter(cors));
            logger.info("CORS enabled for origins: {}", cors);
        }

        // Optional request logging (headers scrubbed): ADDON_REQUEST_LOGGING=true
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "false"))
                || "1".equals(System.getenv().getOrDefault("ADDON_REQUEST_LOGGING", "0"))) {
            server.addFilter(new RequestLoggingFilter());
            logger.info("Request logging enabled (sensitive headers redacted)");
        }

        String storageMode = (rulesStore instanceof com.example.rules.store.DatabaseRulesStore) ? "Database" : "In-Memory";
        logger.info(
                "Rules Add-on starting | baseUrl={} | port={} | contextPath={} | storage={} | env={} | applyChanges={} | skipSignature={}",
                baseUrl,
                port,
                contextPath,
                storageMode,
                envLabel,
                RuntimeFlags.applyChangesEnabled(),
                RuntimeFlags.skipSignatureVerification());
        logger.info("Endpoints: manifest={} settings={} simple={} ifttt={} lifecycleInstall={} lifecycleDelete={} webhook={} health={} rulesApi={}",
                baseUrl + "/manifest.json",
                baseUrl + "/settings",
                baseUrl + "/simple",
                baseUrl + "/ifttt",
                baseUrl + "/lifecycle/installed",
                baseUrl + "/lifecycle/deleted",
                baseUrl + "/webhook",
                baseUrl + "/health",
                baseUrl + "/api/rules");

        // Add shutdown hook for graceful stop
        PooledDatabaseTokenStore managedTokenStore = tokenStore;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Shutting down Rules Add-on...");
                com.example.rules.cache.RuleCache.shutdown();
                if (managedTokenStore != null) {
                    managedTokenStore.close();
                }
                server.stop();
                logger.info("Rules Add-on shutdown complete");
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));

        server.start(port);
    }

    /**
     * Selects the appropriate rules store based on environment configuration.
     *
     * FAIL-FAST: If database URL is configured but connection fails, throws an exception
     * rather than silently falling back to in-memory storage. This prevents subtle data
     * loss bugs where the operator thinks persistence is enabled but rules are lost on restart.
     *
     * @return DatabaseRulesStore if configured and reachable, otherwise in-memory RulesStore
     * @throws IllegalStateException if database is configured but initialization fails
     */
    private static RulesStoreSPI selectRulesStore() {
        // Prefer RULES_DB_URL if present; fallback to DB_URL; else in-memory
        String rulesDbUrl = System.getenv("RULES_DB_URL");
        String dbUrl = System.getenv("DB_URL");
        boolean dbConfigured = (rulesDbUrl != null && !rulesDbUrl.isBlank()) || (dbUrl != null && !dbUrl.isBlank());

        if (dbConfigured) {
            try {
                DatabaseRulesStore store = DatabaseRulesStore.fromEnvironment();
                logger.info("✓ Rules storage initialized with database persistence");
                return store;
            } catch (Exception e) {
                // Database was explicitly configured but failed - this is a deployment error
                String msg = String.format(
                    "FATAL: Rules database storage is configured but unavailable. " +
                    "URL: %s | Error: %s | " +
                    "To fix: verify database credentials in environment variables (DB_URL, DB_USER, DB_PASSWORD, " +
                    "RULES_DB_URL) and ensure database is running and accessible.",
                    dbUrl != null && !dbUrl.isBlank() ? dbUrl : rulesDbUrl,
                    e.getMessage()
                );
                logger.error(msg, e);
                throw new IllegalStateException(msg, e);
            }
        }

        logger.info("✓ Rules storage initialized with in-memory persistence (ephemeral, recommended for dev only)");
        return new RulesStore();
    }

    static PooledDatabaseTokenStore initializeTokenStore(
            boolean persistentTokens,
            String envLabel,
            String dbUrl,
            String dbUser,
            String dbPassword,
            TokenStoreSupplier supplier
    ) {
        boolean hasDbUrl = dbUrl != null && !dbUrl.isBlank();
        if (!persistentTokens || !hasDbUrl) {
            logger.info("Using in-memory token store (ENV={}, DB_URL configured={})", envLabel, hasDbUrl);
            return null;
        }

        try {
            PooledDatabaseTokenStore tokenStore = supplier.get();
            com.clockify.addon.sdk.security.TokenStore.configurePersistence(tokenStore);
            logger.info("Persistent token store enabled (PostgreSQL)");
            return tokenStore;
        } catch (Exception e) {
            String msg = String.format(
                    "FATAL: Rules database storage is configured but unavailable. URL: %s | Error: %s | " +
                    "To fix: verify database credentials in environment variables (DB_URL, DB_USER, DB_PASSWORD, RULES_DB_URL) " +
                    "and ensure database is running and accessible.",
                    dbUrl,
                    e.getMessage()
            );
            logger.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @FunctionalInterface
    interface TokenStoreSupplier {
        PooledDatabaseTokenStore get() throws Exception;
    }

    private static JwtVerifier initializeJwtVerifier() {
        String pem = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY");
        String keyMapJson = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_MAP");
        if ((pem == null || pem.isBlank()) && (keyMapJson == null || keyMapJson.isBlank())) {
            logger.debug("CLOCKIFY_JWT_PUBLIC_KEY not set; settings JWT auto-fill disabled");
            return null;
        }
        try {
            JwtVerifier.Constraints constraints = JwtVerifier.Constraints.fromEnvironment();
            // Enforce sub == manifest key for UI/settings JWTs
            String expectedSubject = "rules";
            if (keyMapJson != null && !keyMapJson.isBlank()) {
                java.util.Map<String, String> pemByKid = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(keyMapJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
                String defaultKid = System.getenv("CLOCKIFY_JWT_DEFAULT_KID");
                JwtVerifier verifier = JwtVerifier.fromPemMap(pemByKid, defaultKid, constraints, expectedSubject);
                logger.info("Verified settings JWTs using {} kid-mapped keys (defaultKid={}, iss={}, aud={}, skew={}s)",
                        pemByKid.size(),
                        logValue(defaultKid),
                        logValue(constraints.expectedIssuer()),
                        logValue(constraints.expectedAudience()),
                        constraints.clockSkewSeconds());
                return verifier;
            }
            JwtVerifier verifier = JwtVerifier.fromPem(pem, constraints, expectedSubject);
            logger.info("Verified settings JWTs using configured public key (iss={}, aud={}, skew={}s)",
                    logValue(constraints.expectedIssuer()),
                    logValue(constraints.expectedAudience()),
                    constraints.clockSkewSeconds());
            return verifier;
        } catch (Exception e) {
            logger.error("Failed to initialize JWT verifier: {}", e.getMessage());
            return null;
        }
    }

    private static void preloadLocalSecrets() {
        String workspaceId = System.getenv("CLOCKIFY_WORKSPACE_ID");
        String installationToken = System.getenv("CLOCKIFY_INSTALLATION_TOKEN");
        if (workspaceId == null || workspaceId.isBlank() || installationToken == null || installationToken.isBlank()) {
            return;
        }

        String apiBaseUrl = System.getenv().getOrDefault("CLOCKIFY_API_BASE_URL", "https://api.clockify.me/api");
        try {
            com.clockify.addon.sdk.security.TokenStore.save(workspaceId, installationToken, apiBaseUrl);
            logger.info("Preloaded installation token for workspace {}", workspaceId);
        } catch (Exception e) {
            logger.warn("Failed to preload local installation token: {}", e.getMessage());
        }
    }

    static String sanitizeContextPath(String baseUrl) {
        String contextPath = "/";
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String sanitized = path.replaceAll("/+$", "");
                if (!sanitized.isEmpty()) {
                    contextPath = sanitized;
                }
            }
        } catch (java.net.URISyntaxException e) {
            logger.warn("Could not parse base URL '{}', using '/' as context path: {}", baseUrl, e.getMessage());
        }
        return contextPath;
    }

    private static HttpResponse workspaceRequired(HttpServletRequest request) {
        return ErrorResponse.of(400, "RULES.WORKSPACE_REQUIRED", "workspaceId is required", request, false);
    }

    private static HttpResponse internalError(HttpServletRequest request, String code, String message, Exception e, boolean retryable) {
        logger.error("{}: {}", code, e.getMessage(), e);
        return ErrorResponse.of(500, code, message, request, retryable, e.getMessage());
    }

    private static String logValue(String value) {
        return (value == null || value.isBlank()) ? "n/a" : value;
    }
}
