---
name: booking-backend
description: >-
  Backend conventions for booking-core Java/Spring Boot service under backend/. Points to Spring patterns and security skills.
  Use when editing controllers, services, entities, or API design for this repository.
---

# Backend (booking-core)

## Stack

- Java 21, Spring Boot 3.3, Spring Data JPA, H2 in-memory for local dev (`backend/`).
- REST base: `http://localhost:28080/api` (see `README.md` and `application.yml`).

## Read these skills first

| Topic | Skill |
|--------|--------|
| Layering, DTOs, validation, exceptions, pagination | `.agents/skills/springboot-patterns/SKILL.md` |
| Security, authz, headers, rate limiting | `.agents/skills/springboot-security/SKILL.md` |
| Domain rules (resource/slot, tenant, state machine) | `.agents/skills/booking-architect/SKILL.md` |

## Repository habits

- Keep controllers thin; business logic in services.
- Respect route prefixes per product area: `/api/client/**`, `/api/merchant/**` or `/merchant/**` as applicable, `/api/system/**` for system admin (see README API overview).

## After changes

- Run `mvn test` or at least compile when touching Java.
- Remember H2 is in-memory: restart clears data in dev.
