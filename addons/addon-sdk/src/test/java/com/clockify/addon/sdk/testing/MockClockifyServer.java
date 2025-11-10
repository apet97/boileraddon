package com.clockify.addon.sdk.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Mock HTTP server for testing Clockify API interactions.
 * Provides configurable response mocking for testing without hitting real API.
 *
 * Usage:
 * <pre>
 * MockClockifyServer server = new MockClockifyServer(8888);
 * server.addMockResponse("GET", "/api/v1/workspaces/123", 200, "{\"id\":\"123\"}");
 * server.start();
 * // ... test code ...
 * server.stop();
 * </pre>
 */
public class MockClockifyServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int port;
    private final Map<String, MockResponse> mockResponses = new HashMap<>();
    private HttpServer server;

    /**
     * Create a new mock Clockify server on the specified port
     */
    public MockClockifyServer(int port) {
        this.port = port;
    }

    /**
     * Add a mock response for a specific HTTP method and path
     */
    public MockClockifyServer addMockResponse(String method, String path, int statusCode, String body) {
        String key = method + ":" + path;
        mockResponses.put(key, new MockResponse(statusCode, body, "application/json"));
        return this;
    }

    /**
     * Add a mock response with custom content type
     */
    public MockClockifyServer addMockResponse(String method, String path, int statusCode, String body, String contentType) {
        String key = method + ":" + path;
        mockResponses.put(key, new MockResponse(statusCode, body, contentType));
        return this;
    }

    /**
     * Add a mock response that returns a JSON error
     */
    public MockClockifyServer addErrorResponse(String method, String path, int statusCode, String errorMessage) {
        String errorJson = MAPPER.createObjectNode()
                .put("message", errorMessage)
                .put("statusCode", statusCode)
                .toString();
        return addMockResponse(method, path, statusCode, errorJson);
    }

    /**
     * Start the mock server
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // Register root handler
        server.createContext("/", new MockHandler());

        server.start();
    }

    /**
     * Stop the mock server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * Get the server URL
     */
    public String getUrl() {
        return "http://127.0.0.1:" + port;
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return server != null;
    }

    /**
     * Get the port this server is running on
     */
    public int getPort() {
        return port;
    }

    /**
     * Mock HTTP handler that responds based on configured mocks
     */
    private class MockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String key = method + ":" + path;

            MockResponse mockResponse = mockResponses.get(key);

            if (mockResponse == null) {
                // Return 404 if no mock configured for this endpoint
                byte[] response = "{\"message\":\"Not found\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
            } else {
                byte[] response = mockResponse.body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", mockResponse.contentType);
                exchange.sendResponseHeaders(mockResponse.statusCode, response.length);
                exchange.getResponseBody().write(response);
            }

            exchange.close();
        }
    }

    /**
     * Internal class for storing mock response data
     */
    private static class MockResponse {
        final int statusCode;
        final String body;
        final String contentType;

        MockResponse(int statusCode, String body, String contentType) {
            this.statusCode = statusCode;
            this.body = body;
            this.contentType = contentType;
        }
    }
}
