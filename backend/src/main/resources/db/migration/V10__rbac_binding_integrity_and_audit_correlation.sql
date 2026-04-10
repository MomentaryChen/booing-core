-- Enforce deterministic binding identity and richer audit metadata.

alter table platform_user_rbac_bindings
  add column merchant_scope_id bigint as (coalesce(merchant_id, 0)) stored;

delete b
from platform_user_rbac_bindings b
join (
  select
    platform_user_id,
    rbac_role_id,
    coalesce(merchant_id, 0) as merchant_scope_id,
    coalesce(max(case when status = 'ACTIVE' then id end), max(id)) as keep_id
  from platform_user_rbac_bindings
  group by platform_user_id, rbac_role_id, coalesce(merchant_id, 0)
) keepers
  on keepers.platform_user_id = b.platform_user_id
 and keepers.rbac_role_id = b.rbac_role_id
 and keepers.merchant_scope_id = coalesce(b.merchant_id, 0)
where b.id <> keepers.keep_id;

alter table platform_user_rbac_bindings
  add unique key uk_pub_user_role_scope (platform_user_id, rbac_role_id, merchant_scope_id);

alter table audit_logs
  add column correlation_id varchar(80) not null default 'legacy',
  add column before_state tinytext null,
  add column after_state tinytext null;

update audit_logs
set correlation_id = concat('legacy-', id)
where correlation_id = 'legacy';
