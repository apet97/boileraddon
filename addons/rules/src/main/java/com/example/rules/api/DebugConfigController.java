package com.example.rules.api;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.example.rules.config.RulesConfiguration;
import com.example.rules.config.RuntimeFlags;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Dev-only configuration snapshot endpoint.
 * Surfaces non-sensitive runtime wiring details (idempotency backend, token store mode, etc.).
 */
public final class DebugConfigController implements RequestHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RulesConfiguration config;
    private final Supplier<String> idempotencyBackendSupplier;
    private final String tokenStoreMode;

    public DebugConfigController(RulesConfiguration config,
                                 Supplier<String> idempotencyBackendSupplier,
                                 String tokenStoreMode) {
        this.config = config;
        this.idempotencyBackendSupplier = idempotencyBackendSupplier;
        this.tokenStoreMode = tokenStoreMode;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("environment", config.environment());
        root.put("dedupTtlMillis", config.webhookDeduplicationTtlMillis());
        root.put("idempotencyBackend", safeString(idempotencyBackendSupplier.get()));

        ObjectNode database = root.putObject("database");
        database.put("rules", config.rulesDatabase().isPresent() ? "configured" : "disabled");
        database.put("shared", config.sharedDatabase().isPresent() ? "configured" : "disabled");

        root.put("tokenStore", tokenStoreMode);
        String jwtMode = config.jwtBootstrap()
                .map(jwt -> jwt.source().name().toLowerCase(Locale.ROOT))
                .orElse("disabled");
        root.put("jwtMode", jwtMode);

        ObjectNode runtime = root.putObject("runtimeFlags");
        runtime.put("applyChanges", RuntimeFlags.applyChangesEnabled());
        runtime.put("skipSignatureVerify", RuntimeFlags.skipSignatureVerification());

        return HttpResponse.ok(root.toString(), "application/json");
    }

    private static String safeString(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
