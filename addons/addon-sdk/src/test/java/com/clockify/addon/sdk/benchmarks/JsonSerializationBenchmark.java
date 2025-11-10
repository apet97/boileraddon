package com.clockify.addon.sdk.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for JSON serialization and deserialization.
 *
 * Critical paths:
 * - Parsing webhook payloads
 * - Serializing API responses
 * - Extracting field values from JSON
 *
 * These operations happen for every webhook and API response.
 *
 * Run with: mvn test -Dtest=JsonSerializationBenchmark -pl addons/addon-sdk
 * Or: java -jar target/benchmarks.jar JsonSerializationBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = "-Xmx2g")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class JsonSerializationBenchmark {

    private ObjectMapper mapper;
    private String webhookPayload;
    private String timeEntryPayload;
    private JsonNode webhookNode;
    private ObjectNode webhookObjectNode;

    @Setup
    public void setup() throws Exception {
        mapper = new ObjectMapper();

        // Create sample webhook payload manually
        ObjectNode webhookEvent = mapper.createObjectNode();
        webhookEvent.put("event", "TIME_ENTRY_CREATED");
        webhookEvent.put("workspaceId", "ws-bench-001");
        webhookEvent.put("workspaceName", "Benchmark Workspace");
        webhookEvent.put("userId", "user-bench-001");
        webhookEvent.put("userName", "Benchmark User");
        webhookEvent.put("userEmail", "bench@example.com");

        // Create time entry object
        ObjectNode timeEntry = mapper.createObjectNode();
        timeEntry.put("id", "entry-bench-001");
        timeEntry.put("description", "Benchmark task");
        timeEntry.put("duration", 3600);
        timeEntry.put("billable", true);
        timeEntry.put("start", "2025-01-10T10:00:00Z");
        timeEntry.put("end", "2025-01-10T11:00:00Z");
        webhookEvent.set("timeEntry", timeEntry);

        webhookPayload = mapper.writeValueAsString(webhookEvent);
        webhookNode = mapper.readTree(webhookPayload);
        webhookObjectNode = (ObjectNode) webhookNode;

        // Time entry payload
        ObjectNode timeEntryData = mapper.createObjectNode();
        timeEntryData.put("id", "entry-123");
        timeEntryData.put("description", "Test task");
        timeEntryData.put("duration", 7200);
        timeEntryData.put("start", "2025-01-10T09:00:00Z");
        timeEntryData.put("end", "2025-01-10T11:00:00Z");
        timeEntryPayload = mapper.writeValueAsString(timeEntryData);
    }

    /**
     * Benchmark: Parse webhook JSON string to JsonNode
     * This happens for every incoming webhook.
     */
    @Benchmark
    public void parseWebhookJsonString(Blackhole bh) throws Exception {
        JsonNode result = mapper.readTree(webhookPayload);
        bh.consume(result);
    }

    /**
     * Benchmark: Serialize JsonNode back to string
     * This happens when forwarding events or building responses.
     */
    @Benchmark
    public void serializeWebhookJsonNode(Blackhole bh) throws Exception {
        String result = mapper.writeValueAsString(webhookNode);
        bh.consume(result);
    }

    /**
     * Benchmark: Extract simple string fields from JSON
     * This is done for every webhook to get workspace/user IDs.
     */
    @Benchmark
    public void extractStringFieldsFromJson(Blackhole bh) throws Exception {
        String workspaceId = webhookNode.get("workspaceId").asText();
        String userId = webhookNode.get("userId").asText();
        String event = webhookNode.get("event").asText();

        bh.consume(workspaceId);
        bh.consume(userId);
        bh.consume(event);
    }

    /**
     * Benchmark: Extract nested object from JSON
     * This happens when accessing timeEntry object in webhooks.
     */
    @Benchmark
    public void extractNestedObjectFromJson(Blackhole bh) throws Exception {
        JsonNode timeEntry = webhookNode.get("timeEntry");
        String id = timeEntry.get("id").asText();
        long duration = timeEntry.get("duration").asLong();
        boolean billable = timeEntry.get("billable").asBoolean();

        bh.consume(id);
        bh.consume(duration);
        bh.consume(billable);
    }

    /**
     * Benchmark: Parse time entry payload and extract fields
     * Tests end-to-end parsing performance.
     */
    @Benchmark
    public void parseAndExtractTimeEntry(Blackhole bh) throws Exception {
        JsonNode timeEntry = mapper.readTree(timeEntryPayload);

        String id = timeEntry.get("id").asText();
        String description = timeEntry.get("description").asText();
        long duration = timeEntry.get("duration").asLong();

        bh.consume(id);
        bh.consume(description);
        bh.consume(duration);
    }

    /**
     * Benchmark: Modify ObjectNode and serialize
     * This tests update operations on JSON payloads.
     */
    @Benchmark
    public void modifyAndSerializeObjectNode(Blackhole bh) throws Exception {
        ObjectNode modified = webhookObjectNode.deepCopy();
        modified.put("processed", true);
        modified.put("processedAt", System.currentTimeMillis());

        String result = mapper.writeValueAsString(modified);
        bh.consume(result);
    }

    /**
     * Benchmark: Create new ObjectNode with multiple fields
     * This tests dynamic payload creation.
     */
    @Benchmark
    public void createObjectNodeWithMultipleFields(Blackhole bh) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        response.put("id", "entry-123");
        response.put("createdAt", System.currentTimeMillis());
        response.put("status", "processed");

        String result = mapper.writeValueAsString(response);
        bh.consume(result);
    }

    /**
     * Benchmark: Array iteration from JSON
     * This tests pagination and list processing.
     */
    @Benchmark
    public void iterateJsonArray(Blackhole bh) throws Exception {
        // Create an array of time entries
        ObjectNode arrayEvent = mapper.createObjectNode();
        for (int i = 0; i < 10; i++) {
            ObjectNode entry = mapper.createObjectNode();
            entry.put("id", "entry-" + i);
            entry.put("description", "Task " + i);
            entry.put("duration", 3600 + (i * 100));
            entry.put("start", "2025-01-10T" + (10 + i) + ":00:00Z");
            arrayEvent.withArray("entries").add(entry);
        }

        String payload = mapper.writeValueAsString(arrayEvent);
        JsonNode parsed = mapper.readTree(payload);

        // Iterate through entries
        for (JsonNode entry : parsed.get("entries")) {
            String id = entry.get("id").asText();
            long duration = entry.get("duration").asLong();
            bh.consume(id);
            bh.consume(duration);
        }
    }

    /**
     * Benchmark: Check field existence in JSON
     * This is done for optional fields validation.
     */
    @Benchmark
    public void checkFieldExistenceInJson(Blackhole bh) throws Exception {
        boolean hasTimeEntry = webhookNode.has("timeEntry");
        boolean hasProjectId = webhookNode.has("projectId");
        boolean hasTaskId = webhookNode.has("taskId");
        boolean hasOptionalField = webhookNode.has("customField");

        bh.consume(hasTimeEntry);
        bh.consume(hasProjectId);
        bh.consume(hasTaskId);
        bh.consume(hasOptionalField);
    }

    /**
     * Benchmark: Filter and transform JSON object
     * This tests processing of JSON for API forwarding.
     */
    @Benchmark
    public void filterAndTransformJson(Blackhole bh) throws Exception {
        ObjectNode filtered = mapper.createObjectNode();

        // Copy selected fields from webhook
        filtered.put("event", webhookNode.get("event").asText());
        filtered.put("workspaceId", webhookNode.get("workspaceId").asText());
        filtered.put("userId", webhookNode.get("userId").asText());

        // Extract nested field
        if (webhookNode.has("timeEntry")) {
            ObjectNode timeEntry = (ObjectNode) webhookNode.get("timeEntry");
            ObjectNode timeEntryData = mapper.createObjectNode();
            timeEntryData.put("id", timeEntry.get("id").asText());
            timeEntryData.put("duration", timeEntry.get("duration").asLong());
            filtered.set("timeEntry", timeEntryData);
        }

        String result = mapper.writeValueAsString(filtered);
        bh.consume(result);
    }
}
