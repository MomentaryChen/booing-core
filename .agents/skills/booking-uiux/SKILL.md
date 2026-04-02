---
name: booking-uiux
description: >-
  UI and UX guidelines for booking-core. Route namespaces, i18n, accessibility, and consistency across client, merchant, and system areas.
  Use when designing flows, copy structure, or reviewing usability of the booking platform UI.
---

# UI/UX (booking-core)

## Product surfaces

Three namespaces must stay visually and verbally consistent where they represent the same product:

- **Client** — `/client` (public storefront; mobile-friendly, minimal steps).
- **Merchant** — `/merchant` (operations-heavy; dashboards, tables, scheduling).
- **System** — `/system` (admin; RBAC-sensitive actions, clear confirmations).

## Internationalization

- Locales: **zh-TW** and **en-US** (see `frontend/src/i18n/`).
- Add or update strings in both languages when introducing user-visible text.

## UX principles

- Prefer clear primary actions and error recovery on booking flows.
- Avoid mixing internal “platform user” wording with end-customer “client” journeys.
- Respect route rules: do not design flows that require forbidden path prefixes (see domain skills).

## When implementing with engineering

- Align with **booking-client-domain**, **booking-merchant-domain**, or **booking-system-admin-domain** depending on the surface.

## Accessibility

- Use semantic HTML, focus order, and sufficient contrast; form labels for inputs.
