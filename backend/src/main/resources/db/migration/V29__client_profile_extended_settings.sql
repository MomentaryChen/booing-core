ALTER TABLE client_profiles
    ADD COLUMN location VARCHAR(160) NULL,
    ADD COLUMN bio VARCHAR(1000) NULL,
    ADD COLUMN theme VARCHAR(16) NULL,
    ADD COLUMN push_notifications BOOLEAN NULL,
    ADD COLUMN marketing_emails BOOLEAN NULL,
    ADD COLUMN security_alerts BOOLEAN NULL,
    ADD COLUMN product_updates BOOLEAN NULL,
    ADD COLUMN two_factor_enabled BOOLEAN NULL;
