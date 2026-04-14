-- Business contact and storefront fields on merchant profile (not customization marketing slots).
ALTER TABLE merchant_profiles ADD COLUMN address VARCHAR(500) NULL;
ALTER TABLE merchant_profiles ADD COLUMN phone VARCHAR(120) NULL;
ALTER TABLE merchant_profiles ADD COLUMN email VARCHAR(120) NULL;
ALTER TABLE merchant_profiles ADD COLUMN website VARCHAR(500) NULL;
