package com.example.rules;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures the packaged artifact (jar) exposes catalog endpoints backed by bundled resources.
 */
public class PackagedCatalogEndpointsTest {

    @Test
    void packagedJarProvidesCatalogData() throws Exception {
        Path jarPath = buildJarFromTargetClasses();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            Class<?> openApiLoader = Class.forName("com.example.rules.spec.OpenAPISpecLoader", true, loader);
            openApiLoader.getMethod("clearCache").invoke(null);
            JsonNode actions = (JsonNode) openApiLoader.getMethod("endpointsToJson").invoke(null);
            assertTrue(actions.path("count").asInt() > 0, "Packaged actions catalog should contain endpoints");

            Class<?> triggersCatalog = Class.forName("com.example.rules.spec.TriggersCatalog", true, loader);
            triggersCatalog.getMethod("clearCache").invoke(null);
            JsonNode triggers = (JsonNode) triggersCatalog.getMethod("triggersToJson").invoke(null);
            assertTrue(triggers.path("count").asInt() > 0, "Packaged triggers catalog should contain entries");
        } finally {
            Files.deleteIfExists(jarPath);
        }
    }

    private Path buildJarFromTargetClasses() throws IOException {
        Path classesDir = Paths.get("target", "classes");
        if (!Files.isDirectory(classesDir)) {
            throw new IllegalStateException("Expected compiled classes at " + classesDir.toAbsolutePath());
        }

        Path jarPath = Files.createTempFile("rules-app-", ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Files.walk(classesDir)
                    .sorted(Comparator.naturalOrder())
                    .filter(Files::isRegularFile)
                    .forEach(path -> addEntry(jar, classesDir, path));
        }
        return jarPath;
    }

    private void addEntry(JarOutputStream jar, Path baseDir, Path file) {
        String entryName = baseDir.relativize(file).toString().replace('\\', '/');
        JarEntry entry = new JarEntry(entryName);
        try {
            jar.putNextEntry(entry);
            Files.copy(file, jar);
            jar.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Failed to add jar entry " + entryName, e);
        }
    }
}
