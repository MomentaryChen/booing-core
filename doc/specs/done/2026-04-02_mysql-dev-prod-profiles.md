# Spec: MySQL switch + dev/prod profiles + dev-user bootstrap

- **Date**: 2026-04-02
- **Owner**: PM (booking-core)
- **Status**: Done (archived in `doc/specs/done/`)

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.

## Problem
Current local/container configuration relies on `application-docker.yml` and does not clearly separate **development** vs **production** runtime behavior. We need:

- A standard **MySQL** datasource configuration.
- A clean **dev/prod** environment split using Spring profiles, with **dev as the default** for local runs.
- A safe, deterministic way to **bootstrap development users** from `booking.platform.dev-users` when running in dev.

## Goals / Non-goals

- **Goals**
  - **MySQL** is the primary DB for local/dev/prod runs.
  - Remove dependency on `application-docker.yml` by using Spring profile conventions.
  - Introduce `dev` and `prod` profiles with **default = dev** (local developer experience).
  - In `dev`, automatically create/update users defined in `booking.platform.dev-users`.

- **Non-goals**
  - No functional changes to booking logic (Resource/Slot rules, state machine, availability).
  - No new product routes or UI work (unless required to validate login).
  - No hardcoded “vertical” business flows in bootstrap (e.g., special-casing merchant vs client beyond existing roles/permissions).

## Scope

### Configuration & profiles
- Replace `application-docker.yml` usage with:
  - `application.yml` (common defaults)
  - `application-dev.yml` (dev profile)
  - `application-prod.yml` (prod profile)
- Datasource defaults updated to MySQL. Secrets must be **environment-variable driven** in prod.

### Dev-user bootstrap (dev only)
- Add a dev-only startup task that reads `booking.platform.dev-users` and ensures:
  - users exist (create if missing)
  - passwords are stored **encoded**
  - roles/permissions are assigned in a controlled way
  - user creation is **tenant-scoped** (see architect feedback)

## Architect-agent feedback (required by workspace rule)
This planning round includes architect feedback, summarized below (full response captured during planning):

- **Resource/Slot abstraction fit**: **N/A** — DB/profile refactor; ensure bootstrap stays in “identity/user bootstrap” space and does not introduce vertical nouns.
- **Strategy extensibility**: **Mostly N/A** — avoid hardcoding business flow in seeding logic; seed via existing role/permission identifiers rather than email/name heuristics.
- **State transition legality**: **N/A** — bootstrap must not touch booking state transitions.
- **Multi-tenant isolation impact**: **High risk** — dev-user creation must be tenant-scoped, must not bypass tenant enforcement, and must not accidentally create cross-tenant “god users” unless explicitly defined as a platform concept.

## Acceptance criteria (quick to verify)

- **Profiles**
  - Running the backend with no explicit profile results in **`dev` profile active**.
  - Running with `SPRING_PROFILES_ACTIVE=prod` results in **`prod` profile active**.
  - `application-docker.yml` is **not referenced** by run docs/scripts and is no longer required to start the app.

- **MySQL**
  - Backend starts successfully using **MySQL** (not H2) and can establish a connection using:
    - username: `bookingcore`
    - password: `bookingcore`
  - A basic health check (or startup log) confirms the active datasource URL is MySQL.

- **Dev-user bootstrap**
  - With `dev` profile active and `booking.platform.dev-users` configured, on startup:
    - missing users are created
    - existing users are updated idempotently (no duplicates)
    - stored passwords are encoded (not plain text)
    - users are created **within the specified tenant scope**

- **Safety**
  - In `prod`, dev-user bootstrap is **disabled by default** and does not execute.
  - There is an explicit **guardrail** so `dev` cannot be accidentally used in a production deployment context (fail-fast or deployment enforcement).

## Migration plan

### Config changes (Spring Boot)

- **Deprecate/stop using**
  - `backend/src/main/resources/application-docker.yml`
  - Any docker-compose/run scripts that activate it via `SPRING_CONFIG_ADDITIONAL_LOCATION` or similar.

- **Introduce**
  - `backend/src/main/resources/application-dev.yml`
  - `backend/src/main/resources/application-prod.yml`

- **Update**
  - `backend/src/main/resources/application.yml`
    - Contains common, environment-agnostic settings.
    - Defines profile defaults for local DX (**default dev**) and placeholders for datasource config.

### Infra scripts (local + container)

- **Local development**
  - Provide a standard way to run MySQL locally:
    - either `docker compose` service `mysql` (preferred)
    - or a local MySQL installation
  - Default local environment uses:
    - host: `localhost`
    - port: `23306`
    - database: `booking_core` (or agreed name)
    - user/pass: `bookingcore` / `bookingcore`

- **Container / production**
  - Use `SPRING_PROFILES_ACTIVE=prod` and env vars for datasource:
    - `SPRING_DATASOURCE_URL`
    - `SPRING_DATASOURCE_USERNAME`
    - `SPRING_DATASOURCE_PASSWORD`
  - Ensure container images do **not** bake in dev defaults that could be used accidentally.

### Expected behavior by environment

- **Default (no profile specified)**
  - Active profile: `dev`
  - Datasource: MySQL local/dev instance
  - Dev-user bootstrap: enabled (if configured), runs idempotently

- **Dev (`SPRING_PROFILES_ACTIVE=dev`)**
  - Same as default; explicit selection for CI/local consistency

- **Prod (`SPRING_PROFILES_ACTIVE=prod`)**
  - Datasource: MySQL prod instance (env-provided)
  - Dev-user bootstrap: disabled by default
  - Guardrail: startup should **fail** if `dev` is active in prod context, or deployments should enforce prod profile

## Implementation notes (constraints / guardrails)

- **Dev-user bootstrap must be dev-only**
  - Hard gate on profile **and** an explicit flag (recommended): `booking.platform.dev-users.enabled=true`.
- **Tenant isolation**
  - `booking.platform.dev-users` entries should include explicit `tenantId` (or be organized per tenant).
  - Creation/update must not bypass tenant scoping rules/interceptors.
- **Security**
  - Passwords must be encoded with the same encoder used by the auth stack.
  - `application-prod.yml` must not contain committed secrets (env placeholders only).
- **DB migrations**
  - Switching DB to MySQL implies schema management. Prefer Flyway/Liquibase; in prod, avoid auto-DDL create/update (use validate/none).

## Risks

- **Prod accidentally running with dev profile**
  - Mitigation: enforce `SPRING_PROFILES_ACTIVE=prod` in deployment + add fail-fast guard when `dev` is active in a prod context.

- **Dev-user bootstrap becomes a backdoor**
  - Mitigation: profile+flag gate; limit allowable roles to seed; never accept blank/default passwords; ensure encoding.

- **Multi-tenant leakage**
  - Mitigation: require tenantId for dev users; run via standard services enforcing tenant scope; avoid global admin unless explicitly defined and controlled.

- **Schema drift / migration issues**
  - Mitigation: migration tool adoption; CI smoke test against MySQL; validate charset/collation/timezone.

- **Breaking docker workflows**
  - Mitigation: update docker-compose env var wiring; provide a single documented path to start services.

## Open questions

- **DB name & timezone**
  - Confirm database name (`booking_core` vs other) and MySQL timezone/collation conventions.

- **Migration tooling**
  - Which tool is the standard here: Flyway or Liquibase? (Impacts how we guarantee schema parity in prod.)

- **Tenant model for dev users**
  - Do we support multiple tenants locally? If yes, what is the canonical way to represent tenant IDs in `booking.platform.dev-users`?

- **“Default dev” placement**
  - Should `spring.profiles.default=dev` live in `application.yml`, or only in local run configs to reduce prod foot-guns?

## Handoff / ownership

- **Architect-agent**: validate tenant-scoped bootstrap approach + prod/dev guardrail strategy.
- **Backend-engineer-agent**: implement MySQL datasource + profile files + dev-user bootstrap + safety gates.
- **DevOps-agent**: update docker-compose / run scripts / env var conventions; remove `application-docker.yml` reliance.
- **QA-agent**: quick verification checklist (profiles, MySQL connection, dev-user seeded, prod bootstrap off).

