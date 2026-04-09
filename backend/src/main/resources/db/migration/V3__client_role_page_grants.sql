-- Backfill CLIENT role navigation grants for environments where platform_pages already existed
-- when PlatformNavigationInitializer ran (initializer skips when count > 0), so CLIENT grants
-- were never inserted. Idempotent: safe to re-run; only inserts missing (platform_role, page_id) pairs.

insert into role_page_grants (platform_role, page_id)
select 'CLIENT', p.id
from platform_pages p
where p.route_key in ('nav.client.todo', 'nav.store.public')
  and not exists (
    select 1
    from role_page_grants g
    where g.platform_role = 'CLIENT'
      and g.page_id = p.id
  );
