-- Enforce that client_profiles rows always reference an existing platform user.
ALTER TABLE client_profiles
  ADD CONSTRAINT fk_client_profiles_platform_user
  FOREIGN KEY (platform_user_id) REFERENCES platform_users (id);
