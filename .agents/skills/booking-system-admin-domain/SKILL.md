---
name: booking-system-admin-domain
description: >-
  System administration features for booking-core. Enforces /system and /api/system; forbids new /admin routes.
  Use when building platform admin, RBAC, audit logs, settings, templates, or dashboards.
---

# System admin domain

## When to use

- System administration: merchants oversight, domain templates, settings, audit logs, platform operations.

## Hard constraints

1. Canonical prefix: **`/system`** for pages, **`/api/system/...`** for APIs (align with existing app patterns).
2. Never introduce **new** System Admin endpoints under `/admin`.
3. Never introduce **new** System Admin pages under `/admin`.
4. If legacy `/admin` appears in touched files, migrate to `/system` in the same change when safe.
5. Keep backend and frontend path changes aligned in one change set.

## Implementation checklist

1. Search for existing System Admin routes and API calls before editing.
2. Update controller/service/repository as needed.
3. Update frontend routes, navigation, and API clients.
4. Preserve backward compatibility only when explicitly requested.
5. Ensure RBAC and audit coverage for admin operations.
6. Run tests/build/lint when available.

## Output expectations

- Summarize changed files and `/system` routes updated.
- List any remaining legacy `/admin` references.
