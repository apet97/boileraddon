package com.example.autotagassistant.security;

import com.clockify.addon.sdk.HttpResponse;
import com.example.autotagassistant.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates incoming webhook requests using the Clockify shared secret scheme.
 */
public final class WebhookSignatureValidator {
    public static final String SIGNATURE_HEADER = "clockify-webhook-signature";
    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final String RAW_BODY_ATTRIBUTE = "clockify.rawBody";

    private WebhookSignatureValidator() {
        // Utility class
    }

    /**
     * Validate the webhook signature using the stored installation token for the workspace.
     *
     * @param request     incoming HTTP request
     * @param workspaceId workspace identifier extracted from the payload
     * @return {@link VerificationResult} describing whether the signature is valid
     */
    public static VerificationResult verify(HttpServletRequest request, String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            logger.warn("Webhook request missing workspaceId claim; rejecting request");
            return VerificationResult.unauthorized("Missing workspace context for webhook signature validation");
        }

        String providedSignature = headerValue(request, SIGNATURE_HEADER);
        if (providedSignature == null || providedSignature.isBlank()) {
            logger.warn("Webhook request for workspace {} is missing {} header", workspaceId, SIGNATURE_HEADER);
            return VerificationResult.unauthorized("Missing webhook signature header");
        }

        TokenStore.WorkspaceToken workspaceToken = TokenStore.get(workspaceId).orElse(null);
        if (workspaceToken == null) {
            logger.warn("No installation token stored for workspace {}; refusing webhook", workspaceId);
            return VerificationResult.unauthorized("Unknown workspace or installation secret");
        }

        try {
            String rawBody = readRawBody(request);
            if (!verifySignature(workspaceToken.authToken(), rawBody, providedSignature)) {
                logger.warn("Webhook signature mismatch for workspace {}", workspaceId);
                return VerificationResult.forbidden("Invalid webhook signature");
            }
            return VerificationResult.success();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read webhook body for signature validation", e);
        }
    }

    /**
     * Compute the expected signature for a raw webhook body.
     * This is exposed to facilitate tests and local tooling.
     */
    public static String computeSignature(String installationToken, String rawBody) {
        Objects.requireNonNull(installationToken, "installationToken");
        Objects.requireNonNull(rawBody, "rawBody");

        try {
            byte[] keyBytes = deriveKey(installationToken);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(computed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }

    private static boolean verifySignature(String installationToken, String rawBody, String providedSignature) {
        String expectedSignature = computeSignature(installationToken, rawBody);

        try {
            byte[] expectedBytes = Base64.getDecoder().decode(expectedSignature);
            byte[] providedBytes = Base64.getDecoder().decode(providedSignature);
            return MessageDigest.isEqual(expectedBytes, providedBytes);
        } catch (IllegalArgumentException e) {
            logger.warn("Received non-Base64 webhook signature");
            return false;
        }
    }

    private static String readRawBody(HttpServletRequest request) throws IOException {
        Object cachedBody = request.getAttribute(RAW_BODY_ATTRIBUTE);
        if (cachedBody instanceof String) {
            return (String) cachedBody;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String body = sb.toString();
        request.setAttribute(RAW_BODY_ATTRIBUTE, body);
        return body;
    }

    private static String headerValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null ? value.trim() : null;
    }

    private static byte[] deriveKey(String installationToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(installationToken.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive webhook signature key", e);
        }
    }

    /**
     * Outcome of webhook signature verification.
     */
    public record VerificationResult(boolean valid, HttpResponse response) {
        private static final VerificationResult VALID = new VerificationResult(true, null);

        public boolean isValid() {
            return valid;
        }

        public static VerificationResult success() {
            return VALID;
        }

        public static VerificationResult unauthorized(String message) {
            return new VerificationResult(false, HttpResponse.error(401, message));
        }

        public static VerificationResult forbidden(String message) {
            return new VerificationResult(false, HttpResponse.error(403, message));
        }
    }
}
