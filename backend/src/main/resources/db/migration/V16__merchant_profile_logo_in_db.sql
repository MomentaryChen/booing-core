-- Store merchant logo image payload directly in DB as data URL/base64.
ALTER TABLE merchant_profiles ADD COLUMN logo_data LONGTEXT NULL;

UPDATE merchant_profiles
SET logo_data = logo_url
WHERE logo_url IS NOT NULL AND TRIM(logo_url) <> '';

ALTER TABLE merchant_profiles DROP COLUMN logo_url;
