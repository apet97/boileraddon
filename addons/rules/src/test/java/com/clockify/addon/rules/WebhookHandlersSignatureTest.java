package com.clockify.addon.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.clockify.addon.sdk.security.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebhookHandlersSignatureTest {
    private static final String WORKSPACE_ID = "ws-123";

    @AfterEach
    void cleanup() {
        TokenStore.clear();
        System.clearProperty("CLOCKIFY_JWT_PUBLIC_KEY");
        System.clearProperty("CLOCKIFY_JWT_EXPECTED_ISS");
        System.clearProperty("CLOCKIFY_JWT_EXPECT_ISS");
    }

    @Test
    void rejectsJwtWithMismatchedAddonKey() throws Exception {
        TokenStore.save(WORKSPACE_ID, "token", "https://api.clockify.me/api");
        JwtFixture fixture = mintJwt("other-addon", WORKSPACE_ID);
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", fixture.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        String body = "{\"workspaceId\":\"" + WORKSPACE_ID + "\",\"timeEntry\":{\"id\":\"t1\",\"description\":\"demo\"}}";
        String jwt = fixture.jwt;

        HttpServletRequest request = buildRequest(body, jwt);
        RequestHandler handler = registerHandler();
        assertNotNull(handler);

        HttpResponse response = handler.handle(request);

        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("invalid jwt subject"));
    }

    @Test
    void acceptsJwtWhenAddonKeyMatches() throws Exception {
        TokenStore.save(WORKSPACE_ID, "token", "https://api.clockify.me/api");
        JwtFixture fixture = mintJwt("rules", WORKSPACE_ID);
        System.setProperty("CLOCKIFY_JWT_PUBLIC_KEY", fixture.pemPublic);
        System.setProperty("CLOCKIFY_JWT_EXPECTED_ISS", "clockify");

        String body = "{\"workspaceId\":\"" + WORKSPACE_ID + "\",\"timeEntry\":{\"id\":\"t1\",\"description\":\"demo\"}}";
        String jwt = fixture.jwt;

        HttpServletRequest request = buildRequest(body, jwt);
        RequestHandler handler = registerHandler();
        assertNotNull(handler);

        HttpResponse response = handler.handle(request);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"no-rules\""));
    }

    private static RequestHandler registerHandler() {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Automate Clockify time entries with if-this-then-that actions.")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon, false);
        return addon.getWebhookHandlers().get("TIME_ENTRY_UPDATED");
    }

    private static HttpServletRequest buildRequest(String body, String jwt) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();

        when(request.getHeader(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return "Clockify-Signature".equalsIgnoreCase(name) ? jwt : null;
        });
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));

        return request;
    }

    private static JwtFixture mintJwt(String addonKey, String workspaceId) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        long exp = Instant.now().plusSeconds(300).getEpochSecond();
        String payloadJson = String.format(
                "{\"iss\":\"clockify\",\"type\":\"addon\",\"sub\":\"%s\",\"workspaceId\":\"%s\",\"exp\":%d}",
                addonKey,
                workspaceId,
                exp
        );

        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        String signingInput = urlEncoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8))
                + "."
                + urlEncoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign((PrivateKey) keyPair.getPrivate());
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String signature = urlEncoder.encodeToString(signer.sign());

        String jwt = signingInput + "." + signature;
        String pemPublic = toPem(keyPair.getPublic().getEncoded());
        return new JwtFixture(jwt, pemPublic);
    }

    private static String toPem(byte[] derBytes) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(derBytes);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    private record JwtFixture(String jwt, String pemPublic) {}
}
