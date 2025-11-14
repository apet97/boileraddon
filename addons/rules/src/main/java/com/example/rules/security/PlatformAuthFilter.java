package com.example.rules.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public final class PlatformAuthFilter implements Filter {
  public static final String ATTR_INSTALLATION_ID = "clockify.installationId";
  public static final String ATTR_WORKSPACE_ID = "clockify.workspaceId";
  public static final String ATTR_USER_ID = "clockify.userId";

  private static final ObjectMapper M = new ObjectMapper();
  private final JwtVerifier verifier;

  public PlatformAuthFilter(JwtVerifier verifier) {
    this.verifier = verifier;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    // Expect portal-signed JWT in Authorization: Bearer <token>
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      response.sendError(401, "missing bearer");
      return;
    }
    String token = auth.substring("Bearer ".length()).trim();

    try {
      JwtVerifier.DecodedJwt jwt = verifier.verify(token);
      JsonNode p = jwt.payload();
      // These claim names are typical. Adjust to your portal's schema if different.
      String installationId = text(p, "installation_id", "installationId");
      String workspaceId = text(p, "workspace_id", "workspaceId", "wid");
      String userId = text(p, "user_id", "uid");

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

  private static String text(JsonNode node, String... names) {
    for (String n : names) {
      JsonNode v = node.get(n);
      if (v != null && v.isTextual()) return v.asText();
    }
    return null;
  }

  /**
   * DEV-ONLY helper for standalone experiments. Production code wires PlatformAuthFilter
   * via {@link com.example.rules.config.RulesConfiguration} and should not read System
   * environment variables directly.
   */
  public static PlatformAuthFilter devFromEnv() throws Exception {
    JwtVerifier.Constraints c = new JwtVerifier.Constraints(
        System.getenv("CLOCKIFY_JWT_EXPECT_ISS"),
        System.getenv("CLOCKIFY_JWT_EXPECT_AUD"),
        30, Set.of("RS256"));
    JwtVerifier v;
    String map = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_MAP");
    String pem = System.getenv("CLOCKIFY_JWT_PUBLIC_KEY_PEM");
    String jwksUri = System.getenv("CLOCKIFY_JWT_JWKS_URI");

    if (jwksUri != null && !jwksUri.isBlank()) {
      // Use JWKS-based key source
      JwksBasedKeySource keySource = new JwksBasedKeySource(java.net.URI.create(jwksUri));
      v = JwtVerifier.fromKeySource(keySource, c);
    } else if (map != null && !map.isBlank()) {
      Map<String,String> m = M.readValue(map, M.getTypeFactory().constructMapType(Map.class, String.class, String.class));
      v = JwtVerifier.fromPemMap(m, System.getenv("CLOCKIFY_JWT_DEFAULT_KID"), c);
    } else if (pem != null && !pem.isBlank()) {
      v = JwtVerifier.fromPem(pem, c);
    } else {
      throw new IllegalStateException("Configure CLOCKIFY_JWT_JWKS_URI, CLOCKIFY_JWT_PUBLIC_KEY_MAP or CLOCKIFY_JWT_PUBLIC_KEY_PEM");
    }
    return new PlatformAuthFilter(v);
  }
}
