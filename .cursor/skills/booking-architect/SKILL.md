---
name: booking-architect
description: >-
  Architecture principles for the booking-core platform. Resource/slot abstraction, strategies, slot engine, state machine, and multi-tenancy.
  Use when proposing ADRs, reviewing designs, or assessing whether a change fits the rule-engine model.
---

# Architecture (booking-core)

## Source of truth

The full rule set lives in:

- `.cursor/rules/booking-rule-engine-architecture.mdc` (always apply in this repo)

## Summary (read the rule file for detail)

1. **Resource abstraction:** Prefer **Resource** + **Slot** + **ResourceType** + **metadata**, not hard-coded vertical nouns everywhere.
2. **Strategies:** Use pluggable **BookingValidationStrategy** (or equivalent) per type; register beans; avoid giant `if/else` in services.
3. **Slot engine:** Store rules and exceptions; **compute** availability; do not pre-expand all future slots in DB.
4. **State machine:** Centralize booking lifecycle transitions; no ad-hoc status writes.
5. **Multi-tenancy:** `tenant_id` on tenant-scoped data; every query/write scoped by tenant from security context.

## When reviewing a change

- Does it belong in **metadata/strategy** vs changing core booking flow?
- Does timeslot logic go through **rules + engine**?
- Are status changes going through a **single transition** path?
- Can tenant scope be bypassed?

## Outputs

- ADR-style recommendation: context, decision, consequences.
- List of affected bounded contexts (client API vs merchant vs system).
