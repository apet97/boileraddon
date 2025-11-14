package com.clockify.addon.sdk.security.jwt;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtVerifierTest {

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final JwtVerifier.Constraints CONSTRAINTS =
            new JwtVerifier.Constraints("clockify", "rules", 60L, java.util.Set.of("RS256"));
    private static final JwtVerifier VERIFIER =
            JwtVerifier.forTesting(KEY_PAIR.getPublic(), CONSTRAINTS, FIXED_CLOCK);

    @Test
    void validToken_passesVerification() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", KEY_PAIR.getPrivate());
        assertDoesNotThrow(() -> VERIFIER.verify(token));
    }

    @Test
    void expiredToken_failsVerification() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).minusSeconds(70), // Expired beyond 60-second clock skew
                Instant.now(FIXED_CLOCK).minusSeconds(120), "clockify", "rules", KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void notYetValidToken_failsVerification() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).plusSeconds(300), "clockify", "rules", KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void issuerMismatchFails() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "other-issuer", "rules", KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void audienceMismatchFails() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "other", KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void invalidSignatureFails() throws Exception {
        KeyPair otherPair = generateKeyPair();
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", otherPair.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void kidSelectsExpectedKey() throws Exception {
        KeyPair kidPair = generateKeyPair();
        Map<String, PublicKey> kidKeys = new HashMap<>();
        kidKeys.put("alpha", kidPair.getPublic());
        JwtVerifier verifier = JwtVerifier.forTesting(kidKeys, null, CONSTRAINTS, FIXED_CLOCK);

        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", kidPair.getPrivate(), "alpha");
        assertDoesNotThrow(() -> verifier.verify(token));

        String wrongKidToken = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", KEY_PAIR.getPrivate(), "unknown");
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(wrongKidToken));
    }

    @Test
    void audienceArrayMatchesAnyValue() throws Exception {
        String token = createTokenWithAudArray(
                Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60),
                "clockify",
                new String[]{"foo", "rules", "bar"},
                KEY_PAIR.getPrivate());
        assertDoesNotThrow(() -> VERIFIER.verify(token));
    }

    @Test
    void audienceArrayMismatchFails() throws Exception {
        String token = createTokenWithAudArray(
                Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60),
                "clockify",
                new String[]{"foo", "bar"},
                KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void unsupportedAlgorithmRejectedEarly() throws Exception {
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules",
                KEY_PAIR.getPrivate(), "HS256", null);
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> VERIFIER.verify(token));
    }

    @Test
    void defaultKidUsedWhenMissingKid() throws Exception {
        KeyPair mapPair = generateKeyPair();
        Map<String, PublicKey> kidKeys = new HashMap<>();
        kidKeys.put("cfg", mapPair.getPublic());
        JwtVerifier verifier = JwtVerifier.forTesting(kidKeys, null, "cfg", CONSTRAINTS, FIXED_CLOCK);

        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", mapPair.getPrivate());
        assertDoesNotThrow(() -> verifier.verify(token));
    }

    @Test
    void missingKidWithoutDefaultKidFails() throws Exception {
        KeyPair mapPair = generateKeyPair();
        Map<String, PublicKey> kidKeys = new HashMap<>();
        kidKeys.put("cfg", mapPair.getPublic());
        JwtVerifier verifier = JwtVerifier.forTesting(kidKeys, null, CONSTRAINTS, FIXED_CLOCK);

        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", mapPair.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(token));
    }

    @Test
    void unknownKidWithDefaultKeyPresentFailsFast() throws Exception {
        KeyPair defaultPair = generateKeyPair();
        KeyPair kidPair = generateKeyPair();
        Map<String, PublicKey> kidKeys = new HashMap<>();
        kidKeys.put("known-kid", kidPair.getPublic());

        // Create verifier with default key and kid keys
        JwtVerifier verifier = JwtVerifier.forTesting(kidKeys, defaultPair.getPublic(), CONSTRAINTS, FIXED_CLOCK);

        // Token with unknown kid should fail fast even though default key exists
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules", defaultPair.getPrivate(), "unknown-kid");
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(token));
    }

    @Test
    void algorithmIntersectionEnforced() throws Exception {
        // Test that only algorithms in both constraints and SAFE_ALGS are allowed
        JwtVerifier.Constraints unsafeConstraints = new JwtVerifier.Constraints("clockify", "rules", 60L,
                java.util.Set.of("RS256", "HS256")); // HS256 is not in SAFE_ALGS
        JwtVerifier verifier = JwtVerifier.forTesting(KEY_PAIR.getPublic(), unsafeConstraints, FIXED_CLOCK);

        // HS256 should be rejected even though it's in constraints
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60), "clockify", "rules",
                KEY_PAIR.getPrivate(), "HS256", null);
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(token));
    }

    @Test
    void subjectMustMatchWhenExpected() throws Exception {
        JwtVerifier verifier = JwtVerifier.forTesting(KEY_PAIR.getPublic(), CONSTRAINTS, FIXED_CLOCK, "rules");
        String valid = createTokenWithSubject(
                Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60),
                "clockify",
                "rules",
                "rules",
                KEY_PAIR.getPrivate());
        assertDoesNotThrow(() -> verifier.verify(valid));

        String mismatch = createTokenWithSubject(
                Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60),
                "clockify",
                "rules",
                "other-addon",
                KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(mismatch));
    }

    @Test
    void missingSubjectFailsWhenExpected() throws Exception {
        JwtVerifier verifier = JwtVerifier.forTesting(KEY_PAIR.getPublic(), CONSTRAINTS, FIXED_CLOCK, "rules");
        String token = createToken(Instant.now(FIXED_CLOCK).plusSeconds(300),
                Instant.now(FIXED_CLOCK).minusSeconds(60),
                "clockify",
                "rules",
                KEY_PAIR.getPrivate());
        assertThrows(JwtVerifier.JwtVerificationException.class, () -> verifier.verify(token));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate test key pair", e);
        }
    }

    private static String createToken(Instant exp, Instant nbf, String iss, String aud, PrivateKey key) throws Exception {
        return createToken(exp, nbf, iss, aud, key, "RS256", null);
    }

    private static String createToken(Instant exp, Instant nbf, String iss, String aud,
                                      PrivateKey key, String kid) throws Exception {
        return createToken(exp, nbf, iss, aud, key, "RS256", kid);
    }

    private static String createToken(Instant exp, Instant nbf, String iss, String aud,
                                      PrivateKey key, String alg, String kid) throws Exception {
        String headerJson = kid == null
                ? String.format("{\"alg\":\"%s\",\"typ\":\"JWT\"}", alg)
                : String.format("{\"alg\":\"%s\",\"typ\":\"JWT\",\"kid\":\"%s\"}", alg, kid);
        String payloadJson = String.format("{\"iss\":\"%s\",\"aud\":\"%s\",\"exp\":%d,\"nbf\":%d}",
                iss, aud, exp.getEpochSecond(), nbf.getEpochSecond());
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload, key);
        return header + "." + payload + "." + signature;
    }

    private static String createTokenWithAudArray(Instant exp, Instant nbf, String iss,
                                                  String[] audiences, PrivateKey key) throws Exception {
        String audJson = java.util.Arrays.stream(audiences)
                .map(a -> "\"" + a + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("\"rules\"");
        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"iss\":\"%s\",\"aud\":[%s],\"exp\":%d,\"nbf\":%d}",
                iss, audJson, exp.getEpochSecond(), nbf.getEpochSecond());
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload, key);
        return header + "." + payload + "." + signature;
    }

    private static String createTokenWithSubject(Instant exp, Instant nbf, String iss, String aud,
                                                 String subject, PrivateKey key) throws Exception {
        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"iss\":\"%s\",\"aud\":\"%s\",\"exp\":%d,\"nbf\":%d,\"sub\":\"%s\"}",
                iss, aud, exp.getEpochSecond(), nbf.getEpochSecond(), subject);
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload, key);
        return header + "." + payload + "." + signature;
    }

    private static String sign(String data, PrivateKey key) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(data.getBytes(StandardCharsets.US_ASCII));
        byte[] sig = signature.sign();
        return base64Url(sig);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
