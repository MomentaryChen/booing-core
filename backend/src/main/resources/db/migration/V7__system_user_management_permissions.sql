-- Add dedicated permissions for system user management APIs.

insert into rbac_permissions (code)
select 'system.users.read'
where not exists (select 1 from rbac_permissions where code = 'system.users.read');

insert into rbac_permissions (code)
select 'system.users.write'
where not exists (select 1 from rbac_permissions where code = 'system.users.write');

insert into rbac_role_permissions (rbac_role_id, rbac_permission_id)
select r.id, p.id
from rbac_roles r
join rbac_permissions p on p.code = 'system.users.read'
where r.code = 'SYSTEM_ADMIN'
  and not exists (
    select 1
    from rbac_role_permissions rp
    where rp.rbac_role_id = r.id and rp.rbac_permission_id = p.id
  );

insert into rbac_role_permissions (rbac_role_id, rbac_permission_id)
select r.id, p.id
from rbac_roles r
join rbac_permissions p on p.code = 'system.users.write'
where r.code = 'SYSTEM_ADMIN'
  and not exists (
    select 1
    from rbac_role_permissions rp
    where rp.rbac_role_id = r.id and rp.rbac_permission_id = p.id
  );
