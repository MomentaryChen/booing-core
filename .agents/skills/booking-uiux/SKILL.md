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

## Visual UI rules (engineering alignment)

- **Visual asset playbook** (hero／empty illustration、lucide 尺寸、16:9 卡片圖、背景漸層／grid）：`doc/specs/done/2026-04-09_visual-assets-dashboard-guidelines.md`（已結案歸檔）
- **Buttons**: Every button includes a meaningful icon (leading/trailing for text buttons; `aria-label` for icon-only). Match shadcn + lucide patterns in code.
- **Empty states**: Use an illustration (SVG-style), not text-only; pair with title, short description, and primary action.
- **Imagery**: No stock photos for decorative or empty-state hero art; flat SaaS illustrations (e.g. unDraw、Storyset，風格需全站一致）與自訂／系統 SVG 皆可，細節見上列規格。
- **Assets**: Prefer **SVG over PNG** for UI graphics and small decorations; use WebP when raster is required.
- **Consistency**: Same stroke weight, palette, radius, and elevation language as the rest of the app (see `.cursor/rules/ui-visual-feature-hints.mdc`).

## When implementing with engineering

- Align with **booking-client-domain**, **booking-merchant-domain**, or **booking-system-admin-domain** depending on the surface.

## Accessibility

- Use semantic HTML, focus order, and sufficient contrast; form labels for inputs.
