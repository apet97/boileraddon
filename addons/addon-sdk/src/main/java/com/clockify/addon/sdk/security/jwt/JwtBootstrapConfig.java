package com.clockify.addon.sdk.security.jwt;

import java.util.Optional;

/**
 * Parsed configuration describing how to bootstrap JWT verification from environment variables.
 */
public record JwtBootstrapConfig(
        Optional<String> publicKeyPem,
        Optional<String> keyMapJson,
        Optional<String> jwksUri,
        Optional<String> defaultKid,
        Optional<String> expectedIssuer,
        Optional<String> expectedAudience,
        long leewaySeconds,
        JwtKeySource source
) {
    public enum JwtKeySource {
        JWKS_URI,
        KEY_MAP,
        PUBLIC_KEY
    }
}
