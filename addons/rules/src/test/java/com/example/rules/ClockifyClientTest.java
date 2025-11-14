package com.example.rules;

import com.clockify.addon.sdk.http.ClockifyHttpClient;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClockifyClientTest {

    @Test
    void webhooksPaginationUsesFilteredTotalForClientSideFilters() throws Exception {
        ClockifyHttpClient http = mock(ClockifyHttpClient.class);
        when(http.get(contains("/workspaces/ws/webhooks"), any(), anyMap()))
                .thenReturn(response(200, webhooksBody(5)));
        ClockifyClient client = new ClockifyClient(http, "token");

        ClockifyClient.PageResult page = client.getWebhooksPage(
                "ws",
                Map.of("event", "TIME_ENTRY_UPDATED"),
                1,
                50
        );

        assertEquals(1, page.items().size());
        assertEquals(1, page.pagination().totalItems());
        assertEquals(false, page.pagination().hasMore());
    }

    @Test
    void webhooksPaginationKeepsApiTotalsWhenOnlyServerFiltersApplied() throws Exception {
        ClockifyHttpClient http = mock(ClockifyHttpClient.class);
        when(http.get(contains("/workspaces/ws/webhooks"), any(), anyMap()))
                .thenReturn(response(200, webhooksBody(7)));
        ClockifyClient client = new ClockifyClient(http, "token");

        ClockifyClient.PageResult page = client.getWebhooksPage(
                "ws",
                Map.of("type", "USER_CREATED"),
                1,
                1
        );

        assertEquals(1, page.items().size());
        assertEquals(7, page.pagination().totalItems());
        assertEquals(true, page.pagination().hasMore());
        assertEquals(2, page.pagination().nextPage());
    }

    private static HttpResponse<String> response(int status, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
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

    private static String webhooksBody(long workspaceCount) {
        return """
                {
                  "webhooks": [
                    {"id":"1","webhookEvent":"TIME_ENTRY_UPDATED","enabled":true,"name":"Update","targetUrl":"https://a"},
                    {"id":"2","webhookEvent":"PROJECT_CREATED","enabled":false,"name":"Project","targetUrl":"https://b"}
                  ],
                  "workspaceWebhookCount": %d
                }
                """.formatted(workspaceCount);
    }
}
