package com.clockify.addon.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable representation of an HTTP response returned by {@link RequestHandler}s.
 */
public class HttpResponse {
    private final int statusCode;
    private final String body;
    private final String contentType;
    private final Map<String, String> headers;

    public HttpResponse(int statusCode, String body, String contentType) {
        this(statusCode, body, contentType, Collections.emptyMap());
    }

    private HttpResponse(int statusCode, String body, String contentType, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
        this.headers = Collections.unmodifiableMap(headers);
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

    /**
     * Returns a new immutable response with a header appended.
     */
    public HttpResponse withHeader(String name, String value) {
        if (name == null || name.isBlank()) {
            return this;
        }
        Map<String, String> mutable = new LinkedHashMap<>(this.headers);
        mutable.put(name, value);
        return new HttpResponse(this.statusCode, this.body, this.contentType, mutable);
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

    public Map<String, String> getHeaders() {
        return headers;
    }
}
