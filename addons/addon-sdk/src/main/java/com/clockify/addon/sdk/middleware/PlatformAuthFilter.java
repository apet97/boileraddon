package com.clockify.addon.sdk.middleware;

import com.clockify.addon.sdk.security.jwt.JwtVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that enforces Clockify platform auth tokens on protected endpoints.
 */
public final class PlatformAuthFilter implements Filter {
    public static final String ATTR_INSTALLATION_ID = "clockify.installationId";
    public static final String ATTR_WORKSPACE_ID = "clockify.workspaceId";
    public static final String ATTR_USER_ID = "clockify.userId";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JwtVerifier verifier;

    public PlatformAuthFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.sendError(401, "missing bearer");
            return;
        }
        String token = auth.substring("Bearer ".length()).trim();

        try {
            JwtVerifier.DecodedJwt jwt = verifier.verify(token);
            JsonNode payload = jwt.payload();
            String installationId = text(payload, "installation_id", "installationId");
            String workspaceId = text(payload, "workspace_id", "workspaceId", "wid");
            String userId = text(payload, "user_id", "uid");

            if (installationId == null || workspaceId == null) {
                response.sendError(403, "missing claims");
                return;
            }
            request.setAttribute(ATTR_INSTALLATION_ID, installationId);
            request.setAttribute(ATTR_WORKSPACE_ID, workspaceId);
            request.setAttribute(ATTR_USER_ID, userId);

            chain.doFilter(request, response);
        } catch (JwtVerifier.JwtVerificationException e) {
            response.sendError(401, "invalid jwt");
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    private static String text(JsonNode node, String... names) {
        for (String n : names) {
            JsonNode v = node.get(n);
            if (v != null && v.isTextual()) {
                return v.asText();
            }
        }
        return null;
    }

    /**
     * DEV-ONLY helper for standalone experiments.
     */
    public static PlatformAuthFilter devFromEnv() throws Exception {
        JwtVerifier.Constraints constraints = new JwtVerifier.Constraints(
                System.getenv("CLOCKIFY_JWT_EXPECT_ISS"),
                System.getenv("CLOCKIFY_JWT_EXPECT_AUD"),
                30, Set.of("RS256"));
        JwtVerifier verifier;
        String map = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_MAP");
        String pem = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_PEM");
        String jwksUri = System.getenv("CLOCKIFY_JWT_JWKS_URI");

        if (jwksUri != null && !jwksUri.isBlank()) {
            var keySource = new com.clockify.addon.sdk.security.jwt.JwksBasedKeySource(java.net.URI.create(jwksUri));
            verifier = JwtVerifier.fromKeySource(keySource, constraints);
        } else if (map != null && !map.isBlank()) {
            Map<String, String> pemMap = MAPPER.readValue(map, MAPPER.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            verifier = JwtVerifier.fromPemMap(pemMap, System.getenv("CLOCKIFY_JWT_DEFAULT_KID"), constraints);
        } else if (pem != null && !pem.isBlank()) {
            verifier = JwtVerifier.fromPem(pem, constraints);
        } else {
            throw new IllegalStateException("Configure CLOCKIFY_JWT_JWKS_URI, CLOCKIFY_JWT_PUBLIC_KEY_MAP or CLOCKIFY_JWT_PUBLIC_KEY_PEM");
        }
        return new PlatformAuthFilter(verifier);
    }
}
