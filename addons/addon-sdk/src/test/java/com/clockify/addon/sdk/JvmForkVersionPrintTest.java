package com.clockify.addon.sdk;

import org.junit.jupiter.api.Test;

/**
 * Utility test to help diagnose the version of the forked JVM
 * used by the Maven Surefire plugin. It only prints when the
 * system property `print.jvm.version` is set to true.
 */
public class JvmForkVersionPrintTest {
    @Test
    void printsForkJvmVersionWhenEnabled() {
        if (Boolean.getBoolean("print.jvm.version")) {
            System.out.println("FORK JVM: " + System.getProperty("java.version"));
        }
    }
}

