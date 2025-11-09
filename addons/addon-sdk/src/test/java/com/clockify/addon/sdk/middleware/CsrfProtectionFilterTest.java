package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsrfProtectionFilterTest {

    private CsrfProtectionFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;
    private Map<String, Object> sessionStore;

    @BeforeEach
    void setup() {
        filter = new CsrfProtectionFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        sessionStore = new HashMap<>();

        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            sessionStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");
    }

    @Test
    void safeRequestIssuesCookie() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/settings");
        when(request.isSecure()).thenReturn(true);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        filter.doFilter(request, response, chain);

        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertEquals("clockify-addon-csrf", cookie.getName());
        assertFalse(cookie.isHttpOnly());
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsPostWithoutToken() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/settings/save");
        when(request.isSecure()).thenReturn(false);
        when(request.getHeader("X-CSRF-Token")).thenReturn(null);
        when(request.getParameter("__csrf")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(403);
        assertTrue(sw.toString().contains("csrf_token_invalid"));
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void allowsPostWithValidHeaderToken() throws Exception {
        sessionStore.put("_csrf_token", "token-123");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/settings/save");
        when(request.isSecure()).thenReturn(true);
        when(request.getHeader("X-CSRF-Token")).thenReturn("token-123");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
