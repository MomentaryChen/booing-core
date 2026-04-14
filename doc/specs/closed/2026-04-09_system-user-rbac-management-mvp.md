# System user management MVP (roles + effective permissions)

Date: 2026-04-09  
Owner: PM + Architect (planning round complete)  
Status: **Closed**  

## Problem

System admins need one practical place in `/system` to manage platform users safely after multi-role support (`platform_user_rbac_bindings`) was introduced. Current capability exists in backend pieces, but MVP needs a clear management surface and contract.

> Domain boundary note: merchant visibility/invitation and cross-tenant booking delegation are defined in `doc/specs/closed/2026-04-09_domain-visibility-invitation-mvp.md`. This document stays focused on platform user + RBAC management.

## Product surfaces

- Primary: `/system` (UI), `/api/system/**` (API)
- Read-only downstream impact: login/context resolution in auth flow (effective permissions source of truth)
- Out of scope this round: `/merchant`, `/client` UI changes

## Scope

### In scope (MVP)

- System page to list/search/filter platform users (username/email/status/role), with pagination and deterministic sorting.
- User detail panel/page with:
  - basic profile fields (minimal editable set),
  - account status toggle (active/disabled) through a controlled transition endpoint.
- Role binding management using existing `platform_user_rbac_bindings`:
  - add binding,
  - remove binding,
  - duplicate add/remove is idempotent.
- Effective permissions preview (read-only, backend-resolved; no frontend recomputation).
- Strict authorization: only system-admin-capable callers can access management APIs.
- Audit logging for all write operations (actor, target, action, before/after summary, timestamp, request correlation).

### Out of scope (post-MVP)

- Custom role/permission designer and policy builder.
- Delegated admin hierarchy / approval workflow.
- Bulk import/export, SCIM/SSO provisioning.
- Impersonation/switch-user simulation.
- Time-bound grants and advanced governance analytics.

## Acceptance criteria

- [ ] System admin can open `/system` user management and list users with filters and pagination.
- [ ] System admin can view a user’s current role bindings and effective permissions.
- [ ] System admin can add/remove role bindings; duplicate operations are idempotent and do not corrupt state.
- [ ] Effective permissions endpoint returns deterministic results derived from current bindings.
- [ ] Non-system-admin callers receive forbidden on all management endpoints.
- [ ] Cross-scope/cross-tenant incompatible role binding attempts are rejected.
- [ ] Illegal account status transitions are rejected with stable API error codes.
- [ ] Every mutation writes an audit record with required fields.
- [ ] UI refresh reflects backend truth after each mutation (no stale role/permission view).
- [ ] Tests cover happy path + forbidden + isolation + transition legality + idempotency/concurrency.

## Risks and mitigations

- Privilege escalation via weak binding validation  
  - Mitigation: server-side scope/tenant checks + negative authorization tests.
- Permission drift between UI and auth truth  
  - Mitigation: one backend permission resolver used everywhere; UI renders resolver output only.
- Illegal status changes through bypass paths  
  - Mitigation: single transition service entrypoint; block direct status updates in API/repository path.
- Concurrent binding updates race  
  - Mitigation: DB uniqueness constraints + transactional updates + idempotent semantics.
- Incomplete traceability for security incidents  
  - Mitigation: mandatory mutation audit events and QA assertions on audit records.

## Dependency order (implementation)

1. Architect + DBA + Backend align data/constraint needs (bindings uniqueness/index/state fields/audit compatibility).
2. DBA delivers Flyway migration(s) for any missing constraints/indexes (idempotent, rollback-aware).
3. Backend delivers domain services:
   - status transition service,
   - role binding service,
   - effective permissions read endpoint,
   - authorization + audit hooks.
4. Backend finalizes `/api/system/platform-users` contract (DTOs/errors/filter/sort/pagination).
5. Frontend (`/system`) implements list/detail/binding/effective-permission views against stable contract.
6. QA executes AC matrix and regression/security checks.
7. Reviewer gate after each commit and before merge/release decision.

## Done definition

Feature is done only when all are true:

- AC checklist above is fully passed.
- Backend + frontend automated tests for new behavior are merged and green.
- QA evidence is attached for API and UI paths (including forbidden and isolation scenarios).
- Reviewer-agent reports no unresolved high/critical findings.
- Spec remains the single source of truth for this feature; follow-up scope is explicitly marked as post-MVP.

## Handoff ownership

- `architect-agent`: guardrails validation (tenancy, transition legality, RBAC boundaries).
- `dba-agent`: migration/constraints/indexes for binding correctness and concurrency safety.
- `backend-engineer-agent`: services, API, authz, audit, tests.
- `frontend-engineer-agent` + `system-admin-agent`: `/system` UI implementation and integration.
- `qa-agent`: acceptance execution + evidence.
- `reviewer-agent`: mandatory quality gate after commits and before final merge/release.

## UIUX redesign round (`/system/users`)

### In scope

- Redesign `/system/users` IA and interaction flow for: list users -> inspect detail -> grant/revoke roles -> preview effective permissions.
- Improve clarity with explicit labels, role/permission grouping, action hierarchy, empty/loading/error states, and safer mutation confirmations.
- Keep existing `/api/system` contracts by default; allow only minimal backward-compatible API response additions when UX-critical.
- Add accessibility and readability improvements (keyboard traversal, focus visibility, semantic headings, contrast-safe tokens).

### Out of scope

- No RBAC domain model redesign, no new role designer, no approval workflow.
- No tenant model change for platform user/RBAC domain. Cross-tenant booking delegation rules are handled by the domain visibility/invitation spec.
- No booking domain flow/state-machine/slot-engine changes.
- No broad backend schema migration unless a confirmed UX blocker cannot be solved in frontend composition.

### Acceptance criteria

- [ ] System admin completes core journey (list -> detail -> role change -> permission preview) without hidden steps or ambiguous labels.
- [ ] Page clearly separates read-only permission preview from writable role-binding actions.
- [ ] Every destructive/high-impact action (disable user, remove role, bulk assign) requires explicit confirmation with target summary.
- [ ] Unauthorized actions are never executable from UI and are still rejected by backend authorization.
- [ ] UI supports loading, empty, no-result, and mutation-error states with actionable guidance.
- [ ] Keyboard-only navigation reaches all controls in logical order; focus state is consistently visible.
- [ ] On large datasets, pagination/filter/search remain responsive and avoid payload-heavy full-matrix rendering.
- [ ] QA evidence confirms zh-TW/en-US labels remain understandable and consistent with system terminology.

### Risks

- Privilege escalation from unclear bulk/batch actions.
- Tenant isolation regression if list/detail scopes are loosened.
- UI/backend auth mismatch causing false affordances.
- Performance degradation with large user-role-permission matrices.
- Audit gaps if mutation intent/result is not visible for operator verification.

### Dependency order

1. PM + architect finalize user journeys, guardrails, and acceptance checklist.
2. UIUX proposes revised IA/wire-level interaction and copy baseline.
3. Frontend + system-admin agent implement redesign against existing APIs.
4. Backend adjusts only UX-blocking contract gaps (if needed), keeping compatibility and authz invariants.
5. QA executes usability + authorization + tenant isolation regression.
6. Reviewer-agent gates merge/release readiness.

### Done definition

- In-scope acceptance criteria all pass with QA evidence.
- No unresolved high/critical reviewer findings.
- Tenant isolation and server-side authorization checks are unchanged or stronger.
- No unapproved schema/domain expansion introduced by redesign.
