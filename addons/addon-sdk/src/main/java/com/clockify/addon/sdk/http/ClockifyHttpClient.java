package com.clockify.addon.sdk.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Production-ready HTTP client for Clockify API with:
 * - Connection and request timeouts
 * - Automatic retries with exponential backoff
 * - Proper error handling and logging
 * - Rate limit awareness
 */
public class ClockifyHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(ClockifyHttpClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authToken;
    private final int maxRetries;
    private final Duration requestTimeout;

    /**
     * Creates a Clockify HTTP client with default settings.
     *
     * @param baseUrl The Clockify API base URL
     * @param authToken The workspace auth token
     */
    public ClockifyHttpClient(String baseUrl, String authToken) {
        this(baseUrl, authToken, 3, Duration.ofSeconds(30));
    }

    /**
     * Creates a Clockify HTTP client with custom settings.
     *
     * @param baseUrl The Clockify API base URL
     * @param authToken The workspace auth token
     * @param maxRetries Maximum retry attempts for failed requests
     * @param requestTimeout Timeout for each request
     */
    public ClockifyHttpClient(String baseUrl, String authToken, int maxRetries, Duration requestTimeout) {
        this.baseUrl = baseUrl;
        this.authToken = authToken;
        this.maxRetries = maxRetries;
        this.requestTimeout = requestTimeout;
        this.objectMapper = new ObjectMapper();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        logger.debug("ClockifyHttpClient initialized: baseUrl={}, timeout={}, maxRetries={}",
                baseUrl, requestTimeout, maxRetries);
    }

    /**
     * Executes a GET request.
     *
     * @param path API path (e.g., "/workspaces/{id}/tags")
     * @return Response body as JsonNode
     * @throws HttpException if request fails
     */
    public JsonNode get(String path) throws HttpException {
        return execute("GET", path, null);
    }

    /**
     * Executes a POST request with JSON body.
     *
     * @param path API path
     * @param body Request body (will be serialized to JSON)
     * @return Response body as JsonNode
     * @throws HttpException if request fails
     */
    public JsonNode post(String path, Object body) throws HttpException {
        return execute("POST", path, body);
    }

    /**
     * Executes a PUT request with JSON body.
     *
     * @param path API path
     * @param body Request body (will be serialized to JSON)
     * @return Response body as JsonNode
     * @throws HttpException if request fails
     */
    public JsonNode put(String path, Object body) throws HttpException {
        return execute("PUT", path, body);
    }

    /**
     * Executes a DELETE request.
     *
     * @param path API path
     * @return Response body as JsonNode
     * @throws HttpException if request fails
     */
    public JsonNode delete(String path) throws HttpException {
        return execute("DELETE", path, null);
    }

    /**
     * Executes an HTTP request with retries and error handling.
     */
    private JsonNode execute(String method, String path, Object body) throws HttpException {
        String url = baseUrl + path;
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(requestTimeout)
                        .header("x-addon-token", authToken)
                        .header("Accept", "application/json");

                // Add method and body
                if ("GET".equals(method)) {
                    requestBuilder.GET();
                } else if ("DELETE".equals(method)) {
                    requestBuilder.DELETE();
                } else if (body != null) {
                    String jsonBody = objectMapper.writeValueAsString(body);
                    requestBuilder.header("Content-Type", "application/json");
                    if ("POST".equals(method)) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                    } else if ("PUT".equals(method)) {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                    }
                } else {
                    throw new HttpException("Body required for " + method + " request", 400);
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                String responseBody = response.body();

                // Success
                if (statusCode >= 200 && statusCode < 300) {
                    logger.debug("{} {} -> {}", method, path, statusCode);
                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        return objectMapper.createObjectNode();
                    }
                    return objectMapper.readTree(responseBody);
                }

                // Rate limit - wait and retry
                if (statusCode == 429) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
                    int waitSeconds = Integer.parseInt(retryAfter);
                    logger.warn("Rate limited on {} {}, waiting {} seconds", method, path, waitSeconds);
                    Thread.sleep(waitSeconds * 1000L);
                    attempt++;
                    continue;
                }

                // Server errors - retry with backoff
                if (statusCode >= 500 && attempt < maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt) * 1000; // Exponential backoff
                    logger.warn("Server error {} on {} {}, retrying in {}ms (attempt {}/{})",
                            statusCode, method, path, backoffMs, attempt + 1, maxRetries);
                    Thread.sleep(backoffMs);
                    attempt++;
                    continue;
                }

                // Client errors - don't retry
                String errorMsg = String.format("%s %s failed with status %d: %s",
                        method, path, statusCode, responseBody);
                logger.error(errorMsg);
                throw new HttpException(errorMsg, statusCode, responseBody);

            } catch (HttpException e) {
                throw e; // Re-throw our custom exceptions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HttpException("Request interrupted: " + e.getMessage(), 0, e);
            } catch (Exception e) {
                lastException = e;
                logger.error("Error executing {} {} (attempt {}/{})",
                        method, path, attempt + 1, maxRetries + 1, e);

                if (attempt < maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt) * 1000;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new HttpException("Request interrupted during retry", 0, ie);
                    }
                    attempt++;
                } else {
                    break;
                }
            }
        }

        // Max retries exceeded
        String errorMsg = String.format("%s %s failed after %d attempts", method, path, maxRetries + 1);
        throw new HttpException(errorMsg, 0, lastException);
    }

    /**
     * Custom exception for HTTP errors.
     */
    public static class HttpException extends Exception {
        private final int statusCode;
        private final String responseBody;

        public HttpException(String message, int statusCode) {
            this(message, statusCode, (String) null);
        }

        public HttpException(String message, int statusCode, String responseBody) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public HttpException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
            this.responseBody = null;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }

        public boolean isServerError() {
            return statusCode >= 500;
        }

        public boolean isRateLimitError() {
            return statusCode == 429;
        }
    }
}
