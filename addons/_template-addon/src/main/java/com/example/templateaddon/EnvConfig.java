package com.example.templateaddon;

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
    private static final String ENV_FILE_NAME = ".env";
    private static final Map<String, String> FILE_VALUES = loadEnvFile();

    private EnvConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static String get(String key) {
        String fromFile = FILE_VALUES.get(key);
        if (fromFile != null) {
            return fromFile;
        }
        return System.getenv(key);
    }

    private static Map<String, String> loadEnvFile() {
        Path path = Paths.get(ENV_FILE_NAME);
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
        return Collections.unmodifiableMap(values);
    }
}
