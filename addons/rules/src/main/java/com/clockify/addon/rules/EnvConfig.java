package com.clockify.addon.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight loader for key/value pairs defined in a local .env file. Values in the environment
 * override entries from the file, mirroring the behavior of many Node/CLI toolchains.
 */
public final class EnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private static final Map<String, String> FILE_VALUES = loadEnvFiles();

    private EnvConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static String get(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return FILE_VALUES.get(key);
    }

    public static Map<String, String> asMap() {
        return FILE_VALUES;
    }

    private static Map<String, String> loadEnvFiles() {
        Map<String, String> values = new HashMap<>();
        merge(values, loadEnvFile(".env"));
        merge(values, loadEnvFile(".env.rules"));
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, String> loadEnvFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Collections.emptyMap();
        }
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring(7).trim();
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex < 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            logger.warn("Failed to read .env file: {}", e.getMessage());
        }
        return values;
    }

    private static void merge(Map<String, String> target, Map<String, String> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }
        target.putAll(additions);
    }
}
