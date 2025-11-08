package com.clockify.addon.sdk;

/**
 * Immutable representation of an HTTP response returned by {@link RequestHandler}s.
 */
public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final String contentType;

    public HttpResponse(int statusCode, String body, String contentType) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
    }

    /**
     * Convenience factory for a {@code 200 OK} plain-text response.
     */
    public static HttpResponse ok(String body) {
        return new HttpResponse(200, body, "text/plain");
    }

    /**
     * Convenience factory for a {@code 200 OK} response with a custom content type.
     */
    public static HttpResponse ok(String body, String contentType) {
        return new HttpResponse(200, body, contentType);
    }

    /**
     * Convenience factory for a non-OK response using {@code text/plain}.
     */
    public static HttpResponse error(int statusCode, String message) {
        return new HttpResponse(statusCode, message, "text/plain");
    }

    /**
     * Convenience factory for a non-OK response with a custom content type.
     */
    public static HttpResponse error(int statusCode, String message, String contentType) {
        return new HttpResponse(statusCode, message, contentType);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }
}
