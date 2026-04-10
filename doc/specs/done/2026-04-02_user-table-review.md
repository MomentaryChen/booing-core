## User table review (backend schema)

- **Date**: 2026-04-02
- **Owner**: PM (booking-core)
- **Status**: Done (archived in `doc/specs/done/`)

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.

### Conclusion

The current backend schema **does not include a `user` / `users` table** that stores all users.

### Evidence

- **Flyway migrations**
  - `backend/src/main/resources/db/migration/V1__init.sql` defines these tables:
    - `merchants`
    - `merchant_profiles`
    - `customization_configs`
    - `dynamic_field_configs`
    - `resource_items`
    - `service_items`
    - `business_hours`
    - `availability_exceptions`
    - `bookings`
    - `platform_pages`
    - `role_page_grants`
    - `system_settings`
    - `domain_templates`
    - `audit_logs`
  - No `user`, `users`, `account`, or similar identity table is created in the baseline migration.

- **JPA entities**
  - `@Entity` classes present (no `User` entity among them):
    - `com.bookingcore.modules.merchant.Merchant` → `merchants`
    - `com.bookingcore.modules.merchant.MerchantProfile` → `merchant_profiles`
    - `com.bookingcore.modules.customization.CustomizationConfig` → `customization_configs`
    - `com.bookingcore.modules.merchant.DynamicFieldConfig` → `dynamic_field_configs`
    - `com.bookingcore.modules.merchant.ResourceItem` → `resource_items`
    - `com.bookingcore.modules.service.ServiceItem` → `service_items`
    - `com.bookingcore.modules.booking.BusinessHours` → `business_hours`
    - `com.bookingcore.modules.booking.AvailabilityException` → `availability_exceptions`
    - `com.bookingcore.modules.booking.Booking` → `bookings`
    - `com.bookingcore.modules.platform.PlatformPage` → `platform_pages`
    - `com.bookingcore.modules.platform.RolePageGrant` → `role_page_grants`
    - `com.bookingcore.modules.admin.SystemSettings` → `system_settings`
    - `com.bookingcore.modules.admin.DomainTemplate` → `domain_templates`
    - `com.bookingcore.modules.admin.AuditLog` → `audit_logs`

### Where “users/auth” are handled instead (current implementation)

- **Login endpoint**
  - `backend/src/main/java/com/bookingcore/api/AuthController.java` exposes `POST /api/auth/login`.

- **Credential source**
  - `backend/src/main/java/com/bookingcore/config/BookingPlatformProperties.java` defines `booking.platform.devUsers` (dev-only credentials) and `booking.platform.jwt.secret` (enables JWT).
  - `backend/src/main/java/com/bookingcore/service/AuthService.java` validates credentials by iterating configured `DevUser` entries (not DB-backed).

- **Authenticated identity shape**
  - `backend/src/main/java/com/bookingcore/security/PlatformPrincipal.java` holds `(username, role, merchantId)`; no persisted user model is referenced.

### Notes / implication

If the platform later needs a persisted, system-wide identity store (e.g., merchants/sub-merchants/system admins in DB), a new migration and a new persisted identity model would be required; today’s backend relies on configuration-driven dev users + JWT for protected API access.

