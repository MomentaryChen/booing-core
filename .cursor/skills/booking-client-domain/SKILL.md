---
name: booking-client-domain
description: >-
  End-customer booking storefront routing for booking-core. Enforces /client pages and /api/client APIs.
  Use when implementing or reviewing client-facing routes, public booking flows, or API paths for end users.
---

# Client domain (public storefront)

## When to use

- Any feature for **end customers** booking through the public storefront.
- When adding or renaming routes, links, or API clients for the client journey.

## Hard routing rules

- All client **API** routes MUST start with `/api/client`.
- All client **page** URLs MUST start with `/client`.
- Do not use `/user`, `/api/user`, or other prefixes for client journeys. Reserve **user** for internal platform accounts (JWT + `PlatformUserRole`: `SYSTEM_ADMIN`, `MERCHANT`, `SUB_MERCHANT`, `CLIENT`).

## Preferred patterns

**API:** `/api/client/{domain}/{resource}`

- `GET /api/client/merchant/{slug}`
- `GET /api/client/availability`
- `POST /api/client/booking/lock`
- `POST /api/client/booking`

**Pages:**

- `/client`
- `/client/booking`
- `/client/booking/success`
- `/client/booking/{slug}`

## Checklist

1. Inspect proposed paths before coding.
2. Refactor non-compliant routes to `/client/*` and `/api/client/*`.
3. Align router, links, and API client calls.

## Output expectations

- Call out route violations first; propose exact replacements.
