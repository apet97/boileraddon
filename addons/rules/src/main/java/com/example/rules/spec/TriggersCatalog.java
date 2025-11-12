package com.example.rules.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a catalog of Clockify webhook triggers from the webhook samples markdown file.
 */
public class TriggersCatalog {

    private static final Logger logger = LoggerFactory.getLogger(TriggersCatalog.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static List<WebhookTrigger> cachedTriggers = null;

    /**
     * Load webhook triggers from the markdown file.
     * Expected format: ## EVENT_NAME followed by ```json ... ```
     */
    public static synchronized List<WebhookTrigger> getTriggers() {
        if (cachedTriggers != null) {
            return cachedTriggers;
        }

        List<WebhookTrigger> triggers = new ArrayList<>();

        try {
            String content = null;

            // Try classpath resource first (for CI environment)
            try (InputStream is = TriggersCatalog.class.getClassLoader()
                    .getResourceAsStream("Clockify_Webhook_JSON_Samples.md")) {
                if (is != null) {
                    content = new String(is.readAllBytes());
                    logger.info("Loaded webhook samples from classpath: Clockify_Webhook_JSON_Samples.md");
                }
            } catch (Exception e) {
                logger.warn("Failed to load webhook samples from classpath", e);
            }

            // If classpath loading failed, try filesystem paths
            if (content == null) {
                // Try root directory first (preferred location) - use relative path from project root
                Path webhookPath = Paths.get("../Clockify_Webhook_JSON_Samples.md");
                if (!Files.exists(webhookPath)) {
                    // Try current directory (for local development)
                    webhookPath = Paths.get("Clockify_Webhook_JSON_Samples.md");
                    if (!Files.exists(webhookPath)) {
                        // Try downloads directory
                        Path dl = Paths.get("../downloads", "Clockify_Webhook_JSON_Samples.md");
                        if (Files.exists(dl)) {
                            webhookPath = dl;
                        } else {
                            // Fallback to current directory downloads
                            dl = Paths.get("downloads", "Clockify_Webhook_JSON_Samples.md");
                            if (Files.exists(dl)) {
                                webhookPath = dl;
                            } else {
                                logger.warn("Webhook samples file not found in any expected location");
                                cachedTriggers = triggers;
                                return triggers;
                            }
                        }
                    }
                }
                content = Files.readString(webhookPath);
                logger.info("Loaded webhook samples from filesystem: {}", webhookPath);
            }

            // Pattern to extract webhook event names (markdown headers like ## EVENT_NAME)
            Pattern headerPattern = Pattern.compile("^## ([A-Z_]+)$", Pattern.MULTILINE);
            Matcher matcher = headerPattern.matcher(content);

            Set<String> seenEvents = new HashSet<>();

            while (matcher.find()) {
                String eventName = matcher.group(1);

                // Skip duplicates
                if (seenEvents.contains(eventName)) {
                    continue;
                }
                seenEvents.add(eventName);

                WebhookTrigger trigger = new WebhookTrigger();
                trigger.event = eventName;
                trigger.name = formatEventName(eventName);
                trigger.description = generateDescription(eventName);
                trigger.category = categorizeEvent(eventName);

                // Extract sample payload fields
                trigger.sampleFields = extractSampleFields(content, eventName);

                triggers.add(trigger);
            }

            cachedTriggers = triggers;
            logger.info("Loaded {} webhook triggers from markdown", triggers.size());
        } catch (Exception e) {
            logger.error("Failed to load webhook triggers", e);
            cachedTriggers = triggers;
        }

        return triggers;
    }

    /**
     * Convert triggers to JSON for API response.
     */
    public static JsonNode triggersToJson() {
        List<WebhookTrigger> triggers = getTriggers();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode triggersArray = mapper.createArrayNode();

        for (WebhookTrigger trigger : triggers) {
            ObjectNode triggerNode = mapper.createObjectNode();
            triggerNode.put("event", trigger.event);
            triggerNode.put("name", trigger.name);
            triggerNode.put("description", trigger.description);
            triggerNode.put("category", trigger.category);

            ArrayNode fieldsArray = mapper.createArrayNode();
            for (String field : trigger.sampleFields) {
                fieldsArray.add(field);
            }
            triggerNode.set("sampleFields", fieldsArray);

            triggersArray.add(triggerNode);
        }

        root.set("triggers", triggersArray);
        root.put("count", triggers.size());
        return root;
    }

    private static String formatEventName(String event) {
        // Convert NEW_TIME_ENTRY to "New Time Entry"
        return Arrays.stream(event.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(event);
    }

    private static String generateDescription(String event) {
        if (event.startsWith("NEW_")) {
            return "Triggered when a new " + formatEventName(event.substring(4)).toLowerCase() + " is created";
        } else if (event.endsWith("_UPDATED")) {
            String entity = event.substring(0, event.length() - 8);
            return "Triggered when a " + formatEventName(entity).toLowerCase() + " is updated";
        } else if (event.endsWith("_DELETED")) {
            String entity = event.substring(0, event.length() - 8);
            return "Triggered when a " + formatEventName(entity).toLowerCase() + " is deleted";
        } else if (event.endsWith("_CREATED")) {
            String entity = event.substring(0, event.length() - 8);
            return "Triggered when a " + formatEventName(entity).toLowerCase() + " is created";
        } else if (event.equals("TIMER_STOPPED")) {
            return "Triggered when a running timer is stopped";
        } else if (event.contains("APPROVED")) {
            return "Triggered when a request is approved";
        } else if (event.contains("REJECTED")) {
            return "Triggered when a request is rejected";
        }
        return "Triggered on " + formatEventName(event).toLowerCase() + " event";
    }

    private static String categorizeEvent(String event) {
        if (event.contains("TIME_ENTRY") || event.contains("TIMER")) {
            return "Time Tracking";
        } else if (event.contains("PROJECT")) {
            return "Projects";
        } else if (event.contains("CLIENT")) {
            return "Clients";
        } else if (event.contains("TAG")) {
            return "Tags";
        } else if (event.contains("TASK")) {
            return "Tasks";
        } else if (event.contains("USER") || event.contains("WORKSPACE")) {
            return "Users & Workspace";
        } else if (event.contains("APPROVAL")) {
            return "Approvals";
        } else if (event.contains("INVOICE")) {
            return "Invoices";
        } else if (event.contains("EXPENSE")) {
            return "Expenses";
        } else if (event.contains("TIME_OFF")) {
            return "Time Off";
        } else if (event.contains("ASSIGNMENT")) {
            return "Scheduling";
        }
        return "Other";
    }

    /**
     * Extract common field names from the sample JSON payload.
     * These are used to suggest placeholder insertions in the UI.
     */
    private static List<String> extractSampleFields(String markdownContent, String eventName) {
        List<String> fields = new ArrayList<>();

        // Common top-level fields for most events
        fields.add("workspaceId");
        fields.add("id");

        if (eventName.contains("TIME_ENTRY") || eventName.contains("TIMER")) {
            fields.addAll(Arrays.asList(
                "description", "userId", "projectId", "taskId", "billable",
                "project.id", "project.name", "project.clientId", "project.clientName",
                "task.id", "task.name", "user.id", "user.name"
            ));
        } else if (eventName.contains("PROJECT")) {
            fields.addAll(Arrays.asList("name", "clientId", "clientName", "billable"));
        } else if (eventName.contains("CLIENT")) {
            fields.addAll(Arrays.asList("name", "archived"));
        } else if (eventName.contains("TAG")) {
            fields.addAll(Arrays.asList("name", "archived"));
        } else if (eventName.contains("TASK")) {
            fields.addAll(Arrays.asList("name", "projectId", "assigneeId", "assigneeIds"));
        } else if (eventName.contains("USER")) {
            fields.addAll(Arrays.asList("email", "name"));
        }

        return fields;
    }

    public static class WebhookTrigger {
        public String event;
        public String name;
        public String description;
        public String category;
        public List<String> sampleFields = new ArrayList<>();
    }
}
