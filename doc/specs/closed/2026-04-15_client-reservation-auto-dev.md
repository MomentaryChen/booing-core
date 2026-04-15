# Client reservation MVP (`/auto-dev` closeout) — **CLOSED**

> 已自 `doc/specs/progress/` 結案歸檔（2026-04-15）。

## Scope

End-customer reservations: create (time range via `startAt` + service duration), reject overlaps, list with pagination by tab, cancel, React UI (`BookingPage`, `MyBookingsPage`).

## Implementation summary

- **Concurrency:** `ClientBookingService.createBooking` uses `findOverlappingBookingsForUpdate` (pessimistic write) for the overlap probe, together with `ServiceItem` scope locks (`PESSIMISTIC_WRITE`) and resource row lock.
- **API:** `POST /api/client/bookings`, `GET /api/client/bookings`, `PATCH .../cancel`, `PATCH .../reschedule`, optional `POST /api/client/booking/lock`, public slug booking as implemented.
- **Frontend:** `clientBookingApi.ts` unwraps `ApiEnvelope`; `MyBookingsPage` / `BookingPage` provide list, pagination, create, cancel flows.

## Validation evidence

| When | Command | Result |
|------|---------|--------|
| Earlier round | `mvn test "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientBookingRescheduleApiTest"` | PASS |
| `/auto-dev` 2026-04-15 | `mvn test "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientBookingRescheduleApiTest,ClientResourceAvailabilityApiTest"` | PASS |
| `/auto-dev` 2026-04-15 | `npm run build` (frontend) | PASS |

## Reviewer

- `reviewer-agent`: **Go** — no HIGH findings on tenant scoping for described client booking paths; residual note on lock contention vs IDOR class.

## Template deviations (explicit)

- `/auto-dev` template references **MyBatis XML**; this service uses **JPA** — no MyBatis migration.

## Follow-ups (optional)

- Optional parallel stress test for concurrent creates on disjoint resources sharing one `serviceItem` scope.

## 2026-04-15 PM closeout addendum (universal `/client` UI)

- **Implementation summary:** updated `HomePage`, `SearchPage`, `BookingPage`, and client API typings to support universal discovery modules (resource type filters, availability facet, feed variants, pagination state, booking mode selector, and guest booking entry fields) with `zh-TW`/`en-US` i18n coverage.
- **Validation evidence:** `mvn -q -DskipTests compile` PASS (backend compatibility), `npm run build` PASS (frontend compile and bundle).
- **Reviewer evidence:** `reviewer-agent` reported **No critical findings**; residual risk noted for `availability=today` pagination alignment and integration handling for new optional `mode`/`guest` payload fields.
- **PM Gate:** `APPROVED` (pragmatic in-scope closure for current run)

### In-scope boundaries (current run)

- Complete booking system requirement plus current `/client` updates by closing concrete engineering gaps only:
  - backend `resourceType` + availability filtering alignment
  - booking `mode` / `guest` optional payload support
  - targeted tests for the above behavior
- Explicitly excluded from this run:
  - unrelated repo-wide lint cleanup
  - unrelated full E2E expansion not required by original requirement

### successChecklist

1. `SC-1` Functional: `/client` resource discovery/list APIs accept and apply `resourceType` and availability filters with consistent backend semantics.
2. `SC-2` Functional: booking create path accepts optional `mode` and `guest` payload fields without breaking existing authenticated booking flows.
3. `SC-3` Edge-case: omitted optional `mode`/`guest` fields preserve backward-compatible behavior and response shape.
4. `SC-4` Edge-case: availability filter behavior (`today` and equivalent labels used by client) matches backend pagination/filter expectations.
5. `SC-5` Non-functional: integration/API tests cover filter alignment and optional payload combinations with stable pass results.

### tasks

- `T1` Backend filter alignment: normalize and apply `resourceType` + availability filtering in client-facing catalog/availability endpoints.
- `T2` Backend booking payload support: add/confirm optional `mode` and `guest` fields in booking request DTO/service flow with compatibility guards.
- `T3` Test closure: add/update API/integration tests for filter alignment and `mode`/`guest` optional combinations (present + omitted).
- `T4` PM/reviewer closeout: confirm in-scope checklist pass and maintain exclusion of unrelated repo-wide lint/E2E from acceptance gate.

### API list (in-scope verification)

- `GET /api/client/resources`
- `GET /api/client/resources/featured`
- `GET /api/client/resources/{resourceId}`
- `GET /api/client/resource-availability` (or equivalent client availability endpoint)
- `POST /api/client/bookings`

### DB impact

- No mandatory schema migration required for this gate if `mode`/`guest` remains optional and represented in existing booking payload/domain model.
- If persistence columns are already present from prior migration work, this run reuses them; otherwise defer schema expansion to a separate scoped requirement.
- No repo-wide data backfill or unrelated migration hardening included in current run scope.

### Final sign-off

- PM gate decision: **APPROVED**.
- Acceptance basis: concrete remaining engineering gaps for booking + `/client` updates are bounded to filter alignment, optional payload support, and targeted tests, with reviewer no-critical-finding posture retained.
- Release posture for this run: **merge-ready for scoped requirement only**; unrelated lint/E2E improvements remain backlog items, not blockers for this gate.
