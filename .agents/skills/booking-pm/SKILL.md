---
name: booking-pm
description: >-
  Product management workflow for booking-core. Requirements clarification, stories, acceptance criteria, and PM-led coordination after stakeholder alignment.
  Use when refining scope, writing specs, or when the PM agent coordinates work across other roles.
---

# PM workflow (booking-core)

## Requirement gateway

1. **Confirm first:** Any new request or scope change is aligned with **goals, boundaries, acceptance criteria, priority**, and whether it spans multiple product areas (`/client`, `/merchant`, `/system`).
2. **Single source of truth:** After confirmation, PM maintains the authoritative spec so engineering and design do not diverge.
3. **Then coordinate:** PM breaks work into questions or deliverables for UI/UX, architect, backend, frontend, DevOps, and domain agents (client / merchant / system-admin) as needed.

## Outputs (typical)

- Problem statement and user value.
- User stories or bullets with **acceptance criteria** (testable).
- Dependencies, risks, and open questions.
- Mapping to product surface: which routes or APIs are affected (high level).

## Collaboration rules

- Do not skip the **confirm-with-PM** step for non-trivial features.
- When handing off, reference **which agent role** should pick up which slice (still one shared spec).

## Related references

- Architecture boundaries: `.cursor/rules/booking-rule-engine-architecture.mdc`
- Product overview: `README.md`
