-- Store service image payload directly in DB as data URL/base64.
ALTER TABLE service_items ADD COLUMN image_data LONGTEXT NULL;
