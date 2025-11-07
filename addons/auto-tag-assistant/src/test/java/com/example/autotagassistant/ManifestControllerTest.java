package com.example.autotagassistant;

import com.example.autotagassistant.sdk.ClockifyManifest;
import com.example.autotagassistant.sdk.HttpResponse;
import org.junit.jupiter.api.Test;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManifestControllerTest {

    @Test
    void detectBaseUrlPrefersHostHeaderBeforeServerName() throws Exception {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("auto-tag-assistant")
                .name("Auto-Tag Assistant")
                .description("Automatically detects and suggests tags for time entries")
                .baseUrl("http://localhost:8080/auto-tag-assistant")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ"})
                .build();
        manifest.getComponents().add(new ClockifyManifest.ComponentEndpoint("sidebar", "/settings", "Auto-Tag Assistant", "ADMINS"));

        ManifestController controller = new ManifestController(manifest);

        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setContextPath("/auto-tag-assistant");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setHeader("Host", "example.com");
        request.setHeader("Forwarded", "proto=https");

        HttpResponse response = controller.handle(request);
        assertEquals("application/json", response.getContentType());
        assertEquals("https://example.com/auto-tag-assistant", manifest.getBaseUrl());
    }

    private static class TestHttpServletRequest implements HttpServletRequest {
        private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private String scheme = "http";
        private String serverName = "localhost";
        private int serverPort = 80;
        private String contextPath = "";

        void setHeader(String name, String value) {
            if (value == null) {
                headers.remove(name);
            } else {
                headers.put(name, value);
            }
        }

        void setScheme(String scheme) {
            this.scheme = scheme;
        }

        void setServerName(String serverName) {
            this.serverName = serverName;
        }

        void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }

        void setContextPath(String contextPath) {
            this.contextPath = contextPath;
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
            return Collections.enumeration(Collections.singletonList(value));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(headers.keySet());
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public String getServerName() {
            return serverName;
        }

        @Override
        public int getServerPort() {
            return serverPort;
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        // --- Methods below are not used in these tests ---

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
        public String getMethod() {
            return "GET";
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
        public String getRequestURI() {
            return "/manifest.json";
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer("http://" + serverName + ":" + serverPort + getRequestURI());
        }

        @Override
        public String getServletPath() {
            return "/manifest.json";
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
        public Collection<Part> getParts() throws IOException, ServletException {
            return Collections.emptyList();
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
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
        public BufferedReader getReader() throws IOException {
            throw new UnsupportedOperationException();
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
        public void setAttribute(String name, Object o) {
        }

        @Override
        public void removeAttribute(String name) {
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
            return serverPort;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            throw new IllegalStateException("Async not supported");
        }

        @Override
        public AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) throws IllegalStateException {
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
