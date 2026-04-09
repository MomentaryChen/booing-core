-- Normalized RBAC core (Phase 2): permissions, roles, grants, per-user bindings.
-- Mirrors RolePermissionCatalog baseline; platform_user_rbac_bindings backfills from platform_users.

create table rbac_permissions (
  id bigint not null auto_increment,
  code varchar(120) not null,
  primary key (id),
  unique key uk_rbac_permissions_code (code)
) engine=InnoDB default charset=utf8mb4;

create table rbac_roles (
  id bigint not null auto_increment,
  code varchar(32) not null,
  primary key (id),
  unique key uk_rbac_roles_code (code)
) engine=InnoDB default charset=utf8mb4;

create table rbac_role_permissions (
  rbac_role_id bigint not null,
  rbac_permission_id bigint not null,
  primary key (rbac_role_id, rbac_permission_id),
  constraint fk_rrp_role foreign key (rbac_role_id) references rbac_roles (id) on delete cascade,
  constraint fk_rrp_perm foreign key (rbac_permission_id) references rbac_permissions (id) on delete cascade
) engine=InnoDB default charset=utf8mb4;

create table platform_user_rbac_bindings (
  id bigint not null auto_increment,
  platform_user_id bigint not null,
  rbac_role_id bigint not null,
  merchant_id bigint,
  status varchar(16) not null,
  primary key (id),
  key idx_pub_platform_user (platform_user_id),
  constraint fk_pub_platform_user foreign key (platform_user_id) references platform_users (id) on delete cascade,
  constraint fk_pub_rbac_role foreign key (rbac_role_id) references rbac_roles (id),
  constraint fk_pub_merchant foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

insert into rbac_permissions (code) values
  ('client.portal.access'),
  ('me.navigation.read'),
  ('merchant.portal.access'),
  ('merchant.registry.manage'),
  ('system.dashboard.read'),
  ('system.settings.write');

insert into rbac_roles (code) values ('SYSTEM_ADMIN'), ('MERCHANT'), ('SUB_MERCHANT'), ('CLIENT');

insert into rbac_role_permissions (rbac_role_id, rbac_permission_id)
select r.id, p.id from rbac_roles r cross join rbac_permissions p
where r.code = 'SYSTEM_ADMIN'
  and p.code in (
    'system.dashboard.read',
    'system.settings.write',
    'merchant.registry.manage',
    'merchant.portal.access',
    'me.navigation.read'
  );

insert into rbac_role_permissions (rbac_role_id, rbac_permission_id)
select r.id, p.id from rbac_roles r cross join rbac_permissions p
where r.code in ('MERCHANT', 'SUB_MERCHANT')
  and p.code in ('merchant.portal.access', 'me.navigation.read');

insert into rbac_role_permissions (rbac_role_id, rbac_permission_id)
select r.id, p.id from rbac_roles r cross join rbac_permissions p
where r.code = 'CLIENT'
  and p.code in ('client.portal.access', 'me.navigation.read');

insert into platform_user_rbac_bindings (platform_user_id, rbac_role_id, merchant_id, status)
select pu.id, r.id, pu.merchant_id, 'ACTIVE'
from platform_users pu
inner join rbac_roles r on r.code = pu.platform_role;
