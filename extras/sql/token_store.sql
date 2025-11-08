-- Example schema for storing installation tokens per workspace
CREATE TABLE IF NOT EXISTS addon_install_tokens (
  workspace_id VARCHAR(64) PRIMARY KEY,
  token TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
