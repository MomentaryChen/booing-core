# booking-core

A full-stack **booking / appointment** platform: merchants manage services, schedules, and bookings; end users book through a public storefront; system administrators oversee merchants, domain templates, settings, and audit logs.

**Languages:** [English](README.md) · [繁體中文](README.zh-TW.md)

## About the platform

booking-core is a **multi-tenant booking platform** designed for teams that need one shared foundation across different business types. Instead of hardcoding one vertical workflow, it keeps the booking engine generic and configurable.

It serves three product surfaces:

- **Client**: end users browse availability and create bookings
- **Merchant**: providers manage resources, schedules, and booking operations
- **System Admin**: platform operators govern tenants, RBAC, and audit visibility

Core capabilities include **Resource/Slot-based booking**, **role-based access control (RBAC)**, **auditability for operational changes**, and **extensibility** through pluggable strategies and typed metadata.

The stack is intentionally practical: **Java 21 + Spring Boot** on the backend and **React + Vite** on the frontend. This design balances delivery speed with long-term maintainability, so new booking domains can be introduced by adding types and rules, not by rewriting core flows.

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


| Layer    | Stack                                                  |
| -------- | ------------------------------------------------------ |
| Backend  | Java 21, Spring Boot 3.3, Spring Data JPA, MySQL       |
| Frontend | React 18, Vite 5, React Router 6, i18n (zh-TW / en-US) |


## Repository layout

```
booking-core/
├── backend/    # Spring Boot API (local: port 28080)
└── frontend/   # Vite + React SPA (dev: port 25173)
```

## Spec lifecycle (open -> progress -> closed)

- New specs start in `doc/specs/open/` with `YYYY-MM-DD_<kebab-case-topic>.md`.
- Once implementation starts, the spec must be moved to `doc/specs/progress/` immediately.
  - Trigger if any one happens: ticket slicing + owner assignment, implementation task created, coding starts, PR opened, or first implementation commit exists.
- Closed specs move to `doc/specs/closed/` with the same filename.
- Keep one active source of truth only: do not keep the same spec in both `open/` and `progress/`.

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


| Path                                                                 | Role                                                      |
| -------------------------------------------------------------------- | --------------------------------------------------------- |
| `/system`                                                            | System admin dashboard                                    |
| `/merchant`, `/merchant/appointments`, `/merchant/settings/schedule` | Merchant tools                                            |
| `/client`                                                            | Client-facing flow (WIP / to-do page)                     |
| `/client/booking/:slug`                                              | Public storefront by merchant slug                          |
| `/store/:slug`                                                       | Redirects to `/client/booking/:slug`                      |


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

All routes are under `/api`, including merchant CRUD, services, business hours, bookings, customization, dynamic fields, resources, availability exceptions, public storefront booking under `**/api/client/...**`, auth `**/api/auth/login**`, and system endpoints (`/api/system/...`).

When `booking.platform.jwt.secret` is set (256+ bit key recommended), JWT auth is enforced for `/api/merchant/**` (roles `MERCHANT`, `SUB_MERCHANT`, or `SYSTEM_ADMIN`) and `/api/system/**` (`SYSTEM_ADMIN`). The static `booking.platform.system-admin-token` still works for `/api/system/**` in that mode.

## Internal admin and manual demo seed

- **Internal `SYSTEM_ADMIN` (operators):** On startup, if no platform user with role `SYSTEM_ADMIN` exists yet, the backend can create one from `INTERNAL_SYSTEM_ADMIN_*` (see `application-dev.yml` / `application-prod.yml`). Set `INTERNAL_SYSTEM_ADMIN_AUTO_PROVISION=false` if you only create admins via SQL or external IAM.
- **Merchant/client demo accounts:** application bootstrap has been removed. Use manual SQL only.

1. Ensure Flyway migrations are applied and the backend has started at least once if you rely on app-provisioned internal admin.
2. Open `backend/src/main/resources/db/manual/seed_manual_baseline.sql`.
3. (Optional) Update the `INPUTS` section (merchant names/slugs/usernames and password hashes) to fit your local setup.
4. Execute the script in MySQL.
5. Run the verification queries at the end of the script.

This script is idempotent and safe to re-run. It does **not** insert a `SYSTEM_ADMIN` row (avoid duplicating the internal admin path).

## License

Specify your license here if applicable.