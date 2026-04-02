---
name: booking-devops
description: >-
  Local run, build, and deploy-adjacent notes for booking-core. Maven backend, pnpm frontend, ports, H2 limitations.
  Use when setting up environments, CI snippets, or troubleshooting dev/prod parity.
---

# DevOps (booking-core)

## Local development

**Backend**

```bash
cd backend
mvn spring-boot:run
```

- Port **28080**; API under `/api`.
- H2 console in dev (see `application.yml`); in-memory DB clears on restart.

**Frontend**

```bash
cd frontend
pnpm install
pnpm dev
```

- Default Vite port **25173**; ensure API base URL matches `README.md`.

## Production build (frontend)

```bash
cd frontend
pnpm build
pnpm preview   # optional
```

## Security / config

- JWT and `booking.platform.*` settings: see `README.md` API overview.

## Future extensions

- Add Dockerfiles, CI pipelines, or staging URLs here when introduced; keep commands aligned with **Maven + pnpm**.
