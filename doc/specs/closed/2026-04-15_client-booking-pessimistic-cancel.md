# Client booking — pessimistic cancel + service lock ordering (closed)

## Summary

- **`cancelMyBooking`** now loads the row with `findByIdAndPlatformUserIdForUpdate` (same as **`rescheduleMyBooking`**), so cancel and reschedule on the **same booking** serialize and avoid stale read / lost update races.
- **`rescheduleMyBooking`** lock order is aligned with create flow: resolve scope -> lock scoped `service_items` (`PESSIMISTIC_WRITE`) -> lock booking row (`FOR UPDATE`) -> overlap scan.
- **`scopedServiceItemIds`** are sorted before `findByMerchantIdAndIdIn` in **create** and **reschedule**, so DB row locks on `service_items` follow a deterministic order and partial-overlap deadlocks between concurrent creates are less likely.
- Comments clarify that **`ServiceItemRepository.findByMerchantIdAndIdIn`** already uses **`@Lock(PESSIMISTIC_WRITE)`** for shared-scope serialization with **`findOverlappingBookingsForUpdate`**.

## Out of scope (explicit)

- MyBatis XML (template conflict) — N/A.

## successChecklist

1. Cancel + reschedule on same booking use **booking row `FOR UPDATE`**.  
2. Reschedule acquires locks in deterministic order: **service scope before booking row**.  
3. No API or DTO change; happy-path cancel/reschedule unchanged for callers.  
4. Double-cancel still **409** with stable error envelope.  
5. **`mvn test`** full suite PASS.  
6. **`npm run build`** PASS (no FE code change).

## Validation

- `mvn test` (full backend) — PASS.  
- `npm run build` (frontend) — PASS.

## PM closeout

- **Decision**: CLOSED — addresses prior reviewer follow-up on **cancel vs reschedule** interleaving and aligns **create/reschedule** lock order at service-scope level.
- **Residual risks**: SQL `IN (...)` does not strictly guarantee row-lock acquisition order on every engine/plan; DB isolation semantics for overlap re-evaluation after cancel; optional cancel audit log.
