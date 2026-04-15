-- Optional storefront category label for merchant settings / catalog grouping hints.
ALTER TABLE merchant_profiles ADD COLUMN store_category VARCHAR(120) NULL;

-- Optional LINE / messenger deep link (separate from public website).
ALTER TABLE merchant_profiles ADD COLUMN line_contact_url VARCHAR(500) NULL;
