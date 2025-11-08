package com.example.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface for handling HTTP requests.
 */
@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}
