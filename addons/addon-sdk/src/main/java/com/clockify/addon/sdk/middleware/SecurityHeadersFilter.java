package com.clockify.addon.sdk.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityHeadersFilter implements Filter {
  public static final String CSP_NONCE_ATTR = "clockify.csp.nonce";
  private static final SecureRandom RNG = new SecureRandom();

  private static String newNonce() {
    byte[] b = new byte[16];
    RNG.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    // Per-request nonce
    String nonce = newNonce();
    request.setAttribute(CSP_NONCE_ATTR, nonce);

    // Security headers
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Permissions-Policy", "browsing-topics=(), geolocation=(), microphone=(), camera=(), payment=()");

    // HSTS only when TLS or terminated upstream
    if (request.isSecure()
        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
      response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }

    // Embed only self and Clockify
    String frameAncestors = System.getenv().getOrDefault(
        "ADDON_FRAME_ANCESTORS",
        System.getenv().getOrDefault(
            "CLOCKIFY_FRAME_ANCESTORS",
            "'self' https://*.clockify.me https://*.clockify.com https://developer.clockify.me"));

    String csp =
        "upgrade-insecure-requests; " +
        "default-src 'self'; " +
        "object-src 'none'; " +
        "img-src 'self' data:; " +
        "connect-src 'self' https://api.clockify.me https://developer.clockify.me; " +
        "base-uri 'self'; form-action 'self'; " +
        "style-src 'nonce-" + nonce + "' 'self'; " +
        "script-src 'nonce-" + nonce + "'; " +
        "frame-ancestors " + frameAncestors + ";";

    response.setHeader("Content-Security-Policy", csp);

    chain.doFilter(request, response);
  }
}
