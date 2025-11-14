package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.clockify.addon.sdk.security.jwt.JwtVerifier;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class PlatformAuthFilterTest {

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
    private static final JwtVerifier.Constraints CONSTRAINTS =
            new JwtVerifier.Constraints("clockify", "rules", 60L, java.util.Set.of("RS256"));

    private PlatformAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PlatformAuthFilter(
                JwtVerifier.forTesting(KEY_PAIR.getPublic(), CONSTRAINTS, FIXED_CLOCK, "rules"));
    }

    @Test
    void missingAuthorizationHeaderReturns401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).sendError(401, "missing bearer");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-token");

        filter.doFilter(request, response, chain);

        verify(response).sendError(401, "invalid jwt");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void missingClaimsReturn403() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String token = signToken(Map.of("installation_id", "inst-1")); // missing workspace_id
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilter(request, response, chain);

        verify(response).sendError(403, "missing claims");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void missingInstallationIdReturns403() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String token = signToken(Map.of("workspace_id", "ws-200")); // missing installation_id
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilter(request, response, chain);

        verify(response).sendError(403, "missing claims");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validTokenSetsAttributesAndContinues() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String token = signToken(Map.of(
                "installation_id", "inst-123",
                "workspace_id", "ws-88",
                "user_id", "user-9"));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(request).setAttribute(PlatformAuthFilter.ATTR_INSTALLATION_ID, "inst-123");
        verify(request).setAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID, "ws-88");
        verify(request).setAttribute(PlatformAuthFilter.ATTR_USER_ID, "user-9");
    }

    private static String signToken(Map<String, String> extraClaims) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", "clockify");
        payload.put("aud", "rules");
        payload.put("exp", Instant.now(FIXED_CLOCK).plusSeconds(300).getEpochSecond());
        payload.put("nbf", Instant.now(FIXED_CLOCK).minusSeconds(60).getEpochSecond());
        payload.put("sub", "rules");
        payload.putAll(extraClaims);

        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payloadJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadEncoded = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payloadEncoded, KEY_PAIR.getPrivate());
        return header + "." + payloadEncoded + "." + signature;
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String sign(String data, PrivateKey key) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(data.getBytes(StandardCharsets.US_ASCII));
        return base64Url(signature.sign());
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate key pair", e);
        }
    }
}
