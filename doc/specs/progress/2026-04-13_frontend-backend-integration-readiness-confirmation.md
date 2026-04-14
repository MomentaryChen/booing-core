# Frontend-Backend Integration Readiness Confirmation

## Context

- Requirement: start confirmation work before frontend/backend implementation coupling.
- Goal: align integration contracts, architecture constraints, and quality gates so implementation can run in parallel with lower rework risk.
- Execution mode: `auto-run-until-done` with `maxRounds=5`, `reviewMode=final_only`.

## Scope

- In scope:
  - Confirm FE-BE integration contract baseline (API, error mapping, auth/tenant boundaries).
  - Confirm architecture fit (resource/slot abstraction, strategy extensibility, state transition legality, multi-tenant isolation).
  - Publish owner assignments and trackable checklist before implementation.
- Out of scope:
  - Large feature coding or unrelated UI redesign.
  - Infra/deploy refactor.

## Acceptance Criteria

1. Integration contract list is defined for target FE-consumed APIs (method/path/auth/tenant scope).
2. Request/response and error behavior are aligned for 401/403/404/409/422/500.
3. UI critical states are mapped to backend responses (loading/empty/error/forbidden/success).
4. Multi-tenant and role boundary rules are explicit and testable.
5. Must-have test matrix (backend + frontend) is documented.
6. Owner assignment and checklist are published with evidence fields.

## Owner Assignments

| Workstream | Owner | Status |
|---|---|---|
| Integration contract and boundary baseline | backend-engineer-agent | done |
| FE contract consumption and fallback mapping | frontend-engineer-agent | done |
| UI states, IA, copy key checklist (zh-TW/en-US) | uiux-agent | done |
| Architecture constraints and go/no-go gate | architect-agent | done |
| Final risk gate before PM adjudication | reviewer-agent | done |
| Scope and acceptance governance | pm-agent | done |

## Tracking Checklist

| Item | Owner | Status | Evidence |
|---|---|---|---|
| Spec confirmed | pm-agent | done | This spec file created in `progress/` |
| Architect alignment completed | architect-agent | done | PM+Architect same-round planning output |
| Backend data plan (migration/index/rollback) completed | backend-engineer-agent | done | No schema work required in this confirmation-only phase; deferred to implementation rounds |
| Implementation delivered | frontend-engineer-agent / backend-engineer-agent | done | Drift guard + namespace guard + merchant teams contract lock delivered |
| Final reviewer gate passed | reviewer-agent | done | Reviewer gate pass, no critical/high findings |

## Risks

- FE/BE contract drift during parallel coding.
- Tenant boundary omission in query/write paths.
- Illegal booking status transition via direct status mutation.
- Strategy fallback to hardcoded branch in service layer.

## Round Log

### Round 0

- Initialized workflow context.
- Completed PM + Architect same-round planning.
- Created this progress spec and published assignments/checklist.

### Round 1

- Ran parallel confirmation tasks: backend-engineer-agent, frontend-engineer-agent, and uiux-agent.
- Backend baseline confirmed:
  - Contract focus endpoints for auth context, client booking, merchant operations, and system transition ops.
  - State machine single entrypoint and strategy-based validation guardrails.
  - Tenant and role boundaries mapped to current controller/service guards.
- Frontend baseline confirmed:
  - Priority integration targets: client booking loop first, merchant ops second, context switch and teams after.
  - Error fallback mapping prepared for 401/403/404/409/422/500.
  - Existing blockers identified: merchant teams contract uncertainty and `/admin` to `/system` governance gap.
- UIUX checklist confirmed:
  - Critical UI states and i18n readiness checklist for zh-TW/en-US.
  - Minimum accessibility and responsive intent checklist established.

### Round 1 Output Snapshot

```json
{
  "plan": {
    "scope": "Pre-implementation FE-BE integration readiness confirmation for booking-core.",
    "tasks": [
      "Confirm backend contract baseline and architecture boundaries",
      "Confirm frontend integration targets and error fallback mapping",
      "Confirm UIUX critical states/i18n/a11y readiness checklist"
    ],
    "dependencies": [
      "PM + Architect planning completed",
      "Current backend/frontend modules available for evidence review"
    ],
    "acceptanceCriteria": [
      "Contract baseline documented",
      "Error mapping aligned for 401/403/404/409/422/500",
      "Tenant and role boundaries explicit",
      "FE and UIUX readiness checklists documented"
    ],
    "ownerAssignments": [
      { "owner": "backend-engineer-agent", "status": "done" },
      { "owner": "frontend-engineer-agent", "status": "done" },
      { "owner": "uiux-agent", "status": "done" },
      { "owner": "reviewer-agent", "status": "in_progress" },
      { "owner": "pm-agent", "status": "done" }
    ],
    "trackingChecklist": [
      { "item": "Spec confirmed", "status": "done" },
      { "item": "Architect alignment completed", "status": "done" },
      { "item": "Backend data plan (migration/index/rollback) completed", "status": "done" },
      { "item": "Implementation delivered", "status": "todo" },
      { "item": "Final reviewer gate passed", "status": "in_progress" }
    ]
  },
  "deliverables": {
    "codeChanges": [],
    "specChanges": [
      "doc/specs/progress/2026-04-13_frontend-backend-integration-readiness-confirmation.md"
    ],
    "testEvidence": []
  },
  "review": {
    "findings": [],
    "status": "pass"
  },
  "verification": {
    "status": "pass",
    "cases": [
      "Backend readiness package delivered",
      "Frontend readiness package delivered",
      "UIUX readiness package delivered"
    ],
    "notes": [
      "Two non-blocking readiness risks remain: teams contract lock and /admin vs /system route governance"
    ]
  },
  "run": {
    "status": "in_progress",
    "round": 1,
    "maxRounds": 5,
    "reviewMode": "final_only",
    "escalationRequired": false
  },
  "issues": [],
  "escalation": {
    "unresolvedFindings": [],
    "failedVerificationCases": [],
    "decisionRequests": []
  }
}
```

### Round 2 (Final Gate + PM Arbitration)

- Executed final reviewer gate (`final_only`):
  - Findings: none (`critical/high`: none).
  - Gate status: pass.
  - Residual risks captured: contract drift risk, `/admin` to `/system` governance gap, merchant teams contract uncertainty.
- Executed mandatory final PM arbitration:
  - Adjudication: `done`.
  - Rationale: readiness confirmation acceptance criteria met for this phase.
  - Next actions prioritized for execution phase:
    - Freeze/version API contracts and add CI drift checks.
    - Resolve `/admin` to `/system` governance timeline.
    - Lock merchant teams contract and update spec checklist evidence.

### Round 3 (Implementation Start)

- Started implementation for first P0 follow-up: contract drift protection.
- Added backend automated test:
  - `backend/src/test/java/com/bookingcore/OpenApiContractDriftTest.java`
  - Compares normative contract `doc/api/booking-core-p0.openapi.yaml` against runtime `/v3/api-docs`.
  - Fails when contract operation (`path#method`) or key operation shape drifts (parameters, requestBody, success response codes/schema refs).
- Updated OpenAPI metadata on `createClientBooking` to keep runtime spec aligned with normative `201` contract.
- Verification:
  - `mvn -Dtest=OpenApiContractDriftTest test` passed.
- Reviewer gate:
  - Re-review result: no high findings, verdict `Go`.

### Round 4 (Namespace Governance Guard)

- Added namespace governance test:
  - `backend/src/test/java/com/bookingcore/ApiNamespaceGovernanceTest.java`
  - Enforces repository guard that source code must not introduce `/api/admin` (backend + frontend source scan).
  - Keeps `/api/system` as the only system-admin API namespace.
- Verification:
  - `mvn "-Dtest=ApiNamespaceGovernanceTest,OpenApiContractDriftTest" test` passed.
- Reviewer gate:
  - No high findings, verdict `Go`.

### Round 5 (Merchant Teams Contract Lock + Final Arbitration)

- Locked merchant teams contract at API and FE boundaries:
  - Updated normative OpenAPI contract:
    - `doc/api/booking-core-p0.openapi.yaml`
    - Added teams schemas and endpoints:
      - `/api/merchant/{merchantId}/teams`
      - `/api/merchant/{merchantId}/teams/{teamId}`
      - `/api/merchant/{merchantId}/teams/{teamId}/members`
      - `/api/merchant/{merchantId}/teams/{teamId}/members/{memberId}`
  - Updated frontend team adapter and types:
    - `frontend/src/shared/lib/merchantTeamApi.ts` (backend DTO mapping + stable FE response shape)
    - `frontend/src/shared/types/merchantTeam.ts` (contract-aligned types)
- Verification:
  - Backend: `mvn "-Dtest=ApiNamespaceGovernanceTest,OpenApiContractDriftTest" test` passed.
  - Frontend: `pnpm build` passed.
- Final reviewer gate:
  - Pass, no high/critical findings.
- Final PM arbitration:
  - Adjudication: `done`.
