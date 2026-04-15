# Merchant Integration Gap Closure

## PM-Agent Structured Output

```json
{
  "projectName": "booking-core",
  "spec": {
    "path": "doc/specs/closed/2026-04-14_merchant-integration-gap-closure.md",
    "status": "open"
  },
  "successChecklist": [
    {
      "id": "AC-01",
      "item": "Merchant can create/update teams and add/remove members from /merchant/teams with role restrictions enforced.",
      "type": "functional",
      "ownerTaskId": "T05"
    },
    {
      "id": "AC-02",
      "item": "Merchant can assign/reassign/release booking staff from /merchant/bookings with clear success/error states.",
      "type": "functional",
      "ownerTaskId": "T06"
    },
    {
      "id": "AC-03",
      "item": "Resource staff candidate flow uses backend candidate endpoint and handles empty/conflict/error states.",
      "type": "functional",
      "ownerTaskId": "T07"
    },
    {
      "id": "AC-04",
      "item": "Payment settings tab persists provider connection status and reload consistency is verified.",
      "type": "functional",
      "ownerTaskId": "T08"
    },
    {
      "id": "AC-05",
      "item": "Notification settings tab persists toggle preferences and reload consistency is verified.",
      "type": "functional",
      "ownerTaskId": "T08"
    },
    {
      "id": "AC-06",
      "item": "Dashboard metrics are loaded from backend aggregate API instead of placeholders.",
      "type": "functional",
      "ownerTaskId": "T09"
    },
    {
      "id": "AC-07",
      "item": "Tenant isolation is enforced for all new merchant APIs, including cross-merchant denial cases.",
      "type": "edge-case",
      "ownerTaskId": "T10"
    },
    {
      "id": "AC-08",
      "item": "401/403/404/409/422 and generic failures are mapped to expected frontend states.",
      "type": "non-functional",
      "ownerTaskId": "T11"
    },
    {
      "id": "AC-09",
      "item": "Final review has no unresolved high/critical findings.",
      "type": "non-functional",
      "ownerTaskId": "T12"
    }
  ],
  "tasks": [
    {
      "id": "T01",
      "name": "Finalize Missing Merchant API Contracts",
      "description": "Define contract for payment settings, notification preferences, and dashboard aggregates with error mapping and tenant boundaries.",
      "assignedTo": "pm-agent",
      "dependsOn": [],
      "input": {
        "requiresArchitectFeedback": true,
        "targetAreas": [
          "/merchant/settings/payment",
          "/merchant/settings/notifications",
          "/merchant/dashboard"
        ]
      }
    },
    {
      "id": "T02",
      "name": "Implement Payment Settings APIs",
      "description": "Implement backend domain/service/repository/controller for merchant payment provider settings with tenant-safe authorization.",
      "assignedTo": "backend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "endpoints": [
          "GET /api/merchant/{merchantId}/payment-settings",
          "PUT /api/merchant/{merchantId}/payment-settings"
        ]
      }
    },
    {
      "id": "T03",
      "name": "Implement Notification Settings APIs",
      "description": "Implement backend domain/service/repository/controller for merchant notification preferences with tenant-safe authorization.",
      "assignedTo": "backend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "endpoints": [
          "GET /api/merchant/{merchantId}/notification-settings",
          "PUT /api/merchant/{merchantId}/notification-settings"
        ]
      }
    },
    {
      "id": "T04",
      "name": "Implement Dashboard Aggregate API",
      "description": "Implement backend aggregate endpoint for merchant dashboard metrics used by dashboard cards.",
      "assignedTo": "backend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "endpoints": [
          "GET /api/merchant/{merchantId}/dashboard-summary"
        ]
      }
    },
    {
      "id": "T05",
      "name": "Complete Teams Page Integration",
      "description": "Integrate create/update/add-member/remove-member actions with loading/empty/error states and i18n copy.",
      "assignedTo": "frontend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "page": "frontend/src/apps/merchant/pages/TeamsPage.tsx"
      }
    },
    {
      "id": "T06",
      "name": "Complete Booking Assignment Integration",
      "description": "Integrate assign/reassign/release booking operations and related UI states in bookings workflow.",
      "assignedTo": "frontend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "page": "frontend/src/apps/merchant/pages/BookingsPage.tsx"
      }
    },
    {
      "id": "T07",
      "name": "Complete Resource Staff Candidate Integration",
      "description": "Integrate resource staff candidate API usage into resource assignment interactions.",
      "assignedTo": "frontend-engineer-agent",
      "dependsOn": ["T01"],
      "input": {
        "page": "frontend/src/apps/merchant/pages/ResourcesPage.tsx"
      }
    },
    {
      "id": "T08",
      "name": "Integrate Settings Payment and Notifications Tabs",
      "description": "Wire settings payment and notification tabs to backend APIs with save/reload/error handling.",
      "assignedTo": "frontend-engineer-agent",
      "dependsOn": ["T02", "T03"],
      "input": {
        "page": "frontend/src/apps/merchant/pages/SettingsPage.tsx"
      }
    },
    {
      "id": "T09",
      "name": "Replace Dashboard Placeholders",
      "description": "Replace static dashboard placeholders with backend summary metrics and proper state mapping.",
      "assignedTo": "frontend-engineer-agent",
      "dependsOn": ["T04"],
      "input": {
        "page": "frontend/src/apps/merchant/pages/DashboardPage.tsx"
      }
    },
    {
      "id": "T10",
      "name": "Add Backend API Coverage",
      "description": "Add tests for authz, tenant isolation, validation, and edge-case behavior for new merchant APIs.",
      "assignedTo": "backend-engineer-agent",
      "dependsOn": ["T02", "T03", "T04"],
      "input": {
        "testFocus": ["tenant isolation", "forbidden access", "invalid payload"]
      }
    },
    {
      "id": "T11",
      "name": "Add Frontend Integration Coverage",
      "description": "Add frontend integration/e2e coverage for teams, booking assignment, settings persistence, and dashboard loading states.",
      "assignedTo": "qa-agent",
      "dependsOn": ["T05", "T06", "T07", "T08", "T09"],
      "input": {
        "testFocus": ["state mapping", "error handling", "critical merchant path"]
      }
    },
    {
      "id": "T12",
      "name": "Run Final Review Gate",
      "description": "Perform reviewer gate with findings-first report and block merge on unresolved high/critical issues.",
      "assignedTo": "reviewer-agent",
      "dependsOn": ["T10", "T11"],
      "input": {
        "mustBlockOnHighOrCritical": true
      }
    }
  ],
  "taskAssignees": [
    { "taskId": "T01", "assignedTo": "pm-agent" },
    { "taskId": "T02", "assignedTo": "backend-engineer-agent" },
    { "taskId": "T03", "assignedTo": "backend-engineer-agent" },
    { "taskId": "T04", "assignedTo": "backend-engineer-agent" },
    { "taskId": "T05", "assignedTo": "frontend-engineer-agent" },
    { "taskId": "T06", "assignedTo": "frontend-engineer-agent" },
    { "taskId": "T07", "assignedTo": "frontend-engineer-agent" },
    { "taskId": "T08", "assignedTo": "frontend-engineer-agent" },
    { "taskId": "T09", "assignedTo": "frontend-engineer-agent" },
    { "taskId": "T10", "assignedTo": "backend-engineer-agent" },
    { "taskId": "T11", "assignedTo": "qa-agent" },
    { "taskId": "T12", "assignedTo": "reviewer-agent" }
  ],
  "checklistTraceability": [
    { "checklistId": "AC-01", "taskId": "T05", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-02", "taskId": "T06", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-03", "taskId": "T07", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-04", "taskId": "T08", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-05", "taskId": "T08", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-06", "taskId": "T09", "assignedTo": "frontend-engineer-agent" },
    { "checklistId": "AC-07", "taskId": "T10", "assignedTo": "backend-engineer-agent" },
    { "checklistId": "AC-08", "taskId": "T11", "assignedTo": "qa-agent" },
    { "checklistId": "AC-09", "taskId": "T12", "assignedTo": "reviewer-agent" }
  ]
}
```

## Parallelization Plan (After Architect Phase)

```json
{
  "sequentialTasks": [
    "T01",
    "T12"
  ],
  "parallelGroups": [
    {
      "groupName": "Core implementation after contract lock",
      "tasks": ["T02", "T03", "T04", "T05", "T06", "T07"]
    },
    {
      "groupName": "UI integration dependent tasks",
      "tasks": ["T08", "T09"]
    },
    {
      "groupName": "Validation",
      "tasks": ["T10", "T11"]
    }
  ]
}
```

## Context Evidence: Current /merchant Gaps

- Not fully integrated:
  - Teams page only lists teams; create/member actions not wired.
  - Booking assignment flows (assign/reassign/release) not wired in booking UI.
  - Resource staff candidate endpoint not wired in resource assignment flow.
  - Settings payment/notifications/team tabs are UI-only.
  - Dashboard still shows placeholder metrics.
- Backend not yet implemented:
  - Payment settings APIs.
  - Notification preference APIs.
  - Dashboard aggregate summary API.

## Status

- Current status: `open`
- Move to `progress` when implementation starts.

## PM Revision After CHANGES_REQUIRED (Test-Only Freeze)

Scope freeze: no production API or behavior changes. This revision only closes two remaining verification gaps with backend tests.

### Gap A: Catalog auth/tenant behavior tests

- Objective: prove catalog endpoints enforce expected auth and tenant isolation behavior.
- Target test file: `backend/src/test/java/com/bookingcore/ClientCatalogApiTest.java`.
- Required scenarios:
  - Unauthenticated request returns 401 for protected catalog endpoint(s).
  - Authenticated client from tenant A cannot access tenant B catalog data.
  - Authenticated client within tenant can access own tenant catalog data.
  - Invalid/missing tenant context is rejected and does not leak cross-tenant data.
- Out-of-scope: modifying endpoint contracts, controller signatures, or response schemas.

### Gap B: Second endpoint old-JWT invalidation test

- Objective: prove old JWT becomes invalid after credential/version-changing operation on a second protected endpoint (not only one endpoint).
- Target test file(s): extend existing auth/profile API tests (prefer `backend/src/test/java/com/bookingcore/AuthMeApiTest.java` or related profile test if better aligned).
- Required scenarios:
  - Acquire JWT v1, call endpoint E1 successfully.
  - Trigger token-version-changing action (e.g., password/credential update).
  - Verify old JWT v1 fails on endpoint E1 and endpoint E2 (second endpoint) with 401/expected auth failure.
  - Verify refreshed JWT v2 succeeds on endpoint E2.
- Out-of-scope: changing token format, auth filter implementation, or endpoint behavior.

### Revised Minimal Checklist

- [ ] C1: `ClientCatalogApiTest` includes auth + cross-tenant denial + same-tenant allow coverage.
- [ ] C2: Catalog tests assert both status code and tenant-safe payload boundaries (no foreign tenant records).
- [ ] C3: Old-JWT invalidation is asserted on two distinct protected endpoints.
- [ ] C4: Post-rotation new JWT succeeds on the second endpoint.
- [ ] C5: Full targeted test run passes with global `mvn` command and no production code changes in diff.
- [ ] C6: Reviewer gate runs after test completion with no unresolved high/critical findings.

### Execution Order (Fast Closure)

1. Add/adjust catalog auth/tenant test cases.
2. Add second-endpoint old-JWT invalidation test case.
3. Run targeted test suite.
4. Run reviewer gate and resolve findings if any.
5. Mark this spec revision as complete for CHANGES_REQUIRED.

### PM Sign-off Criteria

PM sign-off is granted only when all C1-C6 are checked and evidence is attached (test names + pass output + reviewer result). If any item is missing, status remains `changes_required`.

## PM Closeout (2026-04-15)

- closeoutDecision: `CLOSED`
- implementationSummary:
  - Added catalog auth/tenant coverage in `backend/src/test/java/com/bookingcore/ClientCatalogApiTest.java`.
  - Reused existing dual-endpoint JWT invalidation evidence in `backend/src/test/java/com/bookingcore/ClientProfileApiTest.java` (`patchClientProfilePassword_invalidatesOldJwtAndAcceptsNewJwt`).
  - No frontend or production API/schema change applied in this freeze round scope.
- testValidationEvidence:
  - Command: `mvn -q test "-Dtest=ClientCatalogApiTest,ClientProfileApiTest,CredentialRevocationApiTest,AuthMeApiTest"` (PASS).
  - C1-C5: PASS by targeted test evidence and assertions mapping.
  - C6: reviewer gate PASS with no unresolved high/critical findings (scoped freeze-delta review).
- unresolvedFollowUps:
  - Repository has broad pre-existing unrelated changes; keep freeze-round review scoped to round delta artifacts when preparing commit/PR.
- lifecycleUpdate:
  - Moved this spec from `doc/specs/open/` to `doc/specs/closed/` after reviewer pass and full checklist pass.
