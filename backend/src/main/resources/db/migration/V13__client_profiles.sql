CREATE TABLE IF NOT EXISTS client_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_user_id BIGINT NOT NULL UNIQUE,
  display_name VARCHAR(120),
  contact_phone VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- platform_user_id is UNIQUE: implicit index is sufficient for FK lookups (see V15).
