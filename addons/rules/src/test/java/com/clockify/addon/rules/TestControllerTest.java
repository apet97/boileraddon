package com.clockify.addon.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void echoesPayloadWhenProvided() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"foo\":\"bar\"}")));

        HttpResponse response = new TestController().handle(request);
        JsonNode root = OBJECT_MAPPER.readTree(response.getBody());

        assertEquals("rules-ok", root.get("status").asText());
        assertEquals("bar", root.get("echo").get("foo").asText());
    }
}
