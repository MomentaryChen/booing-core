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
| Lint/format when the repo wires them up | `.cursor/skills/fix/SKILL.md` (adapt commands to **pnpm** if instructions say yarn) |
| Client / merchant / system routes | `.cursor/skills/booking-client-domain/SKILL.md` (and sibling domain skills) |
| UI copy, surfaces, a11y, i18n tone | `.cursor/skills/booking-uiux/SKILL.md` — **read before building or changing user-facing pages** |

## UI/UX alignment (required before implementation)

Do **not** implement new pages, major layout changes, or flow-level copy/structure changes **until** UI/UX has been in the loop (same planning round or explicit handoff). Small fixes (typos, non-visual refactors) may skip a full round if already spec-approved.

**UI/UX should provide before coding:** IA (hierarchy, primary actions), copy structure and i18n intent (zh-TW + en-US), key states (loading / empty / error / forbidden), responsive intent, a11y notes (focus, semantics). Follow domain skills so `/client`, `/merchant`, `/system` stay consistent.

**Frontend returns for sign-off:** PR or scoped diff, screenshots of agreed states, responsive spot-check, and a short note on any intentional deviation.

**Disagreement:** document UX intent vs FE constraint; default tie-breaker is **PM + authoritative spec** (`doc/specs/YYYY-MM-DD_*.md`); architectural/security issues may need architect/backend.

## Notes

- This project does **not** currently use Flow in `package.json`. If Flow is added later, also use `.cursor/skills/flow/SKILL.md`.
- Dev server default: `http://localhost:25173`; API `http://localhost:28080/api` with CORS for dev.
