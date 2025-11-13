package com.example.rules.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RulesConfigurationTest {

    @Test
    void prodEnvironmentWithoutJwtBootstrapFails() {
        assertThrows(IllegalStateException.class, () ->
                new RulesConfiguration(
                        "rules",
                        "https://example.com/rules",
                        8080,
                        "https://api.clockify.me/api",
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        60000L,
                        "prod",
                        Optional.empty(),
                        Optional.empty()
                ));
    }

    @Test
    void enablingTokenStoreWithoutDatabaseFails() {
        RulesConfiguration.JwtBootstrapConfig jwt = new RulesConfiguration.JwtBootstrapConfig(
                Optional.of("-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...\n-----END PUBLIC KEY-----"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("clockify"),
                Optional.of("rules"),
                60,
                RulesConfiguration.JwtKeySource.PUBLIC_KEY
        );

        assertThrows(IllegalStateException.class, () ->
                new RulesConfiguration(
                        "rules",
                        "https://example.com/rules",
                        8080,
                        "https://api.clockify.me/api",
                        Optional.empty(),
                        Optional.empty(),
                        true,
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        60000L,
                        "prod",
                        Optional.of(jwt),
                        Optional.empty()
                ));
    }
}
