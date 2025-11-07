package com.example.addon;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class ManifestValidationTest {
    @Test
    public void manifestJsonExists() throws Exception {
        Path p = Paths.get("templates/java-basic-addon/manifest.json");
        assertTrue(Files.exists(p), "manifest.json should exist");
    }
}
