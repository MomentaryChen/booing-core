-- Align availability_exceptions.merchant_id with UUID-based merchant PK when prerequisites are met.
-- Safe behavior:
-- - If merchants.id is not BINARY(16), this migration becomes a no-op.
-- - If availability_exceptions has existing non-UUID rows, this migration becomes a no-op.
--   (Automatic BIGINT -> UUID mapping is not lossless without an explicit mapping table.)

SET @merchant_id_data_type := (
  SELECT DATA_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'merchants'
    AND COLUMN_NAME = 'id'
  LIMIT 1
);

SET @availability_merchant_type := (
  SELECT DATA_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
    AND COLUMN_NAME = 'merchant_id'
  LIMIT 1
);

SET @availability_table_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
);

SET @availability_row_count := IF(@availability_table_exists = 1, (SELECT COUNT(*) FROM availability_exceptions), 0);
SET @can_align := (
  @merchant_id_data_type = 'binary'
  AND @availability_table_exists = 1
  AND (@availability_merchant_type = 'binary' OR @availability_row_count = 0)
);

-- Convert only when needed.
SET @ddl_sql := IF(
  @can_align = 1 AND @availability_merchant_type <> 'binary',
  'ALTER TABLE availability_exceptions MODIFY COLUMN merchant_id BINARY(16) NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure FK/index are aligned with converted type.
SET @fk_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'availability_exceptions'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_availability_exceptions_merchant'
);

SET @ddl_sql := IF(
  @can_align = 1 AND @fk_exists > 0,
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
  @can_align = 1 AND @idx_exists > 0,
  'ALTER TABLE availability_exceptions DROP INDEX idx_availability_exceptions_merchant_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql := IF(
  @can_align = 1,
  'ALTER TABLE availability_exceptions
      ADD KEY idx_availability_exceptions_merchant_id (merchant_id),
      ADD CONSTRAINT fk_availability_exceptions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
