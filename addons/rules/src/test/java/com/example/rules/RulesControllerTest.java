package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.example.rules.ClockifyClient;
import com.example.rules.DynamicWebhookHandlers;
import com.example.rules.engine.Action;
import com.example.rules.engine.Condition;
import com.example.rules.engine.OpenApiCallConfig;
import com.example.rules.engine.Rule;
import com.example.rules.store.RulesStore;
import com.example.rules.web.RequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.rules.security.PlatformAuthFilter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import javax.net.ssl.SSLSession;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RulesControllerTest {

    private RulesController controller;
    private RulesStore store;
    private ObjectMapper mapper;
    private HttpServletRequest request;
    private ClockifyAddon addon;

    @BeforeEach
    void setUp() {
        store = new RulesStore();
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("rules-test")
                .name("Rules Test")
                .baseUrl("http://localhost/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{})
                .build();
        addon = new ClockifyAddon(manifest);
        controller = new RulesController(store, addon);
        mapper = new ObjectMapper();
        request = Mockito.mock(HttpServletRequest.class);
        RequestContext.configureWorkspaceFallback(false);
    }

    @Test
    void testListRules_empty() throws Exception {
        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.isArray());
        assertEquals(0, json.size());
    }

    @Test
    void testListRules_withRules() throws Exception {
        Rule rule = createTestRule("rule-1", "Test Rule");
        store.save("workspace-1", rule);

        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("Test Rule", json.get(0).get("name").asText());
    }

    @Test
    void testListRules_missingWorkspaceId() throws Exception {
        stubWorkspace(request, null);

        HttpResponse response = controller.listRules().handle(request);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("workspaceId is required"));
    }

    @Test
    void testSaveRule_create() throws Exception {
        String ruleJson = """
            {
                "name": "Test Rule",
                "enabled": true,
                "combinator": "AND",
                "conditions": [
                    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"}
                ],
                "actions": [
                    {"type": "add_tag", "args": {"tag": "billable"}}
                ]
            }
            """;

        setupRequestWithBody(ruleJson);
        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.saveRule().handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertEquals("Test Rule", json.get("name").asText());
        assertNotNull(json.get("id").asText());

        // Verify it was saved
        assertEquals(1, store.count("workspace-1"));
    }

    @Test
    void testSaveRuleRegistersDynamicEvent() throws Exception {
        String eventName = "RULES_CONTROLLER_TEST_EVENT";
        String ruleJson = """
            {
                "name": "Trigger Rule",
                "enabled": true,
                "combinator": "AND",
                "conditions": [
                    {"type": "descriptionContains", "operator": "CONTAINS", "value": "alert"}
                ],
                "actions": [
                    {"type": "add_tag", "args": {"tag": "ops"}}
                ],
                "trigger": {
                    "event": "%s"
                }
            }
            """.formatted(eventName);

        setupRequestWithBody(ruleJson);
        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.saveRule().handle(request);

        assertEquals(200, response.getStatusCode());
        assertTrue(addon.getWebhookPathsByEvent().containsKey(eventName));
        assertTrue(addon.getManifest().getWebhooks().stream()
                .anyMatch(endpoint -> eventName.equals(endpoint.getEvent())));
    }

    @Test
    void testSaveRule_invalidJson() throws Exception {
        String ruleJson = """
            {
                "name": "Missing conditions",
                "enabled": true,
                "combinator": "AND"
            }
            """;

        setupRequestWithBody(ruleJson);
        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.saveRule().handle(request);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void testSaveRule_legacyOpenApiPayloadConverted() throws Exception {
        String ruleJson = """
            {
                "name": "Legacy OpenAPI",
                "enabled": true,
                "combinator": "AND",
                "conditions": [
                    {"type": "descriptionContains", "operator": "CONTAINS", "value": "project"}
                ],
                "actions": [
                    {
                        "type": "openapi_call",
                        "endpoint": {
                            "method": "post",
                            "path": "/workspaces/{workspaceId}/projects"
                        },
                        "params": {
                            "workspaceId": {"in": "path", "value": "{{workspaceId}}"},
                            "archived": {"in": "query", "value": "false"}
                        },
                        "body": {
                            "name": "Project {{workspaceId}}"
                        }
                    }
                ]
            }
            """;

        setupRequestWithBody(ruleJson);
        stubWorkspace(request, "workspace-1");

        HttpResponse response = controller.saveRule().handle(request);
        assertEquals(200, response.getStatusCode());

        Rule saved = store.getAll("workspace-1").get(0);
        Action action = saved.getActions().get(0);
        assertEquals("openapi_call", action.getType());
        assertEquals("POST", action.getArgs().get("method"));
        assertEquals("/workspaces/{{workspaceId}}/projects?archived=false", action.getArgs().get("path"));
        assertTrue(action.getArgs().get("body").contains("Project {{workspaceId}}"));
    }

    @Test
    void testOpenApiCallRuleExecutesViaDynamicHandlers() throws Exception {
        String ruleJson = """
            {
                "name": "Webhook project sync",
                "enabled": true,
                "trigger": {"event": "NEW_PROJECT"},
                "conditions": [
                    {"type": "descriptionContains", "operator": "CONTAINS", "value": "sync"}
                ],
                "actions": [
                    {
                        "type": "openapi_call",
                        "args": {
                            "method": "POST",
                            "path": "/workspaces/{{workspaceId}}/projects?archived=false",
                            "body": "{\\"name\\":\\"Project {{workspaceId}}\\"}"
                        }
                    }
                ]
            }
            """;

        setupRequestWithBody(ruleJson);
        stubWorkspace(request, "workspace-1");
        HttpResponse saveResponse = controller.saveRule().handle(request);
        assertEquals(200, saveResponse.getStatusCode());

        HttpServletRequest listRequest = Mockito.mock(HttpServletRequest.class);
        stubWorkspace(listRequest, "workspace-1");
        HttpResponse listResponse = controller.listRules().handle(listRequest);
        Rule[] rules = mapper.readValue(listResponse.getBody(), Rule[].class);
        assertEquals(1, rules.length);
        Action action = rules[0].getActions().get(0);
        assertEquals("openapi_call", action.getType());

        var payload = mapper.readTree("""
            {
                "workspaceId": "workspace-1",
                "event": "NEW_PROJECT"
            }
            """);

        ClockifyClient api = mock(ClockifyClient.class);
        java.net.http.HttpResponse<String> success = httpResponse(200, "{}");
        when(api.openapiCall(any(), anyString(), any())).thenReturn(success);

        Method method = DynamicWebhookHandlers.class.getDeclaredMethod(
                "executeOpenApiCallWithRetry",
                Action.class,
                com.fasterxml.jackson.databind.JsonNode.class,
                String.class,
                ClockifyClient.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(null, action, payload, "workspace-1", api);
        assertTrue(result);

        verify(api).openapiCall(
                OpenApiCallConfig.HttpMethod.POST,
                "/workspaces/workspace-1/projects?archived=false",
                "{\"name\":\"Project workspace-1\"}"
        );
    }

    @Test
    void testDeleteRule() throws Exception {
        Rule rule = createTestRule("rule-1", "Test Rule");
        store.save("workspace-1", rule);

        stubWorkspace(request, "workspace-1");
        when(request.getPathInfo()).thenReturn("/api/rules/rule-1");

        HttpResponse response = controller.deleteRule().handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertTrue(json.get("deleted").asBoolean());

        // Verify it was deleted
        assertEquals(0, store.count("workspace-1"));
    }

    @Test
    void testDeleteRule_notFound() throws Exception {
        stubWorkspace(request, "workspace-1");
        when(request.getPathInfo()).thenReturn("/api/rules/non-existent");

        HttpResponse response = controller.deleteRule().handle(request);

        assertEquals(200, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        assertFalse(json.get("deleted").asBoolean());
    }

    private void setupRequestWithBody(String json) throws Exception {
        JsonNode jsonNode = mapper.readTree(json);
        when(request.getAttribute("clockify.jsonBody")).thenReturn(jsonNode);
        when(request.getAttribute("clockify.rawBody")).thenReturn(json);

        byte[] bytes = json.getBytes();
        ServletInputStream inputStream = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            @Override
            public int read() {
                return bis.read();
            }

            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
            }
        };

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }

    private java.net.http.HttpResponse<String> httpResponse(int status, String body) {
        return new java.net.http.HttpResponse<>() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public java.net.http.HttpRequest request() {
                return HttpRequest.newBuilder(URI.create("https://example.com"))
                        .GET()
                        .build();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (k, v) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<java.net.http.HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create("https://example.com");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    private Rule createTestRule(String id, String name) {
        return new Rule(id, name, true, "AND",
                Collections.singletonList(new Condition("descriptionContains", Condition.Operator.CONTAINS, "test", null)),
                Collections.singletonList(new Action("add_tag", Collections.singletonMap("tag", "test"))),
                null,
                0);
    }

    private void stubWorkspace(HttpServletRequest req, String workspaceId) {
        when(req.getAttribute(PlatformAuthFilter.ATTR_WORKSPACE_ID)).thenReturn(workspaceId);
    }
}
