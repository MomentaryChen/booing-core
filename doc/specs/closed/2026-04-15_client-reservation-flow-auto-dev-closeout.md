# Client reservation flow — auto-dev closeout (2026-04-15)

## Implementation summary

- **Backend:** Already exposes `POST/GET /api/client/bookings`, cancel and reschedule; overlap prevention and pagination covered by existing `ClientBookingService` and tests (`ClientBookingCreateApiTest`, `ClientBookingListApiTest`, etc.).
- **Frontend:** Client app already includes `BookingPage` (create from resource + slots), `MyBookingsPage` (tabs, pagination, cancel, reschedule). **Change in this round:** `createClientBooking` request body aligned to backend DTO (only `resourceId`, `startAt`, optional `notes`). UI fields for `mode` / `guest` remain as placeholders until API supports them.

## Test / validation evidence

- `npm run build` (frontend): PASS  
- `mvn test` (backend): PASS  
- Continuation: `npm run build` + `mvn test` after 409 i18n: PASS  

## Continuation (same day)

- **409 UX:** `BookingPage` maps HTTP 409 to i18n `booking.conflict` (en-US / zh-TW).
- **Agent evidence (strict steps):** Separate `generalPurpose` invocations used as PM, Architect, and QA role outputs for `/auto-dev` traceability.

## Continuation (error envelope)

- **`clientBookingApi`:** Failed responses parse `ApiEnvelope` and attach `data.errorCode` to `ApiError` (e.g. `BOOKING_SLOT_CONFLICT`).
- **`BookingPage`:** Treats `BOOKING_SLOT_CONFLICT` (or legacy `409` without code) as slot conflict copy + bumps `availabilityRefreshKey` to re-fetch slots.
- **Validation:** `npm run build` + `mvn test`: PASS.

## Unresolved follow-ups

- **`/auto-dev` template vs repo:** Command expects MyBatis XML; codebase uses Spring Data JPA — treat JPA as source of truth unless product mandates MyBatis.
- **Guest / booking mode:** Not sent to API; extend `ClientBookingCreateRequest` + service if product requires.
- **MySQL Flyway + JPA validate:** `FlywayMySqlMigrationTest` remains disabled until CHAR(36)/UUID schema migration aligns with Flyway baseline.

## PM sign-off

Delivered scope: client reservation **create / list+pagination / cancel** via existing APIs and UI, with **create payload contract** fixed. **APPROVED** for this increment.
