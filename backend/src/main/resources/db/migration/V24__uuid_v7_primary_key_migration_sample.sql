-- Intentional no-op (replaces an earlier PostgreSQL-oriented sample that used UPDATE ... FROM, random_uuid(),
-- and parallel `id_uuid` columns incompatible with this codebase and MySQL).
--
-- JPA entities already map UUID primary keys; fresh MySQL installs that rely solely on Flyway still use bigint
-- columns from V1–V23. Aligning production MySQL with UUID PKs requires a reviewed, phased migration runbook
-- (not executed here). Flyway must not run dialect-incompatible DDL in this version slot.
SELECT 1;
