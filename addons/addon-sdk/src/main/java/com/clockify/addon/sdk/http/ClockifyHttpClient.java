package com.clockify.addon.sdk.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal HTTP client wrapper with sane timeouts and retries for 429/5xx.
 * Adds the x-addon-token header for workspace-scoped requests.
 */
public class ClockifyHttpClient {
    private final HttpClient client;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;

    public ClockifyHttpClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(10), 3);
    }

    public ClockifyHttpClient(String baseUrl, Duration timeout, int maxRetries) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public HttpResponse<String> get(String path, String addonToken, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = baseRequest(path, addonToken, headers).GET();
        return sendWithRetry(b.build());
    }

    public HttpResponse<String> postJson(String path, String addonToken, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = baseRequest(path, addonToken, headers)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        return sendWithRetry(b.build());
    }

    public HttpResponse<String> postJsonWithIdempotency(String path, String addonToken, String jsonBody, Map<String, String> headers) throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        HttpRequest.Builder b = baseRequest(path, addonToken, headers)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        return sendWithRetry(b.build());
    }

    public HttpResponse<String> putJson(String path, String addonToken, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = baseRequest(path, addonToken, headers)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
        return sendWithRetry(b.build());
    }

    public HttpResponse<String> patchJson(String path, String addonToken, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = baseRequest(path, addonToken, headers)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody));
        return sendWithRetry(b.build());
    }

    public HttpResponse<String> delete(String path, String addonToken, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = baseRequest(path, addonToken, headers).DELETE();
        return sendWithRetry(b.build());
    }

    private HttpRequest.Builder baseRequest(String path, String addonToken, Map<String, String> headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + normalize(path)))
                .timeout(timeout)
                .header("x-addon-token", addonToken)
                .header("Accept", "application/json");
        if (headers != null) {
            headers.forEach(b::header);
        }
        return b;
    }

    private static String normalize(String p) {
        return p.startsWith("/") ? p : "/" + p;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest req) throws Exception {
        int attempt = 0;
        long backoffMs = 300L;
        while (true) {
            attempt++;
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();

            if (code < 500 && code != 429) {
                return resp; // success or client error
            }

            if (attempt > maxRetries) {
                return resp; // give up
            }

            long sleep = retryAfterMillis(resp).orElse(backoffMs);
            Thread.sleep(sleep);
            backoffMs = Math.min(backoffMs * 2, 3000L);
        }
    }

    private Optional<Long> retryAfterMillis(HttpResponse<?> resp) {
        return resp.headers().firstValue("Retry-After").map(v -> {
            try { return Long.parseLong(v) * 1000L; } catch (NumberFormatException e) { return 0L; }
        });
    }
}
