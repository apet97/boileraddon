package com.clockify.addon.sdk.health;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Health check endpoint for monitoring and load balancers.
 * Returns application health status, version info, and system metrics.
 */
public class HealthCheck implements RequestHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String appName;
    private final String appVersion;
    private final List<HealthCheckProvider> customChecks = new ArrayList<>();

    public HealthCheck(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    /**
     * Register a custom health check provider.
     *
     * @param provider Custom health check logic
     */
    public void addHealthCheckProvider(HealthCheckProvider provider) {
        customChecks.add(provider);
    }

    @Override
    public HttpResponse handle(HttpServletRequest request) throws Exception {
        ObjectNode health = objectMapper.createObjectNode();

        // Basic info
        health.put("status", "UP");
        health.put("application", appName);
        health.put("version", appVersion);
        health.put("timestamp", System.currentTimeMillis());

        // Runtime info
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        ObjectNode runtime = health.putObject("runtime");
        runtime.put("uptime", runtimeBean.getUptime());
        runtime.put("startTime", runtimeBean.getStartTime());

        // Memory info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ObjectNode memory = health.putObject("memory");
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        memory.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
        memory.put("heapUsagePercent",
                (memoryBean.getHeapMemoryUsage().getUsed() * 100.0) / memoryBean.getHeapMemoryUsage().getMax());

        // System info
        ObjectNode system = health.putObject("system");
        system.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        system.put("totalMemory", Runtime.getRuntime().totalMemory());
        system.put("freeMemory", Runtime.getRuntime().freeMemory());
        system.put("maxMemory", Runtime.getRuntime().maxMemory());

        // Custom health checks
        boolean allHealthy = true;
        ObjectNode checks = health.putObject("checks");
        for (HealthCheckProvider provider : customChecks) {
            try {
                HealthCheckResult result = provider.check();
                ObjectNode checkNode = checks.putObject(result.getName());
                checkNode.put("status", result.isHealthy() ? "UP" : "DOWN");
                if (result.getMessage() != null) {
                    checkNode.put("message", result.getMessage());
                }
                if (result.getDetails() != null) {
                    checkNode.set("details", objectMapper.valueToTree(result.getDetails()));
                }

                if (!result.isHealthy()) {
                    allHealthy = false;
                }
            } catch (Exception e) {
                ObjectNode checkNode = checks.putObject(provider.getName());
                checkNode.put("status", "DOWN");
                checkNode.put("error", e.getMessage());
                allHealthy = false;
            }
        }

        if (!allHealthy) {
            health.put("status", "DEGRADED");
        }

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(health);
        return HttpResponse.ok(json, "application/json");
    }

    /**
     * Interface for custom health check providers.
     */
    public interface HealthCheckProvider {
        /**
         * Gets the name of this health check.
         */
        String getName();

        /**
         * Performs the health check.
         *
         * @return Health check result
         */
        HealthCheckResult check();
    }

    /**
     * Result of a health check.
     */
    public static class HealthCheckResult {
        private final String name;
        private final boolean healthy;
        private final String message;
        private final Object details;

        public HealthCheckResult(String name, boolean healthy) {
            this(name, healthy, null, null);
        }

        public HealthCheckResult(String name, boolean healthy, String message) {
            this(name, healthy, message, null);
        }

        public HealthCheckResult(String name, boolean healthy, String message, Object details) {
            this.name = name;
            this.healthy = healthy;
            this.message = message;
            this.details = details;
        }

        public String getName() { return name; }
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public Object getDetails() { return details; }
    }
}
