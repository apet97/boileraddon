package com.clockify.addon.sdk.benchmarks;

import com.clockify.addon.sdk.security.WebhookSignatureValidator;
import com.clockify.addon.sdk.testing.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for webhook signature validation.
 *
 * Critical path: Every webhook received requires signature validation.
 * This benchmark measures HMAC-SHA256 validation performance.
 *
 * Run with: mvn test -Dtest=WebhookSignatureBenchmark -pl addons/addon-sdk
 * Or: java -jar target/benchmarks.jar WebhookSignatureBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = "-Xmx2g")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class WebhookSignatureBenchmark {

    private String sharedSecret;
    private byte[] payload;
    private String validSignature;
    private String invalidSignature;
    private ObjectMapper mapper;

    @Setup
    public void setup() throws Exception {
        mapper = new ObjectMapper();
        sharedSecret = "test-secret-key-12345";

        // Create a realistic webhook payload
        JsonNode timeEntry = TestFixtures.WEBHOOK_TIME_ENTRY_CREATED.get("timeEntry");
        String payloadStr = mapper.writeValueAsString(timeEntry);
        payload = payloadStr.getBytes(StandardCharsets.UTF_8);

        // Pre-compute valid signature using HMAC-SHA256
        validSignature = computeHmacSignature(payloadStr, sharedSecret);
        invalidSignature = "invalid-signature-xxxxxxxxxxxxxxxxxxxx";
    }

    /**
     * Benchmark: Validate a webhook with correct HMAC-SHA256 signature
     * This is the happy path for every webhook.
     */
    @Benchmark
    public void validateWebhookWithHmacSignature(Blackhole bh) {
        boolean result = WebhookSignatureValidator.validate(
            validSignature,
            payload,
            sharedSecret
        );
        bh.consume(result);
    }

    /**
     * Benchmark: Validate webhook with invalid signature (should fail quickly)
     * Tests error handling path.
     */
    @Benchmark
    public void validateWebhookWithInvalidSignature(Blackhole bh) {
        boolean result = WebhookSignatureValidator.validate(
            invalidSignature,
            payload,
            sharedSecret
        );
        bh.consume(result);
    }

    /**
     * Benchmark: Validate webhook with empty signature
     * Tests failure path when signature is missing.
     */
    @Benchmark
    public void validateWebhookWithEmptySignature(Blackhole bh) {
        boolean result = WebhookSignatureValidator.validate(
            "",
            payload,
            sharedSecret
        );
        bh.consume(result);
    }

    /**
     * Benchmark: Validate webhook with large payload (10KB)
     * Tests performance with realistic large webhook payloads.
     */
    @Benchmark
    public void validateWebhookWithLargePayload(Blackhole bh) throws Exception {
        // Create a 10KB payload by repeating data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append(new String(payload, StandardCharsets.UTF_8));
        }
        byte[] largePayload = sb.toString().getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacSignature(sb.toString(), sharedSecret);

        boolean result = WebhookSignatureValidator.validate(
            signature,
            largePayload,
            sharedSecret
        );
        bh.consume(result);
    }

    /**
     * Benchmark: Validate webhook with wrong shared secret
     * Tests performance when signature doesn't match secret.
     */
    @Benchmark
    public void validateWebhookWithWrongSecret(Blackhole bh) {
        boolean result = WebhookSignatureValidator.validate(
            validSignature,
            payload,
            "wrong-secret-key"  // Different secret
        );
        bh.consume(result);
    }

    // ============ Helper Methods ============

    private String computeHmacSignature(String payload, String secret) throws Exception {
        // Compute HMAC-SHA256 signature
        // This is a simplified version - actual implementation is in WebhookSignatureValidator
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec keySpec =
            new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.getBytes(StandardCharsets.UTF_8).length,
                "HmacSHA256"
            );
        mac.init(keySpec);
        byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        // Convert bytes to hex string (Java 17 compatible)
        StringBuilder hexString = new StringBuilder();
        for (byte b : result) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
