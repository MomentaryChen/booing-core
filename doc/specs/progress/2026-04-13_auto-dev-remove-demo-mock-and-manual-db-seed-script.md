# Remove Demo/Mock and Manual DB Seed Script

## Context

- Requirement: remove demo/mock data dependency and provide a manual database script for seeding required baseline data.
- Workflow mode: `auto-run-until-done`, `maxRounds=5`, `reviewMode=final_only`.

## Scope

- In scope:
  - Remove runtime fallback/bootstrap behavior that depends on demo/mock credentials.
  - Provide manual SQL script to seed minimum baseline data directly in DB.
  - Update docs for manual execution and verification.
- Out of scope:
  - Rewriting feature flows.
  - Broad UI redesign.

## Owner Assignments

| Workstream | Owner | Status |
|---|---|---|
| Architecture alignment | architect-agent | done |
| Seed script + backend de-mock | backend-engineer-agent | done |
| Frontend no-mock dependency adjustments | frontend-engineer-agent | done |
| Final risk gate | reviewer-agent | done |
| PM acceptance and arbitration | pm-agent | done |

## Tracking Checklist

| Item | Owner | Status | Evidence |
|---|---|---|---|
| Spec confirmed | pm-agent | done | This file in `doc/specs/progress/` |
| Architect alignment completed | architect-agent | done | Same-round planning output |
| Backend data plan (migration/index/rollback) completed | backend-engineer-agent | done | `backend/src/main/resources/db/manual/seed_manual_baseline.sql` |
| Implementation delivered | frontend-engineer-agent / backend-engineer-agent | done | Backend de-mock + manual SQL + docs updated |
| Final reviewer gate passed | reviewer-agent | done | Reviewer gate pass, no high/critical findings |

## Acceptance Criteria

1. No runtime login fallback to demo/mock credentials.
2. No default bootstrap demo merchant/user/client enabled by default.
3. Manual SQL seed script exists and is idempotent for minimum baseline data.
4. README and README.zh-TW include script execution and verification instructions.
5. Build/tests pass for touched paths and reviewer reports no unresolved high/critical issues.

## Round Log

### Round 0

- PM + Architect same-round planning completed.
- Scope fixed to runtime demo/mock removal and manual DB seed path.

### Round 1

- Removed runtime dev login fallback from `AuthService`.
- Disabled default demo bootstrap toggles in `application-dev.yml`.
- Added idempotent manual SQL seed script:
  - `backend/src/main/resources/db/manual/seed_manual_baseline.sql`
- Updated documentation:
  - `README.md`
  - `README.zh-TW.md`
- Verification:
  - `mvn "-Dtest=DevUsersFallbackProfileGuardTest,AuthMeApiTest,ApiNamespaceGovernanceTest,OpenApiContractDriftTest" test` passed.

### Round 2 (Final Gate + PM Arbitration)

- Final reviewer gate: `pass` (no high/critical).
- PM final adjudication: `done`.
