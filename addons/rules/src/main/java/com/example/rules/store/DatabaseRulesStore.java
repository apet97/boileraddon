package com.example.rules.store;

import com.example.rules.engine.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed RulesStore implementation storing rules as JSON per workspace.
 * Schema:
 *   CREATE TABLE IF NOT EXISTS rules (
 *     workspace_id VARCHAR(128) NOT NULL,
 *     rule_id      VARCHAR(128) NOT NULL,
 *     rule_json    TEXT NOT NULL,
 *     PRIMARY KEY(workspace_id, rule_id)
 *   );
 */
public class DatabaseRulesStore implements RulesStoreSPI {
    private static final Logger log = LoggerFactory.getLogger(DatabaseRulesStore.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String url;
    private final String username;
    private final String password;

    public DatabaseRulesStore(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        ensureSchema();
    }

    public static DatabaseRulesStore fromEnvironment() {
        String url = getenvOr("RULES_DB_URL", getenvOr("DB_URL", null));
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing RULES_DB_URL or DB_URL");
        }
        String user = getenvOr("RULES_DB_USERNAME", getenvOr("DB_USER", System.getenv("DB_USERNAME")));
        String pass = getenvOr("RULES_DB_PASSWORD", System.getenv("DB_PASSWORD"));
        return new DatabaseRulesStore(url, user, pass);
    }

    @Override
    public Rule save(String workspaceId, Rule rule) {
        if (workspaceId == null || rule == null) throw new IllegalArgumentException("workspaceId and rule are required");
        try (Connection c = conn()) {
            String json = mapper.writeValueAsString(rule);
            try (PreparedStatement ps = c.prepareStatement("UPDATE rules SET rule_json=? WHERE workspace_id=? AND rule_id=?")) {
                ps.setString(1, json);
                ps.setString(2, workspaceId);
                ps.setString(3, rule.getId());
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement ins = c.prepareStatement("INSERT INTO rules(workspace_id, rule_id, rule_json) VALUES(?,?,?)")) {
                        ins.setString(1, workspaceId);
                        ins.setString(2, rule.getId());
                        ins.setString(3, json);
                        ins.executeUpdate();
                    }
                }
            }
            return rule;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save rule", e);
        }
    }

    @Override
    public Optional<Rule> get(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) return Optional.empty();
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT rule_json FROM rules WHERE workspace_id=? AND rule_id=?")) {
            ps.setString(1, workspaceId);
            ps.setString(2, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString(1);
                    return Optional.of(mapper.readValue(json, Rule.class));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get rule", e);
        }
    }

    @Override
    public List<Rule> getAll(String workspaceId) {
        List<Rule> out = new ArrayList<>();
        if (workspaceId == null) return out;
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT rule_json FROM rules WHERE workspace_id=?")) {
            ps.setString(1, workspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.readValue(rs.getString(1), Rule.class));
                }
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get rules", e);
        }
    }

    @Override
    public List<Rule> getEnabled(String workspaceId) {
        List<Rule> all = getAll(workspaceId);
        all.removeIf(r -> !r.isEnabled());
        return all;
    }

    @Override
    public boolean delete(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) return false;
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rules WHERE workspace_id=? AND rule_id=?")) {
            ps.setString(1, workspaceId);
            ps.setString(2, ruleId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete rule", e);
        }
    }

    @Override
    public int deleteAll(String workspaceId) {
        if (workspaceId == null) return 0;
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rules WHERE workspace_id=?")) {
            ps.setString(1, workspaceId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all rules", e);
        }
    }

    @Override
    public boolean exists(String workspaceId, String ruleId) {
        if (workspaceId == null || ruleId == null) return false;
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rules WHERE workspace_id=? AND rule_id=?")) {
            ps.setString(1, workspaceId);
            ps.setString(2, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence", e);
        }
    }

    @Override
    public int count(String workspaceId) {
        if (workspaceId == null) return 0;
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM rules WHERE workspace_id=?")) {
            ps.setString(1, workspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rules", e);
        }
    }

    @Override
    public void clear() {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM rules");
        } catch (SQLException e) {
            log.warn("Failed to clear rules: {}", e.getMessage());
        }
    }

    private Connection conn() throws SQLException {
        return (username == null || username.isBlank())
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, username, password);
    }

    private void ensureSchema() {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rules (" +
                    "workspace_id VARCHAR(128) NOT NULL, " +
                    "rule_id VARCHAR(128) NOT NULL, " +
                    "rule_json TEXT NOT NULL, " +
                    "PRIMARY KEY(workspace_id, rule_id))");
        } catch (SQLException e) {
            log.warn("Could not ensure rules schema: {}", e.getMessage());
        }
    }

    private static String getenvOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
