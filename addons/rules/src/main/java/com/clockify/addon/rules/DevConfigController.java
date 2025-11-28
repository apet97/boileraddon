package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

final class DevConfigController implements RequestHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RulesConfiguration config;

    DevConfigController(RulesConfiguration config) {
        this.config = config;
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("addonKey", config.addonKey());
        root.put("environment", config.environment());
        root.put("baseUrl", config.baseUrl());
        root.put("tokenStore", "memory");
        root.put("applyChanges", config.applyChanges());
        String jwtMode = config.jwtBootstrap()
                .map(jwt -> jwt.source().name().toLowerCase(Locale.ROOT))
                .orElse("disabled");
        root.put("jwtMode", jwtMode);
        root.put("devOnly", config.isDev());
        return HttpResponse.ok(root.toString(), "application/json");
    }
}
