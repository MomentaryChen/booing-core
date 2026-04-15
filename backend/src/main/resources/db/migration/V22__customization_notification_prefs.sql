-- Merchant notification preferences (merchant portal settings), stored with customization.
ALTER TABLE customization_configs ADD COLUMN notification_new_booking BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE customization_configs ADD COLUMN notification_cancellation BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE customization_configs ADD COLUMN notification_daily_summary BOOLEAN NOT NULL DEFAULT FALSE;
