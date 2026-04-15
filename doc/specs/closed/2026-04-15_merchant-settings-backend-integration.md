# Merchant Settings — backend integration (closed)



## Summary



- Extended `GET/PUT /api/merchant/{id}/customization` with persisted notification preference booleans (`V22`).

- `PUT /api/merchant/{id}/business-hours` accepts an empty list to clear hours; validates intervals **before** delete; method is `@Transactional`.

- Merchant Settings UI: `?panel=` deep links, notification toggles saved via customization API, weekly hours always synced on Save, payment connect buttons disabled with honest copy.



## Validation



- `mvn -DskipTests compile` (backend) — PASS.

- `npm run build` (frontend) — PASS.

- `mvn test` (backend full suite) — PASS with deterministic `test` profile: `internal-system-admin` auto-provision + `TestIntegrationUserSeed` for portal demo users; `MerchantPortalSettingsApiTest` covers customization notification persistence, business-hours clear/invalid interval, and cross-tenant customization 403.



## PM closeout (2026-04-15)



- **Status**: Closed after reviewer review of test-harness and settings API coverage; all checklist-backed automation green locally.

- **Delivery notes**: OpenAPI P0 contract updated for client booking list/create responses to match runtime envelope schema refs; integration tests assert stable API error codes via `$.data.errorCode`; merchant-scoped tests use email-shaped usernames to satisfy login identifier policy.

- **Residual / follow-ups** (unchanged product scope): wire notification flags into outbound delivery channels; tax/delivery/webhook persistence when defined.



## Follow-ups



- Wire notification flags into actual outbound notification pipeline (email/SMS/push).

- Tax, delivery, webhook persistence when product scope is defined.

