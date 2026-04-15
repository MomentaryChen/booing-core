ALTER TABLE client_profiles
    ADD COLUMN language VARCHAR(16) NULL,
    ADD COLUMN timezone VARCHAR(64) NULL,
    ADD COLUMN currency VARCHAR(16) NULL,
    ADD COLUMN email_notifications BOOLEAN NULL,
    ADD COLUMN sms_notifications BOOLEAN NULL;

ALTER TABLE platform_users
    ADD COLUMN password_updated_at TIMESTAMP NULL,
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
