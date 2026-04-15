-- Force-align availability_exceptions.merchant_id to UUID storage when merchant PK is UUID.
-- This migration is destructive ONLY for availability_exceptions data:
-- - If merchant_id is not binary and merchants.id is binary, it truncates availability_exceptions,
--   then converts merchant_id to BINARY(16), and recreates FK/index.
-- - If prerequisites are not met, it becomes a no-op.

SET @merchant_id_data_type := (
  SELECT DATA_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'merchants'
    AND COLUMN_NAME = 'id'
  LIMIT 1
);

SET @availability_table_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
);

SET @availability_merchant_type := (
  SELECT DATA_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
    AND COLUMN_NAME = 'merchant_id'
  LIMIT 1
);

SET @can_force_align := (
  @merchant_id_data_type = 'binary'
  AND @availability_table_exists = 1
  AND @availability_merchant_type <> 'binary'
);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_availability_exceptions_merchant'
);

SET @ddl_sql := IF(
  @can_force_align = 1 AND @fk_exists > 0,
  'ALTER TABLE availability_exceptions DROP FOREIGN KEY fk_availability_exceptions_merchant',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
    AND INDEX_NAME = 'idx_availability_exceptions_merchant_id'
);

SET @ddl_sql := IF(
  @can_force_align = 1 AND @idx_exists > 0,
  'ALTER TABLE availability_exceptions DROP INDEX idx_availability_exceptions_merchant_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- User accepted clearing data; reset incompatible rows before type conversion.
SET @ddl_sql := IF(
  @can_force_align = 1,
  'TRUNCATE TABLE availability_exceptions',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @can_force_align = 1,
  'ALTER TABLE availability_exceptions MODIFY COLUMN merchant_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @can_force_align = 1,
  'ALTER TABLE availability_exceptions
      ADD KEY idx_availability_exceptions_merchant_id (merchant_id),
      ADD CONSTRAINT fk_availability_exceptions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
