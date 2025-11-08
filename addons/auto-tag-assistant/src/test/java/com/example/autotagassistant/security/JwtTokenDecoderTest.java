package com.example.autotagassistant.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenDecoderTest {

    @Test
    void decodeSplitsHeaderPayloadAndSignature() {
        String token = buildToken("{\"alg\":\"RS256\"}", "{\"backendUrl\":\"https://api.example.com\"}", "signature");

        JwtTokenDecoder.DecodedJwt decoded = JwtTokenDecoder.decode(token);

        assertEquals("RS256", decoded.header().get("alg").asText());
        assertEquals("https://api.example.com", decoded.payload().get("backendUrl").asText());
        assertEquals("signature", decoded.signature());
    }

    @Test
    void extractEnvironmentClaimsReturnsUrls() {
        ObjectNode payload = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        payload.put("backendUrl", "https://api.clockify.me/api");
        payload.put("apiUrl", "https://api.clockify.me/api/v1");
        payload.put("reportsUrl", "https://reports.clockify.me");
        payload.put("locationsUrl", "https://locations.clockify.me");
        payload.put("screenshotsUrl", "https://screens.clockify.me");

        JwtTokenDecoder.EnvironmentClaims claims = JwtTokenDecoder.extractEnvironmentClaims(payload);
        Map<String, String> map = claims.asMap();

        assertEquals("https://api.clockify.me/api", claims.backendUrl());
        assertEquals("https://api.clockify.me/api/v1", claims.apiUrl());
        assertEquals("https://reports.clockify.me", map.get("reportsUrl"));
        assertEquals(5, map.size());
    }

    @Test
    void decodeRejectsInvalidTokens() {
        assertThrows(IllegalArgumentException.class, () -> JwtTokenDecoder.decode("not-a-jwt"));
    }

    private static String buildToken(String headerJson, String payloadJson, String signature) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
