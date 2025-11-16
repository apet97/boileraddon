package com.example.overtime;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvertimeAppDevConfigTest {

    @Test
    void registersEndpointOnlyInDev() {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("overtime")
                .name("Overtime")
                .description("Test")
                .baseUrl("http://localhost:8080/overtime")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon devAddon = new ClockifyAddon(manifest);
        OvertimeConfiguration devConfig = new OvertimeConfiguration(
                "overtime",
                "http://localhost:8080/overtime",
                8080,
                "dev",
                Optional.empty()
        );
        OvertimeApp.registerDevConfigEndpoint(devAddon, devConfig);
        assertTrue(devAddon.getEndpoints().containsKey("/debug/config"));

        ClockifyAddon prodAddon = new ClockifyAddon(manifest);
        OvertimeConfiguration prodConfig = new OvertimeConfiguration(
                "overtime",
                "http://localhost:8080/overtime",
                8080,
                "prod",
                Optional.empty()
        );
        OvertimeApp.registerDevConfigEndpoint(prodAddon, prodConfig);
        assertFalse(prodAddon.getEndpoints().containsKey("/debug/config"));
    }
}
