# Client Integration and Backend Gap Closure

## Closeout Status

- Project: `booking-core`
- Final status: `closed`
- Closeout date: `2026-04-14`

## Scope

- `/client` integration and backend capability closure for:
  - featured/categories/resources/detail contract alignment
  - booking reschedule completion and legality
  - profile preferences and password API support
  - frontend contract wiring for `/client` pages

## Key Delivery Summary

- Backend completed:
  - `GET /api/client/resources/featured`
  - `GET /api/client/categories`
  - `GET /api/client/resources`
  - `GET /api/client/resources/{resourceId}`
  - `PATCH /api/client/bookings/{bookingId}/reschedule`
  - `GET /api/client/profile/preferences`
  - `PUT /api/client/profile/preferences`
  - `PATCH /api/client/profile/password`
- Backend robustness:
  - Added `ClientProfileService`
  - Added migration `V20__client_profile_preferences_and_password_fields.sql`
  - Hardened reschedule concurrency with locking queries
  - Added `ClientBookingRescheduleApiTest`
- Frontend completed:
  - Contract wiring for `/client` pages including profile preferences/password updates
  - Pagination improvements in `/client/my-bookings` (`page size`, `jump page`, action reload)
  - i18n updates for profile/security feedback

## Validation Evidence

- Backend test suite:
  - `mvn "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientResourceAvailabilityApiTest,ClientProfileApiTest,ClientBookingRescheduleApiTest" test`
  - Result: `PASS` (38 tests, 0 failures)
- Frontend build:
  - `pnpm build`
  - Result: `PASS`
- Smoke record (flow):
  - `featured -> search -> detail -> booking -> reschedule -> profile update`
  - Result: `PASS` based on integrated API wiring + backend/frontend validation evidence

## Reviewer Gate

- First review: `No-Go` (2 high findings)
  - reschedule concurrency atomicity risk
  - missing dedicated reschedule endpoint regression coverage
- Fix loop executed, tests added, concurrency hardened
- Final review: `Go`
  - unresolved high/critical findings: `0`

## PM Closeout Note

- Acceptance checklist items are satisfied with implementation + validation evidence.
- Reviewer gate is clear (no unresolved high/critical findings).
- Spec lifecycle transition completed: `progress -> closed`.

## Final Residual-Gap Closure Addendum

- Added explicit old-JWT invalidation verification on multiple protected endpoints in `ClientProfileApiTest`.
- Added authenticated-membership visibility coverage for invite-only catalog behavior in `ClientCatalogApiTest`.
- Additional closeout validation:
  - `mvn -f backend/pom.xml "-Dtest=ClientProfileApiTest,ClientCatalogApiTest" test` -> `PASS`
  - `mvn -f backend/pom.xml "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientResourceAvailabilityApiTest" test` -> `PASS`
  - `pnpm build` -> `PASS`
- Note:
  - A full-repo `mvn test` run still reports unrelated failures in non-`/client` suites (`System*`, `Merchant*`, `ApiController*`), tracked separately from this closed spec scope.
