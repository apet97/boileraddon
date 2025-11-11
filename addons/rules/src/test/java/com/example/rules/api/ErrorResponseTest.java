package com.example.rules.api;

import com.clockify.addon.sdk.middleware.DiagnosticContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorResponseTest {

    @Test
    void producesProblemJsonEnvelope() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(DiagnosticContextFilter.REQUEST_ID_ATTR)).thenReturn("req-xyz");
        when(request.getRequestURI()).thenReturn("/rules/api/test");

        var response = ErrorResponse.of(400, "RULES.INVALID", "Invalid request", request, false);

        assertEquals("application/problem+json", response.getContentType());
        assertTrue(response.getBody().contains("\"code\":\"RULES.INVALID\""));
        assertTrue(response.getBody().contains("\"requestId\""));
    }
}
