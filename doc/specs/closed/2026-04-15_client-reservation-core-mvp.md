# Client reservation core MVP (closed)

## Summary

Aligns with `/auto-dev` requirement: authenticated clients can **create** a booking for a time window, **avoid double-booking** for the same conflict scope (overlap query + transactional create), **list** bookings with **pagination** (`tab`, `page`, `size`), and **cancel** eligible bookings. React client app provides **BookingPage** (create flow) and **MyBookingsPage** (tabs, pagination, cancel, loading/error).

**Out of scope (template conflict):** MyBatis XML — this codebase uses **Spring Data JPA** (`BookingRepository`, specifications, pessimistic overlap query).

## Implementation map

| Area | Location |
|------|-----------|
| Create + overlap | `ClientBookingService#createBooking`, `BookingRepository#findOverlappingBookingsForUpdate` |
| List + pagination | `ClientBookingService#listMyBookings`, `ClientBookingSpecifications` |
| Cancel | `ClientBookingService#cancelMyBooking` |
| REST | `ClientBookingController` — `POST /bookings`, `GET /bookings`, `PATCH /bookings/{id}/cancel`, reschedule |
| UI | `frontend/src/apps/client/pages/BookingPage.tsx`, `MyBookingsPage.tsx`, `clientBookingApi.ts` |

## successChecklist (verified)

1. Create booking returns **201** with envelope `code=0` (existing + list tests).  
2. Overlapping slot returns **409** and no extra row (`ClientBookingCreateApiTest#createClientBooking_conflict_returns409AndNoSideEffect`).  
3. Pagination: `page` negative → **0**; `size` > 100 → **100** (`ClientBookingListApiTest` new cases).  
4. Unknown `tab` → same filter semantics as **upcoming** (`ClientBookingSpecifications` default branch).  
5. Second cancel → **409**, `data.errorCode=STATE_CONFLICT` (`ClientBookingListApiTest#cancelTwice_secondRequest_returnsConflict`).  
6. Unauthenticated list **401**; merchant list **403** (existing tests).  
7. Frontend **`npm run build`** PASS.

## Validation

- `mvn test` — full backend suite (run after this spec’s test additions).  
- `mvn test -Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientBookingRescheduleApiTest` — PASS.  
- `npm run build` (frontend) — PASS.

## PM closeout

- **Status**: Closed — scope satisfied by **existing** product surface plus **gap-filling API tests** on list/cancel edges.  
- **Follow-ups (reviewer / backlog)**  
  - Harden **shared service scope** concurrency if two resources may not share capacity (product decision + possible pessimistic lock on `ServiceItem` or advisory key).  
  - `cancelMyBooking` vs reschedule **race** — consider `FOR UPDATE` / optimistic version on `Booking`.  
  - Optional **multi-threaded** integration test (MockMvc is not thread-safe; prefer `@SpringBootTest(web=RANDOM_PORT)` + parallel HTTP if needed).
