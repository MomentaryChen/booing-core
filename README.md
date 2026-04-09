# booking-core

A full-stack **booking / appointment** platform: merchants manage services, schedules, and bookings; end users book through a public storefront; system administrators oversee merchants, domain templates, settings, and audit logs.

**Languages:** [English](README.md) · [繁體中文](README.zh-TW.md)

## System overview

booking-core is a booking platform built around **configurable rules + pluggable strategies** so one engine can support multiple industries without hardcoding domain-specific flows. It models anything bookable as a **Resource** and models time/capacity as a **Slot**. Availability is derived from composable rules and exceptions (e.g., open hours, closures), and booking lifecycle transitions are enforced to avoid illegal state changes.

- **Core capabilities**: Resource/Slot abstraction, availability rules + exceptions, computed availability
- **Lifecycle**: centralized state transitions (create → confirm/cancel/expire → complete)
- **Roles**: Client (book), Merchant (supply & exceptions), System Admin (governance & audit)
- **Non-goals**: no built-in industry terms/flows; no pre-expanding massive future slot rows
- **Extensibility**: add new `ResourceType` / metadata schema / strategies, not new pipelines

## Architecture principles

- **Resource/Slot abstraction**: differences live in `resource_type` and `metadata` (JSON), keeping the core pipeline generic
- **Strategy-based validation**: choose validation strategies by resource type instead of long `if/else` chains in services
- **On-demand Slot Engine**: compute candidate slots from rules + exceptions, then reconcile with booked/locked data (no full-year pre-generation)
- **State machine legality**: one transition entry point; reject illegal status jumps
- **Multi-tenancy isolation**: tables carry `tenant_id`, and read/write paths must enforce tenant scoping by default

## Tech stack

| Layer    | Stack |
| -------- | ----- |
| Backend  | Java 21, Spring Boot 3.3, Spring Data JPA, MySQL |
| Frontend | React 18, Vite 5, React Router 6, i18n (zh-TW / en-US) |

## Repository layout

```
booking-core/
├── backend/    # Spring Boot API (local: port 28080)
└── frontend/   # Vite + React SPA (dev: port 25173)
```

## Prerequisites

- **JDK 21** and **Maven 3.6+** (for the backend)
- **Node.js 18+** and **pnpm** (for the frontend; `packageManager` is pinned in `frontend/package.json`)

## Run the backend

```bash
cd backend
mvn spring-boot:run
```

- REST API base: `http://localhost:28080/api`
 
Backend defaults to **dev profile** (`spring.profiles.default=dev`) and uses **MySQL**. Configure via env vars:

- `DB_HOST` / `DB_PORT` / `DB_NAME`
- `DB_USERNAME` / `DB_PASSWORD`
- `JWT_SECRET` (dev has a safe default; prod must provide)

## Run the frontend

```bash
cd frontend
pnpm install
pnpm dev
```

Open the URL shown by Vite (default `http://localhost:25173`). The dev client calls the API at `http://localhost:28080/api`; CORS is enabled for `http://localhost:25173`.

**Production build:**

```bash
pnpm build
pnpm preview   # optional local preview of the build
```

## App areas (routes)

| Path | Role |
| ---- | ---- |
| `/system` | System admin dashboard |
| `/merchant`, `/merchant/appointments`, `/merchant/settings/schedule` | Merchant tools |
| `/client` | Client-facing flow (WIP / to-do page) |
| `/client/booking/:slug` | Public storefront by merchant slug (e.g. `demo-merchant`) |
| `/store/:slug` | Redirects to `/client/booking/:slug` |

## Screenshots (generated)

Run `qa-agent` Playwright screenshots, then reference these files in docs:

- `qa-agent/artifacts/screenshots/home.png`
- `qa-agent/artifacts/screenshots/system.png`
- `qa-agent/artifacts/screenshots/merchant-login.png`
- `qa-agent/artifacts/screenshots/merchant-register.png`
- `qa-agent/artifacts/screenshots/merchant-dashboard.png`
- `qa-agent/artifacts/screenshots/merchant-appointments.png`
- `qa-agent/artifacts/screenshots/merchant-schedule-settings.png`
- `qa-agent/artifacts/screenshots/client.png`
- `qa-agent/artifacts/screenshots/client-booking-demo-merchant.png`
- `qa-agent/artifacts/screenshots/store-redirect-demo-merchant.png`

## API overview

All routes are under `/api`, including merchant CRUD, services, business hours, bookings, customization, dynamic fields, resources, availability exceptions, public storefront booking under **`/api/client/...`**, auth **`/api/auth/login`**, and system endpoints (`/api/system/...`).

When `booking.platform.jwt.secret` is set (256+ bit key recommended), JWT auth is enforced for `/api/merchant/**` (roles `MERCHANT`, `SUB_MERCHANT`, or `SYSTEM_ADMIN`) and `/api/system/**` (`SYSTEM_ADMIN`). The static `booking.platform.system-admin-token` still works for `/api/system/**` in that mode. Dev-only accounts for issuing tokens are listed under `booking.platform.dev-users`. Leave `jwt.secret` empty for open local development (default).

## Bootstrap env/secret matrix (production-safe)

The deployment contract is environment-driven. `infra/docker-compose.yml` now assumes `prod` behavior by default.

| Environment | Bootstrap toggles default | SYSTEM_ADMIN password source | Default merchant user password source | Credential logging (`booking.platform.auth.log-dev-bootstrap-credentials`) | Owner |
| ----------- | ------------------------- | ---------------------------- | ------------------------------------- | --------------------------------------------------------------------------- | ----- |
| dev/local | Allowed to be enabled for fast setup | Dev env file or local secret (non-production only) | Dev env file or local secret (non-production only) | Allowed only for temporary debugging; must be turned off after verification | Backend on-call engineer (execution) + PM on duty (approval) |
| staging | Follows production contract (default OFF; enable only per release plan) | Secret manager / CI protected env var only | Secret manager / CI protected env var only | Must remain `false` (no plaintext credential logging) | Release manager (approval) + DevOps on-call (execution) |
| prod | OFF by default; one-time controlled enablement only | Secret manager / runtime secret injection only | Secret manager / runtime secret injection only | Must remain `false` (no plaintext credential logging) | Platform owner (approval) + DevOps on-call (execution) |

### Staging minimum executable contract

- Staging must use `prod`-equivalent bootstrap/security contract by default: bootstrap toggles OFF unless explicitly approved for one-time initialization.
- Any bootstrap run in staging requires pre-provisioned secrets for admin/merchant-user credentials, and should be executed with a rollback owner + audit trail.
- Plaintext bootstrap credential logs are forbidden in staging and production.

### One-time bootstrap + rollback window runbook (staging/prod)

Use this flow only when initializing a brand-new environment or after a destructive reset. Keep bootstrap toggles OFF during normal deploys.

1. **Pre-flight (T-30m)**
   - Confirm owner pair from the matrix above (approval + execution) and open a change ticket.
   - Confirm secret keys are present in secret manager / CI:
     - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD`
     - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_DEFAULT_MERCHANT_USER_PASSWORD`
   - Confirm credential logging guardrail: `BOOKING_PLATFORM_AUTH_LOG_DEV_BOOTSTRAP_CREDENTIALS=false`.

2. **Open bootstrap window (T-5m)**
   - Set one-time toggles to `true` for this deployment only:
     - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_SYSTEM_ADMIN_ENABLED=true`
     - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_DEFAULT_MERCHANT_ENABLED=true`
     - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_DEFAULT_MERCHANT_USER_ENABLED=true`
   - Deploy backend once with the same image you plan to keep running.

3. **Verify seed result (T+0m to T+10m)**
   - Verify health endpoint is up.
   - Verify one successful SYSTEM_ADMIN login and one successful default MERCHANT login.
   - Verify no plaintext credentials in logs (search for username/password markers in the deploy log stream).
   - Verify idempotency guard by checking there is no duplicate default merchant slug/user in DB.

4. **Close bootstrap window (immediately after verification)**
   - Set all three bootstrap toggles back to `false`.
   - Redeploy backend (or restart with updated env) so runtime returns to steady-state contract.
   - Attach verification evidence to the change ticket (login proof, log redaction proof, toggle-off proof).

5. **Rollback window (first 60 minutes after close)**
   - Owner pair remains online for 60 minutes.
   - If bootstrap output is wrong or security checks fail:
     - Keep toggles `false`.
     - Roll back to last known-good release artifact.
     - Restore database from pre-bootstrap snapshot / PITR if data integrity is impacted.
     - Rotate both bootstrap passwords in secret manager before any retry.

### Staging executable contract (prod-equivalent)

Apply this contract to every staging deployment unless a one-time bootstrap window is explicitly opened.

- Required env baseline:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_SYSTEM_ADMIN_ENABLED=false`
  - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_DEFAULT_MERCHANT_ENABLED=false`
  - `BOOKING_PLATFORM_AUTH_BOOTSTRAP_DEFAULT_MERCHANT_USER_ENABLED=false`
  - `BOOKING_PLATFORM_AUTH_LOG_DEV_BOOTSTRAP_CREDENTIALS=false`
- Required secret sources:
  - Admin and merchant bootstrap passwords must come from secret manager / CI protected variables, never from git-tracked files.
- Deployment gate:
  - Block release if any bootstrap toggle is `true` without an approved change ticket.
  - Block release if credential logging is not `false`.
- Verification gate:
  - Run one admin API smoke check and one merchant API smoke check after deploy.
  - Confirm no plaintext credential output in staging logs.

## License

Specify your license here if applicable.
