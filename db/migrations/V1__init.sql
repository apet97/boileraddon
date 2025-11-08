-- Initial schema for Clockify add-on boilerplate
-- Tokens
CREATE TABLE IF NOT EXISTS addon_tokens (
  workspace_id VARCHAR(255) PRIMARY KEY,
  auth_token   TEXT NOT NULL,
  api_base_url VARCHAR(512),
  created_at   BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000,
  last_accessed_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())*1000
);
CREATE INDEX IF NOT EXISTS idx_tokens_created  ON addon_tokens(created_at);
CREATE INDEX IF NOT EXISTS idx_tokens_accessed ON addon_tokens(last_accessed_at);

-- Rules (optional; used by rules add-on)
CREATE TABLE IF NOT EXISTS rules (
  workspace_id VARCHAR(128) NOT NULL,
  rule_id      VARCHAR(128) NOT NULL,
  rule_json    TEXT NOT NULL,
  PRIMARY KEY (workspace_id, rule_id)
);

