package com.example.addon.sdk;

/**
 * Simple HTTP response wrapper.
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

    public static HttpResponse ok(String body) {
        return new HttpResponse(200, body, "text/plain");
    }

    public static HttpResponse ok(String body, String contentType) {
        return new HttpResponse(200, body, contentType);
    }

    public static HttpResponse error(int statusCode, String message) {
        return new HttpResponse(statusCode, message, "text/plain");
    }

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
