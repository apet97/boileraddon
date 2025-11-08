package com.example.autotagassistant;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.example.autotagassistant.security.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookHandlersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        TokenStore.clear();
    }

    @Test
    void handlerAppliesTagsWhenProjectNameNestedStructurePresent() throws Exception {
        String workspaceId = "workspace-id";
        String timeEntryId = "time-entry-id";
        String apiPrefix = "/api/v1";
        String tagsPath = String.format("%s/workspaces/%s/tags", apiPrefix, workspaceId);
        String timeEntryPath = String.format("%s/workspaces/%s/time-entries/%s", apiPrefix, workspaceId, timeEntryId);

        AtomicBoolean updateCalled = new AtomicBoolean(false);
        AtomicReference<String> lastPutBody = new AtomicReference<>();

        server.createContext(tagsPath, new JsonHandler(exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondWithJson(exchange, 405, "{\"error\":\"Unsupported method\"}");
                return;
            }

            respondWithJson(exchange, 200, """
                [
                  {
                    "id": "tag-omega",
                    "name": "omega-project"
                  }
                ]
                """);
        }));

        server.createContext(timeEntryPath, new JsonHandler(exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondWithJson(exchange, 200, """
                    {
                      "id": "time-entry-id",
                      "description": "Working on Omega initiative",
                      "timeInterval": {
                        "start": "2024-01-01T00:00:00Z",
                        "end": "2024-01-01T01:00:00Z"
                      },
                      "tagIds": []
                    }
                    """);
                return;
            }

            if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondWithJson(exchange, 405, "{\"error\":\"Unsupported method\"}");
                return;
            }

            updateCalled.set(true);
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            lastPutBody.set(new String(bodyBytes, StandardCharsets.UTF_8));

            respondWithJson(exchange, 200, lastPutBody.get());
        }));

        server.start();

        String baseUrl = "http://localhost:" + port + "/api";
        TokenStore.save(workspaceId, "token-value", baseUrl);

        ClockifyManifest manifest = ClockifyManifest
            .v1_3Builder()
            .key("auto-tag-assistant")
            .name("Auto-Tag Assistant")
            .description("Automatically detects and suggests tags for time entries")
            .baseUrl(baseUrl)
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"})
            .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon);

        RequestHandler handler = addon.getWebhookHandlers().get("NEW_TIME_ENTRY");
        assertNotNull(handler, "Expected webhook handler to be registered for NEW_TIME_ENTRY");

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("workspaceId", workspaceId);
        ObjectNode timeEntry = payload.putObject("timeEntry");
        timeEntry.put("id", timeEntryId);
        timeEntry.put("description", "Working on Omega initiative");
        ObjectNode project = timeEntry.putObject("project");
        project.put("name", "Omega Project");

        TestWebhookRequest request = new TestWebhookRequest("POST", payload);
        request.setHeader(WebhookSignatureValidator.SIGNATURE_HEADER,
            WebhookSignatureValidator.computeSignature("token-value", payload.toString()));
        HttpResponse response = handler.handle(request);

        assertEquals(200, response.getStatusCode());

        JsonNode responseJson = OBJECT_MAPPER.readTree(response.getBody());
        assertEquals("success", responseJson.get("status").asText());

        List<String> appliedTagNames = new ArrayList<>();
        for (JsonNode tagNode : responseJson.withArray("appliedTags")) {
            appliedTagNames.add(tagNode.get("name").asText());
        }

        assertTrue(appliedTagNames.contains("omega-project"), "Expected nested project name to yield a tag suggestion");
        assertTrue(updateCalled.get(), "Expected handler to invoke tag update when suggestions exist");

        JsonNode putRequestJson = OBJECT_MAPPER.readTree(Objects.requireNonNull(lastPutBody.get()));
        assertEquals("tag-omega", putRequestJson.withArray("tagIds").get(0).asText());
    }

    @Test
    void webhookRejectsMissingSignature() throws Exception {
        String workspaceId = "workspace-id";

        TokenStore.save(workspaceId, "token-value", "https://api.clockify.me/api");

        ClockifyManifest manifest = ClockifyManifest
            .v1_3Builder()
            .key("auto-tag-assistant")
            .name("Auto-Tag Assistant")
            .description("Automatically detects and suggests tags for time entries")
            .baseUrl("https://example.com/auto-tag-assistant")
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"TIME_ENTRY_READ"})
            .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon);

        RequestHandler handler = addon.getWebhookHandlers().get("NEW_TIME_ENTRY");

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("workspaceId", workspaceId);
        payload.putObject("timeEntry").put("id", "ignored");

        TestWebhookRequest request = new TestWebhookRequest("POST", payload);
        HttpResponse response = handler.handle(request);

        assertEquals(401, response.getStatusCode());
        assertEquals("Missing webhook signature header", response.getBody());
    }

    @Test
    void webhookRejectsInvalidSignature() throws Exception {
        String workspaceId = "workspace-id";

        TokenStore.save(workspaceId, "token-value", "https://api.clockify.me/api");

        ClockifyManifest manifest = ClockifyManifest
            .v1_3Builder()
            .key("auto-tag-assistant")
            .name("Auto-Tag Assistant")
            .description("Automatically detects and suggests tags for time entries")
            .baseUrl("https://example.com/auto-tag-assistant")
            .minimalSubscriptionPlan("FREE")
            .scopes(new String[]{"TIME_ENTRY_READ"})
            .build();
        ClockifyAddon addon = new ClockifyAddon(manifest);
        WebhookHandlers.register(addon);

        RequestHandler handler = addon.getWebhookHandlers().get("NEW_TIME_ENTRY");

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("workspaceId", workspaceId);
        payload.putObject("timeEntry").put("id", "ignored");

        TestWebhookRequest request = new TestWebhookRequest("POST", payload);
        request.setHeader(WebhookSignatureValidator.SIGNATURE_HEADER, "bogus");
        HttpResponse response = handler.handle(request);

        assertEquals(403, response.getStatusCode());
        assertEquals("Invalid webhook signature", response.getBody());
    }

    private static void respondWithJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class JsonHandler implements HttpHandler {
        private final HttpHandler delegate;

        private JsonHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            delegate.handle(exchange);
        }
    }

    private static final class TestWebhookRequest implements HttpServletRequest {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final String method;
        private final String body;

        private TestWebhookRequest(String method, JsonNode body) {
            this.method = method;
            this.body = body.toString();
            attributes.put("clockify.jsonBody", body);
            attributes.put("clockify.rawBody", this.body);
        }

        void setHeader(String name, String value) {
            if (value == null) {
                headers.remove(name);
            } else {
                headers.put(name, value);
            }
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        @Override
        public void setAttribute(String name, Object o) {
            if (o == null) {
                attributes.remove(name);
            } else {
                attributes.put(name, o);
            }
        }

        @Override
        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getRequestURI() {
            return "/webhook";
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer("http://localhost/webhook");
        }

        @Override
        public String getServletPath() {
            return "/webhook";
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new StringReader(body));
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = headers.get(name);
            if (value == null) {
                return Collections.emptyEnumeration();
            }
            List<String> values = new ArrayList<>();
            values.add(value);
            return Collections.enumeration(values);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(headers.keySet());
        }

        // --- Methods below return defaults suitable for tests ---

        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        @Override
        public long getDateHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIntHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public HttpSession getSession(boolean create) {
            return null;
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void login(String username, String password) throws ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logout() throws ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Part> getParts() {
            return Collections.emptyList();
        }

        @Override
        public Part getPart(String name) {
            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
            // Ignored
        }

        @Override
        public int getContentLength() {
            return body.getBytes(StandardCharsets.UTF_8).length;
        }

        @Override
        public long getContentLengthLong() {
            return getContentLength();
        }

        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public ServletInputStream getInputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String[] getParameterValues(String name) {
            return new String[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return new HashMap<>();
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public String getServerName() {
            return "localhost";
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public String getRemoteAddr() {
            return "127.0.0.1";
        }

        @Override
        public String getRemoteHost() {
            return "localhost";
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return Collections.enumeration(Collections.singletonList(getLocale()));
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        @Override
        public String getRealPath(String path) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return "localhost";
        }

        @Override
        public String getLocalAddr() {
            return "127.0.0.1";
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() {
            throw new IllegalStateException("Async not supported");
        }

        @Override
        public AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) {
            throw new IllegalStateException("Async not supported");
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            throw new IllegalStateException("Async not supported");
        }

        @Override
        public DispatcherType getDispatcherType() {
            return DispatcherType.REQUEST;
        }
    }
}
