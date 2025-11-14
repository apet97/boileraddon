package com.clockify.addon.sdk.security.jwt;

import java.net.URI;
import java.util.Map;

/**
 * Factory for building {@link JwtVerifier} instances from {@link JwtBootstrapConfig}.
 */
public final class JwtVerifierFactory {
    private JwtVerifierFactory() {}

    public static JwtVerifier create(JwtBootstrapConfig config,
                                     JwtVerifier.Constraints constraints,
                                     String expectedSubject) throws Exception {
        return switch (config.source()) {
            case JWKS_URI -> buildFromJwks(config, constraints, expectedSubject);
            case KEY_MAP -> buildFromKeyMap(config, constraints, expectedSubject);
            case PUBLIC_KEY -> buildFromPem(config, constraints, expectedSubject);
        };
    }

    private static JwtVerifier buildFromJwks(JwtBootstrapConfig config,
                                             JwtVerifier.Constraints constraints,
                                             String expectedSubject) throws Exception {
        String uri = config.jwksUri().orElseThrow(() -> new IllegalArgumentException("CLOCKIFY_JWT_JWKS_URI must be set"));
        JwtVerifier verifier = JwtVerifier.fromKeySource(
                new JwksBasedKeySource(URI.create(uri)),
                constraints,
                expectedSubject
        );
        return verifier;
    }

    private static JwtVerifier buildFromKeyMap(JwtBootstrapConfig config,
                                               JwtVerifier.Constraints constraints,
                                               String expectedSubject) throws Exception {
        String json = config.keyMapJson()
                .orElseThrow(() -> new IllegalArgumentException("CLOCKIFY_JWT_PUBLIC_KEY_MAP must be set"));
        Map<String, String> pemByKid = JwtVerifier.parsePemMap(json);
        if (pemByKid.isEmpty()) {
            throw new IllegalArgumentException("CLOCKIFY_JWT_PUBLIC_KEY_MAP did not contain any entries");
        }
        return JwtVerifier.fromPemMap(pemByKid, config.defaultKid().orElse(null), constraints, expectedSubject);
    }

    private static JwtVerifier buildFromPem(JwtBootstrapConfig config,
                                            JwtVerifier.Constraints constraints,
                                            String expectedSubject) throws Exception {
        String pem = config.publicKeyPem()
                .orElseThrow(() -> new IllegalArgumentException("CLOCKIFY_JWT_PUBLIC_KEY(_PEM) must be set"));
        return JwtVerifier.fromPem(pem, constraints, expectedSubject);
    }
}
