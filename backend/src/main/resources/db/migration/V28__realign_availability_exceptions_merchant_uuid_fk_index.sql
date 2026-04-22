-- Align MySQL schema with UUID-based JPA entities.
-- This migration is intentionally destructive for affected domain tables:
-- - BIGINT primary keys/fks mapped to UUID entities are converted to BINARY(16).
-- - Rows in affected tables are truncated before type conversion.
-- - Foreign keys and deterministic uniqueness constraints are recreated.

SET @merchant_id_data_type := (
  SELECT DATA_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'merchants'
    AND COLUMN_NAME = 'id'
  LIMIT 1
);

SET @needs_alignment := (@merchant_id_data_type <> 'binary');

SET @ddl_sql := IF(@needs_alignment = 1, 'SET FOREIGN_KEY_CHECKS = 0', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'TRUNCATE TABLE resource_staff_assignments',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE team_members', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE service_teams', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE merchant_invitations', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE merchant_memberships', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE client_profiles', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE platform_user_rbac_bindings', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE bookings', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE availability_exceptions', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE business_hours', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE service_items', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE resource_items', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE dynamic_field_configs', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE customization_configs', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE merchant_profiles', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE platform_users', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'TRUNCATE TABLE merchants', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE platform_user_rbac_bindings DROP INDEX uk_pub_user_role_scope, DROP COLUMN merchant_scope_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE merchant_profiles DROP FOREIGN KEY fk_merchant_profiles_merchant',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE customization_configs DROP FOREIGN KEY fk_customization_configs_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE dynamic_field_configs DROP FOREIGN KEY fk_dynamic_field_configs_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_items DROP FOREIGN KEY fk_resource_items_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_items DROP FOREIGN KEY fk_service_items_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE business_hours DROP FOREIGN KEY fk_business_hours_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE availability_exceptions DROP FOREIGN KEY fk_availability_exceptions_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_service_item', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_platform_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE platform_users DROP FOREIGN KEY fk_platform_users_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE platform_user_rbac_bindings DROP FOREIGN KEY fk_pub_platform_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE platform_user_rbac_bindings DROP FOREIGN KEY fk_pub_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_invitations DROP FOREIGN KEY fk_merchant_invitations_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_invitations DROP FOREIGN KEY fk_merchant_invitations_invitee_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_memberships DROP FOREIGN KEY fk_merchant_memberships_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_memberships DROP FOREIGN KEY fk_merchant_memberships_platform_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_teams DROP FOREIGN KEY fk_service_teams_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE team_members DROP FOREIGN KEY fk_team_members_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE team_members DROP FOREIGN KEY fk_team_members_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_staff_assignments DROP FOREIGN KEY fk_rsa_merchant', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_staff_assignments DROP FOREIGN KEY fk_rsa_booking', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_staff_assignments DROP FOREIGN KEY fk_rsa_resource', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_staff_assignments DROP FOREIGN KEY fk_rsa_staff_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE client_profiles DROP FOREIGN KEY fk_client_profiles_platform_user', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchants MODIFY COLUMN id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE platform_users MODIFY COLUMN id BINARY(16) NOT NULL, MODIFY COLUMN merchant_id BINARY(16) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_items MODIFY COLUMN id BINARY(16) NOT NULL, MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_items MODIFY COLUMN id BINARY(16) NOT NULL, MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE bookings
      MODIFY COLUMN id BINARY(16) NOT NULL,
      MODIFY COLUMN merchant_id BINARY(16) NOT NULL,
      MODIFY COLUMN service_item_id BINARY(16) NOT NULL,
      MODIFY COLUMN platform_user_id BINARY(16) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_profiles MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE customization_configs MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE dynamic_field_configs MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE business_hours MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE availability_exceptions MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE merchant_invitations
      MODIFY COLUMN merchant_id BINARY(16) NOT NULL,
      MODIFY COLUMN invitee_user_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE merchant_memberships
      MODIFY COLUMN merchant_id BINARY(16) NOT NULL,
      MODIFY COLUMN platform_user_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_teams MODIFY COLUMN merchant_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE team_members
      MODIFY COLUMN merchant_id BINARY(16) NOT NULL,
      MODIFY COLUMN user_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE resource_staff_assignments
      MODIFY COLUMN merchant_id BINARY(16) NOT NULL,
      MODIFY COLUMN booking_id BINARY(16) NOT NULL,
      MODIFY COLUMN resource_id BINARY(16) NOT NULL,
      MODIFY COLUMN staff_user_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE platform_user_rbac_bindings
      MODIFY COLUMN platform_user_id BINARY(16) NOT NULL,
      MODIFY COLUMN merchant_id BINARY(16) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE client_profiles MODIFY COLUMN platform_user_id BINARY(16) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE platform_users
      ADD CONSTRAINT fk_platform_users_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE merchant_profiles ADD CONSTRAINT fk_merchant_profiles_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE customization_configs ADD CONSTRAINT fk_customization_configs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE dynamic_field_configs ADD CONSTRAINT fk_dynamic_field_configs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE resource_items ADD CONSTRAINT fk_resource_items_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_items ADD CONSTRAINT fk_service_items_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE business_hours ADD CONSTRAINT fk_business_hours_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE availability_exceptions ADD CONSTRAINT fk_availability_exceptions_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE bookings
      ADD CONSTRAINT fk_bookings_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
      ADD CONSTRAINT fk_bookings_service_item FOREIGN KEY (service_item_id) REFERENCES service_items (id),
      ADD CONSTRAINT fk_bookings_platform_user FOREIGN KEY (platform_user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE merchant_invitations
      ADD CONSTRAINT fk_merchant_invitations_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
      ADD CONSTRAINT fk_merchant_invitations_invitee_user FOREIGN KEY (invitee_user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE merchant_memberships
      ADD CONSTRAINT fk_merchant_memberships_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
      ADD CONSTRAINT fk_merchant_memberships_platform_user FOREIGN KEY (platform_user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'ALTER TABLE service_teams ADD CONSTRAINT fk_service_teams_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE team_members
      ADD CONSTRAINT fk_team_members_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
      ADD CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE resource_staff_assignments
      ADD CONSTRAINT fk_rsa_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
      ADD CONSTRAINT fk_rsa_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
      ADD CONSTRAINT fk_rsa_resource FOREIGN KEY (resource_id) REFERENCES resource_items (id),
      ADD CONSTRAINT fk_rsa_staff_user FOREIGN KEY (staff_user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE platform_user_rbac_bindings
      ADD CONSTRAINT fk_pub_platform_user FOREIGN KEY (platform_user_id) REFERENCES platform_users (id) ON DELETE CASCADE,
      ADD CONSTRAINT fk_pub_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE client_profiles
      ADD CONSTRAINT fk_client_profiles_platform_user FOREIGN KEY (platform_user_id) REFERENCES platform_users (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @needs_alignment = 1,
  'ALTER TABLE platform_user_rbac_bindings
      ADD COLUMN merchant_scope_id BINARY(16)
        AS (COALESCE(merchant_id, UNHEX(''00000000000000000000000000000000''))) STORED,
      ADD UNIQUE KEY uk_pub_user_role_scope (platform_user_id, rbac_role_id, merchant_scope_id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(@needs_alignment = 1, 'SET FOREIGN_KEY_CHECKS = 1', 'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
