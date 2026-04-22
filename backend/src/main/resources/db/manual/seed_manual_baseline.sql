-- Manual demo seed (MySQL)
--
-- IMPORTANT (UUID / JPA alignment):
--   The Java domain model uses UUID primary keys. This manual seed supports both:
--   - legacy BIGINT auto-increment ids
--   - UUID/BINARY(16) ids introduced by newer migrations
--
-- Purpose:
--   1) Optional demo data: merchant + merchant/client platform users (not internal SYSTEM_ADMIN).
--   2) Internal SYSTEM_ADMIN is created by the application on first boot when configured
--      (booking.platform.auth.internal-system-admin.*); do not duplicate here.
--   3) Idempotent inserts.
--
-- Usage:
--   1) Update the variables in the "INPUTS" section.
--   2) Execute this SQL manually after Flyway migrations (and after backend has run once if you rely
--      on app-provisioned admin).
--   3) Re-running is safe (idempotent).

START TRANSACTION;

SET @uses_binary_uuid_ids = (
  SELECT CASE WHEN DATA_TYPE = 'binary' THEN 1 ELSE 0 END
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'merchants'
    AND COLUMN_NAME = 'id'
  LIMIT 1
);

-- =========================
-- INPUTS (edit before run)
-- =========================
SET @merchant_name = 'Manual Seed Merchant';
SET @merchant_slug = 'manual-seed-merchant';
SET @merchant_service_limit = 20;
SET @merchant2_name = 'Urban Fade Studio';
SET @merchant2_slug = 'urban-fade-studio';
SET @merchant3_name = 'Serenity Spa Lab';
SET @merchant3_slug = 'serenity-spa-lab';

SET @merchant_username = 'merchant.manual@demo.local';
SET @merchant2_username = 'merchant.urban@demo.local';
SET @merchant3_username = 'merchant.serenity@demo.local';
SET @client_username = 'client.manual@demo.local';

-- Optional: rename legacy demo usernames from older seed revisions (safe if rows don't exist).
-- NOTE: MySQL forbids `UPDATE t ... WHERE NOT EXISTS (SELECT ... FROM t)` on the same table; use JOIN instead.
UPDATE platform_users legacy
LEFT JOIN platform_users conflict
  ON conflict.username = 'merchant.manual@demo.local'
 AND conflict.id <> legacy.id
SET legacy.username = 'merchant.manual@demo.local'
WHERE legacy.username = 'merchant_manual'
  AND conflict.id IS NULL;

UPDATE platform_users legacy
LEFT JOIN platform_users conflict
  ON conflict.username = 'merchant.urban@demo.local'
 AND conflict.id <> legacy.id
SET legacy.username = 'merchant.urban@demo.local'
WHERE legacy.username = 'merchant_urban'
  AND conflict.id IS NULL;

UPDATE platform_users legacy
LEFT JOIN platform_users conflict
  ON conflict.username = 'merchant.serenity@demo.local'
 AND conflict.id <> legacy.id
SET legacy.username = 'merchant.serenity@demo.local'
WHERE legacy.username = 'merchant_serenity'
  AND conflict.id IS NULL;

UPDATE platform_users legacy
LEFT JOIN platform_users conflict
  ON conflict.username = 'client.manual@demo.local'
 AND conflict.id <> legacy.id
SET legacy.username = 'client.manual@demo.local'
WHERE legacy.username = 'client_manual'
  AND conflict.id IS NULL;

-- IMPORTANT:
-- password_hash must be bcrypt (Spring Security format), not plaintext.
-- Example placeholder format: '$2a$10$...'
-- Demo bcrypt hashes (Spring BCryptPasswordEncoder) for:
-- - merchants: DemoMerchant123!
-- - client: DemoClient123!
SET @merchant_password_hash = '$2a$10$EZU3lPSyVh26zAeEkyCeXOgfLgy8lkd9AawLW8qGNO6xzvNAB3Kmi';
SET @merchant2_password_hash = '$2a$10$EZU3lPSyVh26zAeEkyCeXOgfLgy8lkd9AawLW8qGNO6xzvNAB3Kmi';
SET @merchant3_password_hash = '$2a$10$EZU3lPSyVh26zAeEkyCeXOgfLgy8lkd9AawLW8qGNO6xzvNAB3Kmi';
SET @client_password_hash = '$2a$10$8zufREAXtI3t9cNeE1Xizupn2LN5vwDkr4pjJd5SGQ4L3KsvBgSCm';

-- =========================
-- Seed merchant
-- =========================
INSERT INTO merchants (id, name, slug, active, service_limit, visibility)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_name, @merchant_slug, b'1', @merchant_service_limit, 'PUBLIC'
WHERE NOT EXISTS (
  SELECT 1 FROM merchants WHERE slug = @merchant_slug
);

SET @merchant_id = (SELECT id FROM merchants WHERE slug = @merchant_slug LIMIT 1);
-- Optional extra merchants for richer storefront/UI demo
INSERT INTO merchants (id, name, slug, active, service_limit, visibility)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_name, @merchant2_slug, b'1', 20, 'PUBLIC'
WHERE NOT EXISTS (
  SELECT 1 FROM merchants WHERE slug = @merchant2_slug
);

INSERT INTO merchants (id, name, slug, active, service_limit, visibility)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_name, @merchant3_slug, b'1', 20, 'PUBLIC'
WHERE NOT EXISTS (
  SELECT 1 FROM merchants WHERE slug = @merchant3_slug
);

SET @merchant2_id = (SELECT id FROM merchants WHERE slug = @merchant2_slug LIMIT 1);
SET @merchant3_id = (SELECT id FROM merchants WHERE slug = @merchant3_slug LIMIT 1);

-- =========================
-- Seed platform users (demo roles only)
-- =========================
INSERT INTO platform_users (
  id,
  username,
  password_hash,
  platform_role,
  merchant_id,
  enabled,
  credential_version,
  failed_login_count
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_username, @merchant_password_hash, 'MERCHANT', @merchant_id, b'1', 0, 0
WHERE NOT EXISTS (
  SELECT 1 FROM platform_users WHERE username = @merchant_username
);

INSERT INTO platform_users (
  id,
  username,
  password_hash,
  platform_role,
  merchant_id,
  enabled,
  credential_version,
  failed_login_count
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_username, @merchant2_password_hash, 'MERCHANT', @merchant2_id, b'1', 0, 0
WHERE NOT EXISTS (
  SELECT 1 FROM platform_users WHERE username = @merchant2_username
);

INSERT INTO platform_users (
  id,
  username,
  password_hash,
  platform_role,
  merchant_id,
  enabled,
  credential_version,
  failed_login_count
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_username, @merchant3_password_hash, 'MERCHANT', @merchant3_id, b'1', 0, 0
WHERE NOT EXISTS (
  SELECT 1 FROM platform_users WHERE username = @merchant3_username
);

INSERT INTO platform_users (
  id,
  username,
  password_hash,
  platform_role,
  merchant_id,
  enabled,
  credential_version,
  failed_login_count
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @client_username, @client_password_hash, 'CLIENT', NULL, b'1', 0, 0
WHERE NOT EXISTS (
  SELECT 1 FROM platform_users WHERE username = @client_username
);

SET @merchant_user_id = (SELECT id FROM platform_users WHERE username = @merchant_username LIMIT 1);
SET @merchant2_user_id = (SELECT id FROM platform_users WHERE username = @merchant2_username LIMIT 1);
SET @merchant3_user_id = (SELECT id FROM platform_users WHERE username = @merchant3_username LIMIT 1);
SET @client_user_id = (SELECT id FROM platform_users WHERE username = @client_username LIMIT 1);

-- =========================
-- Seed merchant membership
-- =========================
INSERT INTO merchant_memberships (merchant_id, platform_user_id, membership_status, joined_at, updated_at)
SELECT @merchant_id, @merchant_user_id, 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM merchant_memberships
  WHERE merchant_id = @merchant_id
    AND platform_user_id = @merchant_user_id
);

INSERT INTO merchant_memberships (merchant_id, platform_user_id, membership_status, joined_at, updated_at)
SELECT @merchant2_id, @merchant2_user_id, 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM merchant_memberships
  WHERE merchant_id = @merchant2_id
    AND platform_user_id = @merchant2_user_id
);

INSERT INTO merchant_memberships (merchant_id, platform_user_id, membership_status, joined_at, updated_at)
SELECT @merchant3_id, @merchant3_user_id, 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM merchant_memberships
  WHERE merchant_id = @merchant3_id
    AND platform_user_id = @merchant3_user_id
);

-- =========================
-- Seed RBAC bindings
-- =========================
SET @role_merchant_id = (SELECT id FROM rbac_roles WHERE code = 'MERCHANT' LIMIT 1);
SET @role_client_id = (SELECT id FROM rbac_roles WHERE code = 'CLIENT' LIMIT 1);

INSERT INTO platform_user_rbac_bindings (platform_user_id, rbac_role_id, merchant_id, status)
SELECT @merchant_user_id, @role_merchant_id, @merchant_id, 'ACTIVE'
WHERE @role_merchant_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM platform_user_rbac_bindings
    WHERE platform_user_id = @merchant_user_id
      AND rbac_role_id = @role_merchant_id
      AND merchant_scope_id = @merchant_id
  );

INSERT INTO platform_user_rbac_bindings (platform_user_id, rbac_role_id, merchant_id, status)
SELECT @merchant2_user_id, @role_merchant_id, @merchant2_id, 'ACTIVE'
WHERE @role_merchant_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM platform_user_rbac_bindings
    WHERE platform_user_id = @merchant2_user_id
      AND rbac_role_id = @role_merchant_id
      AND merchant_scope_id = @merchant2_id
  );

INSERT INTO platform_user_rbac_bindings (platform_user_id, rbac_role_id, merchant_id, status)
SELECT @merchant3_user_id, @role_merchant_id, @merchant3_id, 'ACTIVE'
WHERE @role_merchant_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM platform_user_rbac_bindings
    WHERE platform_user_id = @merchant3_user_id
      AND rbac_role_id = @role_merchant_id
      AND merchant_scope_id = @merchant3_id
  );

INSERT INTO platform_user_rbac_bindings (platform_user_id, rbac_role_id, merchant_id, status)
SELECT @client_user_id, @role_client_id, NULL, 'ACTIVE'
WHERE @role_client_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM platform_user_rbac_bindings
    WHERE platform_user_id = @client_user_id
      AND rbac_role_id = @role_client_id
      AND merchant_scope_id = IF(@uses_binary_uuid_ids = 1, UNHEX('00000000000000000000000000000000'), 0)
  );

-- =========================
-- Seed service catalog (multi-merchant diversity for UI)
-- =========================
INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_id, 'Classic Haircut', 30, 450.00, 'Hair'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant_id AND name = 'Classic Haircut'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_id, 'Haircut + Wash', 45, 650.00, 'Hair'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant_id AND name = 'Haircut + Wash'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_id, 'Beard Trim', 20, 300.00, 'Grooming'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant_id AND name = 'Beard Trim'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_id, 'Skin Fade', 40, 700.00, 'Hair'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Skin Fade'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_id, 'Kids Cut', 25, 380.00, 'Family'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Kids Cut'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_id, 'Scalp Detox', 35, 520.00, 'Care'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Scalp Detox'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_id, 'Aromatherapy Massage', 60, 1200.00, 'Relax'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Aromatherapy Massage'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_id, 'Facial Deep Clean', 50, 980.00, 'Skincare'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Facial Deep Clean'
);

INSERT INTO service_items (id, merchant_id, name, duration_minutes, price, category)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_id, 'Shoulder Release', 30, 680.00, 'Therapy'
WHERE NOT EXISTS (
  SELECT 1 FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Shoulder Release'
);

SET @svc_haircut = (
  SELECT id FROM service_items WHERE merchant_id = @merchant_id AND name = 'Classic Haircut' LIMIT 1
);
SET @svc_haircut_wash = (
  SELECT id FROM service_items WHERE merchant_id = @merchant_id AND name = 'Haircut + Wash' LIMIT 1
);
SET @svc_beard = (
  SELECT id FROM service_items WHERE merchant_id = @merchant_id AND name = 'Beard Trim' LIMIT 1
);
SET @svc_skin_fade = (
  SELECT id FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Skin Fade' LIMIT 1
);
SET @svc_kids = (
  SELECT id FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Kids Cut' LIMIT 1
);
SET @svc_scalp = (
  SELECT id FROM service_items WHERE merchant_id = @merchant2_id AND name = 'Scalp Detox' LIMIT 1
);
SET @svc_aroma = (
  SELECT id FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Aromatherapy Massage' LIMIT 1
);
SET @svc_facial = (
  SELECT id FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Facial Deep Clean' LIMIT 1
);
SET @svc_shoulder = (
  SELECT id FROM service_items WHERE merchant_id = @merchant3_id AND name = 'Shoulder Release' LIMIT 1
);

-- =========================
-- Seed resources mapped to services (for richer slot/booking UI)
-- =========================
INSERT INTO resource_items (
  id, merchant_id, name, type, category, capacity, service_items_json, price, active
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant_id, 'Chair A', 'ROOM', 'Barber Chair', 1,
       IF(@uses_binary_uuid_ids = 1,
          CONCAT('["', BIN_TO_UUID(@svc_haircut, 1), '","', BIN_TO_UUID(@svc_haircut_wash, 1), '","', BIN_TO_UUID(@svc_beard, 1), '"]'),
          CONCAT('[', @svc_haircut, ',', @svc_haircut_wash, ',', @svc_beard, ']')),
       0.00, b'1'
WHERE NOT EXISTS (
  SELECT 1 FROM resource_items WHERE merchant_id = @merchant_id AND name = 'Chair A'
);

INSERT INTO resource_items (
  id, merchant_id, name, type, category, capacity, service_items_json, price, active
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant2_id, 'Fade Station 1', 'ROOM', 'Premium Station', 1,
       IF(@uses_binary_uuid_ids = 1,
          CONCAT('["', BIN_TO_UUID(@svc_skin_fade, 1), '","', BIN_TO_UUID(@svc_kids, 1), '","', BIN_TO_UUID(@svc_scalp, 1), '"]'),
          CONCAT('[', @svc_skin_fade, ',', @svc_kids, ',', @svc_scalp, ']')),
       0.00, b'1'
WHERE NOT EXISTS (
  SELECT 1 FROM resource_items WHERE merchant_id = @merchant2_id AND name = 'Fade Station 1'
);

INSERT INTO resource_items (
  id, merchant_id, name, type, category, capacity, service_items_json, price, active
)
SELECT IF(@uses_binary_uuid_ids = 1, UUID_TO_BIN(UUID(), 1), NULL),
       @merchant3_id, 'Aroma Room', 'ROOM', 'Spa Suite', 1,
       IF(@uses_binary_uuid_ids = 1,
          CONCAT('["', BIN_TO_UUID(@svc_aroma, 1), '","', BIN_TO_UUID(@svc_facial, 1), '","', BIN_TO_UUID(@svc_shoulder, 1), '"]'),
          CONCAT('[', @svc_aroma, ',', @svc_facial, ',', @svc_shoulder, ']')),
       0.00, b'1'
WHERE NOT EXISTS (
  SELECT 1 FROM resource_items WHERE merchant_id = @merchant3_id AND name = 'Aroma Room'
);

-- =========================
-- Seed merchant storefront profiles
-- =========================
INSERT INTO merchant_profiles (merchant_id, description, logo_data, address, phone, email, website)
SELECT @merchant_id,
       'Neighborhood barber studio focused on fast and clean classic cuts.',
       'https://images.unsplash.com/photo-1622286344073-c8f8f9d8fbb6?auto=format&fit=crop&w=800&q=80',
       'No. 21, Lane 8, Renai Rd, Taipei',
       '+886-2-2700-1001',
       'hello@manualseedbarber.demo',
       'https://manualseedbarber.demo'
WHERE NOT EXISTS (
  SELECT 1 FROM merchant_profiles WHERE merchant_id = @merchant_id
);

INSERT INTO merchant_profiles (merchant_id, description, logo_data, address, phone, email, website)
SELECT @merchant2_id,
       'Modern fade specialists with family friendly quick booking flow.',
       'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?auto=format&fit=crop&w=800&q=80',
       'No. 77, Zhongxiao E. Rd, Taipei',
       '+886-2-2771-2202',
       'booking@urbanfade.demo',
       'https://urbanfade.demo'
WHERE NOT EXISTS (
  SELECT 1 FROM merchant_profiles WHERE merchant_id = @merchant2_id
);

INSERT INTO merchant_profiles (merchant_id, description, logo_data, address, phone, email, website)
SELECT @merchant3_id,
       'Wellness space for massage, skincare and stress recovery sessions.',
       'https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=800&q=80',
       'No. 5, Songjiang Rd, Taipei',
       '+886-2-2508-3303',
       'care@serenityspa.demo',
       'https://serenityspa.demo'
WHERE NOT EXISTS (
  SELECT 1 FROM merchant_profiles WHERE merchant_id = @merchant3_id
);

-- =========================
-- Seed storefront customization
-- =========================
INSERT INTO customization_configs (
  merchant_id, theme_color, theme_preset, hero_title, booking_flow_text,
  invite_code, terms_text, announcement_text, faq_json, buffer_minutes,
  homepage_sections_json, category_order_json
)
SELECT @merchant_id,
       '#1E40AF',
       'CLASSIC',
       'Manual Seed Barber',
       'Pick a chair and confirm in under 30 seconds.',
       'SEED-BARBER',
       'Please arrive 5 minutes before your appointment.',
       'Weekday walk-in support is available after 14:00.',
       '[{"q":"Can I reschedule?","a":"Yes, up to 2 hours before start."}]',
       10,
       '["hero","services","booking_flow","faq"]',
       '["Hair","Grooming"]'
WHERE NOT EXISTS (
  SELECT 1 FROM customization_configs WHERE merchant_id = @merchant_id
);

INSERT INTO customization_configs (
  merchant_id, theme_color, theme_preset, hero_title, booking_flow_text,
  invite_code, terms_text, announcement_text, faq_json, buffer_minutes,
  homepage_sections_json, category_order_json
)
SELECT @merchant2_id,
       '#0F766E',
       'MODERN',
       'Urban Fade Studio',
       'Choose your stylist station and lock your time slot instantly.',
       'URBAN-FADE',
       'Late arrivals beyond 10 minutes may be rescheduled.',
       'First booking gets a complimentary style consultation.',
       '[{"q":"Do you accept kids?","a":"Yes, kids cut service is available."}]',
       5,
       '["hero","services","team","faq"]',
       '["Hair","Family","Care"]'
WHERE NOT EXISTS (
  SELECT 1 FROM customization_configs WHERE merchant_id = @merchant2_id
);

INSERT INTO customization_configs (
  merchant_id, theme_color, theme_preset, hero_title, booking_flow_text,
  invite_code, terms_text, announcement_text, faq_json, buffer_minutes,
  homepage_sections_json, category_order_json
)
SELECT @merchant3_id,
       '#7C3AED',
       'CALM',
       'Serenity Spa Lab',
       'Select a treatment room and reserve your recovery session.',
       'SERENE-NOW',
       'Please notify us 12 hours in advance for cancellations.',
       'This month features a 15% weekday daytime spa campaign.',
       '[{"q":"What should I bring?","a":"Just arrive with comfortable clothing."}]',
       15,
       '["hero","services","announcement","faq"]',
       '["Relax","Skincare","Therapy"]'
WHERE NOT EXISTS (
  SELECT 1 FROM customization_configs WHERE merchant_id = @merchant3_id
);

-- =========================
-- Seed business hours
-- =========================
INSERT INTO business_hours (merchant_id, day_of_week, start_time, end_time)
SELECT @merchant_id, d.day_of_week, d.start_time, d.end_time
FROM (
  SELECT 'MONDAY' AS day_of_week, '10:00:00' AS start_time, '20:00:00' AS end_time
  UNION ALL SELECT 'TUESDAY', '10:00:00', '20:00:00'
  UNION ALL SELECT 'WEDNESDAY', '10:00:00', '20:00:00'
  UNION ALL SELECT 'THURSDAY', '10:00:00', '20:00:00'
  UNION ALL SELECT 'FRIDAY', '10:00:00', '21:00:00'
  UNION ALL SELECT 'SATURDAY', '11:00:00', '19:00:00'
) d
WHERE NOT EXISTS (
  SELECT 1
  FROM business_hours h
  WHERE h.merchant_id = @merchant_id
    AND h.day_of_week = d.day_of_week
    AND h.start_time = d.start_time
    AND h.end_time = d.end_time
);

INSERT INTO business_hours (merchant_id, day_of_week, start_time, end_time)
SELECT @merchant2_id, d.day_of_week, d.start_time, d.end_time
FROM (
  SELECT 'MONDAY' AS day_of_week, '09:30:00' AS start_time, '20:30:00' AS end_time
  UNION ALL SELECT 'TUESDAY', '09:30:00', '20:30:00'
  UNION ALL SELECT 'WEDNESDAY', '09:30:00', '20:30:00'
  UNION ALL SELECT 'THURSDAY', '09:30:00', '20:30:00'
  UNION ALL SELECT 'FRIDAY', '09:30:00', '21:30:00'
  UNION ALL SELECT 'SATURDAY', '10:00:00', '20:00:00'
  UNION ALL SELECT 'SUNDAY', '10:00:00', '18:00:00'
) d
WHERE NOT EXISTS (
  SELECT 1
  FROM business_hours h
  WHERE h.merchant_id = @merchant2_id
    AND h.day_of_week = d.day_of_week
    AND h.start_time = d.start_time
    AND h.end_time = d.end_time
);

INSERT INTO business_hours (merchant_id, day_of_week, start_time, end_time)
SELECT @merchant3_id, d.day_of_week, d.start_time, d.end_time
FROM (
  SELECT 'MONDAY' AS day_of_week, '11:00:00' AS start_time, '20:00:00' AS end_time
  UNION ALL SELECT 'TUESDAY', '11:00:00', '20:00:00'
  UNION ALL SELECT 'WEDNESDAY', '11:00:00', '20:00:00'
  UNION ALL SELECT 'THURSDAY', '11:00:00', '20:00:00'
  UNION ALL SELECT 'FRIDAY', '11:00:00', '21:00:00'
  UNION ALL SELECT 'SATURDAY', '11:00:00', '21:00:00'
) d
WHERE NOT EXISTS (
  SELECT 1
  FROM business_hours h
  WHERE h.merchant_id = @merchant3_id
    AND h.day_of_week = d.day_of_week
    AND h.start_time = d.start_time
    AND h.end_time = d.end_time
);

-- =========================
-- Refresh demo password hashes (idempotent)
-- =========================
UPDATE platform_users
SET password_hash = @merchant_password_hash
WHERE username IN (@merchant_username, @merchant2_username, @merchant3_username);

UPDATE platform_users
SET password_hash = @client_password_hash
WHERE username = @client_username;

COMMIT;

-- =========================
-- Verification queries
-- =========================
SELECT id, username, platform_role, merchant_id, enabled
FROM platform_users
WHERE username IN (@merchant_username, @merchant2_username, @merchant3_username, @client_username);

SELECT merchant_id, platform_user_id, membership_status
FROM merchant_memberships
WHERE platform_user_id IN (@merchant_user_id, @merchant2_user_id, @merchant3_user_id)
ORDER BY merchant_id, platform_user_id;

SELECT b.platform_user_id, r.code AS role_code, b.merchant_id, b.status
FROM platform_user_rbac_bindings b
JOIN rbac_roles r ON r.id = b.rbac_role_id
WHERE b.platform_user_id IN (@merchant_user_id, @merchant2_user_id, @merchant3_user_id, @client_user_id)
ORDER BY b.platform_user_id, role_code;

SELECT m.slug, s.name AS service_name, s.duration_minutes, s.price, s.category
FROM service_items s
JOIN merchants m ON m.id = s.merchant_id
WHERE m.slug IN (@merchant_slug, @merchant2_slug, @merchant3_slug)
ORDER BY m.slug, s.id;

SELECT m.slug, p.description, p.phone, p.email, c.theme_color, c.theme_preset
FROM merchants m
LEFT JOIN merchant_profiles p ON p.merchant_id = m.id
LEFT JOIN customization_configs c ON c.merchant_id = m.id
WHERE m.slug IN (@merchant_slug, @merchant2_slug, @merchant3_slug)
ORDER BY m.slug;

SELECT m.slug, h.day_of_week, h.start_time, h.end_time
FROM merchants m
JOIN business_hours h ON h.merchant_id = m.id
WHERE m.slug IN (@merchant_slug, @merchant2_slug, @merchant3_slug)
ORDER BY m.slug, h.day_of_week, h.start_time;
