# Responsibility Allocation (RACI) — MySQL switch + dev/prod profiles + local ports

- **Date**: 2026-04-02
- **Status**: Done (archived in `doc/specs/done/`)
- **Scope context**: Switch DB to **MySQL**, introduce **dev/prod Spring profiles**, remove reliance on `application-docker.yml`, set **default profile = dev**, add **dev bootstrap users**, adjust **local/container ports**.
- **Governance rules (repo)**:
  - **PM+Architect joint planning is mandatory** for planning outputs. (Architect feedback is captured in `doc/specs/done/2026-04-02_mysql-dev-prod-profiles.md`.)
  - **Post-commit review gate**: after each commit, **reviewer-agent must perform a full review** before merge/release decisions.

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.

## Roles and boundaries (what each role owns / must approve / must not do)

### RACI summary (by deliverable)

| Deliverable | PM | architect-agent | backend-engineer-agent | frontend-engineer-agent | devops-agent | qa-agent | reviewer-agent |
|---|---|---|---|---|---|---|---|
| **D1. Spec + acceptance criteria** (single source of truth) | **A/R** | C | C | C | C | C | C |
| **D2. Backend config** (`application.yml`, `application-dev.yml`, `application-prod.yml`) | C | C | **A/R** | I | C | C | **R (gate)** |
| **D3. MySQL runtime wiring** (datasource, docker compose envs) | I | C | **R** | I | **A/R** | C | **R (gate)** |
| **D4. Dev-user bootstrap** (`booking.platform.dev-users`, dev-only safety) | C | **A (tenant isolation guardrails)** | **A/R** | I | I | C | **R (gate)** |
| **D5. Local ports and docs** (mysql/redis/backend/frontend) | I | I | C | I | **A/R** | C | **R (gate)** |
| **D6. QA verification pack** (profiles, MySQL, bootstrap, ports) | C | I | C | I | C | **A/R** | C |

Legend: **R** = Responsible (does the work), **A** = Accountable (final owner), **C** = Consulted, **I** = Informed. **Gate** = required review before merge/release.

---

### PM (pm-agent)

- **Owns (deliverables)**:
  - **D1**: Final spec as single source of truth, including scope, acceptance criteria, risks, and handoff slices.
  - Coordination checklist across backend/devops/QA/review.
- **Must review/approve**:
  - Any scope expansion beyond “DB/profile/ports/bootstrap” (e.g., booking domain changes, new routes).
  - Acceptance criteria changes that impact verification workload or release risk.
- **Must not do**:
  - Make unilateral architectural calls that bypass architect feedback (esp. tenant isolation & safety guardrails).
  - Declare “ready to merge/release” without reviewer-agent gate.

### architect-agent

- **Owns (deliverables)**:
  - Architecture guardrails for this change set:
    - **Multi-tenant isolation impact** (highest risk area for bootstrap/seeding).
    - “No vertical hardcoding” principle (keep bootstrap generic; avoid business-flow logic).
- **Must review/approve**:
  - **D4** dev bootstrap design: tenant scoping + preventing cross-tenant leakage/backdoor users.
  - Any “dev default” decisions that could become prod foot-guns (fail-fast strategy, deployment enforcement).
- **Must not do**:
  - Implement production code changes directly (unless explicitly asked to switch roles).
  - Accept designs that bypass tenant enforcement or allow silent prod execution of dev-only bootstrap.

### backend-engineer-agent

- **Owns (deliverables)**:
  - **D2** Spring profiles + backend config correctness.
  - **D3** MySQL datasource correctness (runtime behavior, JPA DDL modes per profile).
  - **D4** dev-user bootstrap implementation & safety gates (dev-only, idempotent, encoded passwords).
- **Must review/approve**:
  - PR/changes that touch auth bootstrap or security-sensitive config keys (JWT secret, admin tokens).
  - Schema/migration implications when moving from H2/test to MySQL (esp. prod `ddl-auto=validate` behavior).
- **Must not do**:
  - Commit secrets into `application-prod.yml` or code.
  - Add new business flow logic tied to vertical nouns (per rule-engine architecture guideline).
  - Bypass reviewer-agent gate after commits.

### frontend-engineer-agent

- **Owns (deliverables)**:
  - Only **if needed** for validation: minimal client-side configuration compatibility when ports/base paths change (e.g., API base path alignment).
- **Must review/approve**:
  - Any frontend env var changes (Vite build args, API base path) that impact local run or container routing.
- **Must not do**:
  - Change product routes or UX flow as part of this infra/config task without PM approval.
  - Introduce new `/admin` routes (system admin is `/system`).

### devops-agent

- **Owns (deliverables)**:
  - **D3** docker-compose wiring (env vars, service dependency ordering).
  - **D5** local ports alignment + run instructions (compose + local run).
  - Ensure removal of reliance on deprecated `application-docker.yml` in deployment/run docs/scripts.
- **Must review/approve**:
  - Any container runtime settings that select profiles (e.g., `SPRING_PROFILES_ACTIVE=prod`) and env var naming conventions.
  - Port mapping changes and reverse-proxy/base path changes that affect access patterns.
- **Must not do**:
  - Bake dev defaults into prod images (e.g., static JWT secret, dev users enabled).
  - Alter domain logic; keep scope to runtime/infra/config.

### qa-agent

- **Owns (deliverables)**:
  - **D6** verification checklist + evidence (logs/screenshots where relevant):
    - Default profile = dev
    - prod profile uses env-provided DB creds; dev bootstrap does not run
    - MySQL connectivity via compose/local port mapping
    - Ports are reachable as documented
- **Must review/approve**:
  - “Ready for merge” signal from a functional verification standpoint (still subject to reviewer gate).
- **Must not do**:
  - Expand scope into feature testing of booking flow (separate epic/spec).
  - Mark release-ready if reviewer-agent flags high/critical findings.

### reviewer-agent

- **Owns (deliverables)**:
  - Mandatory **post-commit** review focusing on defects/risks:
    - security (secrets, JWT, bootstrap backdoor)
    - multi-tenant isolation leakage
    - regression risk (profiles/ports breaking local/prod)
    - legality/safety of state transitions is **out of scope** here unless touched
- **Must review/approve**:
  - Every commit that touches backend config/auth bootstrap/infra ports.
  - Explicitly call out “No critical findings” or block with high/critical items.
- **Must not do**:
  - Merge-approve when high/critical issues remain unresolved.

---

## Current deliverable status (done / remaining / risks)

### D1. Spec + acceptance criteria

- **Status**: **Done (drafted)** — `doc/specs/done/2026-04-02_mysql-dev-prod-profiles.md`
- **Remaining**:
  - Ensure spec status moves from “Proposed” to “Accepted” once reviewer/QA complete and risks are addressed.
- **Risks**:
  - “Default dev” placement can be a prod foot-gun if deployment enforcement is weak.

### D2. Backend config (profiles)

- **Status**: **Done**
  - `application.yml`: `spring.profiles.default=dev`, `server.port=28080`
  - `application-dev.yml`: MySQL local defaults + `booking.platform.dev-users` seed list
  - `application-prod.yml`: env-driven datasource, redis cache, `ddl-auto=validate`
- **Remaining**:
  - Confirm there is **no remaining reference** to `application-docker.yml` in docs/scripts (codebase-wide check).
- **Risks**:
  - Misconfiguration could cause prod to start with dev defaults if env vars/profiles are missing.

### D3. MySQL runtime wiring (compose + envs)

- **Status**: **Mostly done**
  - `infra/docker-compose.yml`: MySQL `23306:3306`, Redis `26379:6379`, backend `28080:8080`, frontend `25173:80`
  - backend env uses `SPRING_PROFILES_ACTIVE=prod` and `DB_URL/DB_USERNAME/DB_PASSWORD`
- **Remaining**:
  - Validate database name consistency (`bookingcore` vs spec mention `booking_core`) across docs/config.
- **Risks**:
  - Charset/timezone differences between dev/prod MySQL may surface later; should be standardized.

### D4. Dev-user bootstrap (dev-only)

- **Status**: **Config done; implementation/guardrails unknown from current context**
  - `application-dev.yml` defines `booking.platform.dev-users`
- **Remaining**:
  - Confirm implementation is **dev-only**, **idempotent**, **password-encoded**, and **tenant-scoped** (architect sign-off required).
  - Confirm prod profile cannot accidentally execute bootstrap.
- **Risks**:
  - Potential backdoor if dev-users can be enabled in prod or if roles are overly permissive.
  - Tenant leakage if bootstrap bypasses tenant enforcement.

### D5. Local ports and docs

- **Status**: **Ports done; docs alignment unknown**
  - Compose ports appear aligned and non-default (reduce collisions).
- **Remaining**:
  - Ensure README/run docs reflect: backend `28080`, frontend `25173`, mysql `23306`, redis `26379`.
- **Risks**:
  - Developer confusion if docs are stale; CI/local scripts might assume old ports.

### D6. QA verification pack

- **Status**: **Remaining**
- **Remaining**:
  - Execute smoke checks for:
    - default dev profile activation
    - prod profile activation via env
    - MySQL connection success on dev (local) and prod (compose backend)
    - dev-users available only in dev context
    - port reachability
- **Risks**:
  - “Works on my machine” drift if verification isn’t run on a clean environment.

