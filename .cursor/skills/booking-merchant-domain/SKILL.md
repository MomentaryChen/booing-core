---
name: booking-merchant-domain
description: >-
  Merchant dashboard and APIs for booking-core. Enforces /merchant pages and /api/merchant APIs.
  Use when building merchant scheduling, services, appointments, or merchant-facing UI.
---

# Merchant domain

## When to use

- Merchant-side booking features: scheduling, inventory, appointments, settings.

## Hard routing rules

- All merchant **HTTP API** routes MUST be under **`/api/merchant`** (Spring controllers use `@RequestMapping("/api/merchant")`).
- All merchant **page** URLs MUST start with **`/merchant`**.
- Do not place merchant features under unrelated prefixes; keep merchant APIs discoverable under `/api/merchant/**`.

## Preferred patterns

**API:** `/api/merchant/...`

- `GET /api/merchant/1/customization`
- `POST /api/merchant/merchants`
- `POST /api/merchant/register`

**Pages:**

- `/merchant/login`
- `/merchant/dashboard`
- `/merchant/appointments`
- `/merchant/services`
- `/merchant/settings`

## Checklist

1. Inspect paths before coding.
2. Align new APIs with `/api/merchant/**` and pages with `/merchant/**`.
3. Align router, links, and API client calls with `SecurityConfig` rules for `/api/merchant/**`.

## Output expectations

- Call out route violations first; propose exact replacements.
