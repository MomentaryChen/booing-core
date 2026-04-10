-- Add system users page to backend-driven navigation (idempotent).

insert into platform_pages (route_key, frontend_path, label_key, sort_order, active)
select 'nav.system.users', '/system/users', 'navSystemUsers', 15, b'1'
where not exists (
  select 1 from platform_pages where route_key = 'nav.system.users'
);

insert into role_page_grants (platform_role, page_id)
select 'SYSTEM_ADMIN', p.id
from platform_pages p
where p.route_key = 'nav.system.users'
  and not exists (
    select 1
    from role_page_grants g
    where g.platform_role = 'SYSTEM_ADMIN'
      and g.page_id = p.id
  );
