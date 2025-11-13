package com.example.rules;

import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import com.clockify.addon.sdk.security.TokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RulesAppTokenStoreTest {

    @AfterEach
    void resetTokenStore() {
        TokenStore.configurePersistence(null);
    }

    @Test
    void initializeTokenStoreThrowsWhenDatabaseUnavailable() {
        String jdbcUrl = "jdbc:postgresql://db-host:5432/rules";

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                RulesApp.initializeTokenStore(
                        true,
                        "prod",
                        jdbcUrl,
                        "user",
                        "secret",
                        () -> { throw new RuntimeException("boom"); }
                ));

        assertTrue(ex.getMessage().contains("FATAL"));
        assertTrue(ex.getMessage().contains(jdbcUrl));
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void initializeTokenStoreSkipsWhenPersistenceDisabled() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);

        PooledDatabaseTokenStore result = RulesApp.initializeTokenStore(
                false,
                "dev",
                "jdbc:postgresql://db-host:5432/rules",
                "user",
                "secret",
                () -> {
                    invoked.set(true);
                    return Mockito.mock(PooledDatabaseTokenStore.class);
                }
        );

        assertNull(result);
        assertFalse(invoked.get(), "Token store factory should not be invoked when persistence is disabled");
    }

    @Test
    void initializeTokenStoreReturnsStoreWhenSuccessful() throws Exception {
        PooledDatabaseTokenStore store = Mockito.mock(PooledDatabaseTokenStore.class);

        PooledDatabaseTokenStore result = RulesApp.initializeTokenStore(
                true,
                "prod",
                "jdbc:postgresql://db-host:5432/rules",
                "user",
                "secret",
                () -> store
        );

        assertSame(store, result);
    }
}

