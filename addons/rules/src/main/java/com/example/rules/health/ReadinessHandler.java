package com.example.rules.health;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import com.example.rules.store.RulesStoreSPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight readiness probe for container orchestrators.
 * Reports DOWN (503) if the rules store or token store cannot be reached.
 */
public class ReadinessHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReadinessHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RulesStoreSPI rulesStore;
    private final PooledDatabaseTokenStore tokenStore;

    public ReadinessHandler(RulesStoreSPI rulesStore, PooledDatabaseTokenStore tokenStore) {
        this.rulesStore = rulesStore;
        this.tokenStore = tokenStore;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        boolean rulesReady = isRulesStoreReady();
        boolean tokenReady = isTokenStoreReady();

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        String status = (rulesReady && tokenReady) ? "READY" : "DEGRADED";
        root.put("status", status);

        ObjectNode checks = root.putObject("checks");
        checks.put("rulesStore", rulesReady ? "UP" : "DOWN");
        checks.put("tokenStore", tokenStore == null ? "SKIPPED" : (tokenReady ? "UP" : "DOWN"));

        int statusCode = (rulesReady && tokenReady) ? 200 : 503;
        return new HttpResponse(statusCode, root.toString(), "application/json");
    }

    private boolean isRulesStoreReady() {
        if (rulesStore == null) {
            return true;
        }
        try {
            rulesStore.getAll("health-probe");
            return true;
        } catch (Exception e) {
            logger.warn("Rules store readiness failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenStoreReady() {
        if (tokenStore == null) {
            return true;
        }
        try {
            tokenStore.count();
            return true;
        } catch (Exception e) {
            logger.warn("Token store readiness failed: {}", e.getMessage());
            return false;
        }
    }
}
