package com.example.templateaddon.sdk;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface for handling HTTP requests inside the embedded Jetty server.
 */
@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}
