alter table resource_items
  add column assigned_staff_ids_json tinytext not null default ('[]');
