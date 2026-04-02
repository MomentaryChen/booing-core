---
name: booking-frontend
description: >-
  Frontend conventions for booking-core React/Vite app under frontend/. Package manager pnpm; React Router and i18n.
  Use when editing UI, routes, or API calls from the SPA.
---

# Frontend (booking-core)

## Stack

- React 18, Vite 5, React Router 6, i18n zh-TW / en-US.
- **Package manager:** `pnpm` (pinned in `frontend/package.json`). Use `pnpm install`, `pnpm dev`, `pnpm build` — not `yarn` unless the project changes.

## Read these skills

| Topic | Skill |
|--------|--------|
| Lint/format when the repo wires them up | `.agents/skills/fix/SKILL.md` (adapt commands to **pnpm** if instructions say yarn) |
| Client / merchant / system routes | `.agents/skills/booking-client-domain/SKILL.md` (and sibling domain skills) |

## Notes

- This project does **not** currently use Flow in `package.json`. If Flow is added later, also use `.agents/skills/flow/SKILL.md`.
- Dev server default: `http://localhost:25173`; API `http://localhost:28080/api` with CORS for dev.
