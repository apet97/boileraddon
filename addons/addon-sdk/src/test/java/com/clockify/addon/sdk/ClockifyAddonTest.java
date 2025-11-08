package com.clockify.addon.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClockifyAddonTest {

    @Test
    void registerCustomEndpointNormalizesPaths() {
        ClockifyManifest manifest = ClockifyManifest
                .v1_3Builder()
                .key("test-addon")
                .name("Test Add-on")
                .description("Test manifest")
                .baseUrl("https://example.com/addon")
                .minimalSubscriptionPlan("FREE")
                .scopes(new String[]{"TIME_ENTRY_READ"})
                .build();

        ClockifyAddon addon = new ClockifyAddon(manifest);

        RequestHandler slashHandler = request -> HttpResponse.ok("slash");
        RequestHandler noSlashHandler = request -> HttpResponse.ok("no-slash");

        addon.registerCustomEndpoint("/settings", slashHandler);

        assertSame(slashHandler, addon.getEndpoints().get("/settings"));

        addon.registerCustomEndpoint("settings", noSlashHandler);

        assertEquals(1, addon.getEndpoints().size());
        assertSame(noSlashHandler, addon.getEndpoints().get("/settings"));
    }
}
