package com.clockify.addon.sdk.testutil;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Test helpers to mint RS256 JWTs and serve a JWKS in unit/integration tests.
 */
public final class SignatureTestUtil {

    private SignatureTestUtil() {}

    /** Holds an RSA keypair and identifiers for tests. */
    public static final class RsaFixture {
        public final RSAKey jwk;          // Nimbus JWK with both keys
        public final String kid;          // key id
        public final String pemPublic;    // PEM PUBLIC KEY

        private RsaFixture(RSAKey jwk) {
            this.jwk = jwk;
            this.kid = jwk.getKeyID();
            this.pemPublic = toPemPublic(jwk);
        }

        public static RsaFixture generate(String kid) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) kp.getPublic())
                        .privateKey(kp.getPrivate())
                        .keyID(kid)
                        .build();
                return new RsaFixture(jwk);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static String toPemPublic(RSAKey rsa) {
            try {
                var pub = rsa.toRSAPublicKey();
                var encoded = pub.getEncoded(); // X.509 SubjectPublicKeyInfo
                var b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
                return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Build and sign a JWT with common Clockify claims. */
    public static String rs256Jwt(RsaFixture fixture, Builder b) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(fixture.kid)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(b.iss)
                    .subject(b.sub)
                    .expirationTime(Date.from(b.exp))
                    .claim("type", b.type);

            if (b.workspaceId != null) claims.claim("workspaceId", b.workspaceId);
            if (b.nbf != null) claims.notBeforeTime(Date.from(b.nbf));
            if (b.aud != null) claims.audience(b.aud);

            SignedJWT jwt = new SignedJWT(header, claims.build());
            jwt.sign(new RSASSASigner(fixture.jwk.toPrivateKey()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Fluent builder for JWT claims. Defaults: iss=clockify, type=addon, exp=now+300s. */
    public static final class Builder {
        String iss = "clockify";
        String type = "addon";
        String sub;                    // manifest key (required)
        String workspaceId;           // optional claim
        Instant exp = Instant.now().plusSeconds(300);
        Instant nbf;
        String aud;

        public Builder sub(String s) { this.sub = s; return this; }
        public Builder workspaceId(String w) { this.workspaceId = w; return this; }
        public Builder iss(String i) { this.iss = i; return this; }
        public Builder type(String t) { this.type = t; return this; }
        public Builder exp(Instant e) { this.exp = e; return this; }
        public Builder nbf(Instant n) { this.nbf = n; return this; }
        public Builder aud(String a) { this.aud = a; return this; }
    }

    /** Simple JWKS HTTP server for tests; call start(), then stop(). */
    public static final class JwksServer {
        private final HttpServer server;
        public final String url; // e.g., http://127.0.0.1:54321/.well-known/jwks.json

        private JwksServer(HttpServer s, String url) {
            this.server = s;
            this.url = url;
        }

        public static JwksServer start(RsaFixture... fixtures) {
            try {
                HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                s.setExecutor(Executors.newSingleThreadExecutor());
                java.util.List<com.nimbusds.jose.jwk.JWK> keys =
                        java.util.Arrays.stream(fixtures)
                                .map(f -> (com.nimbusds.jose.jwk.JWK) f.jwk.toPublicJWK())
                                .collect(java.util.stream.Collectors.toList());
                var jwkSet = new JWKSet(keys);
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var body = mapper.writeValueAsString(jwkSet.toJSONObject());

                s.createContext("/.well-known/jwks.json", new JsonHandler(body, Map.of("ETag", "\"test-etag\"")));
                s.start();
                String url = "http://127.0.0.1:" + s.getAddress().getPort() + "/.well-known/jwks.json";
                return new JwksServer(s, url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void stop() { server.stop(0); }

        private record JsonHandler(String body, Map<String,String> headers) implements HttpHandler {
            @Override public void handle(HttpExchange ex) throws IOException {
                headers.forEach((k,v) -> ex.getResponseHeaders().add(k, v));
                byte[] bytes = body.getBytes();
                ex.sendResponseHeaders(200, bytes.length);
                try (var os = ex.getResponseBody()) { os.write(bytes); }
            }
        }
    }
}
