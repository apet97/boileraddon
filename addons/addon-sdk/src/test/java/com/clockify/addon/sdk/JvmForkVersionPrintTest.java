package com.clockify.addon.sdk;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility test to help diagnose the version of the forked JVM
 * used by the Maven Surefire plugin. It only prints when the
 * system property `print.jvm.version` is set to true.
 */
public class JvmForkVersionPrintTest {

    private static final Logger logger = LoggerFactory.getLogger(JvmForkVersionPrintTest.class);
    @Test
    void printsForkJvmVersionWhenEnabled() {
        if (Boolean.getBoolean("print.jvm.version")) {
            String jvmVersion = System.getProperty("java.version");
            logger.info("FORK JVM: {}", jvmVersion);
            // Keep System.out for diagnostic purposes in test environments
            System.out.println("FORK JVM: " + jvmVersion);
        }
    }
}

