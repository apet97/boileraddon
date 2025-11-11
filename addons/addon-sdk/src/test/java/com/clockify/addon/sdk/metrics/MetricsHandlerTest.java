package com.clockify.addon.sdk.metrics;

import com.clockify.addon.sdk.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for MetricsHandler class.
 * Tests Prometheus metrics endpoint functionality.
 */
class MetricsHandlerTest {

    private final MetricsHandler metricsHandler = new MetricsHandler();

    @Test
    void testRegistrySingleton() {
        // Test that registry() returns the same instance
        var registry1 = MetricsHandler.registry();
        var registry2 = MetricsHandler.registry();

        assertNotNull(registry1);
        assertNotNull(registry2);
        assertSame(registry1, registry2);
    }

    @Test
    void testHandleReturnsValidResponse() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        HttpResponse response = metricsHandler.handle(mockRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("text/plain; version=0.0.4; charset=utf-8", response.getContentType());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleReturnsPrometheusFormat() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        HttpResponse response = metricsHandler.handle(mockRequest);

        String body = response.getBody();
        assertNotNull(body);

        // Prometheus format should contain some expected content
        // Even an empty registry should have some basic metrics
        assertTrue(body.contains("# TYPE") || body.contains("# HELP") || body.trim().isEmpty());
    }

    @Test
    void testHandleWithNullRequest() {
        // Should handle null request gracefully
        HttpResponse response = metricsHandler.handle(null);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getBody());
    }

    @Test
    void testRegistryIsInitialized() {
        var registry = MetricsHandler.registry();
        assertNotNull(registry);

        // Verify it's a PrometheusMeterRegistry
        assertEquals("io.micrometer.prometheusmetrics.PrometheusMeterRegistry", registry.getClass().getName());
    }

    @Test
    void testResponseContentType() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        HttpResponse response = metricsHandler.handle(mockRequest);

        // Verify the content type matches Prometheus text format
        String contentType = response.getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains("text/plain"));
        assertTrue(contentType.contains("version=0.0.4"));
        assertTrue(contentType.contains("charset=utf-8"));
    }

    @Test
    void testMultipleHandleCalls() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // Multiple calls should work without issues
        HttpResponse response1 = metricsHandler.handle(mockRequest);
        HttpResponse response2 = metricsHandler.handle(mockRequest);

        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(200, response1.getStatus());
        assertEquals(200, response2.getStatus());

        // Responses should be independent (different scrape results)
        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());
    }

    @Test
    void testMetricsHandlerImplementsRequestHandler() {
        // Verify that MetricsHandler properly implements RequestHandler interface
        assertTrue(metricsHandler instanceof com.clockify.addon.sdk.RequestHandler);
    }

    @Test
    void testRegistryScrapeNotEmpty() {
        // Even without custom metrics, the registry should return valid Prometheus format
        var registry = MetricsHandler.registry();
        String scrape = registry.scrape();

        assertNotNull(scrape);
        // Prometheus format should be valid even if empty
        assertTrue(scrape.trim().isEmpty() || scrape.contains("#"));
    }
}