package com.example.templateaddon;

import com.clockify.addon.sdk.ClockifyAddon;
import com.clockify.addon.sdk.ClockifyManifest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateAddonAppDevConfigTest {

    @Test
    void registersDebugEndpointOnlyInDev() {
        ClockifyManifest manifest = ClockifyManifest.v1_3Builder()
                .key("_template-addon")
                .name("Template")
                .description("Test")
                .baseUrl("http://localhost:8080/template")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon devAddon = new ClockifyAddon(manifest);
        TemplateAddonConfiguration devConfig = new TemplateAddonConfiguration(
                "_template-addon",
                "http://localhost:8080/template",
                8080,
                "dev",
                Optional.empty()
        );
        TemplateAddonApp.registerDevConfigEndpoint(devAddon, devConfig);
        assertTrue(devAddon.getEndpoints().containsKey("/debug/config"));

        ClockifyAddon prodAddon = new ClockifyAddon(manifest);
        TemplateAddonConfiguration prodConfig = new TemplateAddonConfiguration(
                "_template-addon",
                "http://localhost:8080/template",
                8080,
                "prod",
                Optional.empty()
        );
        TemplateAddonApp.registerDevConfigEndpoint(prodAddon, prodConfig);
        assertFalse(prodAddon.getEndpoints().containsKey("/debug/config"));
    }
}
