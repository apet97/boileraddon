package com.example.rules.health;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.security.PooledDatabaseTokenStore;
import com.example.rules.store.RulesStore;
import com.example.rules.store.RulesStoreSPI;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReadinessHandlerTest {

    @Test
    void readinessPassesForInMemoryStore() throws Exception {
        ReadinessHandler handler = new ReadinessHandler(new RulesStore(), null);

        HttpResponse response = handler.handle(Mockito.mock(HttpServletRequest.class));
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("READY"));
    }

    @Test
    void readinessFailsWhenRulesStoreThrows() throws Exception {
        RulesStoreSPI failingStore = new RulesStoreSPI() {
            @Override public com.example.rules.engine.Rule save(String workspaceId, com.example.rules.engine.Rule rule) { throw new UnsupportedOperationException(); }
            @Override public Optional<com.example.rules.engine.Rule> get(String workspaceId, String ruleId) { return Optional.empty(); }
            @Override public List<com.example.rules.engine.Rule> getAll(String workspaceId) { throw new RuntimeException("boom"); }
            @Override public List<com.example.rules.engine.Rule> getEnabled(String workspaceId) { return List.of(); }
            @Override public boolean delete(String workspaceId, String ruleId) { return false; }
            @Override public int deleteAll(String workspaceId) { return 0; }
            @Override public boolean exists(String workspaceId, String ruleId) { return false; }
            @Override public int count(String workspaceId) { return 0; }
            @Override public void clear() { }
        };

        ReadinessHandler handler = new ReadinessHandler(failingStore, null);

        HttpResponse response = handler.handle(Mockito.mock(HttpServletRequest.class));
        assertEquals(503, response.getStatusCode());
        assertTrue(response.getBody().contains("DEGRADED"));
    }

    @Test
    void readinessFailsWhenTokenStoreUnavailable() throws Exception {
        PooledDatabaseTokenStore tokenStore = Mockito.mock(PooledDatabaseTokenStore.class);
        Mockito.when(tokenStore.count()).thenThrow(new RuntimeException("db down"));

        ReadinessHandler handler = new ReadinessHandler(new RulesStore(), tokenStore);

        HttpResponse response = handler.handle(Mockito.mock(HttpServletRequest.class));
        assertEquals(503, response.getStatusCode());
        assertTrue(response.getBody().contains("DEGRADED"));
    }
}
