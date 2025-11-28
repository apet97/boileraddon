package com.clockify.addon.rules;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesAppDevConfigTest {

    @Test
    void registersDebugEndpointOnlyInDev() {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("rules")
                .name("Rules")
                .description("Test")
                .baseUrl("http://localhost:8080/rules")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ", "TIME_ENTRY_WRITE", "TAG_READ", "TAG_WRITE"})
                .build();

        ClockifyAddon devAddon = new ClockifyAddon(manifest);
        RulesConfiguration devConfig = new RulesConfiguration(
                "rules",
                "http://localhost:8080/rules",
                8080,
                "dev",
                Optional.empty(),
                false
        );
        RulesApp.registerDevConfigEndpoint(devAddon, devConfig);
        assertTrue(devAddon.getEndpoints().containsKey("/debug/config"));

        ClockifyAddon prodAddon = new ClockifyAddon(manifest);
        RulesConfiguration prodConfig = new RulesConfiguration(
                "rules",
                "http://localhost:8080/rules",
                8080,
                "prod",
                Optional.empty(),
                false
        );
        RulesApp.registerDevConfigEndpoint(prodAddon, prodConfig);
        assertFalse(prodAddon.getEndpoints().containsKey("/debug/config"));
    }
}
