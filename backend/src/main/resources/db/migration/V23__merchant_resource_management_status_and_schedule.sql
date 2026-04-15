alter table service_items
  add column active bit not null default b'1';

alter table resource_items
  add column maintenance bit not null default b'0',
  add column business_hours_json tinytext not null default ('[]');

create index idx_service_items_merchant_active
  on service_items (merchant_id, active);

create index idx_resource_items_merchant_active
  on resource_items (merchant_id, active);

create index idx_resource_items_merchant_maintenance
  on resource_items (merchant_id, maintenance);
