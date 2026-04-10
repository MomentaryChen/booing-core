# Spec: Platform user table + roles (Flyway V2)

- **Date**: 2026-04-02
- **Owner**: PM (booking-core)
- **Status**: Done (archived in `doc/specs/done/`)

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.

## Problem

The platform currently has:

- Role-aware navigation/authorization concepts (`PlatformUserRole`, `role_page_grants`)
- JWT claims that carry `role` and optional `merchantId`
- Dev-only login via `booking.platform.dev-users` (config-backed), **without a persistent user table**

This blocks basic platform identity requirements (persistent accounts, disable/enable, auditability) and leaves an “obvious missing” schema element that should have existed earlier.

## Scope

- Add a persistent **platform user table** (`platform_users`) in MySQL.
- Each user has exactly **one platform role** (reuse `PlatformUserRole` names).
- Support an optional `merchant_id` relationship for merchant-scoped roles.
- Define constraints/indexes and a Flyway migration plan (**V2**).
- Define a **dev seeding plan** that sources from `booking.platform.dev-users` (dev profile only), without putting environment-specific seed data into Flyway prod migrations.

## Non-goals

- Implement a full production authentication system (password reset, MFA, OAuth, email verification).
- Change booking domain logic (Resource/Slot rules, availability engine, booking state machine).
- Redesign permissions beyond the existing role + `role_page_grants` model.
- Replace `booking.platform.dev-users` login mechanism in this same change (this spec only defines the table + seeding plan).

## Data model

### Table: `platform_users`

**Purpose**: persistent platform identities used by system admin, merchant console, sub-merchant accounts, and (optionally) internal “client actor” identities.

#### Columns

| Column | Type | Null | Notes |
|---|---:|:---:|---|
| `id` | `bigint` auto-increment | no | Primary key |
| `username` | `varchar(120)` | no | Unique login name (see collation note) |
| `password_hash` | `varchar(255)` | no | Encoded (bcrypt/argon2) string, **not** plaintext |
| `platform_role` | `varchar(32)` | no | Must match `PlatformUserRole` enum name |
| `merchant_id` | `bigint` | yes | Merchant scope for merchant roles (see role mapping) |
| `enabled` | `bit` | no | Default `1` |
| `created_at` | `datetime(6)` | no | Default `CURRENT_TIMESTAMP(6)` |
| `updated_at` | `datetime(6)` | no | Default `CURRENT_TIMESTAMP(6)` + `ON UPDATE CURRENT_TIMESTAMP(6)` |
| `last_login_at` | `datetime(6)` | yes | Optional |

#### Constraints

- **Primary key**: `PRIMARY KEY (id)`
- **Username uniqueness**: `UNIQUE KEY uk_platform_users_username (username)`
  - Collation decision impacts whether uniqueness is case-sensitive. For now, follow the DB default; if we need explicit behavior, set table/column collation in V2.
- **Merchant relationship**: `FOREIGN KEY (merchant_id) REFERENCES merchants(id)`
  - Delete behavior: **RESTRICT** is preferred for safety/auditability (merchant deletion should be explicit), but **SET NULL** is acceptable if operations require merchant deletion without manual user cleanup. Decide during implementation and document.
- **Role ↔ merchant scope (recommended DB-level rule)**:
  - `SYSTEM_ADMIN` / `CLIENT` ⇒ `merchant_id IS NULL`
  - `MERCHANT` / `SUB_MERCHANT` ⇒ `merchant_id IS NOT NULL`
  - Implementation options:
    - **Preferred**: `CHECK` constraint (requires MySQL 8.0.16+; confirm runtime)
    - **Fallback**: enforce in application service layer and add tests (still keep column nullable for forward-compat)

#### Indexes

- `KEY idx_platform_users_merchant_id (merchant_id)` for merchant-scoped queries.
- Optional: `KEY idx_platform_users_role (platform_role)` if needed for admin listing/reporting.

## Role mapping (reuse `PlatformUserRole`)

The `platform_users.platform_role` string values **must** match `com.bookingcore.security.PlatformUserRole`:

- `SYSTEM_ADMIN`
- `MERCHANT`
- `SUB_MERCHANT`
- `CLIENT`

Notes:

- This keeps alignment with existing `role_page_grants.platform_role` (also `varchar(32)`), and with JWT role claim usage.
- A future hardening option is to normalize roles into a `platform_roles` lookup table and reference it from both `platform_users` and `role_page_grants` (out of scope for V2).

## Relationship to `merchant_id` (tenant boundary)

- **Current system boundary**: the platform already uses `merchant_id` broadly; for this scope, treat `merchant_id` as the effective tenant boundary for merchant-console access.
- **Rule**:
  - `MERCHANT` and `SUB_MERCHANT` users are scoped to exactly one `merchant_id`.
  - `SYSTEM_ADMIN` is platform-wide and should not be merchant-scoped.
  - `CLIENT` is currently a platform role in code; whether it should be merchant-scoped depends on future storefront account modeling (out of scope). For now, keep `merchant_id` NULL for `CLIENT` to avoid implying a tenant model we’re not enforcing elsewhere.

## Migration plan (Flyway V2)

- **Add** `backend/src/main/resources/db/migration/V2__create_platform_users.sql`
  - Create `platform_users` with `engine=InnoDB default charset=utf8mb4` (match V1 style)
  - Add constraints + indexes as specified above
  - Do **not** insert environment-specific users in this migration

## Dev seeding plan (from `booking.platform.dev-users`)

Goal: Keep Flyway migrations environment-agnostic while still making local development easy and consistent.

- **Source of truth in dev**: `booking.platform.dev-users` entries:
  - `username`, `password`, `role`, optional `merchantId`
- **Seeding behavior (dev profile only)**:
  - On startup, upsert `platform_users` rows for each configured dev user:
    - create if missing
    - update `password_hash` and `enabled` if present (idempotent)
  - Passwords must be stored as **encoded hashes** (bcrypt/argon2), never plaintext.
  - For `MERCHANT` / `SUB_MERCHANT`, set `merchant_id` to the provided `merchantId`, or auto-resolve a default merchant as current dev bootstrap does (best-effort).
- **Safety guardrails**:
  - Must not run in `prod` profile.
  - Recommended additional gate: explicit flag like `booking.platform.dev-users.enabled=true` (even under `dev`) to avoid accidental execution in shared environments.

## Architect feedback (required by workspace rule)

Summary from architect-agent review:

- **Resource/slot abstraction fit**: **N/A** (identity/auth plane). Use platform-generic naming (`platform_users`) to cover all roles without vertical hardcoding.
- **Strategy extensibility**: keep authorization data-driven; continue using role grants (`role_page_grants`) instead of proliferating role `if/else` in services.
- **State transition legality**: **N/A** for booking lifecycle. For users, keep it minimal (`enabled`) unless committing to a full status transition policy + audit.
- **Multi-tenant isolation impact**:
  - Treat merchant scope as derived from authenticated principal (JWT/user), **never** from request parameters for authorization.
  - Enforce merchant scoping at repository/query layer where possible to reduce controller-level leakage risk.
- **Tenant model decision**: treat `merchant_id` as tenant boundary for now; adding a full `tenant_id` system now would be a wide, high-risk migration. Design `platform_users` so a future tenant abstraction can be introduced cleanly.
- **DB-level role/merchant constraints**: recommended rule: `SYSTEM_ADMIN/CLIENT => merchant_id NULL`, `MERCHANT/SUB_MERCHANT => merchant_id NOT NULL`.
- **Operational/security risks**: privilege escalation and cross-merchant leakage; role changes should be audited (`audit_logs`) and protected.

## DBA feedback (migration/constraints)

Summary from dba-agent guidance:

- **Columns**: `id`, `username`, `password_hash`, `platform_role`, `merchant_id`, `enabled`, `created_at`, `updated_at`, `last_login_at`.
- **Indexes**: `uk_platform_users_username`, `idx_platform_users_merchant_id` (optional role index).
- **Role validation**:
  - Minimal now: store role as `varchar(32)` aligned with `PlatformUserRole`.
  - Hardening later: introduce `platform_roles` lookup table and FK from `role_page_grants` + `platform_users`.
- **Flyway**: `V2__create_platform_users.sql`, forward-only, no data backfill required today.
- **Charset/collation**: keep `utf8mb4`; decide whether username uniqueness should be case-sensitive via explicit collation if needed.
- **Seeding**: keep seed data out of core migrations; implement dev-only seeding via dev profile initializer or dev-only Flyway locations.

## Acceptance criteria

- **Schema**:
  - Flyway V2 creates `platform_users` in MySQL with the columns and constraints described above.
  - `username` is unique; `merchant_id` FK exists; indexes exist as specified.
- **Role alignment**:
  - `platform_role` values are compatible with `PlatformUserRole` enum names.
- **Merchant scoping**:
  - DB and/or service-level validation prevents invalid combinations of `(platform_role, merchant_id)` (per role mapping rules).
- **Dev seeding plan is implementable**:
  - Running with `dev` profile and `booking.platform.dev-users` configured can deterministically populate `platform_users` (idempotent, hashed passwords).
  - Seeding does not run in `prod` by default.

## Risks / open questions

- **MySQL CHECK constraints**: confirm runtime MySQL version supports effective `CHECK` enforcement; otherwise rely on application validation + tests.
- **Username collation**: decide whether usernames must be case-sensitive; affects unique constraint behavior.
- **FK delete behavior**: choose RESTRICT vs SET NULL for `merchant_id` on merchant deletion; RESTRICT is safer for auditability.
- **`CLIENT` modeling**: whether platform-level `CLIENT` accounts should be merchant-scoped is a separate product decision (storefront auth vs platform auth).

## Handoff / ownership

- **architect-agent**: confirm merchant scope rules, tenant boundary assumptions, and guardrails for cross-merchant access.
- **dba-agent**: review final DDL for constraints/indexes/collation; confirm Flyway migration ordering and MySQL version capabilities.
- **backend-engineer-agent**: implement Flyway V2 migration and dev-only seeding initializer (from `booking.platform.dev-users`).
- **reviewer-agent**: required review after implementation commits (security + tenant isolation focus).

