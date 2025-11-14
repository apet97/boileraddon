package com.clockify.addon.sdk.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveHeaderFilterTest {

    @Test
    void sensitiveHeadersAreRedactedButNormalHeadersRemain() throws Exception {
        SensitiveHeaderFilter filter = new SensitiveHeaderFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AtomicReference<HttpServletRequest> wrappedRequest = new AtomicReference<>();

        when(request.getHeader("Authorization")).thenReturn("Bearer secret");
        when(request.getHeader("X-Addon-Token")).thenReturn("clockify-token");
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getHeaders("Authorization")).thenReturn(Collections.enumeration(
                Collections.singletonList("Bearer secret")));

        FilterChain chain = (ServletRequest req, ServletResponse res) -> wrappedRequest.set((HttpServletRequest) req);

        filter.doFilter(request, response, chain);

        HttpServletRequest wrapped = wrappedRequest.get();
        assertEquals("[REDACTED]", wrapped.getHeader("Authorization"));
        assertEquals("[REDACTED]", wrapped.getHeader("X-Addon-Token"));
        assertEquals("req-123", wrapped.getHeader("X-Request-Id"));
        assertEquals("[REDACTED]", wrapped.getHeaders("Authorization").nextElement());
    }
}
