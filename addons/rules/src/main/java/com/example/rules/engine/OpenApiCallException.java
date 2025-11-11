package com.example.rules.engine;

/**
 * Provides structured error details when an OpenAPI action fails.
 */
public class OpenApiCallException extends Exception {
    private final int statusCode;
    private final boolean retryable;
    private final String responseBody;
    private final Long retryAfterMillis;

    public OpenApiCallException(String message, int statusCode, boolean retryable, String responseBody, Long retryAfterMillis) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.responseBody = responseBody;
        this.retryAfterMillis = retryAfterMillis;
    }

    public OpenApiCallException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = retryable;
        this.responseBody = null;
        this.retryAfterMillis = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
