package com.clockify.addon.sdk;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Functional interface for handling servlet requests routed through the SDK.
 * Handlers can throw checked exceptions which are translated into {@link HttpResponse}
 * objects by {@link AddonServlet}.
 */
@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpServletRequest request) throws Exception;
}
