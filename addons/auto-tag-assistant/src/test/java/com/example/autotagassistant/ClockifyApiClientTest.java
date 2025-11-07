package com.example.autotagassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockifyApiClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void updateTimeEntryTagsFetchesExistingEntryAndSendsMergedPayload() throws Exception {
        String workspaceId = "workspace";
        String timeEntryId = "time-entry";
        String endpointPath = String.format("/workspaces/%s/time-entries/%s", workspaceId, timeEntryId);

        String existingEntryBody = """
            {
              "id": "time-entry",
              "description": "Original description",
              "billable": true,
              "projectId": "project-123",
              "taskId": "task-456",
              "start": "2023-01-01T09:00:00Z",
              "end": "2023-01-01T10:00:00Z",
              "customFieldValues": [
                {
                  "customFieldId": "cf-1",
                  "value": "abc"
                }
              ],
              "tagIds": ["old-tag"]
            }
            """;

        AtomicInteger getCalls = new AtomicInteger();
        AtomicReference<String> putBody = new AtomicReference<>();

        server.createContext(endpointPath, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    getCalls.incrementAndGet();
                    respondWithJson(exchange, 200, existingEntryBody);
                } else if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
                    putBody.set(new String(bodyBytes, StandardCharsets.UTF_8));

                    try {
                        JsonNode requestJson = OBJECT_MAPPER.readTree(putBody.get());
                        assertEquals("Original description", requestJson.get("description").asText());
                        assertTrue(requestJson.get("billable").asBoolean());
                        assertEquals("project-123", requestJson.get("projectId").asText());
                        assertEquals("task-456", requestJson.get("taskId").asText());
                        assertEquals("2023-01-01T09:00:00Z", requestJson.get("start").asText());
                        assertEquals("2023-01-01T10:00:00Z", requestJson.get("end").asText());

                        ArrayNode requestTags = (ArrayNode) requestJson.get("tagIds");
                        assertEquals(2, requestTags.size());
                        assertEquals("new-tag-1", requestTags.get(0).asText());
                        assertEquals("new-tag-2", requestTags.get(1).asText());
                    } catch (Exception e) {
                        respondWithJson(exchange, 500, "{\"error\":\"Invalid payload\"}");
                        return;
                    }

                    JsonNode responseJson;
                    try {
                        JsonNode requestJson = OBJECT_MAPPER.readTree(putBody.get());
                        com.fasterxml.jackson.databind.node.ObjectNode responseObject = OBJECT_MAPPER.createObjectNode();
                        responseObject.put("id", timeEntryId);
                        responseObject.setAll((com.fasterxml.jackson.databind.node.ObjectNode) requestJson);
                        responseJson = responseObject;
                    } catch (Exception e) {
                        respondWithJson(exchange, 500, "{\"error\":\"Invalid payload\"}");
                        return;
                    }

                    respondWithJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(responseJson));
                } else {
                    respondWithJson(exchange, 405, "{\"error\":\"Unsupported method\"}");
                }
            }
        });

        ClockifyApiClient client = new ClockifyApiClient("http://localhost:" + port, "token-value");

        JsonNode response = client.updateTimeEntryTags(workspaceId, timeEntryId, new String[]{"new-tag-1", "new-tag-2"});

        assertEquals(1, getCalls.get(), "Time entry should be fetched before updating");
        assertEquals(timeEntryId, response.get("id").asText());
        assertEquals("Original description", response.get("description").asText());
        assertEquals("new-tag-1", response.get("tagIds").get(0).asText());
        assertEquals("new-tag-2", response.get("tagIds").get(1).asText());

        JsonNode requestJson = OBJECT_MAPPER.readTree(putBody.get());
        assertEquals(Arrays.asList("new-tag-1", "new-tag-2"),
                Arrays.asList(OBJECT_MAPPER.convertValue(requestJson.get("tagIds"), String[].class)));
    }

    private static void respondWithJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
