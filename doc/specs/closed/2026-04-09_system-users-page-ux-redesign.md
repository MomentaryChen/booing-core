# System Users Page UX Redesign (System Admin)

Date: 2026-04-09  
Owner: PM / system-admin domain  
Status: Done (已結案)
Surface: `/system/users` (`frontend/src/pages/system/SystemUsersPage.jsx`)  
Domain: System-admin  
Iteration target: 1 sprint / 1 implementation round
Closed on: 2026-04-09

Done definition:
- Redesigned `/system/users` UX is merged with full feature parity and no API contract changes.
- All acceptance criteria are verified by QA with traceable evidence.
- reviewer-agent confirms no unresolved high/critical findings for release readiness.

## Findings

### Problem statement
- The current `/system/users` experience supports key admin capabilities but likely has high cognitive load for multi-step account governance tasks (status management, role binding, permission verification).
- In a multi-role platform, system admins need to make safe access changes quickly with clear impact visibility; UX should reduce misconfiguration risk without changing backend contract.

### Product goals
- Improve decision speed and confidence for system-admin user governance.
- Make status and RBAC changes auditable in the UI flow (who/what impact) through clearer state presentation and confirmation UX.
- Preserve all current capabilities and API contract while reducing task friction.

### Primary user jobs
- Find the right user quickly from a large list.
- Understand current account state and role bindings before taking action.
- Enable or disable accounts safely with clear consequence messaging.
- Add or remove RBAC bindings accurately and verify effective permissions immediately.
- Validate that granted access matches intended platform responsibilities.

## Scope (Must / Should / Could)

### Must (in this iteration)
- Keep existing backend API contract unchanged (request/response shapes, endpoints, and current capability set).
- Redesign page information architecture into clear task zones:
  - User list/discovery zone.
  - User detail zone.
  - Access control action zone (status + RBAC binding management).
  - Effective permissions visibility zone.
- Improve action clarity and safety:
  - Explicit account status badges and role-binding state.
  - Confirm dialogs for high-impact actions (disable account, remove binding).
  - Inline success/error feedback per action.
- Preserve feature parity:
  - List users.
  - View detail.
  - Enable/disable account.
  - Add/remove RBAC bindings.
  - View effective permissions.
- Handle critical states in UI:
  - Loading, empty list, no-result search/filter, action in progress, API failure, stale selection after list refresh.

### Should (if capacity allows in same sprint)
- Add lightweight list filters/sorting based on already-available fields (no new API contract).
- Add optimistic-but-safe interaction patterns (disable action controls while mutation pending; avoid double submit).
- Add unsaved/dirty-state guard for editable RBAC changes before navigation.

### Could (defer-safe)
- Bulk operations (multi-user status or binding updates) if they require API expansion.
- Advanced permission diff visualization if it requires new backend aggregation.
- Per-admin customization/persistence of table view preferences.

## Acceptance Criteria

1. Feature parity is preserved: system admin can complete all five existing workflows on `/system/users` without using legacy UI.
2. No backend API contract change is required; frontend reuses existing endpoints and DTO fields.
3. User list supports reliable selection and detail synchronization:
   - selecting a row updates detail panel correctly;
   - selection resets or rebinds safely when selected user no longer matches current list state.
4. Account enable/disable flow:
   - shows current status clearly before action;
   - requires explicit confirmation;
   - shows mutation loading state;
   - reflects server result and error states without page reload.
5. RBAC binding add/remove flow:
   - current bindings are clearly visible;
   - add/remove actions are explicit and reversible through supported API actions;
   - high-risk remove action requires confirmation.
6. Effective permissions view is accessible from the same page context and clearly tied to selected user.
7. Core UX states are implemented and testable: loading, empty, no result, server error, mutation error/success.
8. Accessibility baseline:
   - keyboard-reachable major controls;
   - visible focus indicators;
   - status and action feedback exposed as text, not color-only signals.
9. QA can validate all ACs in one test pass on `/system/users` without backend schema/API migration.

## Metrics & Non-goals

### Success metrics (first release window)
- Task completion rate for five core workflows >= current baseline (target +10% where measurable).
- Median time to complete "change account status" reduced by 20%.
- Median time to complete "update RBAC binding and verify effective permissions" reduced by 20%.
- Reduction in failed/retried admin mutations on `/system/users` by 15%.
- Reduction in support/ops clarification requests related to system-user access changes.

### Non-goals (this iteration)
- No new authorization model design.
- No API contract or DB schema redesign.
- No cross-page IA redesign outside `/system/users`.
- No bulk governance workflow introduction if backend support is missing.

## Risks / Rollout

### Risks
- Hidden coupling in current UI logic could regress edge-case flows during layout/interaction redesign.
- Multi-role semantics may be misunderstood if role names/effective permissions are not clearly contextualized.
- Existing API latency/error behavior can degrade perceived UX if loading/mutation states are incomplete.

### Mitigations
- Keep strict parity test matrix mapped to the five existing capabilities.
- Add frontend integration tests for each mutation path and error handling.
- Feature-flag rollout for `/system/users` redesign with fallback to previous page implementation.

### Rollout notes
- Stage rollout:
  1) internal QA + system-admin pilot users;
  2) limited tenant rollout;
  3) full rollout after metrics sanity check.
- Monitor post-release:
  - mutation failure rate;
  - user action cancellation/confirmation ratios;
  - support tickets tagged to system-user management.

## Open questions for architect

1. Multi-role conflict handling: when a user has bindings from multiple scopes, what ordering/precedence must UI communicate explicitly?
2. Effective permissions source of truth: is current endpoint computation definitive for real-time governance decisions, or are there eventual-consistency windows to disclose?
3. Tenant/isolation signaling: does `/system/users` need explicit tenant context indicators to prevent operator mistakes in multi-tenant operations?
4. Safety constraints: are there architecturally mandated guardrails (for example, preventing disable of last active SYSTEM_ADMIN) that UI must enforce or only backend should enforce?
5. Auditability expectations: for this iteration, should UX expose additional operation context (request ID/timestamp) to align with audit log standards already defined by architecture?

## Handoff (single-spec, non-overlapping slices)

- `uiux-agent`: IA, interaction flow, copy states, accessibility behavior for `/system/users`.
- `frontend-engineer-agent`: implement redesign in `frontend/src/pages/system/SystemUsersPage.jsx` with existing API contract.
- `system-admin-agent`: validate domain fit for `/system` namespace behaviors and wording.
- `qa-agent`: execute acceptance criteria matrix and capture evidence.
- `reviewer-agent`: perform post-implementation risk review before merge decision.
