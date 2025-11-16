package com.example.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import com.example.rules.cache.DatabaseWebhookIdempotencyStore;
import com.example.rules.cache.InMemoryWebhookIdempotencyStore;
import com.example.rules.cache.WebhookIdempotencyStore;
import com.example.rules.config.RulesConfiguration;
import com.clockify.addon.sdk.security.jwt.JwtBootstrapConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesAppTest {

    private WebhookIdempotencyStore createdStore;

    @AfterEach
    void cleanupStore() throws Exception {
        if (createdStore != null) {
            createdStore.close();
        }
        createdStore = null;
        System.clearProperty("ENV");
    }

    @Test
    void configureDedupStoreUsesDatabaseWhenSettingsPresent() throws Exception {
        RulesConfiguration.DatabaseSettings db = new RulesConfiguration.DatabaseSettings(
                "jdbc:h2:mem:rules-app-dedup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        createdStore = RulesApp.configureDedupStore(db);
        assertInstanceOf(DatabaseWebhookIdempotencyStore.class, createdStore);
    }

    @Test
    void configureDedupStoreFallsBackToMemoryWhenNoDatabaseConfigured() throws Exception {
        createdStore = RulesApp.configureDedupStore(null);
        assertInstanceOf(InMemoryWebhookIdempotencyStore.class, createdStore);
    }

    @Test
    void debugEndpointRegisteredOnlyInDev() {
        ClockifyManifest manifestDev = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon devAddon = new ClockifyAddon(manifestDev);
        System.setProperty("ENV", "dev");
        RulesApp.registerDebugEndpoint(devAddon, sampleConfig("dev"), false);
        assertTrue(devAddon.getEndpoints().containsKey("/debug/config"));

        ClockifyManifest manifestProd = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();
        ClockifyAddon prodAddon = new ClockifyAddon(manifestProd);
        System.setProperty("ENV", "prod");
        RulesApp.registerDebugEndpoint(prodAddon, sampleConfig("prod"), true);
        assertFalse(prodAddon.getEndpoints().containsKey("/debug/config"));
    }

    private RulesConfiguration sampleConfig(String env) {
        Optional<JwtBootstrapConfig> jwt = "dev".equalsIgnoreCase(env)
                ? Optional.empty()
                : Optional.of(minimalJwt());
        return new RulesConfiguration(
                "rules",
                "http://localhost:8080/rules",
                8080,
                "https://api.clockify.me/api",
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                false,
                120_000L,
                env,
                jwt,
                Optional.empty()
        );
    }

    private JwtBootstrapConfig minimalJwt() {
        return new JwtBootstrapConfig(
                Optional.of("-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...\n-----END PUBLIC KEY-----"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("clockify"),
                Optional.of("rules"),
                60,
                JwtBootstrapConfig.JwtKeySource.PUBLIC_KEY
        );
    }
}
